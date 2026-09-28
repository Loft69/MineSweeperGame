package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SupplyDropEvent extends GameEvent {

    private final Random random = new Random();
    private Location dropLocation = null;
    private TextDisplay hologram = null;
    private TextDisplay markerBeam = null;
    private boolean collected = false;
    private UUID collectorId = null;

    public SupplyDropEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        if (collected) return "🪂 Припасы забрали!";
        return "🪂 Сброс припасов";
    }

    @Override
    public int getDurationSeconds() {
        return 120;
    }

    @Override
    public String getRewardDescription() {
        return "8 Детекторов (первому!)";
    }

    @Override
    protected void onStart() {
        Location avg = getAveragePlayerLocation();
        if (avg == null) {
            end();
            return;
        }

        World world = session.getArenaWorld();

        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 100 + random.nextInt(201);
        int tx = avg.getBlockX() + (int) (Math.cos(angle) * dist);
        int tz = avg.getBlockZ() + (int) (Math.sin(angle) * dist);

        world.getChunkAt(tx >> 4, tz >> 4).load(true);
        int ty = world.getHighestBlockYAt(tx, tz) + 1;

        dropLocation = new Location(world, tx + 0.5, ty, tz + 0.5);

        dropLocation.getBlock().setType(Material.GLOWSTONE);

        Location holoLoc = dropLocation.clone().add(0, 3, 0);
        hologram = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        hologram.text(mm.deserialize("<yellow><bold>🪂 ПРИПАСЫ</bold></yellow>\n<gray>Подойдите чтобы забрать!"));
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setBackgroundColor(Color.fromARGB(150, 50, 50, 0));
        hologram.setShadowed(true);
        hologram.setViewRange(3.0f);

        Location beamLoc = dropLocation.clone().add(0, 15, 0);
        markerBeam = (TextDisplay) world.spawnEntity(beamLoc, EntityType.TEXT_DISPLAY);
        markerBeam.text(mm.deserialize("<yellow>⬇ ⬇ ⬇</yellow>"));
        markerBeam.setBillboard(Display.Billboard.CENTER);
        markerBeam.setViewRange(5.0f);
        markerBeam.setShadowed(true);

        broadcast("<green>🪂 Припасы сброшены!");
        broadcast("<yellow>Координаты: <white>X=" + tx + " Z=" + tz);
        broadcast("<gray>Первый кто подойдёт — заберёт <green>8 детекторов</green> + бонусный лут!");

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
    }

    @Override
    protected void onEnd() {
        cleanup();

        if (!collected) broadcast("<red>Никто не забрал припасы. Они исчезли.");
    }

    @Override
    protected void onForceEnd() {
        cleanup();
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (collected || dropLocation == null) return;

        for (Player player : getOnlinePlayers()) {
            if (player.isDead()) continue;
            if (!player.getWorld().equals(dropLocation.getWorld())) continue;

            double distSq = player.getLocation().distanceSquared(dropLocation);
            if (distSq <= 9.0) { // 3 блока
                collectDrop(player);
                return;
            }
        }

        World world = dropLocation.getWorld();

        for (int y = 0; y < 20; y++) world.spawnParticle(Particle.END_ROD, dropLocation.clone().add(0, y, 0), 1, 0.05, 0, 0.05, 0);

        for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) {
            double px = dropLocation.getX() + Math.cos(a) * 2;
            double pz = dropLocation.getZ() + Math.sin(a) * 2;
            world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, px, dropLocation.getY() + 0.5, pz), 1, 0, 0, 0, 0);
        }

        if (elapsedSeconds > 0 && elapsedSeconds % 30 == 0) broadcast("<gray>🪂 Припасы ждут: X=" + dropLocation.getBlockX() + " Z=" + dropLocation.getBlockZ());
    }

    private void collectDrop(Player player) {
        collected = true;
        collectorId = player.getUniqueId();

        giveDetectors(player, 8);

        giveItemSafe(player, new ItemStack(Material.GOLDEN_APPLE, 2));
        giveItemSafe(player, new ItemStack(Material.IRON_INGOT, 8));
        giveItemSafe(player, new ItemStack(Material.DIAMOND, 2));
        giveItemSafe(player, new ItemStack(Material.COOKED_BEEF, 16));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);

        broadcast("<green><bold>🪂 " + player.getName() + " забрал припасы!</bold>");

        cleanup();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) end();
        }, 60L);
    }

    private void giveItemSafe(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (var left : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
    }

    private void cleanup() {
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
            hologram = null;
        }
        if (markerBeam != null && !markerBeam.isDead()) {
            markerBeam.remove();
            markerBeam = null;
        }
        if (dropLocation != null)
            if (dropLocation.getBlock().getType() == Material.GLOWSTONE) dropLocation.getBlock().setType(Material.AIR);
    }

    private Location getAveragePlayerLocation() {
        List<Player> players = getOnlinePlayers();
        if (players.isEmpty()) return null;
        double x = 0, y = 0, z = 0;
        for (Player p : players) {
            x += p.getLocation().getX();
            y += p.getLocation().getY();
            z += p.getLocation().getZ();
        }
        return new Location(session.getArenaWorld(), x / players.size(), y / players.size(), z / players.size());
    }
}