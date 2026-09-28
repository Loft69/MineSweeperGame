package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpeedBoostRaceEvent extends GameEvent {

    private final Random random = new Random();
    private int currentRound = 0;
    private final int totalRounds = 3;
    private Location checkpointLocation = null;
    private TextDisplay checkpointHologram = null;
    private boolean roundActive = false;
    private final Set<UUID> roundWinners = new HashSet<>();

    public SpeedBoostRaceEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        return "⚡ Скоростной забег (Раунд " + currentRound + "/" + totalRounds + ")";
    }

    @Override
    public int getDurationSeconds() {
        return 150;
    }

    @Override
    public String getRewardDescription() {
        return "+4 Детектора за раунд";
    }

    @Override
    protected void onStart() {
        broadcast("<yellow>⚡ Скоростной забег! 3 раунда!");
        broadcast("<gray>Добегите до контрольной точки первым!");
        broadcast("<gray>Награда: <green>+4 детектора</green> за каждый раунд.");

        for (Player p : getOnlinePlayers()) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, getDurationSeconds() * 20, 3, false, true, true));

        startNextRound();
    }

    @Override
    protected void onEnd() {
        cleanup();

        for (Player p : getOnlinePlayers()) p.removePotionEffect(PotionEffectType.SPEED);

        broadcast("<gray>Забег завершён! Победителей раундов: <white>" + roundWinners.size());
    }

    @Override
    protected void onForceEnd() {
        cleanup();
        for (Player p : getOnlinePlayers()) p.removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (!roundActive || checkpointLocation == null) return;

        for (Player player : getOnlinePlayers()) {
            if (player.getLocation().distanceSquared(checkpointLocation) <= 9) { // 3 блока
                roundActive = false;
                roundWinners.add(player.getUniqueId());

                broadcast("<green><bold>⚡ " + player.getName() + " добежал первым!</bold> +4 детектора!");
                giveDetectors(player, 4);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

                player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);

                cleanup();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive()) startNextRound();
                }, 100L);

                return;
            }
        }

        if (checkpointLocation != null) checkpointLocation.getWorld().spawnParticle(Particle.END_ROD, checkpointLocation.clone().add(0.5, 2, 0.5), 5, 0.3, 1, 0.3, 0.02);
    }

    private void startNextRound() {
        currentRound++;
        if (currentRound > totalRounds) {
            end();
            return;
        }

        Location avg = getAverageLocation();
        if (avg == null) { end(); return; }

        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 80 + random.nextInt(121);
        int tx = avg.getBlockX() + (int) (Math.cos(angle) * dist);
        int tz = avg.getBlockZ() + (int) (Math.sin(angle) * dist);

        World world = session.getArenaWorld();
        world.getChunkAt(tx >> 4, tz >> 4).load(true);
        int ty = world.getHighestBlockYAt(tx, tz) + 1;

        checkpointLocation = new Location(world, tx, ty, tz);

        Location holoLoc = checkpointLocation.clone().add(0.5, 2.5, 0.5);
        checkpointHologram = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        checkpointHologram.text(mm.deserialize("<yellow><bold>⚡ КОНТРОЛЬНАЯ ТОЧКА</bold></yellow>\n<gray>Раунд " + currentRound + "/" + totalRounds));
        checkpointHologram.setBillboard(Display.Billboard.CENTER);
        checkpointHologram.setBackgroundColor(Color.fromARGB(150, 50, 50, 0));
        checkpointHologram.setShadowed(true);
        checkpointHologram.setViewRange(2.0f);

        Block marker = checkpointLocation.getBlock();
        marker.setType(Material.BEACON);

        roundActive = true;

        broadcast("<yellow><bold>⚡ Раунд " + currentRound + "!</bold></yellow>");
        broadcast("<white>Контрольная точка: X=" + tx + " Z=" + tz + " (дистанция ~" + (int) dist + " блоков)");

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
    }

    private void cleanup() {
        if (checkpointHologram != null && !checkpointHologram.isDead()) {
            checkpointHologram.remove();
            checkpointHologram = null;
        }
        if (checkpointLocation != null) {
            checkpointLocation.getBlock().setType(Material.AIR);
            checkpointLocation = null;
        }
        roundActive = false;
    }

    private Location getAverageLocation() {
        List<Player> players = getOnlinePlayers();
        if (players.isEmpty()) return null;
        double x = 0, z = 0;
        for (Player p : players) {
            x += p.getLocation().getX();
            z += p.getLocation().getZ();
        }
        return new Location(session.getArenaWorld(), x / players.size(), 100, z / players.size());
    }
}