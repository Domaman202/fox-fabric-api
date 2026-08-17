package net.fabricmc.fabric.impl.networking;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.fabricmc.fabric.mixin.networking.accessor.EntityTrackerAccessor;

import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.Map;

public class EntityTrackersMapWrapper implements Int2ObjectMap<EntityTrackerAccessor> {
	private final ReferenceList<Entity> trackedEntities;
	private EntityTrackerAccessor defaultValue = null;

	public EntityTrackersMapWrapper(ReferenceList<Entity> trackedEntities) {
		this.trackedEntities = trackedEntities;
	}

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
}
