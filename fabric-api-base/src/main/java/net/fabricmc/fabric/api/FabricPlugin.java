package net.fabricmc.fabric.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class FabricPlugin {
	private static Plugin instance;

	public static Plugin getInstance() {
		if (instance == null)
			instance = Bukkit.getPluginManager().getPlugin("FoliaFoxHelperCWS");
		if (instance == null)
			throw new IllegalStateException("Plugin not ready!");
		return instance;
	}
}
