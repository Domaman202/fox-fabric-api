package net.fabricmc.fabric.mixin.event.lifecycle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

@Mixin(targets = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder", remap = false)
public abstract class NewChunkHolderMixin {
	@Shadow
	private FullChunkStatus currentFullChunkStatus;

	@Shadow
	public abstract ChunkAccess getCurrentChunk();   // ← возвращает ChunkAccess, не LevelChunk

	@Inject(
			method = "updateCurrentState",
			at = @At("HEAD"),
			remap = false,
			require = 0
	)
	private void onFullChunkStatusChange(FullChunkStatus to, CallbackInfo ci) {  // ← CallbackInfo (void-метод)
		FullChunkStatus from = this.currentFullChunkStatus;
		if (from == to) {
			return;
		}

		ChunkAccess chunk = this.getCurrentChunk();
		if (!(chunk instanceof LevelChunk levelChunk)) {
			return;
		}

		ServerLevel level = (ServerLevel) levelChunk.getLevel();
		if (level == null) {
			return;
		}

		// 1. FULL_CHUNK_STATUS_CHANGE
		ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.invoker()
				.onFullChunkStatusChange(level, levelChunk, from, to);

		// 2. CHUNK_UNLOAD: чанк перестаёт быть FULL — семантически совпадает с vanilla
		if (to == FullChunkStatus.INACCESSIBLE) {
			ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, levelChunk);
		}
	}
}
