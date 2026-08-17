package net.fabricmc.fabric.mixin.networking.accessor;

import net.minecraft.server.level.ServerPlayer;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
	@Invoker
	CraftPlayer invokeGetBukkitEntity();
}
