package net.fabricmc.fabric.mixin.networking;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.fabricmc.fabric.impl.networking.EntityTrackersMapWrapper;
import net.fabricmc.fabric.mixin.networking.accessor.EntityTrackerAccessor;

import net.fabricmc.fabric.mixin.networking.accessor.LevelAccessor;

import net.minecraft.server.level.ChunkMap;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
	@Shadow
	@Final
	ServerLevel level;

	private Int2ObjectMap<EntityTrackerAccessor> fox$getEntityTrackers() {
		ReferenceList<Entity> trackedEntities = ((LevelAccessor) this.level).invokeGetCurrentWorldData().trackerEntities;
		return new EntityTrackersMapWrapper(trackedEntities);
	}
}
