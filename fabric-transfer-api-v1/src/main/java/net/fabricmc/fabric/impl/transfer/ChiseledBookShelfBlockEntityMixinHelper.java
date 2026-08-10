package net.fabricmc.fabric.impl.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import net.fabricmc.fabric.mixin.transfer.ChiseledBookShelfBlockEntityAccessor;

import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

public class ChiseledBookShelfBlockEntityMixinHelper extends SnapshotParticipant<Integer> {
	private final ChiseledBookShelfBlockEntity entity;

	public ChiseledBookShelfBlockEntityMixinHelper(ChiseledBookShelfBlockEntity entity) {
		this.entity = entity;
	}

	@Override
	protected Integer createSnapshot() {
		return entity.getLastInteractedSlot();
	}

	@Override
	protected void readSnapshot(Integer snapshot) {
		((ChiseledBookShelfBlockEntityAccessor) entity).setLastInteractedSlot(snapshot);
	}

	@Override
	protected void onFinalCommit() {
		((ChiseledBookShelfBlockEntityAccessor) entity).invokeUpdateState(entity.getLastInteractedSlot());
	}
}
