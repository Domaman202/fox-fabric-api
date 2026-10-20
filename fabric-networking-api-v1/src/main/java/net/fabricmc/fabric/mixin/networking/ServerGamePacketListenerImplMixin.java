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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import net.fabricmc.fabric.impl.networking.PacketListenerExtensions;
import net.fabricmc.fabric.impl.networking.UntrackedPacketListener;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.fabricmc.fabric.impl.networking.server.ServerPlayNetworkAddon;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 999)
abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl implements PacketListenerExtensions {
	@Unique
	private ServerPlayNetworkAddon addon;

	ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie arg) {
		super(server, connection, arg);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void initAddon(CallbackInfo ci) {
		this.addon = new ServerPlayNetworkAddon((ServerGamePacketListenerImpl) (Object) this, connection, server);

		if (!(this instanceof UntrackedPacketListener)) {
			this.addon.lateInit();
		}
	}

	@Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
	private void handleCustomPayloadReceivedAsync(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
		try {
			if (getAddon().handle(packet.payload())) {
				ci.cancel();
			}
		} catch (RunningOnDifferentThreadException e) {
			this.server.packetProcessor().scheduleIfPossible(this, packet);
			ci.cancel();
		}
	}

	/**
	 * Runs once {@code handleConfigurationAcknowledged} has completely finished.
	 *
	 * <p>At this point:
	 * <ul>
	 *   <li>vanilla Fabric: {@code setupInboundProtocol} has been executed, the config
	 *       listener is now installed as the current inbound listener;</li>
	 *   <li>Paper: additionally, {@code PlayerConnectionReconfigureEvent.callEvent()}
	 *       has already fired, so plugins observed the reconfiguration <em>before</em>
	 *       we kick off Fabric's config phase.</li>
	 * </ul>
	 *
	 * <p>Either way {@link Connection#getPacketListener()} returns the freshly created
	 * {@link ServerConfigurationPacketListenerImpl} and we can start the Fabric config
	 * phase on it — no need to bridge values between multiple hooks.
	 */
	@Inject(method = "handleConfigurationAcknowledged", at = @At("RETURN"))
	private void onConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket packet, CallbackInfo ci) {
		PacketListener current = this.connection.getPacketListener();
		if (!(current instanceof ServerConfigurationPacketListenerImpl configPacketListener)) {
			return;
		}

		ServerNetworkingImpl.getAddon(configPacketListener).setReconfiguring();

		if (addon.requestedReconfigure()) {
			configPacketListener.startConfiguration();
		}
	}

	@Override
	public ServerPlayNetworkAddon getAddon() {
		return this.addon;
	}
}
