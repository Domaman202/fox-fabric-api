package net.fabricmc.fabric.api.message.v1;

import net.fabricmc.fabric.mixin.message.ChatDecoratorAccessor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ChatDecoratorWrapper implements ChatDecorator {
	@Override
	public Component decorate(@Nullable ServerPlayer sender, Component message) {
		return ServerMessageDecoratorEvent.EVENT.invoker().decorate(sender, message);
	}

	public CompletableFuture<Component> decorate(@Nullable ServerPlayer sender, @Nullable CommandSourceStack commandSourceStack, Component message) {
		return ((ChatDecoratorAccessor) (Object) ServerMessageDecoratorEvent.EVENT.invoker()).invokeDecorate(sender, commandSourceStack, message);
	}
}
