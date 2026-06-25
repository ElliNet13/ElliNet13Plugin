package com.ellinet13.ellinet13plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;

import java.util.Map;
import java.util.logging.Level;

/**
 * Sends fake block packets to players when they join
 */
public class FakeBlockPlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final FakeBlockManager fakeBlockManager;

    public FakeBlockPlayerListener(JavaPlugin plugin, ProtocolManager protocolManager, FakeBlockManager fakeBlockManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.fakeBlockManager = fakeBlockManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> sendFakeBlocksToPlayer(player), 20L);
    }

    /**
     * Sends all fake block packets to a player
     */
    public void sendFakeBlocksToPlayer(Player player) {
        Map<String, Material> allFakeBlocks = fakeBlockManager.getAllFakeBlocks();
        String worldName = player.getWorld().getName();

        int sent = 0;
        for (Map.Entry<String, Material> entry : allFakeBlocks.entrySet()) {
            String key = entry.getKey();
            Material fakeMaterial = entry.getValue();

            // Only send blocks from the same world
            if (!key.startsWith(worldName + ";")) {
                continue;
            }

            String[] parts = key.split(";", 4);
            if (parts.length != 4) continue;

            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                Location location = new Location(player.getWorld(), x, y, z);
                // Use display material (consider reveal mode)
                Material displayMaterial = fakeBlockManager.getDisplayMaterial(fakeMaterial);
                sendFakeBlock(player, location, displayMaterial);
                sent++;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning(() -> "Invalid block position in config: " + key);
            }
        }

        if (sent > 0) {
            int sentCount = sent;
            plugin.getLogger().info(() -> "Sent " + sentCount + " fake block(s) to player " + player.getName());
        }
    }

    private void sendFakeBlock(Player player, Location location, Material material) {
        PacketContainer packet = protocolManager.createPacket(com.comphenix.protocol.PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        packet.getBlockData().write(0, WrappedBlockData.createData(material));

        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to send fake block packet", exception);
        }
    }
}
