package com.axon.findgame.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameSession {

    private final World arenaWorld;
    private final Location spawnLocation;
    private final Location bombLocation;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final long startTime;
    private boolean active = true;

    private PlayerData.HologramEntry globalClosestHologram = null;

    public GameSession(World arenaWorld, Location spawnLocation, Location bombLocation) {
        this.arenaWorld = arenaWorld;
        this.spawnLocation = spawnLocation;
        this.bombLocation = bombLocation;
        this.startTime = System.currentTimeMillis();
    }

    public World getArenaWorld() { return arenaWorld; }
    public Location getSpawnLocation() { return spawnLocation; }
    public Location getBombLocation() { return bombLocation; }
    public Map<UUID, PlayerData> getPlayers() { return players; }
    public long getStartTime() { return startTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public PlayerData getPlayerData(UUID uuid) { return players.get(uuid); }
    public void addPlayer(UUID uuid, PlayerData data) { players.put(uuid, data); }

    public PlayerData.HologramEntry getGlobalClosestHologram() { return globalClosestHologram; }
    public void setGlobalClosestHologram(PlayerData.HologramEntry entry) { this.globalClosestHologram = entry; }

    public double getDistanceToBomb(Location loc) {
        return loc.toVector().distance(bombLocation.toVector());
    }

    public void clearAllHolograms() {
        for (PlayerData pd : players.values()) {
            pd.clearHolograms();
        }
        globalClosestHologram = null;
    }

    public String getDirectionToBomb(Location from) {
        double dx = bombLocation.getBlockX() - from.getBlockX();
        double dz = bombLocation.getBlockZ() - from.getBlockZ();

        StringBuilder dir = new StringBuilder();

        if (Math.abs(dx) < 5 && Math.abs(dz) < 5) return "⬤ Совсем рядом!";

        if (dz < -10) dir.append("↑С");
        else if (dz > 10) dir.append("↓Ю");

        if (dx < -10) dir.append("←З");
        else if (dx > 10) dir.append("→В");

        if (dir.isEmpty()) return "⬤ Очень близко!";

        return dir.toString();
    }

    public String getBombDirectionSign(Location from) {
        double dx = bombLocation.getBlockX() - from.getBlockX();
        double dz = bombLocation.getBlockZ() - from.getBlockZ();

        String xSign = dx >= 0 ? "+" : "-";
        String zSign = dz >= 0 ? "+" : "-";

        return "X: " + xSign + " Z: " + zSign;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public String getFormattedElapsedTime() {
        long elapsed = getElapsedSeconds();
        long hours = elapsed / 3600;
        long minutes = (elapsed % 3600) / 60;
        long seconds = elapsed % 60;

        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }
}
