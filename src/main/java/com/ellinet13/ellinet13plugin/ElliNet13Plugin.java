package com.ellinet13.ellinet13plugin;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ElliNet13Plugin extends JavaPlugin {

    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        getCommand("fakeblocks").setExecutor(new FakeBlocksCommand(this, protocolManager));
        getLogger().info("ElliNet13Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ElliNet13Plugin disabled!");
    }
}