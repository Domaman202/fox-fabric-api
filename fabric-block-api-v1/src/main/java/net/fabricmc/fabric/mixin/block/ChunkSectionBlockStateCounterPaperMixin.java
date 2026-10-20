package net.fabricmc.fabric.mixin.block;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Paper-only equivalent of {@link ChunkSectionBlockStateCounterMixin}.
 *
 * <p>Paper rewrote {@code recalcBlockCounts()} and inlined the counting logic,
 * removing the {@code LevelChunkSection$1BlockCounter} local class. There are two
 * {@code BlockState.isAir()} calls we need to neutralize:
 *
 * <ol>
 *   <li>In the {@code maybeHas(...)} lambda — bypassed by replacing the predicate
 *       passed to {@code LevelChunkSection.maybeHas(Predicate)}.</li>
 *   <li>In the hot loop — bypassed by redirecting the {@code isAir()} INVOKE itself.</li>
 * </ol>
 *
 * <p>Both redirects use {@code require = 0} so the mixin is a no-op on vanilla
 * (where neither call site exists inside {@code recalcBlockCounts}).
 *
 * <p>Mojang bug: <a href="https://bugs.mojang.com/browse/MC-232360">MC-232360</a>.
 */
@Mixin(LevelChunkSection.class)
public abstract class ChunkSectionBlockStateCounterPaperMixin {
	@Unique
	private static boolean fabric_isVanillaAir(BlockState state) {
		return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
	}

	/**
	 * Prevents {@code maybeHas((s) -> !s.isAir())} from short-circuiting when all
	 * blocks in the section are modded "air-like" (isAir() == true, but not vanilla air).
	 * Without this, the whole loop would be skipped and the modded blocks would be
	 * lost on save/reload (MC-232360).
	 */
	@Redirect(
			method = "recalcBlockCounts",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;maybeHas(Ljava/util/function/Predicate;)Z"
			),
			require = 0
	)
	private boolean fabric_maybeHasNonVanillaAir(LevelChunkSection instance, Predicate<BlockState> originalPredicate) {
		return instance.maybeHas(state -> !fabric_isVanillaAir(state));
	}

	/**
	 * Makes the counting loop count modded "air-like" blocks as non-air,
	 * so {@code nonEmptyBlockCount > 0} and the section is not treated as empty.
	 */
	@Redirect(
			method = "recalcBlockCounts",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
			),
			require = 0
	)
	private boolean fabric_isAirIgnoringModdedAir(BlockState state) {
		return fabric_isVanillaAir(state);
	}
}
