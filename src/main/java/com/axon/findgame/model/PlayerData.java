package com.axon.findgame.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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

    public void removeHologram(HologramEntry entry) {
        holograms.remove(entry);
    }

    public void clearHolograms() {
        for (HologramEntry entry : holograms) entry.remove();
        holograms.clear();
    }

    public ItemStack getChekerItem() {
        return this.checkerItem;
    }

    public void clearCheckerItem() {
        this.checkerItem = null;
    }

    public record HologramEntry(Location blockLocation, double distanceToBomb, TextDisplay mainDisplay, TextDisplay closestDisplay) {
        public void remove() {
            if (mainDisplay != null && !mainDisplay.isDead()) mainDisplay.remove();
            if (closestDisplay != null && !closestDisplay.isDead()) closestDisplay.remove();
        }

        public boolean isClosest() {
            return closestDisplay != null && !closestDisplay.isDead() && closestDisplay.isEmpty();
        }

        public void laterRemove(JavaPlugin plugin, int seconds) {
            Bukkit.getScheduler().runTaskLater(plugin, task -> remove(), 20L * seconds);
        }


    }
}