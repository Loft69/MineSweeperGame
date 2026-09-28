package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MeteorShowerEvent extends GameEvent {

    private final Random random = new Random();
    private final Set<UUID> deadPlayers = new HashSet<>();
    private final Set<UUID> meteorEntities = new HashSet<>();
    private BukkitTask meteorSpawnTask;
    private int phase = 1;

    public MeteorShowerEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        return "☄️ Метеоритный дождь (Фаза " + phase + ")";
    }

    @Override
    public int getDurationSeconds() {
        return 90;
    }

    @Override
    public String getRewardDescription() {
        return "+3 Детектора (если никто не погиб)";
    }

    @Override
    protected void onStart() {
        broadcast("<red><bold>☄️ МЕТЕОРИТНЫЙ ДОЖДЬ!</bold></red>");
        broadcast("<gray>Метеориты летят В ВАС! Уворачивайтесь!");
        broadcast("<gray>Если <white>никто</white> не погибнет — все получат <green>+3 детектора</green>!");

        meteorSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive()) return;

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

            if (elapsed < 20) {
                phase = 1;
                if (random.nextInt(4) != 0) return;
            } else if (elapsed < 45) {
                phase = 2;
                if (random.nextInt(2) != 0) return;
            } else if (elapsed < 70) {
                phase = 3;
            } else {
                phase = 4;
                for (Player p : getOnlinePlayers()) if (!p.isDead()) spawnMeteor(p);
            }

            for (Player player : getOnlinePlayers())
                if (!player.isDead()) spawnMeteor(player);

        }, 10L, 5L);

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
    }

    @Override
    protected void onEnd() {
        stopMeteorTask();
        cleanupMeteors();

        if (deadPlayers.isEmpty()) {
            broadcast("<green><bold>✔ ВСЕ ВЫЖИЛИ!</bold> Каждый получает +3 детектора!");
            for (Player p : getOnlinePlayers()) {
                giveDetectors(p, 3);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } else broadcast("<red>Погибших: " + deadPlayers.size() + ". Награда не выдана.");
    }

    @Override
    protected void onForceEnd() {
        stopMeteorTask();
        cleanupMeteors();
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (elapsedSeconds == 20) {
            broadcast("<yellow>☄️ Фаза 2: Метеориты усиливаются!");
            for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.8f);
        }
        if (elapsedSeconds == 45) {
            broadcast("<red>☄️ Фаза 3: <bold>ИНТЕНСИВНЫЙ ОБСТРЕЛ!</bold>");
            for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.5f);
        }
        if (elapsedSeconds == 70) {
            broadcast("<dark_red><bold>☄️ Фаза 4: АРМАГЕДДОН! 20 секунд осталось!</bold>");
            for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
        }
    }

    private void spawnMeteor(Player target) {
        Location playerLoc = target.getLocation();
        World world = playerLoc.getWorld();


        double angle = random.nextDouble() * 2 * Math.PI;


        double spawnDist = 30 + random.nextInt(20);

        double heightOffset = 10 + random.nextInt(16);

        double spawnX = playerLoc.getX() + Math.cos(angle) * spawnDist;
        double spawnZ = playerLoc.getZ() + Math.sin(angle) * spawnDist;
        double spawnY = playerLoc.getY() + heightOffset;

        Location spawnLoc = new Location(world, spawnX, spawnY, spawnZ);

        double offsetX = (random.nextDouble() - 0.5) * 6;
        double offsetZ = (random.nextDouble() - 0.5) * 6;
        Location targetLoc = playerLoc.clone().add(offsetX, 0, offsetZ);

        Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();

        double speed = 1.2 + (phase * 0.3);
        direction.multiply(speed);

        Fireball fireball = world.spawn(spawnLoc, Fireball.class);
        fireball.setDirection(direction);
        fireball.setYield(2.5f + (phase * 0.5f));
        fireball.setIsIncendiary(false);
        fireball.setVisualFire(true);
        fireball.setGravity(false);

        meteorEntities.add(fireball.getUniqueId());

        world.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.3f);

        world.spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 1, 1, 1, 0.02);
    }

    @EventHandler
    public void onMeteorHit(ProjectileHitEvent event) {
        if (!isActive()) return;

        if (event.getEntity() instanceof Fireball fireball) {
            if (!meteorEntities.remove(fireball.getUniqueId())) return;

            Location hitLoc = fireball.getLocation();

            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 3, 1, 1, 1, 0);
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 50, 2, 2, 2, 0.1);
            hitLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, hitLoc, 30, 3, 3, 3, 0.05);

            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (!isActive()) return;
        Player player = event.getEntity();
        if (session.getPlayers().containsKey(player.getUniqueId())) {
            deadPlayers.add(player.getUniqueId());
            broadcast("<red>💀 " + player.getName() + " погиб от метеорита!");
        }
    }

    private void stopMeteorTask() {
        if (meteorSpawnTask != null) {
            meteorSpawnTask.cancel();
            meteorSpawnTask = null;
        }
    }

    private void cleanupMeteors() {
        if (session == null || session.getArenaWorld() == null) return;
        for (Entity entity : session.getArenaWorld().getEntities())
            if (entity instanceof Fireball && meteorEntities.contains(entity.getUniqueId())) entity.remove();
        meteorEntities.clear();
    }
}