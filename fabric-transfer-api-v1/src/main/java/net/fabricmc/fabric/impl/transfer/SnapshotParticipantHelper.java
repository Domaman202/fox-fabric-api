package net.fabricmc.fabric.impl.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import net.fabricmc.fabric.mixin.transfer.ChiseledBookShelfBlockEntityAccessor;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

public class SnapshotParticipantHelper extends SnapshotParticipant {
	ChiseledBookShelfBlockEntity outer;

	public SnapshotParticipantHelper(ChiseledBookShelfBlockEntity outer) {
		this.outer = outer;
	}

	@Override
	protected Integer createSnapshot() {
		return outer.getLastInteractedSlot();
	}

	@Override
	protected void readSnapshot(Object snapshot) {
		((ChiseledBookShelfBlockEntityAccessor) outer).setLastInteractedSlot((Integer) snapshot);
	}

	@Override
	protected void onFinalCommit() {
		((ChiseledBookShelfBlockEntityAccessor) outer).invokeUpdateState(outer.getLastInteractedSlot());
	}
}
