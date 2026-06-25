package com.ellinet13.ellinet13plugin;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class FakeBlocksCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;

    public FakeBlocksCommand(JavaPlugin plugin, ProtocolManager protocolManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
    }

    private Component color(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null || !(sender instanceof Player player)) {
            if (sender != null) {
                sender.sendMessage(color("&cOnly players can run this command."));
            }
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(color("&cUsage: /fakeblocks <from> <to>"));
            return true;
        }

        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(worldEditPlugin instanceof WorldEditPlugin worldEdit)) {
            player.sendMessage(color("&cWorldEdit is not loaded. Install WorldEdit to use this command."));
            return true;
        }

        LocalSession session = worldEdit.getSession(player);
        Region selection;
        try {
            selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(color("&cYou must create a complete WorldEdit selection first."));
            return true;
        }

        Material fromMaterial = Material.matchMaterial(args[0]);
        Material toMaterial = Material.matchMaterial(args[1]);

        if (fromMaterial == null || toMaterial == null) {
            player.sendMessage(color("&cInvalid material names. Example: /fakeblocks stone diamond_block"));
            return true;
        }

        BlockVector3 minimum = selection.getMinimumPoint();
        BlockVector3 maximum = selection.getMaximumPoint();

        int changed = 0;
        int limit = 10000;
        for (int x = minimum.x(); x <= maximum.x(); x++) {
            for (int y = minimum.y(); y <= maximum.y(); y++) {
                for (int z = minimum.z(); z <= maximum.z(); z++) {
                    if (changed >= limit) {
                        player.sendMessage(color("&eSelection too large. Only the first " + limit + " matching blocks were faked."));
                        x = maximum.x() + 1;
                        y = maximum.y() + 1;
                        break;
                    }

                    Location location = new Location(player.getWorld(), x, y, z);
                    if (location.getBlock().getType() != fromMaterial) {
                        continue;
                    }

                    sendFakeBlock(player, location, toMaterial);
                    changed++;
                }
            }
        }

        if (changed == 0) {
            player.sendMessage(color("&eNo blocks matching " + fromMaterial.name() + " were found in your selection."));
        } else {
            player.sendMessage(color("&aSent fake block updates for " + changed + " block(s) in your selection."));
        }

        return true;
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
