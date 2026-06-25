package com.ellinet13.ellinet13plugin;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ElliNet13Plugin extends JavaPlugin {

    private ProtocolManager protocolManager;
    private FakeBlockManager fakeBlockManager;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();

        // Initialize FakeBlockManager
        fakeBlockManager = new FakeBlockManager(this);

        // Register command
        getCommand("fakeblocks").setExecutor(new FakeBlocksCommand(this, protocolManager, fakeBlockManager));

        // Register packet listener
        FakeBlockPacketListener packetListener = new FakeBlockPacketListener(this, fakeBlockManager);
        protocolManager.addPacketListener(packetListener);

        // Register player join listener
        getServer().getPluginManager().registerEvents(
            new FakeBlockPlayerListener(this, protocolManager, fakeBlockManager),
            this
        );

        getLogger().info("ElliNet13Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ElliNet13Plugin disabled!");
    }
}