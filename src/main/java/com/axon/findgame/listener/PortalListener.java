package com.axon.findgame.listener;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PortalListener implements Listener {

    private final FindGame plugin;

    public PortalListener(FindGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        event.setCancelled(true);
        player.sendMessage(plugin.msg("portal-blocked"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameActive() || !gm.isPlayerInGame(player.getUniqueId())) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || cause == PlayerTeleportEvent.TeleportCause.END_PORTAL || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("portal-blocked"));
        }
    }
}