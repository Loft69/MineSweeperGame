package com.axon.findgame.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    @Getter
    private final UUID playerId;
    @Getter
    private final Location previousLocation;
    @Getter
    private final List<HologramEntry> holograms = new ArrayList<>();
    @Getter
    private int checksUsed = 0;
    @Setter
    private ItemStack checkerItem;

    public PlayerData(UUID playerId, Location previousLocation) {
        this.playerId = playerId;
        this.previousLocation = previousLocation;
    }

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

    public ItemStack getChekerItem() {
        return this.checkerItem;
    }

    public void clearCheckerItem() {
        this.checkerItem = null;
    }

    public record HologramEntry(Location blockLocation, double distanceToBomb, TextDisplay mainDisplay, TextDisplay closestDisplay) {}
}