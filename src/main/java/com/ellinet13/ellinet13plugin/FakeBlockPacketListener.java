package com.ellinet13.ellinet13plugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Listens for block change packets and re-sends them with fake block data if needed
 */
public class FakeBlockPacketListener extends PacketAdapter {

    private final FakeBlockManager fakeBlockManager;
    private final JavaPlugin plugin;

    private static final Set<PacketType> BLOCK_CHANGE_PACKETS = ImmutableSet.of(
        PacketType.Play.Server.BLOCK_CHANGE
    );

    public FakeBlockPacketListener(JavaPlugin plugin, FakeBlockManager fakeBlockManager) {
        super(plugin, BLOCK_CHANGE_PACKETS);
        this.plugin = plugin;
        this.fakeBlockManager = fakeBlockManager;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChange(event);
        }
    }

    /**
     * Handle single block change packet
     */
    private void handleBlockChange(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PacketContainer packet = event.getPacket();
        BlockPosition position = packet.getBlockPositionModifier().read(0);

        if (position == null) return;

        Material fakeMaterial = fakeBlockManager.getFakeBlock(
            player.getWorld(),
            position.getX(),
            position.getY(),
            position.getZ()
        );

        if (fakeMaterial != null) {
            // Use display material (consider reveal mode)
            Material displayMaterial = fakeBlockManager.getDisplayMaterial(fakeMaterial);
            packet.getBlockData().write(0, WrappedBlockData.createData(displayMaterial));
        }
    }
}