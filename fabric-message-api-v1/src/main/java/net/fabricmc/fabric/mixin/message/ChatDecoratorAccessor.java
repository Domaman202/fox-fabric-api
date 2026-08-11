package net.fabricmc.fabric.mixin.message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatDecorator;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatDecorator.class)
public interface ChatDecoratorAccessor {
	@Invoker
	CompletableFuture<Component> invokeDecorate(@Nullable ServerPlayer sender, @Nullable CommandSourceStack commandSourceStack, Component message);
}
