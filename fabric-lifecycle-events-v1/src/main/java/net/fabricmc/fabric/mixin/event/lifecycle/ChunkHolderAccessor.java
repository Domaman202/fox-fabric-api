package net.fabricmc.fabric.mixin.event.lifecycle;

import net.minecraft.server.level.ChunkHolder;

import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkHolder.class)
public interface ChunkHolderAccessor {
	@Invoker
	LevelChunk invokeGetFullChunkNowUnchecked();
}
