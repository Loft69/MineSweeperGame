package com.axon.findgame.listener;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProtectionListener implements Listener {

    private final FindGame plugin;

    public ProtectionListener(FindGame plugin) {
        this.plugin = plugin;
    }

    private Set<Material> getBannedMaterials() {
        Set<Material> banned = new HashSet<>();
        List<String> list = plugin.getConfig().getStringList("banned-blocks");
        for (String name : list) {
            try { banned.add(Material.valueOf(name.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        return banned;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        if (getBannedMaterials().contains(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("banned-block"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item != null && getBannedMaterials().contains(item.getType())) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("banned-block"));
            player.getInventory().remove(item.getType());
        }

        if (event.getClickedBlock() != null && getBannedMaterials().contains(event.getClickedBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        if (getBannedMaterials().contains(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        Set<Material> banned = getBannedMaterials();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current != null && banned.contains(current.getType()) || banned.contains(cursor.getType())) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("banned-block"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        if (getBannedMaterials().contains(event.getItemDrop().getItemStack().getType())) event.setCancelled(true);
    }
}