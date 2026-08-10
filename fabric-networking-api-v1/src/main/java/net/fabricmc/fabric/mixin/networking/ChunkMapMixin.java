package net.fabricmc.fabric.mixin.networking;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.fabricmc.fabric.mixin.networking.accessor.ChunkMapAccessor;

import net.fabricmc.fabric.mixin.networking.accessor.EntityTrackerAccessor;

import net.fabricmc.fabric.mixin.networking.accessor.LevelAccessor;

import net.minecraft.server.level.ChunkMap;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Map;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
	@Shadow
	@Final
	public ServerLevel level;

	private Int2ObjectMap<EntityTrackerAccessor> fox$getEntityTrackers() {
		ReferenceList<Entity> trackedEntities = ((LevelAccessor) this.level).invokeGetCurrentWorldData().trackerEntities;
		return new Int2ObjectMap<>() {
			EntityTrackerAccessor defaultValue = null;

			@Override
			public int size() {
				return trackedEntities.size();
			}

			@Override
			public void defaultReturnValue(EntityTrackerAccessor rv) {
				defaultValue = rv;
			}

			@Override
			public EntityTrackerAccessor defaultReturnValue() {
				return defaultValue;
			}

			@Override
			public ObjectSet<Entry<EntityTrackerAccessor>> int2ObjectEntrySet() {
				return null;
			}

			@Override
			public IntSet keySet() {
				return null;
			}

			@Override
			public ObjectCollection<EntityTrackerAccessor> values() {
				return null;
			}

			@Override
			public boolean containsKey(int key) {
				return false;
			}

			@Override
			public EntityTrackerAccessor get(int key) {
				return Arrays.stream(trackedEntities.getRawDataUnchecked()).filter(it -> it.getId() == key).findFirst().map(value -> (EntityTrackerAccessor) ((EntityTrackerEntity) value).moonrise$getTrackedEntity()).orElseGet(() -> defaultValue);
			}

			@Override
			public boolean isEmpty() {
				return trackedEntities.size() == 0;
			}

			@Override
			public boolean containsValue(Object value) {
				return Arrays.stream(trackedEntities.getRawDataUnchecked()).anyMatch(it -> ((EntityTrackerEntity) it).moonrise$getTrackedEntity() == value);
			}

			@Override
			public void putAll(Map<? extends Integer, ? extends EntityTrackerAccessor> m) {
				for (EntityTrackerAccessor entity : m.values()) {
					trackedEntities.add(entity.getEntity());
				}
			}
		};
	}
}
