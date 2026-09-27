package com.axon.findgame.listener;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.GameManager;
import com.axon.findgame.model.GameSession;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class GameListener implements Listener {

    private final FindGame plugin;

    public GameListener(FindGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!gm.isDetector(item)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);
        gm.checkBlock(player, block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        if (gm.isBombBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            gm.triggerBombExplosion(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gm = plugin.getGameManager();

        if (!gm.isPlayerInGame(player.getUniqueId())) return;

        event.getDrops().removeIf(gm::isDetector);

        event.deathMessage(null);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (!gm.isPlayerInGame(player.getUniqueId())) return;

        GameSession session = gm.getCurrentSession();
        if (session != null && session.isActive()) {
            event.setRespawnLocation(session.getSpawnLocation());
        } else if (session != null) {
            var playerData = session.getPlayerData(player.getUniqueId());
            if (playerData != null && playerData.getPreviousLocation() != null) {
                event.setRespawnLocation(playerData.getPreviousLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (gm.isGameActive() && gm.isPlayerInGame(player.getUniqueId())) {
            long remaining = gm.getSessionPlayers().stream().filter(p -> !p.getUniqueId().equals(player.getUniqueId())).count();

            if (remaining == 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (gm.isGameActive()) gm.endGame(false);
                }, 20L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (!gm.isGameActive()) return;

        if (gm.isPlayerInGame(player.getUniqueId())) {
            GameSession session = gm.getCurrentSession();
            if (session != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(session.getSpawnLocation());

                    boolean hasDetector = false;
                    for (var item : player.getInventory().getContents()) {
                        if (gm.isDetector(item)) {
                            hasDetector = true;
                            break;
                        }
                    }
                    if (!hasDetector) {
                        ItemStack det = gm.createDetectorItem();
                        det.setAmount(5);
                        player.getInventory().addItem(det);
                    }
                }, 5L);
            }
        }
    }
}