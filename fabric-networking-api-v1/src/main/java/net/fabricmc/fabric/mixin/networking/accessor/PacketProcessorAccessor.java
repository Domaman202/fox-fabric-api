package net.fabricmc.fabric.mixin.networking.accessor;

import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;

import net.minecraft.network.protocol.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PacketProcessor.class)
public interface PacketProcessorAccessor {
	@Invoker
	<T extends PacketListener> boolean invokeScheduleIfPossible(T packetListener, Packet<T> packet);
}
