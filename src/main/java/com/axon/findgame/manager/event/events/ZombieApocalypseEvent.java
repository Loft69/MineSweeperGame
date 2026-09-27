package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ZombieApocalypseEvent extends GameEvent {

    private int currentWave = 0;
    private int totalWaves = 5;
    private int zombiesAlive = 0;
    private int zombiesKilledThisWave = 0;
    private int zombiesPerWave = 5;
    private final Set<UUID> spawnedZombies = new HashSet<>();
    private final Set<UUID> survivedPlayers = new HashSet<>();
    private boolean waveActive = false;
    private long lastWaveTime = 0;

    private final Random random = new Random();

    public ZombieApocalypseEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        if (currentWave > 0) return "🧟 Зомби-апокалипсис (Волна " + currentWave + "/" + totalWaves + ")";
        return "🧟 Зомби-апокалипсис";
    }

    @Override
    public int getDurationSeconds() {
        return 300;
    }

    @Override
    public String getRewardDescription() {
        return "Подсказка направления бомбы";
    }

    @Override
    protected void onStart() {
        session.getArenaWorld().setGameRule(GameRule.DO_MOB_SPAWNING, false);
        session.getArenaWorld().setDifficulty(Difficulty.HARD);

        survivedPlayers.addAll(session.getPlayers().keySet());

        totalWaves = 5;
        zombiesPerWave = Math.max(3, getOnlinePlayers().size() * 3);

        broadcast("<red><bold>🧟 ЗОМБИ-АПОКАЛИПСИС!</bold></red>");
        broadcast("<gray>Выживите " + totalWaves + " волн зомби!");
        broadcast("<gray>Награда: <green>координаты направления бомбы (знаки ±)");
        broadcast("<red><bold>Это будет СЛОЖНО!</bold></red>");

        lastWaveTime = System.currentTimeMillis();
    }

    @Override
    protected void onEnd() {
        cleanupZombies();

        session.getArenaWorld().setDifficulty(Difficulty.PEACEFUL);

        List<Player> survivors = new ArrayList<>();
        for (UUID uuid : survivedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) survivors.add(p);
        }

        if (!survivors.isEmpty() && currentWave >= totalWaves) {
            broadcast("<green><bold>🎖 Вы выжили!</bold></green>");

            for (Player survivor : survivors) {
                String dirSign = session.getBombDirectionSign(survivor.getLocation());
                survivor.sendMessage(mm.deserialize("<gold><bold>📍 Подсказка:</bold> <white>Бомба находится в направлении: " + dirSign));
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
        } else broadcast("<red>Не все волны пройдены. Награда не выдана.");
    }

    @Override
    protected void onForceEnd() {
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

        if (currentWave >= totalWaves && !waveActive) {
            end();
            return;
        }

        long timeSinceLastWave = (System.currentTimeMillis() - lastWaveTime) / 1000;

        if (!waveActive && timeSinceLastWave >= 5) startNextWave();

        if (waveActive && zombiesAlive <= 0) {
            waveActive = false;
            lastWaveTime = System.currentTimeMillis();
            broadcast("<green>Волна " + currentWave + " зачищена!</green>");

            zombiesPerWave += 2;
        }
    }

    private void startNextWave() {
        currentWave++;
        waveActive = true;
        zombiesKilledThisWave = 0;

        broadcast("<red><bold>⚔ Волна " + currentWave + "/" + totalWaves + "!</bold></red>");
        broadcast("<gray>Зомби: <white>" + zombiesPerWave);

        List<Player> players = getOnlinePlayers();
        int zombiesPerPlayer = Math.max(1, zombiesPerWave / players.size());

        zombiesAlive = 0;

        for (Player player : players) for (int i = 0; i < zombiesPerPlayer; i++) spawnZombie(player.getLocation(), currentWave);

        for (Player p : players) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);
    }

    private void spawnZombie(Location near, int wave) {
        World world = near.getWorld();

        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = 15 + random.nextInt(11);
        int x = near.getBlockX() + (int) (Math.cos(angle) * dist);
        int z = near.getBlockZ() + (int) (Math.sin(angle) * dist);

        world.getChunkAt(x >> 4, z >> 4).load(true);
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location spawnLoc = new Location(world, x + 0.5, y, z + 0.5);

        Zombie zombie;
        if (wave >= 4 && random.nextInt(3) == 0) zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.HUSK);
        else zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);

        double baseHealth = 20.0 + (wave * 5.0);
        var maxHealthAttr = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(baseHealth);
            zombie.setHealth(baseHealth);
        }

        var speedAttr = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.25 + (wave * 0.02));

        var dmgAttr = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(3.0 + (wave * 1.5));

        if (wave >= 3) {
            zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            zombie.getEquipment().setHelmetDropChance(0.0f);
        }
        if (wave >= 5) {
            zombie.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            zombie.getEquipment().setChestplateDropChance(0.0f);
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            zombie.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        zombie.setShouldBurnInDay(false);
        zombie.setRemoveWhenFarAway(false);

        spawnedZombies.add(zombie.getUniqueId());
        zombiesAlive++;
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!isActive()) return;

        UUID entityId = event.getEntity().getUniqueId();
        if (spawnedZombies.remove(entityId)) {
            zombiesAlive = Math.max(0, zombiesAlive - 1);
            zombiesKilledThisWave++;

            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    private void cleanupZombies() {
        if (session == null || session.getArenaWorld() == null) return;

        for (Entity entity : session.getArenaWorld().getEntities()) if (entity instanceof Zombie && spawnedZombies.contains(entity.getUniqueId())) entity.remove();

        spawnedZombies.clear();
        zombiesAlive = 0;
    }
}