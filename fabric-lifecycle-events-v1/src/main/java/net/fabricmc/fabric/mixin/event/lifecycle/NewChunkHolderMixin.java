package net.fabricmc.fabric.mixin.event.lifecycle;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NewChunkHolder.class)
public abstract class NewChunkHolderMixin {
	@Shadow
	private FullChunkStatus currentFullChunkStatus;

	@Shadow
	private ChunkAccess currentChunk;

	@Final
	@Shadow
	public ServerLevel world;

	@Inject(method = "updateCurrentState", at = @At("HEAD"))
	private void onStatusChange(FullChunkStatus newStatus, CallbackInfo ci) {
		FullChunkStatus oldStatus = this.currentFullChunkStatus;
		if (oldStatus != newStatus) {
			ChunkAccess chunk = this.currentChunk;
			ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.invoker().onChunkLevelTypeChange(this.world, (LevelChunk) chunk, oldStatus, newStatus);
		}
	}

	@WrapOperation(method = "unloadStage2", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
	private void onChunkUnload(ServerLevel instance, LevelChunk levelChunk, Operation<Void> original) {
		ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(instance, levelChunk);
		original.call(instance, levelChunk);
	}
}
