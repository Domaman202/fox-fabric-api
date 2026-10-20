/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 * ... лицензия ...
 */

package net.fabricmc.fabric.mixin.event.lifecycle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(
			method = "placeNewPlayer",
			at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundUpdateRecipesPacket")
	)
	private void hookOnPlayerConnect(Connection connection, ServerPlayer player, CommonListenerCookie arg, CallbackInfo ci) {
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.invoker().onSyncDataPackContents(player, true);
	}

	@Inject(
			method = "reloadTagData",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/ClientboundUpdateTagsPacket;<init>(Ljava/util/Map;)V")
	)
	private void hookOnDataPacksReloaded(CallbackInfo ci) {
		for (ServerPlayer player : ((PlayerList) (Object) this).getPlayers()) {
			ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.invoker().onSyncDataPackContents(player, false);
		}
	}
}
