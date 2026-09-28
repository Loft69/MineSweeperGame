package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ZombieApocalypseEvent extends GameEvent {

    private int currentWave = 0;
    private final int totalWaves = 3;
    private final Set<UUID> spawnedZombies = new HashSet<>();
    private final Set<UUID> survivedPlayers = new HashSet<>();

    private BukkitTask waveSpawnTask;
    private long waveStartTime = 0;
    private boolean waveActive = false;
    private boolean betweenWaves = false;
    private long betweenWaveStart = 0;
    private int zombiesKilled = 0;

    private final Random random = new Random();

    public ZombieApocalypseEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        if (betweenWaves) return "🧟 Зомби-апокалипсис (Перерыв...)";
        if (waveActive) return "🧟 Зомби-апокалипсис (Волна " + currentWave + "/" + totalWaves + ")";
        return "🧟 Зомби-апокалипсис";
    }

    @Override
    public int getDurationSeconds() {
        return 220;
    }

    @Override
    public String getRewardDescription() {
        return "Направление бомбы (±)";
    }

    @Override
    protected void onStart() {
        session.getArenaWorld().setDifficulty(Difficulty.HARD);
        survivedPlayers.addAll(session.getPlayers().keySet());

        broadcast("<red><bold>🧟 ЗОМБИ-АПОКАЛИПСИС!</bold></red>");
        broadcast("<gray>3 волны зомби! Выживите все!");
        broadcast("<gray>Награда выжившим: <green>направление бомбы</green>");
        broadcast("<red><bold>⚠ Это будет СЛОЖНО!</bold></red>");
        broadcast("");
        broadcast("<gray>Волна 1: 2 зомби каждые 3 сек (60 сек)");
        broadcast("<gray>Волна 2: 3 зомби каждые 3 сек (60 сек)");
        broadcast("<gray>Волна 3: 5 зомби каждые 2 сек (60 сек)");

        betweenWaves = true;
        betweenWaveStart = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) startNextWave();
        }, 100L);

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);
    }

    @Override
    protected void onEnd() {
        stopWaveTask();
        cleanupZombies();
        session.getArenaWorld().setDifficulty(Difficulty.PEACEFUL);

        List<Player> survivors = new ArrayList<>();
        for (UUID uuid : survivedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) survivors.add(p);
        }

        broadcast("<gray>Зомби уничтожено: <white>" + zombiesKilled);

        if (!survivors.isEmpty() && currentWave >= totalWaves) {
            broadcast("<green><bold>🎖 Вы выжили!</bold></green>");

            for (Player survivor : survivors) {
                String dirSign = session.getBombDirectionSign(survivor.getLocation());
                survivor.sendMessage(mm.deserialize("<gold><bold>📍 Подсказка:</bold> <white>Направление бомбы: " + dirSign));
                survivor.playSound(survivor.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            for (UUID uuid : session.getPlayers().keySet()) {
                if (survivedPlayers.contains(uuid)) continue;
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    String dirSign = session.getBombDirectionSign(session.getSpawnLocation());
                    p.sendMessage(mm.deserialize("<gray>Выжившие получили подсказку: бомба в направлении " + dirSign));
                }
            }
        } else broadcast("<red>Не все волны пройдены или все погибли. Награда не выдана.");
    }

    @Override
    protected void onForceEnd() {
        stopWaveTask();
        cleanupZombies();
        session.getArenaWorld().setDifficulty(Difficulty.PEACEFUL);
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        survivedPlayers.removeIf(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            return p == null || !p.isOnline() || p.isDead();
        });

        if (survivedPlayers.isEmpty()) {
            broadcast("<red><bold>Все погибли! Ивент провален.</bold></red>");
            end();
            return;
        }

        if (waveActive) {
            long waveElapsed = (System.currentTimeMillis() - waveStartTime) / 1000;
            long waveDuration = 60;

            if (waveElapsed >= waveDuration) {
                waveActive = false;
                stopWaveTask();

                broadcast("<green>✔ Волна " + currentWave + " завершена! " + "Убито зомби: " + zombiesKilled);

                cleanupZombies();

                if (currentWave >= totalWaves) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (isActive()) end();
                    }, 40L);
                } else {
                    betweenWaves = true;
                    betweenWaveStart = System.currentTimeMillis();
                    broadcast("<yellow>Следующая волна через 10 секунд... Подготовьтесь!");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (isActive()) startNextWave();
                    }, 200L);
                }
            }
        }
    }

    private void startNextWave() {
        currentWave++;
        waveActive = true;
        betweenWaves = false;
        waveStartTime = System.currentTimeMillis();

        int zombiesPerSpawn;
        int spawnIntervalTicks;

        switch (currentWave) {
            case 1 -> {
                zombiesPerSpawn = 2;
                spawnIntervalTicks = 60;
            }
            case 2 -> {
                zombiesPerSpawn = 3;
                spawnIntervalTicks = 60;
            }
            default -> {
                zombiesPerSpawn = 5;
                spawnIntervalTicks = 40;
            }
        }

        broadcast("<red><bold>⚔ ВОЛНА " + currentWave + "/" + totalWaves + "!</bold></red>");
        broadcast("<gray>Зомби: <white>" + zombiesPerSpawn + "</white> каждые <white>" + (spawnIntervalTicks / 20) + "</white> сек | Длительность: <white>60 сек");

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);

        final int zombiesCount = zombiesPerSpawn;
        waveSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive() || !waveActive) {
                stopWaveTask();
                return;
            }

            for (UUID uuid : survivedPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline() || player.isDead()) continue;

                for (int i = 0; i < zombiesCount; i++) spawnZombie(player.getLocation(), currentWave);
            }

        }, 20L, spawnIntervalTicks);
    }

    private void spawnZombie(Location near, int wave) {
        World world = near.getWorld();

        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 12 + random.nextInt(11);
        int x = near.getBlockX() + (int) (Math.cos(angle) * dist);
        int z = near.getBlockZ() + (int) (Math.sin(angle) * dist);

        world.getChunkAt(x >> 4, z >> 4).load(true);
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location spawnLoc = new Location(world, x + 0.5, y, z + 0.5);

        Zombie zombie;
        if (wave >= 3 && random.nextInt(4) == 0) zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.HUSK);
        else if (wave >= 2 && random.nextInt(5) == 0) zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.DROWNED);
        else zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);


        double baseHealth = switch (wave) {
            case 1 -> 20.0;
            case 2 -> 25.0;
            case 3 -> 30.0;
            default -> 20.0;
        };

        var maxHealthAttr = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(baseHealth);
            zombie.setHealth(baseHealth);
        }

        var speedAttr = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            double speed = switch (wave) {
                case 1 -> 0.25;
                case 2 -> 0.28;
                case 3 -> 0.32;
                default -> 0.25;
            };
            speedAttr.setBaseValue(speed);
        }

        var dmgAttr = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) {
            double dmg = switch (wave) {
                case 1 -> 3.0;
                case 2 -> 5.0;
                case 3 -> 7.0;
                default -> 3.0;
            };
            dmgAttr.setBaseValue(dmg);
        }

        if (wave >= 2) {
            zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            zombie.getEquipment().setHelmetDropChance(0.0f);
        }
        if (wave >= 3) {
            zombie.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            zombie.getEquipment().setChestplateDropChance(0.0f);
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            zombie.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        zombie.setShouldBurnInDay(false);
        zombie.setRemoveWhenFarAway(false);

        zombie.customName(mm.deserialize("<red>Зомби <gray>[Волна " + wave + "]"));
        zombie.setCustomNameVisible(false);

        spawnedZombies.add(zombie.getUniqueId());
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!isActive()) return;

        UUID entityId = event.getEntity().getUniqueId();
        if (spawnedZombies.remove(entityId)) {
            zombiesKilled++;

            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (!isActive()) return;
        Player player = event.getEntity();
        if (survivedPlayers.remove(player.getUniqueId())) broadcast("<red>💀 " + player.getName() + " пал в бою!");
    }

    private void stopWaveTask() {
        if (waveSpawnTask != null) {
            waveSpawnTask.cancel();
            waveSpawnTask = null;
        }
    }

    private void cleanupZombies() {
        if (session == null || session.getArenaWorld() == null) return;

        for (Entity entity : session.getArenaWorld().getEntities())
            if (entity instanceof Zombie && spawnedZombies.contains(entity.getUniqueId())) entity.remove();
        spawnedZombies.clear();
    }
}