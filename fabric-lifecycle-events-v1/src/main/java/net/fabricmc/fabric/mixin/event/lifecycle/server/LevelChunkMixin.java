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

package net.fabricmc.fabric.mixin.event.lifecycle.server;

import java.util.Map;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.block.entity.BlockEntityType;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This is a server only mixin for good reason:
 * Since all block entity tracking is now on the world chunk, we inject into WorldChunk.
 * In order to prevent client logic from being loaded due to the mixin, we have a mixin for the client and this one for the server.
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
	@Shadow
	public abstract Level getLevel();

	@ModifyExpressionValue(
			method = "setBlockEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;canvas$setBlockEntityInBuckets(IIILnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/world/level/block/entity/BlockEntity;")
	)
	private BlockEntity onLoadBlockEntity(BlockEntity removedBlockEntity, BlockEntity blockEntity) {
		// Only fire the load event if the block entity has actually changed
		if (blockEntity != null && blockEntity != removedBlockEntity) {
			if (this.getLevel() instanceof ServerLevel) {
				ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(blockEntity, (ServerLevel) this.getLevel());
			}
		}

		return removedBlockEntity;
	}

	@Inject(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V", shift = At.Shift.AFTER))
	private void onRemoveBlockEntity(BlockEntity blockEntity, CallbackInfo info, @Local(ordinal = 1) BlockEntity removedBlockEntity) {
		if (this.getLevel() instanceof ServerLevel) {
			ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removedBlockEntity, (ServerLevel) this.getLevel());
		}
	}


	@Inject(
			method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;canvas$removeFromBuckets(III)Lnet/minecraft/world/level/block/entity/BlockEntity;", shift = At.Shift.AFTER)
	)
	private void onRemoveBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType, CallbackInfoReturnable<BlockEntity> cir, @Local(ordinal = 0) BlockEntity blockEntity) {
		if (blockEntity != null && this.getLevel() instanceof ServerLevel serverLevel) {
			ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(blockEntity, serverLevel);
		}
	}

	@Inject(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"))
	private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci, @Local @Nullable BlockEntity removed) {
		if (removed != null && this.getLevel() instanceof ServerLevel) {
			ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removed, (ServerLevel) this.getLevel());
		}
	}
}
