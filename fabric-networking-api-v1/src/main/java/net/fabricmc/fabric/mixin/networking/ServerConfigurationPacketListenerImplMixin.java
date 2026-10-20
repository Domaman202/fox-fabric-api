/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.networking;

import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationPacketListenerImpl;
import net.fabricmc.fabric.impl.networking.FabricRegistryFriendlyByteBuf;
import net.fabricmc.fabric.impl.networking.PacketListenerExtensions;
import net.fabricmc.fabric.impl.networking.server.ServerConfigurationNetworkAddon;

// We want to apply a bit earlier than other mods which may not use us in order to prevent refCount issues
@Mixin(value = ServerConfigurationPacketListenerImpl.class, priority = 900)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImpl implements PacketListenerExtensions, FabricServerConfigurationPacketListenerImpl {
	@Shadow
	@Nullable
	private ConfigurationTask currentTask;

	@Shadow
	protected abstract void finishCurrentTask(ConfigurationTask.Type key);

	@Shadow
	@Final
	private Queue<ConfigurationTask> configurationTasks;

	@Shadow
	public abstract boolean isAcceptingMessages();

	@Shadow
	public abstract void startConfiguration();

	@Unique
	private ServerConfigurationNetworkAddon addon;

	@Unique
	private boolean sentConfiguration;

	@Unique
	private boolean earlyTaskExecution;

	/**
	 * Detected once at class-load. On Paper, {@code startConfiguration()} fires a
	 * synchronous Bukkit event ({@code PlayerConnectionReconfigureEvent} or equivalent),
	 * which Paper requires to run on the main server thread.
	 */
	@Unique
	private static final boolean FABRIC_IS_PAPER = fabric_detectPaper();

	@Unique
	private static boolean fabric_detectPaper() {
		try {
			Class.forName("io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public ServerConfigurationPacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie arg) {
		super(server, connection, arg);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void initAddon(CallbackInfo ci) {
		this.addon = new ServerConfigurationNetworkAddon((ServerConfigurationPacketListenerImpl) (Object) this, this.server);
		// A bit of a hack but it allows the field above to be set in case someone registers handlers during INIT event which refers to said field
		this.addon.lateInit();
	}

	@Inject(method = "startConfiguration", at = @At("HEAD"), cancellable = true)
	private void onClientReady(CallbackInfo ci) {
		// Paper-only fix: startConfiguration() fires a synchronous Bukkit event, which
		// must run on the main thread. Fabric's networking stack calls this method
		// from Netty (inside handleCustomPayload → ServerConfigurationNetworkAddon.receiveRegistration).
		// Defer the whole call to the main thread to avoid
		// "IllegalStateException: ... may only be triggered synchronously.".
		//
		// On vanilla Fabric, keep the original synchronous behaviour: the event doesn't
		// exist there, so there's no reason to change the threading model.
		if (FABRIC_IS_PAPER && !this.server.isSameThread()) {
			ci.cancel();
			this.server.execute(this::startConfiguration);
			return;
		}

		// Send the initial channel registration packet
		if (this.addon.startConfiguration()) {
			if (currentTask != null) {
				throw new IllegalStateException("A task is already running: " + currentTask.type().id());
			}

			ci.cancel();
			return;
		}

		// Ready to start sending packets
		if (!sentConfiguration) {
			this.addon.preConfiguration();
			sentConfiguration = true;
			earlyTaskExecution = true;
		}

		// Run the early tasks
		if (earlyTaskExecution) {
			if (pollEarlyTasks()) {
				ci.cancel();
				return;
			} else {
				earlyTaskExecution = false;
			}
		}

		// All early tasks should have been completed
		if (currentTask != null || !configurationTasks.isEmpty()) {
			throw new IllegalStateException("All early tasks should have been completed, current: " + currentTask + ", queued: " + configurationTasks.size());
		}

		// Run the vanilla tasks.
		this.addon.configuration();
	}

	@Unique
	private boolean pollEarlyTasks() {
		if (!earlyTaskExecution) {
			throw new IllegalStateException("Early task execution has finished");
		}

		if (this.currentTask != null) {
			throw new IllegalStateException("Task " + this.currentTask.type().id() + " has not finished yet");
		}

		if (!this.isAcceptingMessages()) {
			return false;
		}

		final ConfigurationTask task = this.configurationTasks.poll();

		if (task != null) {
			this.currentTask = task;
			task.start(this::send);
			return true;
		}

		return false;
	}

	@Override
	public ServerConfigurationNetworkAddon getAddon() {
		return addon;
	}

	@Override
	public void addTask(ConfigurationTask task) {
		configurationTasks.add(task);
	}

	@Override
	public void completeTask(ConfigurationTask.Type key) {
		if (!earlyTaskExecution) {
			finishCurrentTask(key);
			return;
		}

		final ConfigurationTask.Type currentKey = this.currentTask != null ? this.currentTask.type() : null;

		if (!key.equals(currentKey)) {
			throw new IllegalStateException("Unexpected request for task finish, current task: " + currentKey + ", requested: " + key);
		}

		this.currentTask = null;
		startConfiguration();
	}

	@WrapOperation(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;decorator(Lnet/minecraft/core/RegistryAccess;)Ljava/util/function/Function;"))
	private Function<ByteBuf, RegistryFriendlyByteBuf> bindChannelInfo(RegistryAccess registryManager, Operation<Function<ByteBuf, RegistryFriendlyByteBuf>> original) {
		return original.call(registryManager).andThen(registryByteBuf -> {
			FabricRegistryFriendlyByteBuf fabricRegistryFriendlyByteBuf = (FabricRegistryFriendlyByteBuf) registryByteBuf;
			fabricRegistryFriendlyByteBuf.fabric_setSendableConfigurationChannels(Set.copyOf(addon.getSendableChannels()));
			return registryByteBuf;
		});
	}
}
