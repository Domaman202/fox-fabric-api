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

package net.fabricmc.fabric.mixin.entity.event.effect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.impl.entity.event.effect.MobEffectUtil;

import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	private LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@WrapMethod(method = "canBeAffected")
	private boolean allowAddEffect(MobEffectInstance effectInstance, Operation<Boolean> original) {
		if (this.isClient()) {
			return original.call(effectInstance);
		}

		if (!ServerMobEffectEvents.ALLOW_ADD.invoker().allowAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext())) {
			return false;
		}

		return original.call(effectInstance);
	}

	@Inject(
			method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;Z)Z",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
			)
	)
	private void beforeAddEffect(MobEffectInstance newEffect, Entity source, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause, boolean fireEvent, CallbackInfoReturnable<Boolean> cir) {
		if (this.isClient()) {
			return;
		}

		ServerMobEffectEvents.BEFORE_ADD.invoker().beforeAdd(newEffect, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "forceAddEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
					shift = At.Shift.AFTER
			)
	)
	private void beforeForceAddEffect(MobEffectInstance effectInstance, Entity entity, CallbackInfo ci) {
		if (this.isClient()) {
			return;
		}

		ServerMobEffectEvents.BEFORE_ADD.invoker().beforeAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "onEffectAdded",
			at = @At("RETURN")
	)
	private void afterAddEffect(MobEffectInstance effectInstance, Entity entity, CallbackInfo ci) {
		if (this.isClient()) {
			return;
		}

		ServerMobEffectEvents.AFTER_ADD.invoker().afterAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext());
	}

	@WrapOperation(
			method = "removeAllEffects(Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"
			)
	)
	private void allowRemoveAllEffects(
			LivingEntity instance,
			Collection<MobEffectInstance> effects,
			Operation<Void> original
	) {
		if (this.isClient()) {
			original.call(instance, effects);
			return;
		}

		List<MobEffectInstance> toActuallyRemove = new ArrayList<>(effects.size());

		for (MobEffectInstance effect : effects) {
			boolean cannotRemove = !ServerMobEffectEvents.ALLOW_EARLY_REMOVE.invoker()
					.allowEarlyRemove(effect, this.self(), MobEffectUtil.getCommandContext());

			if (cannotRemove) {
				// эффект уже выдернут из activeEffects через iterator.remove()
				// выше в цикле — возвращаем его обратно в карту.
				instance.getActiveEffectsMap().put(effect.getEffect(), effect);
			} else {
				toActuallyRemove.add(effect);
			}
		}

		original.call(instance, toActuallyRemove);
	}

	@WrapMethod(method = "removeEffect")
	private boolean allowRemoveEffect(Holder<MobEffect> holder, Operation<Boolean> original) {
		if (this.isClient()) {
			return original.call(holder);
		}

		MobEffectInstance effectInstance = this.self().getEffect(holder);

		if (effectInstance == null) {
			return original.call(holder);
		}

		boolean cannotRemove = !ServerMobEffectEvents.ALLOW_EARLY_REMOVE.invoker()
				.allowEarlyRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());

		if (cannotRemove) {
			return false;
		}

		return original.call(holder);
	}

	@Inject(
			method = "removeEffect",
			at = @At("HEAD")
	)
	private void beforeRemoveEffect(Holder<MobEffect> holder, CallbackInfoReturnable<Boolean> cir) {
		if (this.isClient()) {
			return;
		}

		MobEffectInstance effectInstance = this.self().getEffect(holder);

		if (effectInstance == null) {
			return;
		}

		ServerMobEffectEvents.BEFORE_REMOVE.invoker()
				.beforeRemove(effectInstance, (LivingEntity) (Object) this, MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "tickEffects",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Iterator;remove()V"
			)
	)
	private void beforeExpireRemoveEffect(CallbackInfo ci, @Local(name = "effect") MobEffectInstance effect) {
		if (this.isClient()) {
			return;
		}

		ServerMobEffectEvents.BEFORE_REMOVE.invoker()
				.beforeRemove(effect, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "removeAllEffects(Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"
			)
	)
	private void beforeRemoveAllEffects(
			EntityPotionEffectEvent.Cause cause,
			CallbackInfoReturnable<Boolean> cir,
			@Local List<MobEffectInstance> toRemove
	) {
		for (MobEffectInstance effectInstance : toRemove) {
			ServerMobEffectEvents.BEFORE_REMOVE.invoker()
					.beforeRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());
		}
	}

	@Inject(
			method = "onEffectsRemoved",
			at = @At("RETURN")
	)
	private void afterRemoveEffect(Collection<MobEffectInstance> collection, CallbackInfo ci) {
		if (this.isClient()) {
			return;
		}

		for (MobEffectInstance effectInstance : collection) {
			ServerMobEffectEvents.AFTER_REMOVE.invoker()
					.afterRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());
		}
	}

	@Unique
	private boolean isClient() {
		return this.level().isClientSide();
	}

	@Unique
	private LivingEntity self() {
		return (LivingEntity) (Object) this;
	}
}
