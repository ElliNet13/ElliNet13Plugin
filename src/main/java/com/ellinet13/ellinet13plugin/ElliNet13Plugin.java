package com.ellinet13.ellinet13plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class ElliNet13Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ElliNet13Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ElliNet13Plugin disabled!");
    }
}