package net.fabricmc.fabric.mixin.networking.accessor;

import io.papermc.paper.threadedregions.RegionizedWorldData;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Level.class)
public interface LevelAccessor {
	@Invoker
	RegionizedWorldData invokeGetCurrentWorldData();
}
