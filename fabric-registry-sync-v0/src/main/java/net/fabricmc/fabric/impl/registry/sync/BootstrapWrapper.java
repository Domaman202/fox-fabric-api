package net.fabricmc.fabric.impl.registry.sync;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

public class BootstrapWrapper {
	public static Operation<Void> INVOKE_BOOTSTRAP = null;
	public static Runnable BOOTSTRAP_ARGUMENT = null;
}
