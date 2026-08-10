package net.fabricmc.fabric.mixin.transfer;

import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChiseledBookShelfBlockEntity.class)
public interface ChiseledBookShelfBlockEntityAccessor {
	@Accessor
	void setLastInteractedSlot(int value);

	@Invoker
	void invokeUpdateState(int i);
}
