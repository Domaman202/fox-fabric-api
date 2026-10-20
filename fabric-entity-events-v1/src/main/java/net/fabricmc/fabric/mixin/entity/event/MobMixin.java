package net.fabricmc.fabric.mixin.entity.event;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

@Mixin(Mob.class)
public class MobMixin {
	@ModifyArg(
			method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$CancellingAfterConversion;Lorg/bukkit/event/entity/EntityTransformEvent$TransformReason;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Lnet/minecraft/world/entity/Mob;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Z"
			),
			index = 0
	)
	private Entity afterEntityConverted(Entity converted, @Local(argsOnly = true) ConversionParams conversionContext) {
		ServerLivingEntityEvents.MOB_CONVERSION.invoker().onConversion((Mob) (Object) this, (Mob) converted, conversionContext);
		return converted;
	}
}
