package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TreasureHuntEvent extends GameEvent {

    private final Random random = new Random();
    private final List<TreasureChest> chests = new ArrayList<>();
    private int revealedCount = 0;

    private static final Material[] BONUS_LOOT = {
            Material.DIAMOND, Material.IRON_INGOT, Material.GOLDEN_APPLE,
            Material.ENDER_PEARL, Material.COOKED_BEEF, Material.BREAD,
            Material.IRON_PICKAXE, Material.IRON_SWORD, Material.SHIELD,
            Material.IRON_BOOTS, Material.BOW, Material.ARROW,
            Material.TORCH, Material.OAK_PLANKS, Material.COBBLESTONE
    };

    public TreasureHuntEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        long opened = chests.stream().filter(c -> c.opened).count();
        return "🗺️ Сокровища (" + opened + "/" + chests.size() + " открыто)";
    }

    @Override
    public int getDurationSeconds() {
        return 180;
    }

    @Override
    public String getRewardDescription() {
        return "Детекторы + лут в сундуках";
    }

    @Override
    protected void onStart() {
        World world = session.getArenaWorld();
        Location spawn = session.getSpawnLocation();

        broadcast("<gold>🗺️ 5 сундуков с сокровищами появились на карте!");
        broadcast("<gray>Координаты будут раскрываться каждые 30 секунд...");
        broadcast("<gray>В каждом сундуке: <green>детекторы</green> + полезный лут!");

        for (int i = 0; i < 5; i++) {
            Location chestLoc = findSafeChestLocation(world, spawn, 50 + random.nextInt(200));
            if (chestLoc == null) continue;

            Block block = chestLoc.getBlock();
            block.setType(Material.CHEST);

            final int index = i;
            final Location loc = chestLoc.clone();
            Bukkit.getScheduler().runTaskLater(plugin, () -> fillChest(loc, index), 2L);

            Location holoLoc = chestLoc.clone().add(0.5, 1.8, 0.5);
            TextDisplay display = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
            display.text(mm.deserialize("<gold><bold>🗺️ Сокровище #" + (i + 1) + "</bold></gold>\n<gray>Откройте меня!"));
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(Color.fromARGB(150, 50, 30, 0));
            display.setShadowed(true);
            display.setViewRange(1.5f);

            chests.add(new TreasureChest(chestLoc, display));
        }

        if (!chests.isEmpty()) revealChest(0);
    }

    private void fillChest(Location loc, int index) {
        Block block = loc.getBlock();
        if (block.getType() != Material.CHEST) return;

        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) return;

        Inventory inv = chest.getBlockInventory();
        inv.clear();

        int detectorCount = 2 + random.nextInt(4);
        ItemStack detectors = plugin.getGameManager().createDetectorItem();
        detectors.setAmount(detectorCount);
        inv.setItem(13, detectors);

        int lootItems = 3 + random.nextInt(4);
        Set<Integer> usedSlots = new HashSet<>();
        usedSlots.add(13);

        for (int i = 0; i < lootItems; i++) {
            int slot;
            do {
                slot = random.nextInt(27);
            } while (usedSlots.contains(slot));
            usedSlots.add(slot);

            Material mat = BONUS_LOOT[random.nextInt(BONUS_LOOT.length)];
            int amount;
            if (mat.getMaxStackSize() > 16) amount = 2 + random.nextInt(8);
            else amount = 1 + random.nextInt(2);

            inv.setItem(slot, new ItemStack(mat, amount));
        }

        chest.update(true);

        plugin.getLogger().info("[TREASURE] Сундук #" + (index + 1) + " заполнен на " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " (детекторов: " + detectorCount + ")");
    }

    @Override
    protected void onEnd() {
        for (TreasureChest tc : chests) {
            if (!tc.opened && tc.location.getBlock().getType() == Material.CHEST) tc.location.getBlock().setType(Material.AIR);
            if (tc.hologram != null && !tc.hologram.isDead()) tc.hologram.remove();
        }

        long opened = chests.stream().filter(c -> c.opened).count();
        broadcast("<gray>Охота завершена! Открыто сундуков: <white>" + opened + "/" + chests.size());
        chests.clear();
    }

    @Override
    protected void onForceEnd() {
        for (TreasureChest tc : chests) {
            if (tc.location.getBlock().getType() == Material.CHEST) tc.location.getBlock().setType(Material.AIR);
            if (tc.hologram != null && !tc.hologram.isDead()) tc.hologram.remove();
        }
        chests.clear();
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        int shouldReveal = Math.min(chests.size(), (int) (elapsedSeconds / 30) + 1);

        while (revealedCount < shouldReveal) {
            revealChest(revealedCount);
            revealedCount++;
        }

        for (TreasureChest tc : chests) {
            if (!tc.opened && tc.location.getBlock().getType() == Material.CHEST) {
                tc.location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, tc.location.clone().add(0.5, 1.2, 0.5), 3, 0.3, 0.3, 0.3, 0);

                for (int y = 1; y <= 5; y++) tc.location.getWorld().spawnParticle(Particle.END_ROD, tc.location.clone().add(0.5, y, 0.5), 1, 0.05, 0, 0.05, 0);
            }
        }

        if (chests.stream().allMatch(c -> c.opened)) {
            broadcast("<green><bold>Все сундуки открыты!</bold>");
            end();
        }
    }

    private void revealChest(int index) {
        if (index >= chests.size()) return;
        TreasureChest tc = chests.get(index);
        Location loc = tc.location;

        broadcast("<gold>📍 Сокровище #" + (index + 1) + ": <white>X=" + loc.getBlockX() + " Y=" + loc.getBlockY() + " Z=" + loc.getBlockZ());

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!isActive()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!session.getPlayers().containsKey(player.getUniqueId())) return;

        // Получаем локацию инвентаря
        Inventory inv = event.getInventory();
        Location invLoc = inv.getLocation();
        if (invLoc == null) return;

        for (TreasureChest tc : chests) {
            if (tc.opened) continue;

            if (tc.location.getBlockX() == invLoc.getBlockX() && tc.location.getBlockY() == invLoc.getBlockY() && tc.location.getBlockZ() == invLoc.getBlockZ()) {
                tc.opened = true;

                broadcast("<green>✔ " + player.getName() + " открыл Сокровище #" + (chests.indexOf(tc) + 1) + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

                if (tc.hologram != null && !tc.hologram.isDead()) tc.hologram.text(mm.deserialize("<green><bold>✔ Открыто</bold></green>\n<gray>" + player.getName()));

                break;
            }
        }
    }

    private Location findSafeChestLocation(World world, Location center, int radius) {
        for (int attempt = 0; attempt < 100; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 30 + random.nextInt(radius);
            int x = center.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * dist);

            world.getChunkAt(x >> 4, z >> 4).load(true);
            int y = world.getHighestBlockYAt(x, z);

            Block surface = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);

            if (surface.getType().isSolid() && !surface.isLiquid() && above.getType().isAir()) {

                Location loc = new Location(world, x, y + 1, z);
                boolean tooClose = false;
                for (TreasureChest tc : chests) {
                    if (tc.location.distanceSquared(loc) < 400) { // 20 блоков
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) return loc;
            }
        }
        return null;
    }

    private static class TreasureChest {
        final Location location;
        final TextDisplay hologram;
        boolean opened;

        TreasureChest(Location location, TextDisplay hologram) {
            this.location = location;
            this.hologram = hologram;
            this.opened = false;
        }
    }
}