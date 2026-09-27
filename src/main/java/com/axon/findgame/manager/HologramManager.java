package com.axon.findgame.manager;

import com.axon.findgame.FindGame;
import com.axon.findgame.model.GameSession;
import com.axon.findgame.model.PlayerData;
import com.axon.findgame.model.PlayerData.HologramEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;

public class HologramManager {

    private final FindGame plugin;
    private final MiniMessage mm;

    public HologramManager(FindGame plugin) {
        this.plugin = plugin;
        this.mm = plugin.mm();
    }

    public HologramEntry createHintHologram(GameSession session, PlayerData playerData, Location blockLocation) {
        double distance = session.getDistanceToBomb(blockLocation);
        String hintText = getHintText(distance);

        String direction = session.getDirectionToBomb(blockLocation);
        boolean dirEnabled = plugin.getConfig().getBoolean("direction-enabled", true);

        StringBuilder fullText = new StringBuilder(hintText);
        if (dirEnabled) fullText.append("\n<gray>Направление: <white>").append(direction).append("</white></gray>");

        World world = blockLocation.getWorld();
        Location holoLoc = blockLocation.clone().add(0.5, 1.5, 0.5);

        TextDisplay mainDisplay = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        mainDisplay.text(mm.deserialize(fullText.toString()));
        mainDisplay.setBillboard(Display.Billboard.CENTER);
        mainDisplay.setBackgroundColor(Color.fromARGB(140, 0, 0, 0));
        mainDisplay.setShadowed(true);
        mainDisplay.setViewRange(1.0f);

        Location closestLoc = holoLoc.clone().add(0, 0.6, 0);
        TextDisplay closestDisplay = (TextDisplay) world.spawnEntity(closestLoc, EntityType.TEXT_DISPLAY);
        closestDisplay.text(Component.empty());
        closestDisplay.setBillboard(Display.Billboard.CENTER);
        closestDisplay.setBackgroundColor(Color.fromARGB(150, 0, 200, 255));
        closestDisplay.setShadowed(true);
        closestDisplay.setViewRange(1.0f);

        HologramEntry entry = new HologramEntry(blockLocation, distance, mainDisplay, closestDisplay);
        playerData.addHologram(entry);

        return entry;
    }

    public boolean updateClosestMarker(GameSession session, HologramEntry newEntry) {
        Component closestComponent = plugin.msgRaw("closest-marker");

        HologramEntry currentClosest = session.getGlobalClosestHologram();

        if (currentClosest == null || newEntry.distanceToBomb() < currentClosest.distanceToBomb()) {
            if (currentClosest != null && currentClosest.closestDisplay() != null && !currentClosest.closestDisplay().isDead()) currentClosest.closestDisplay().text(Component.empty());

            if (newEntry.closestDisplay() != null && !newEntry.closestDisplay().isDead()) newEntry.closestDisplay().text(closestComponent);

            session.setGlobalClosestHologram(newEntry);
            return true;
        }

        return false;
    }

    public TextDisplay createBannedBlocksHologram(Location spawnLocation) {
        List<String> bannedBlocks = plugin.getConfig().getStringList("banned-blocks");

        StringBuilder text = new StringBuilder();
        text.append("<red><bold>⛔ Запрещённые блоки:</bold></red>");

        for (String block : bannedBlocks) {
            try {
                Material.valueOf(block.toUpperCase());
                text.append("\n<gray>• <white>").append(formatName(block)).append("</white></gray>");
            } catch (IllegalArgumentException ignored) {}
        }

        text.append("\n\n<yellow>Эти блоки нельзя использовать!");

        World world = spawnLocation.getWorld();
        Location holoLoc = spawnLocation.clone().add(0, 4, 0);

        TextDisplay display = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        display.text(mm.deserialize(text.toString()));
        display.setBillboard(Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(180, 30, 0, 0));
        display.setShadowed(true);
        display.setViewRange(0.8f);
        display.setLineWidth(300);

        return display;
    }

    private String getHintText(double distance) {
        List<?> hintsList = plugin.getConfig().getList("hints");
        if (hintsList == null) return "<gray>???</gray>";

        for (Object obj : hintsList)
            if (obj instanceof Map<?, ?> map) {
                Object distObj = map.get("distance");
                Object textObj = map.get("text");

                if (distObj instanceof Number num && textObj instanceof String text)
                    if (distance <= num.doubleValue()) return text;
            }

        return "<dark_blue><bold>⛄ Арктический холод!</bold></dark_blue>";
    }

    private String formatName(String name) {
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}