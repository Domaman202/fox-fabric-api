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

package net.fabricmc.fabric.mixin.entity.event;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;

import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(Entity.class)
abstract class EntityMixin {
	@Shadow
	private Level level;

	@ModifyVariable(method = "teleportAsync(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/lang/Float;Ljava/lang/Float;Lnet/minecraft/world/phys/Vec3;Lorg/bukkit/event/player/PlayerTeleportEvent$TeleportCause;JLjava/util/function/Consumer;)Z", at = @At("HEAD"))
	private Consumer<Entity> afterWorldChanged(Consumer<Entity> teleportComplete) {
		ServerLevel originLevel = (ServerLevel) this.level;
		return (entity) -> {
			teleportComplete.accept(entity);
			ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.invoker().afterChangeWorld((Entity) (Object) this, entity, originLevel, (ServerLevel) entity.level());
		};
	}
}
