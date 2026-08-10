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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
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

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Shadow protected abstract void onEffectsRemoved(Collection<MobEffectInstance> effects);

	private LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@WrapMethod(method = "canBeAffected")
	private boolean allowAddEffect(MobEffectInstance effectInstance, Operation<Boolean> original) {
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
	private void beforeAddEffect(MobEffectInstance effectInstance, Entity entity, EntityPotionEffectEvent.Cause cause, boolean fireEvent, CallbackInfoReturnable<Boolean> cir) {
		ServerMobEffectEvents.BEFORE_ADD.invoker().beforeAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext());
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
		ServerMobEffectEvents.BEFORE_ADD.invoker().beforeAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "onEffectAdded",
			at = @At("RETURN")
	)
	private void afterAddEffect(MobEffectInstance effectInstance, Entity entity, CallbackInfo ci) {
		ServerMobEffectEvents.AFTER_ADD.invoker().afterAdd(effectInstance, this.self(), MobEffectUtil.getCommandContext());
	}

	/**
	 * @author DomamaN202
	 * @reason Z
	 */
	@Overwrite
	public boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			return false;
		}
		if (self.getActiveEffectsMap().isEmpty()) {
			return false;
		}

		List<MobEffectInstance> toRemove = new LinkedList<>();
		Iterator<MobEffectInstance> iterator = self.getActiveEffectsMap().values().iterator();

		while (iterator.hasNext()) {
			MobEffectInstance effect = iterator.next();
			EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(self, effect, null, cause, EntityPotionEffectEvent.Action.CLEARED);

			// Проверяем кастомное событие раннего удаления
			boolean cannotRemove = !ServerMobEffectEvents.ALLOW_EARLY_REMOVE.invoker()
					.allowEarlyRemove(effect, self, MobEffectUtil.getCommandContext());

			if (!event.isCancelled() && !cannotRemove) {
				iterator.remove();
				toRemove.add(effect);
			}
		}

		this.onEffectsRemoved(toRemove);
		return !toRemove.isEmpty();
	}

	@WrapMethod(method = "removeEffect(Lnet/minecraft/core/Holder;Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z")
	private boolean allowRemoveEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause, Operation<Boolean> original) {
		MobEffectInstance effectInstance = this.self().getEffect(effect);

		if (effectInstance == null) {
			return original.call(effect, cause);
		}

		boolean cannotRemove = !ServerMobEffectEvents.ALLOW_EARLY_REMOVE.invoker()
				.allowEarlyRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());

		if (cannotRemove) {
			return false;
		}

		return original.call(effect, cause);
	}

	@Inject(
			method = "removeEffect(Lnet/minecraft/core/Holder;Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z",
			at = @At("HEAD")
	)
	private void beforeRemoveEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause, CallbackInfoReturnable<Boolean> cir) {
		MobEffectInstance effectInstance = this.self().getEffect(effect);

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
	private void beforeExpireRemoveEffect(CallbackInfo ci, @Local MobEffectInstance effectInstance) {
		ServerMobEffectEvents.BEFORE_REMOVE.invoker()
				.beforeRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "removeAllEffects(Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/Iterator;remove()V"
			)
	)
	private void beforeRemoveEffect(EntityPotionEffectEvent.Cause cause, CallbackInfoReturnable<Boolean> cir, @Local MobEffectInstance effect) {
		ServerMobEffectEvents.BEFORE_REMOVE.invoker().beforeRemove(effect, this.self(), MobEffectUtil.getCommandContext());
	}

	@Inject(
			method = "onEffectsRemoved",
			at = @At("RETURN")
	)
	private void afterRemoveEffect(Collection<MobEffectInstance> collection, CallbackInfo ci) {
		for (MobEffectInstance effectInstance : collection) {
			ServerMobEffectEvents.AFTER_REMOVE.invoker()
					.afterRemove(effectInstance, this.self(), MobEffectUtil.getCommandContext());
		}
	}

	@Unique
	private LivingEntity self() {
		return (LivingEntity) (Object) this;
	}
}
