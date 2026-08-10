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

package net.fabricmc.fabric.mixin.item;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.data.util.Conversions;

import net.minecraft.core.Registry;

import net.minecraft.server.packs.resources.Resource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import net.fabricmc.fabric.impl.item.EnchantmentUtil;

@Mixin(RegistryDataLoader.class)
abstract class RegistryDataLoaderMixin {
	@WrapOperation(
			method = "loadElementFromResource",
			at = @At(
					value = "INVOKE",
					target = "Lio/papermc/paper/registry/PaperRegistryListenerManager;registerWithListeners(Lnet/minecraft/core/Registry;Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;Lio/papermc/paper/registry/data/util/Conversions;)V"
			)
	)
	@SuppressWarnings("unchecked")
	private static <M> void enchantmentKey(
			PaperRegistryListenerManager instance,
			Registry<M> registry,
			ResourceKey<M> objectKey,
			M object,
			RegistrationInfo registryEntryInfo,
			Conversions conversions,
			Operation<Void> original,
			@Local Resource resource
	) {
		if (object instanceof Enchantment enchantment) {
			Enchantment modified = EnchantmentUtil.modify((ResourceKey<Enchantment>) objectKey, enchantment, EnchantmentUtil.determineSource(resource));

			if (modified != null) {
				object = (M) modified;

				// Clear the knownPackInfo to force the server to sync the data pack to the client
				registryEntryInfo = new RegistrationInfo(Optional.empty(), registryEntryInfo.lifecycle());
			}
		}

		original.call(instance, registry, objectKey, object, registryEntryInfo, conversions);
	}
}
