package com.axon.findgame.model;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private final Location previousLocation;
    private final List<HologramEntry> holograms = new ArrayList<>();
    private int checksUsed = 0;

    public PlayerData(UUID playerId, Location previousLocation) {
        this.playerId = playerId;
        this.previousLocation = previousLocation;
    }

    public UUID getPlayerId() { return playerId; }
    public Location getPreviousLocation() { return previousLocation; }
    public List<HologramEntry> getHolograms() { return holograms; }
    public int getChecksUsed() { return checksUsed; }
    public void incrementChecksUsed() { checksUsed++; }

    public void addHologram(HologramEntry entry) {
        holograms.add(entry);
    }

    public void clearHolograms() {
        for (HologramEntry entry : holograms) {
            if (entry.mainDisplay() != null && !entry.mainDisplay().isDead()) entry.mainDisplay().remove();
            if (entry.closestDisplay() != null && !entry.closestDisplay().isDead()) entry.closestDisplay().remove();
        }
        holograms.clear();
    }

    public record HologramEntry(Location blockLocation, double distanceToBomb, TextDisplay mainDisplay, TextDisplay closestDisplay) {}
}