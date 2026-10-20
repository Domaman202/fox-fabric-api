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

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.impl.item.ItemComponentTooltipProviderRegistryImpl;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.fabricmc.fabric.impl.item.VanillaTooltipProviderOrder;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements FabricItemStack {
	@Shadow
	public abstract Item getItem();

	@Shadow
	public abstract void shrink(int i);

	@WrapOperation(
			method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;Z)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;Z)V"
			)
	)
	private void hookDamage(
			ItemStack instance,
			int amount,
			ServerLevel serverLevel,
			LivingEntity player,
			Consumer<Item> consumer,
			boolean force,
			Operation<Void> original,
			@Local(argsOnly = true) LivingEntity entity,
			@Local(argsOnly = true) EquipmentSlot slot
	) {
		CustomDamageHandler handler = ((ItemExtensions) getItem()).fabric_getCustomDamageHandler();

    /*
        Оригинальная логика из Fabric: пропускаем вызов handler'а в креативе,
        потому что ванильный метод (после 24w21a) всё равно отбросит изменение
        прочности у творческого игрока — вычислять заведомо выбрасываемое
        значение не имеет смысла (и защищает от порчи стака модами).

        Исключение: force = true. В Paper `processDurabilityChange` проверяет
        `player.hasInfiniteMaterials() && !force`, то есть при force=true
        ваниль сама применит изменение — значит, handler тоже должен отработать.
    */

		if (handler != null && (force || !entity.hasInfiniteMaterials())) {
			MutableBoolean mut = new MutableBoolean(false);
			amount = handler.hurtAndBreak((ItemStack) (Object) this, amount, entity, slot, () -> {
				mut.setTrue();
				this.shrink(1);
				consumer.accept(this.getItem());
			});

			if (mut.booleanValue()) return;
		}

		original.call(instance, amount, serverLevel, player, consumer, force);
	}

	@ModifyArg(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
	private DataComponentType<?> preAppendComponentTooltip(
			DataComponentType<?> componentType,
			@Local(argsOnly = true) Item.TooltipContext context,
			@Local(argsOnly = true) TooltipDisplay displayComponent,
			@Local(argsOnly = true) TooltipFlag type,
			@Local(argsOnly = true) Consumer<Component> componentConsumer,
			@Share("index") LocalIntRef index
	) {
		preAppendTooltip(componentType, context, displayComponent, componentConsumer, type, index);
		return componentType;
	}

	@ModifyArg(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipDisplay;shows(Lnet/minecraft/core/component/DataComponentType;)Z"))
	private DataComponentType<?> preShouldDisplay(
			DataComponentType<?> componentType,
			@Local(argsOnly = true) Item.TooltipContext context,
			@Local(argsOnly = true) TooltipDisplay displayComponent,
			@Local(argsOnly = true) TooltipFlag type,
			@Local(argsOnly = true) Consumer<Component> componentConsumer,
			@Share("index") LocalIntRef index
	) {
		preAppendTooltip(componentType, context, displayComponent, componentConsumer, type, index);
		return componentType;
	}

	@Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;)V"))
	private void preAttributeModifiers(
			Item.TooltipContext context,
			TooltipDisplay displayComponent,
			@Nullable Player player,
			TooltipFlag type,
			Consumer<Component> componentConsumer,
			CallbackInfo ci,
			@Share("index") LocalIntRef index
	) {
		// Special case: attribute modifiers are extracted into a separate method
		preAppendTooltip(DataComponents.ATTRIBUTE_MODIFIERS, context, displayComponent, componentConsumer, type, index);
	}

	@Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/Identifier;"))
	private void postTooltipsAdvanced(
			Item.TooltipContext context,
			TooltipDisplay displayComponent,
			@Nullable Player player,
			TooltipFlag type,
			Consumer<Component> componentConsumer,
			CallbackInfo ci,
			@Share("index") LocalIntRef index
	) {
		preAppendTooltip(null, context, displayComponent, componentConsumer, type, index);
	}

	@ModifyExpressionValue(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z"))
	private boolean postTooltipsNonAdvanced(
			boolean isAdvanced,
			Item.TooltipContext context,
			TooltipDisplay displayComponent,
			@Nullable Player player,
			TooltipFlag type,
			Consumer<Component> componentConsumer,
			@Share("index") LocalIntRef index
	) {
		if (!isAdvanced) {
			preAppendTooltip(null, context, displayComponent, componentConsumer, type, index);
		}

		return isAdvanced;
	}

	@Unique
	private void preAppendTooltip(
			@Nullable DataComponentType<?> componentType,
			Item.TooltipContext context,
			TooltipDisplay displayComponent,
			Consumer<Component> componentConsumer,
			TooltipFlag tooltipFlag,
			LocalIntRef index
	) {
		if (!ItemComponentTooltipProviderRegistryImpl.hasModdedEntries()) {
			return;
		}

		if (index.get() == 0) {
			ItemComponentTooltipProviderRegistryImpl.onFirst((ItemStack) (Object) this, context, displayComponent, componentConsumer, tooltipFlag);
		}

		List<DataComponentType<?>> vanillaOrder = VanillaTooltipProviderOrder.getVanillaOrder();

		if (index.get() > vanillaOrder.size()) {
			return;
		}

		// Find out which vanilla tooltip providers we may have skipped over and run their anchored providers first

		while (true) {
			if (index.get() > 0) {
				DataComponentType<?> prevComponentInOrder = vanillaOrder.get(index.get() - 1);
				HashSet<DataComponentType<?>> cycleDetector = new HashSet<>();
				cycleDetector.add(prevComponentInOrder);
				ItemComponentTooltipProviderRegistryImpl.onAfter((ItemStack) (Object) this, prevComponentInOrder, context, displayComponent, componentConsumer, tooltipFlag, cycleDetector);
			}

			if (index.get() == vanillaOrder.size()) {
				index.set(index.get() + 1);
				break;
			}

			DataComponentType<?> componentInOrder = vanillaOrder.get(index.get());
			HashSet<DataComponentType<?>> cycleDetector = new HashSet<>();
			cycleDetector.add(componentInOrder);
			ItemComponentTooltipProviderRegistryImpl.onBefore((ItemStack) (Object) this, componentInOrder, context, displayComponent, componentConsumer, tooltipFlag, cycleDetector);
			index.set(index.get() + 1);

			if (componentInOrder == componentType) {
				break;
			}
		}

		if (componentType == null) {
			ItemComponentTooltipProviderRegistryImpl.onLast((ItemStack) (Object) this, context, displayComponent, componentConsumer, tooltipFlag);
		}
	}
}
