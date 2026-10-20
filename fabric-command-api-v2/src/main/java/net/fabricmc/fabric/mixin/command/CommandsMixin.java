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

package net.fabricmc.fabric.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

@Mixin(Commands.class)
public abstract class CommandsMixin {
	@Shadow
	@Final
	private CommandDispatcher<CommandSourceStack> dispatcher;

	/**
	 * Paper добавил post-processing (bukkit permissions + minecraft: псевдонимы)
	 * между регистрацией vanilla-команд и setConsumer. Инжект в setConsumer
	 * пропустил бы это post-processing для Fabric-команд. Поэтому инжектимся
	 * в первый getRoot() — это точка непосредственно перед post-processing,
	 * но уже после всех register().
	 */
	@Inject(
			method = "<init>(Lnet/minecraft/commands/Commands$CommandSelection;Lnet/minecraft/commands/CommandBuildContext;Z)V",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/brigadier/CommandDispatcher;getRoot()Lcom/mojang/brigadier/tree/RootCommandNode;",
					ordinal = 0,
					shift = At.Shift.BEFORE
			)
	)
	private void fabric_addCommands(
			Commands.CommandSelection selection,
			CommandBuildContext buildContext,
			boolean modern,
			CallbackInfo ci
	) {
		CommandRegistrationCallback.EVENT.invoker().register(this.dispatcher, buildContext, selection);
	}
}
