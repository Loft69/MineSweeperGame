package com.axon.findgame.manager;

import com.axon.findgame.FindGame;
import org.bukkit.*;
import org.bukkit.block.Block;

import java.io.File;
import java.util.Random;
import java.util.UUID;

public class WorldManager {

    private final FindGame plugin;
    private final Random random = new Random();

    public WorldManager(FindGame plugin) {
        this.plugin = plugin;
    }

    public World createArenaWorld() {
        String prefix = plugin.getConfig().getString("arena-world-prefix", "fg_arena_");
        String worldName = prefix + UUID.randomUUID().toString().substring(0, 8);
        long seed = random.nextLong();

        plugin.getLogger().info("Создаю мир: " + worldName + " (seed: " + seed + ")");

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.seed(seed);
        creator.generateStructures(true);

        World world = creator.createWorld();

        if (world != null) {
            world.setDifficulty(Difficulty.HARD);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.DO_FIRE_TICK, true);
            world.setGameRule(GameRule.MOB_GRIEFING, true);
            world.setGameRule(GameRule.NATURAL_REGENERATION, true);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
            world.setTime(6000);
            world.setStorm(false);
            world.setThundering(false);
        }

        return world;
    }

    public void deleteWorld(World world) {
        if (world == null) return;
        String worldName = world.getName();
        plugin.getLogger().info("Удаляю мир: " + worldName);

        World fallback = Bukkit.getWorlds().getFirst();
        for (var player : world.getPlayers()) player.teleport(fallback.getSpawnLocation());

        Bukkit.unloadWorld(world, false);

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        deleteFolder(worldFolder);
        plugin.getLogger().info("Мир " + worldName + " удалён.");
    }

    public void setupWorldBorder(World world, Location center, double size) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(size);
        border.setWarningDistance(10);
        border.setWarningTime(5);
        border.setDamageBuffer(2);
        border.setDamageAmount(2.0);
    }

    public Location findSpawnLocation(World world) {
        int radius = 150;
        java.util.Random random = new java.util.Random();

        for (int attempt = 0; attempt < 500; attempt++) {
            int x = random.nextInt(radius * 2 + 1) - radius;
            int z = random.nextInt(radius * 2 + 1) - radius;

            world.getChunkAt(x >> 4, z >> 4).load(true);

            int highestY = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, highestY, z);

            if (block.isLiquid()) continue;

            if (!block.getType().isSolid()) continue;

//            Block above1 = block.getRelative(0, 1, 0);
//            Block above2 = block.getRelative(0, 2, 0);
//            if (!above1.isEmpty() || !above2.isEmpty()) continue;

            return new Location(world, x + 0.5, highestY + 1, z + 0.5);
        }

        world.getChunkAt(0, 0).load(true);
        int fallbackY = world.getHighestBlockYAt(0, 0);
        return new Location(world, 0.5, fallbackY + 1, 0.5);
    }

    public Location chooseBombLocation(World world, Location spawnLocation) {
        int halfSize = plugin.getConfig().getInt("arena-half-size", 1000);

        for (int attempt = 0; attempt < 300; attempt++) {
            int dx = random.nextInt(halfSize * 2 + 1) - halfSize;
            int dz = random.nextInt(halfSize * 2 + 1) - halfSize;

            if (Math.abs(dx) < 50 && Math.abs(dz) < 50) continue;

            int x = spawnLocation.getBlockX() + dx;
            int z = spawnLocation.getBlockZ() + dz;

            world.getChunkAt(x >> 4, z >> 4).load(true);

            int surfaceY = world.getHighestBlockYAt(x, z);
            int depth = random.nextInt(50);
            int bombY = surfaceY - depth;

            if (bombY < world.getMinHeight() + 1) continue;

            Block block = world.getBlockAt(x, bombY, z);
            if (block.getType().isSolid() && !block.isLiquid()) return block.getLocation();
        }

        int fx = spawnLocation.getBlockX() + 200;
        int fz = spawnLocation.getBlockZ() + 200;
        world.getChunkAt(fx >> 4, fz >> 4).load(true);
        int fy = world.getHighestBlockYAt(fx, fz);
        return new Location(world, fx, fy, fz);
    }

    private void deleteFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files)
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
        }
        folder.delete();
    }
}