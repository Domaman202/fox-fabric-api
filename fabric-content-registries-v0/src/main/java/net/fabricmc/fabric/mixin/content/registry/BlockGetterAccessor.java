package net.fabricmc.fabric.mixin.content.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockGetter.class)
public interface BlockGetterAccessor {
	@Invoker
	@Nullable BlockState invokeGetBlockStateIfLoaded(BlockPos var1);
}
