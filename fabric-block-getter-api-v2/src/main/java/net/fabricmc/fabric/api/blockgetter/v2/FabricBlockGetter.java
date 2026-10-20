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

package net.fabricmc.fabric.api.blockgetter.v2;

import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * General-purpose Fabric-provided extensions for {@link BlockGetter} subclasses.
 *
 * <p>These extensions were designed primarily for use by methods invoked during chunk building, but
 * they can also be used in other contexts.
 *
 * <p>Note: This interface is automatically implemented on all {@link BlockGetter} instances via Mixin and interface injection.
 */
public interface FabricBlockGetter {
	/**
	 * Checks whether biome retrieval is supported. The returned value will not change between
	 * multiple calls of this method. See {@link #getBiomeFabric(BlockPos)} for more information.
	 *
	 * @return whether biome retrieval is supported
	 * @see #getBiomeFabric(BlockPos)
	 */
	default boolean hasBiomes() {
		return false;
	}

	/**
	 * Retrieves the biome at the given position if biome retrieval is supported. If
	 * {@link #hasBiomes()} returns {@code true}, this method will always return a non-null
	 * {@link Holder} whose {@link Holder#value() value} is non-null. If
	 * {@link #hasBiomes()} returns {@code false}, this method will always return {@code null}.
	 *
	 * <p>Prefer using {@link LevelReader#getBiome(BlockPos)} instead of this method if this instance
	 * is known to implement {@link LevelReader}.
	 *
	 * @implNote Implementations which do not return null are encouraged to use the plains biome as
	 * the default value, for example when the biome at the given position is unknown.
	 *
	 * @param pos the position for which to retrieve the biome
	 * @return the biome, or null if biome retrieval is not supported
	 * @see #hasBiomes()
	 */
	@UnknownNullability
	default Holder<Biome> getBiomeFabric(BlockPos pos) {
		return null;
	}
}
