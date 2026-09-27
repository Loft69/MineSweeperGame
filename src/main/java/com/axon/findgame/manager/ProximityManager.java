package com.axon.findgame.manager;

import com.axon.findgame.FindGame;
import com.axon.findgame.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProximityManager {

    private final FindGame plugin;

    public ProximityManager(FindGame plugin) {
        this.plugin = plugin;
    }

    public boolean areAllPlayersCloseEnough(GameSession session) {
        double radius = plugin.getConfig().getDouble("proximity-radius", 15);
        double radiusSq = radius * radius;

        List<Player> onlinePlayers = getOnlinePlayersInSession(session);
        if (onlinePlayers.size() <= 1) return true;

        for (int i = 0; i < onlinePlayers.size(); i++) {
            for (int j = i + 1; j < onlinePlayers.size(); j++) {
                Player a = onlinePlayers.get(i);
                Player b = onlinePlayers.get(j);

                if (!a.getWorld().equals(b.getWorld())) return false;
                if (a.getLocation().distanceSquared(b.getLocation()) > radiusSq) return false;
            }
        }

        return true;
    }

    public List<Player> getOnlinePlayersInSession(GameSession session) {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : session.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) result.add(player);
        }
        return result;
    }
}