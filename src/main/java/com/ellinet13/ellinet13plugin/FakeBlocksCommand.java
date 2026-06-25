package com.ellinet13.ellinet13plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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

public class FakeBlocksCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final FakeBlockManager fakeBlockManager;

    // Permissions
    private static final String PERM_REPLACE = "ellinet13plugin.fakeblocks.replace";
    private static final String PERM_FILL = "ellinet13plugin.fakeblocks.fill";
    private static final String PERM_CLEAR = "ellinet13plugin.fakeblocks.clear";
    private static final String PERM_LIST = "ellinet13plugin.fakeblocks.list";
    private static final String PERM_REVEAL = "ellinet13plugin.fakeblocks.reveal";
    private static final List<String> MATERIAL_NAMES = Arrays.stream(Material.values())
        .filter(Material::isBlock)
        .map(material -> material.name().toLowerCase())
        .toList();

    public FakeBlocksCommand(JavaPlugin plugin, ProtocolManager protocolManager, FakeBlockManager fakeBlockManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.fakeBlockManager = fakeBlockManager;
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

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "fill" -> {
                if (!player.hasPermission(PERM_FILL)) {
                    player.sendMessage(color("&cYou don't have permission to use this command."));
                    return true;
                }
                handleFill(player, args);
            }
            case "clear" -> {
                if (!player.hasPermission(PERM_CLEAR)) {
                    player.sendMessage(color("&cYou don't have permission to use this command."));
                    return true;
                }
                handleClear(player);
            }
            case "list" -> {
                if (!player.hasPermission(PERM_LIST)) {
                    player.sendMessage(color("&cYou don't have permission to use this command."));
                    return true;
                }
                handleList(player);
            }
            case "reveal" -> {
                if (!player.hasPermission(PERM_REVEAL)) {
                    player.sendMessage(color("&cYou don't have permission to use this command."));
                    return true;
                }
                handleReveal(player);
            }
            case "replace" -> {
                if (!player.hasPermission(PERM_REPLACE)) {
                    player.sendMessage(color("&cYou don't have permission to use this command."));
                    return true;
                }
                handleReplace(player, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                player.sendMessage(color("&cUnknown subcommand. Use /fakeblocks for help."));
                sendUsage(player);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(getAllowedSubcommands(player), args[0]);
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("replace") && player.hasPermission(PERM_REPLACE) && (args.length == 2 || args.length == 3)) {
            return filterPrefix(MATERIAL_NAMES, args[args.length - 1]);
        }

        if (subcommand.equals("fill") && player.hasPermission(PERM_FILL) && args.length == 2) {
            return filterPrefix(MATERIAL_NAMES, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> getAllowedSubcommands(Player player) {
        return List.of(
            new Subcommand("replace", PERM_REPLACE),
            new Subcommand("fill", PERM_FILL),
            new Subcommand("clear", PERM_CLEAR),
            new Subcommand("list", PERM_LIST),
            new Subcommand("reveal", PERM_REVEAL)
        ).stream()
            .filter(subcommand -> player.hasPermission(subcommand.permission()))
            .map(Subcommand::name)
            .toList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String normalizedPrefix = prefix.toLowerCase();
        return options.stream()
            .filter(option -> option.startsWith(normalizedPrefix))
            .toList();
    }

    private record Subcommand(String name, String permission) {
    }

    private void sendUsage(Player player) {
        player.sendMessage(color("&cUsage: /fakeblocks <subcommand>"));
        player.sendMessage(color("&c  replace <from> <to> - Replace specific blocks in selection"));
        player.sendMessage(color("&c  fill <to> - Replace ALL blocks in selection"));
        player.sendMessage(color("&c  clear - Clear fake blocks in selection"));
        player.sendMessage(color("&c  list - Show fake block count"));
        player.sendMessage(color("&c  reveal - Toggle reveal mode (green wool)"));
    }

    /**
     * Handle /fakeblocks replace <from> <to> - replaces specific blocks
     */
    private void handleReplace(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(color("&cUsage: /fakeblocks replace <from> <to>"));
            return;
        }

        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(worldEditPlugin instanceof WorldEditPlugin worldEdit)) {
            player.sendMessage(color("&cWorldEdit is not loaded. Install WorldEdit to use this command."));
            return;
        }

        Region selection = getSelection(player, worldEdit);
        if (selection == null) return;

        Material fromMaterial = Material.matchMaterial(args[0]);
        Material toMaterial = Material.matchMaterial(args[1]);

        if (fromMaterial == null || toMaterial == null) {
            player.sendMessage(color("&cInvalid material names. Example: /fakeblocks replace stone diamond_block"));
            return;
        }

        if (fromMaterial.isAir() && !fakeBlockManager.isAirReplacementAllowed()) {
            player.sendMessage(color("&cReplacing air with fake blocks is disabled in fakeblocks.yml."));
            return;
        }

        int changed = processSelection(player, selection, player.getWorld(), (x, y, z) -> {
            Location location = new Location(player.getWorld(), x, y, z);
            if (location.getBlock().getType() == fromMaterial) {
                if (!fakeBlockManager.setFakeBlock(player.getWorld(), x, y, z, toMaterial)) {
                    return false;
                }
                sendFakeBlock(player, location, toMaterial);
                return true;
            }
            return false;
        }, 10000);

        if (changed == 0) {
            player.sendMessage(color("&eNo blocks matching " + fromMaterial.name() + " were found in your selection."));
        } else {
            player.sendMessage(color("&aSent fake block updates for " + changed + " block(s) in your selection."));
        }
    }

    /**
     * Handle /fakeblocks fill <to> - replaces all blocks in selection
     */
    private void handleFill(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(color("&cUsage: /fakeblocks fill <to>"));
            return;
        }

        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(worldEditPlugin instanceof WorldEditPlugin worldEdit)) {
            player.sendMessage(color("&cWorldEdit is not loaded. Install WorldEdit to use this command."));
            return;
        }

        Region selection = getSelection(player, worldEdit);
        if (selection == null) return;

        Material toMaterial = Material.matchMaterial(args[1]);

        if (toMaterial == null) {
            player.sendMessage(color("&cInvalid material name. Example: /fakeblocks fill diamond_block"));
            return;
        }

        int changed = processSelection(player, selection, player.getWorld(), (x, y, z) -> {
            Location location = new Location(player.getWorld(), x, y, z);
            if (!fakeBlockManager.setFakeBlock(player.getWorld(), x, y, z, toMaterial)) {
                return false;
            }
            sendFakeBlock(player, location, toMaterial);
            return true;
        }, 10000);

        if (changed == 0) {
            if (fakeBlockManager.isAirReplacementAllowed()) {
                player.sendMessage(color("&eNo blocks found in your selection."));
            } else {
                player.sendMessage(color("&eNo non-air blocks found in your selection."));
            }
        } else {
            player.sendMessage(color("&aSent fake block updates for " + changed + " block(s) in your selection (all blocks replaced)."));
        }
    }

    /**
     * Handle /fakeblocks clear - clears fake blocks in selection only
     */
    private void handleClear(Player player) {
        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(worldEditPlugin instanceof WorldEditPlugin worldEdit)) {
            player.sendMessage(color("&cWorldEdit is not loaded. Install WorldEdit to use this command."));
            return;
        }

        Region selection = getSelection(player, worldEdit);
        if (selection == null) return;

        BlockVector3 minimum = selection.getMinimumPoint();
        BlockVector3 maximum = selection.getMaximumPoint();

        // Clear fake blocks in the region and get the removed ones
        var clearedBlocks = fakeBlockManager.clearFakeBlocksInRegion(
            player.getWorld(),
            minimum.x(), maximum.x(),
            minimum.y(), maximum.y(),
            minimum.z(), maximum.z()
        );

        // Send the real blocks back to the player
        for (Map.Entry<String, Material> entry : clearedBlocks.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(";", 4);
            if (parts.length != 4) continue;

            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                Location location = new Location(player.getWorld(), x, y, z);
                Material realMaterial = location.getBlock().getType();

                sendFakeBlock(player, location, realMaterial);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning(() -> "Invalid block position: " + key);
            }
        }

        if (clearedBlocks.isEmpty()) {
            player.sendMessage(color("&eNo fake blocks found in your selection."));
        } else {
            player.sendMessage(color("&aCleared " + clearedBlocks.size() + " fake block(s) and restored real blocks."));
        }
    }

    /**
     * Handle /fakeblocks list - shows fake block count
     */
    private void handleList(Player player) {
        int count = fakeBlockManager.getFakeBlockCount();
        boolean revealMode = fakeBlockManager.isRevealMode();
        String revealStatus = revealMode ? " &c(REVEAL MODE ON)" : "";
        player.sendMessage(color("&aTotal fake blocks: " + count + revealStatus));
    }

    /**
     * Handle /fakeblocks reveal - toggles reveal mode
     */
    private void handleReveal(Player player) {
        boolean newState = fakeBlockManager.toggleRevealMode();
        if (newState) {
            player.sendMessage(color("&aReveal mode enabled - fake blocks will appear as green wool."));
            // Refresh all fake blocks for this player
            refreshFakeBlocksForPlayer(player);
        } else {
            player.sendMessage(color("&aReveal mode disabled - fake blocks will appear as their real fake material."));
            // Refresh all fake blocks for this player
            refreshFakeBlocksForPlayer(player);
        }
    }

    /**
     * Refreshes all fake block packets for a player
     */
    private void refreshFakeBlocksForPlayer(Player player) {
        Map<String, Material> allFakeBlocks = fakeBlockManager.getAllFakeBlocks();
        String worldName = player.getWorld().getName();

        int sent = 0;
        for (Map.Entry<String, Material> entry : allFakeBlocks.entrySet()) {
            String key = entry.getKey();

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

                if (!fakeBlockManager.canDisplayFakeBlockAt(player.getWorld(), x, y, z)) {
                    continue;
                }

                Location location = new Location(player.getWorld(), x, y, z);
                Material displayMaterial = fakeBlockManager.getDisplayMaterial(entry.getValue());
                sendFakeBlock(player, location, displayMaterial);
                sent++;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning(() -> "Invalid block position in config: " + key);
            }
        }

        if (sent > 0) {
            int sentCount = sent;
            plugin.getLogger().info(() -> "Refreshed " + sentCount + " fake block(s) for player " + player.getName());
        }
    }

    @FunctionalInterface
    private interface BlockProcessor {
        boolean process(int x, int y, int z);
    }

    private int processSelection(Player player, Region selection, @SuppressWarnings("unused") org.bukkit.World world, BlockProcessor processor, int limit) {
        BlockVector3 minimum = selection.getMinimumPoint();
        BlockVector3 maximum = selection.getMaximumPoint();

        int changed = 0;
        for (int x = minimum.x(); x <= maximum.x(); x++) {
            for (int y = minimum.y(); y <= maximum.y(); y++) {
                for (int z = minimum.z(); z <= maximum.z(); z++) {
                    if (changed >= limit) {
                        player.sendMessage(color("&eSelection too large. Only the first " + limit + " blocks were processed."));
                        return changed;
                    }

                    if (processor.process(x, y, z)) {
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    private Region getSelection(Player player, WorldEditPlugin worldEdit) {
        LocalSession session = worldEdit.getSession(player);
        try {
            return session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            player.sendMessage(color("&cYou must create a complete WorldEdit selection first."));
            return null;
        }
    }

    public void sendFakeBlock(Player player, Location location, Material material) {
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
