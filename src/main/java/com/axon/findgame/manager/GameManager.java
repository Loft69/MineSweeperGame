package com.axon.findgame.manager;

import com.axon.findgame.FindGame;
import com.axon.findgame.model.GameSession;
import com.axon.findgame.model.PlayerData;
import com.axon.findgame.model.PlayerData.HologramEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.*;

public class GameManager {

    private final FindGame plugin;
    private final MiniMessage mm;
    private GameSession currentSession = null;
    private TextDisplay bannedBlocksHologram = null;

    public static final String DETECTOR_KEY = "findgame_detector";

    public GameManager(FindGame plugin) {
        this.plugin = plugin;
        this.mm = plugin.mm();
    }

    public void startGame() {
        if (currentSession != null && currentSession.isActive()) return;

        Collection<? extends Player> allPlayers = Bukkit.getOnlinePlayers();
        if (allPlayers.isEmpty()) return;

        for (Player p : allPlayers) p.sendMessage(plugin.msg("game-starting"));

        World arenaWorld = plugin.getWorldManager().createArenaWorld();
        if (arenaWorld == null) {
            for (Player p : allPlayers) p.sendMessage(mm.deserialize("<red>Ошибка создания мира!"));
            return;
        }

        Location spawnLocation = plugin.getWorldManager().findSpawnLocation(arenaWorld);

        double borderSize = plugin.getConfig().getDouble("world-border-size", 2000);
        plugin.getWorldManager().setupWorldBorder(arenaWorld, spawnLocation, borderSize);

        Location bombLocation = plugin.getWorldManager().chooseBombLocation(arenaWorld, spawnLocation);

        currentSession = new GameSession(arenaWorld, spawnLocation, bombLocation);

        int detectorAmount = plugin.getConfig().getInt("detector-amount", 32);

        for (Player player : allPlayers) {
            PlayerData data = new PlayerData(player.getUniqueId(), player.getLocation().clone());
            currentSession.addPlayer(player.getUniqueId(), data);

            player.teleport(spawnLocation);
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExp(0f);
            player.setLevel(0);

            ItemStack detector = createDetectorItem();
            detector.setAmount(detectorAmount);
            player.getInventory().setItem(0, detector);

            Title title = Title.title(
                    mm.deserialize("<gradient:gold:red><bold>💣 FindGame</bold></gradient>"),
                    mm.deserialize("<gray>Найдите заминированный блок!"),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            player.showTitle(title);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage(plugin.msg("game-start", "%detectors%", String.valueOf(detectorAmount)));
        }

        bannedBlocksHologram = plugin.getHologramManager().createBannedBlocksHologram(spawnLocation);

        plugin.getScoreboardManager().startUpdating();

        plugin.getEventManager().startScheduler(currentSession);

//        plugin.getLogger().info("[GAME] Бомба: X=" + bombLocation.getBlockX()
//                + " Y=" + bombLocation.getBlockY()
//                + " Z=" + bombLocation.getBlockZ()
//                + " Блок: " + bombLocation.getBlock().getType());
    }

    public void checkBlock(Player player, Location blockLocation) {
        if (currentSession == null || !currentSession.isActive()) return;

        PlayerData playerData = currentSession.getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Location bomb = currentSession.getBombLocation();

        if (blockLocation.getBlockX() == bomb.getBlockX() && blockLocation.getBlockY() == bomb.getBlockY() && blockLocation.getBlockZ() == bomb.getBlockZ()) {
            winGame(player);
            return;
        }

        int detectorSlot = findDetectorSlot(player);
        if (detectorSlot == -1) {
            player.sendMessage(plugin.msg("no-detectors"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!plugin.getProximityManager().areAllPlayersCloseEnough(currentSession)) {
            int radius = plugin.getConfig().getInt("proximity-radius", 15);
            String msgStr = plugin.msgStr("not-close-enough").replace("%radius%", String.valueOf(radius));
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            player.sendMessage(mm.deserialize(prefix + msgStr));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            return;
        }

        consumeDetector(player, detectorSlot);

        playerData.incrementChecksUsed();

        HologramEntry entry = plugin.getHologramManager().createHintHologram(currentSession, playerData, blockLocation);

        boolean isNewClosest = plugin.getHologramManager().updateClosestMarker(currentSession, entry);

        if (isNewClosest) {
            broadcastToSession(plugin.msg("new-closest"));
            for (Player p : getSessionPlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            player.sendMessage(plugin.msg("not-closer"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }

        player.getWorld().spawnParticle(Particle.ENCHANT, blockLocation.clone().add(0.5, 1.2, 0.5), 30, 0.3, 0.3, 0.3, 0.5);
    }

    public void triggerBombExplosion(Player triggerPlayer) {
        if (currentSession == null || !currentSession.isActive()) return;
        currentSession.setActive(false);

        Location bomb = currentSession.getBombLocation();
        World world = bomb.getWorld();

        float explosionPower = (float) plugin.getConfig().getDouble("bomb-explosion-power", 10);
        double killRadius = plugin.getConfig().getDouble("bomb-kill-radius", 50);

        Location explosionLoc = bomb.clone().add(0.5, 0.5, 0.5);
        world.createExplosion(explosionLoc, explosionPower, false, true);

        for (Entity entity : world.getNearbyEntities(explosionLoc, killRadius, killRadius, killRadius))
            if (entity instanceof LivingEntity living)
                living.setHealth(0);

        Title title = Title.title(
                mm.deserialize("<red><bold>💥 ВЗРЫВ!</bold></red>"),
                mm.deserialize("<gray>" + triggerPlayer.getName() + " наступил на мину..."),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        );

        for (Player p : getSessionPlayers()) {
            p.showTitle(title);
            p.sendMessage(plugin.msg("game-lose"));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> endGame(false), 80L);
    }

    private void winGame(Player winner) {
        if (currentSession == null || !currentSession.isActive()) return;
        currentSession.setActive(false);

        Title title = Title.title(
                mm.deserialize("<gold><bold>🎉 ПОБЕДА!</bold></gold>"),
                mm.deserialize("<green>" + winner.getName() + " разминировал бомбу!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        );

        for (Player p : getSessionPlayers()) {
            p.showTitle(title);
            p.sendMessage(plugin.msg("game-win"));
            p.sendMessage(plugin.msg("defused"));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 2, 0), 100, 3, 3, 3, 0.1);
        }

        if (plugin.getConfig().getBoolean("rewards.enabled", false)) {
            List<String> commands = plugin.getConfig().getStringList("rewards.commands");
            for (Player p : getSessionPlayers())
                for (String cmd : commands)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> endGame(true), 80L);
    }

    public void endGame(boolean won) {
        if (currentSession == null) return;

        plugin.getEventManager().stopScheduler();

        plugin.getScoreboardManager().stopUpdating();
        plugin.getScoreboardManager().clearAllScoreboards();

        for (Player p : getSessionPlayers()) p.sendMessage(plugin.msg("game-end"));

        World fallback = Bukkit.getWorlds().getFirst();
        for (Map.Entry<UUID, PlayerData> entry : currentSession.getPlayers().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);

            Location prev = entry.getValue().getPreviousLocation();
            if (prev != null && prev.getWorld() != null) player.teleport(prev);
            else player.teleport(fallback.getSpawnLocation());
        }

        currentSession.clearAllHolograms();
        if (bannedBlocksHologram != null && !bannedBlocksHologram.isDead()) {
            bannedBlocksHologram.remove();
            bannedBlocksHologram = null;
        }

        World arenaWorld = currentSession.getArenaWorld();
        currentSession = null;

        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getWorldManager().deleteWorld(arenaWorld), 20L);
    }

    public void forceEndGame() {
        if (currentSession == null) return;

        plugin.getEventManager().stopScheduler();
        plugin.getScoreboardManager().stopUpdating();
        plugin.getScoreboardManager().clearAllScoreboards();

        World fallback = Bukkit.getWorlds().get(0);
        for (Map.Entry<UUID, PlayerData> entry : currentSession.getPlayers().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            player.getInventory().clear();
            Location prev = entry.getValue().getPreviousLocation();
            if (prev != null && prev.getWorld() != null) player.teleport(prev);
            else player.teleport(fallback.getSpawnLocation());
        }

        currentSession.clearAllHolograms();
        if (bannedBlocksHologram != null && !bannedBlocksHologram.isDead()) bannedBlocksHologram.remove();

        World arenaWorld = currentSession.getArenaWorld();
        currentSession = null;
        plugin.getWorldManager().deleteWorld(arenaWorld);
    }

    public ItemStack createDetectorItem() {
        String materialName = plugin.getConfig().getString("detector-item", "COMPASS");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        String nameStr = plugin.getConfig().getString("detector-name", "<gradient:gold:yellow><bold>🔍 Детектор мин</bold></gradient>");
        meta.displayName(mm.deserialize(nameStr));

        List<String> loreConfig = plugin.getConfig().getStringList("detector-lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreConfig) lore.add(mm.deserialize(line));
        meta.lore(lore);

        var key = new NamespacedKey(plugin, DETECTOR_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isDetector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var key = new NamespacedKey(plugin, DETECTOR_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    public boolean isBombBlock(Location blockLocation) {
        if (currentSession == null) return false;
        Location bomb = currentSession.getBombLocation();
        return blockLocation.getBlockX() == bomb.getBlockX() && blockLocation.getBlockY() == bomb.getBlockY() && blockLocation.getBlockZ() == bomb.getBlockZ();
    }

    private int findDetectorSlot(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDetector(item)) return i;
        }
        return -1;
    }

    private void consumeDetector(Player player, int slot) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null) return;

        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItem(slot, null);
    }

    public boolean isGameActive() {
        return currentSession != null && currentSession.isActive();
    }

    public GameSession getCurrentSession() { return currentSession; }

    public boolean isPlayerInGame(UUID uuid) {
        return currentSession != null && currentSession.getPlayers().containsKey(uuid);
    }

    public List<Player> getSessionPlayers() {
        if (currentSession == null) return Collections.emptyList();
        List<Player> result = new ArrayList<>();
        for (UUID uuid : currentSession.getPlayers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return result;
    }

    public void broadcastToSession(Component message) {
        for (Player p : getSessionPlayers()) p.sendMessage(message);
    }
}