package com.yourserver.warpcount;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WarpCountGUI implements CommandExecutor, Listener {

    private static final int MAX_DISTANCE = 500;
    private static final String GUI_TITLE =
            ChatColor.DARK_GRAY + "Warp Player Counts";

    private final WarpCountPlugin plugin;
    private final Essentials essentials;

    private final Map<UUID, Inventory> openGuis = new LinkedHashMap<>();

    private BukkitTask refreshTask;

    public WarpCountGUI(WarpCountPlugin plugin, Essentials essentials) {
        this.plugin = plugin;
        this.essentials = essentials;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("warpcount.use")) {
            player.sendMessage(
                    ChatColor.RED + "You don't have permission to do that."
            );
            return true;
        }

        Inventory inventory = buildInventory();

        openGuis.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);

        return true;
    }

    private List<String> getWarpNames() {
        try {
            return new ArrayList<>(essentials.getWarps().getList());
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Unable to load Essentials warps: "
                            + exception.getMessage()
            );
            return new ArrayList<>();
        }
    }

    private Inventory buildInventory() {
        List<String> warps = getWarpNames();

        int rows = Math.max(
                1,
                (int) Math.ceil(warps.size() / 9.0D)
        );

        int slots = Math.min(54, rows * 9);

        WarpCountHolder holder = new WarpCountHolder();

        Inventory inventory = Bukkit.createInventory(
                holder,
                slots,
                GUI_TITLE
        );

        holder.setInventory(inventory);
        fillInventory(inventory, warps);

        return inventory;
    }

    private void fillInventory(
            Inventory inventory,
            List<String> warps
    ) {
        inventory.clear();

        int slot = 0;

        for (String warpName : warps) {
            if (slot >= inventory.getSize()) {
                break;
            }

            inventory.setItem(
                    slot,
                    buildWarpItem(warpName)
            );

            slot++;
        }
    }

    private ItemStack buildWarpItem(String warpName) {
        int playerCount = countPlayersNearWarp(warpName);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    ChatColor.AQUA + warpName
            );

            List<String> lore = new ArrayList<>();

            lore.add(
                    ChatColor.GRAY
                            + String.valueOf(playerCount)
                            + (playerCount == 1
                            ? " player"
                            : " players")
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private int countPlayersNearWarp(String warpName) {
        Location warpLocation;

        try {
            warpLocation = essentials.getWarps().getWarp(warpName);
        } catch (Exception exception) {
            return 0;
        }

        if (warpLocation == null || warpLocation.getWorld() == null) {
            return 0;
        }

        World warpWorld = warpLocation.getWorld();
        int count = 0;

        for (Player player : warpWorld.getPlayers()) {
            Location playerLocation = player.getLocation();

            boolean withinX =
                    Math.abs(
                            playerLocation.getX()
                                    - warpLocation.getX()
                    ) <= MAX_DISTANCE;

            boolean withinY =
                    Math.abs(
                            playerLocation.getY()
                                    - warpLocation.getY()
                    ) <= MAX_DISTANCE;

            boolean withinZ =
                    Math.abs(
                            playerLocation.getZ()
                                    - warpLocation.getZ()
                    ) <= MAX_DISTANCE;

            if (withinX && withinY && withinZ) {
                count++;
            }
        }

        return count;
    }

    public void startRefreshTask() {
        stopRefreshTask();

        refreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshAllOpenGuis,
                40L,
                40L
        );
    }

    public void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void refreshAllOpenGuis() {
        if (openGuis.isEmpty()) {
            return;
        }

        List<String> warps = getWarpNames();

        for (Map.Entry<UUID, Inventory> entry : openGuis.entrySet()) {
            UUID playerId = entry.getKey();
            Inventory inventory = entry.getValue();

            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline()) {
                continue;
            }

            if (!(player.getOpenInventory()
                    .getTopInventory()
                    .getHolder() instanceof WarpCountHolder)) {
                continue;
            }

            fillInventory(inventory, warps);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof WarpCountHolder)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof WarpCountHolder)) {
            return;
        }

        openGuis.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    public void closeAllGuis() {
        for (UUID playerId : new ArrayList<>(openGuis.keySet())) {
            Player player = Bukkit.getPlayer(playerId);

            if (player != null && player.isOnline()) {
                if (player.getOpenInventory()
                        .getTopInventory()
                        .getHolder() instanceof WarpCountHolder) {
                    player.closeInventory();
                }
            }
        }

        openGuis.clear();
    }

    private static final class WarpCountHolder implements InventoryHolder {

        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
