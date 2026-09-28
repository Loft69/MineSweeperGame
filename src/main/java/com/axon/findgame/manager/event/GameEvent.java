package com.axon.findgame.manager.event;

import com.axon.findgame.FindGame;
import com.axon.findgame.model.GameSession;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.UUID;

public abstract class GameEvent implements Listener {

    protected final FindGame plugin;
    protected final MiniMessage mm;
    @Getter
    protected GameSession session;
    protected BukkitTask tickTask;
    protected long startTime;
    @Getter
    protected boolean active = false;

    public GameEvent(FindGame plugin) {
        this.plugin = plugin;
        this.mm = plugin.mm();
    }


    public abstract String getDisplayName();
    public abstract int getDurationSeconds();
    public abstract String getRewardDescription();
    protected abstract void onStart();
    protected abstract void onEnd();
    protected abstract void onTick(long elapsedSeconds);

    protected void onForceEnd() {
        onEnd();
    }

    public final void start(GameSession session) {
        this.session = session;
        this.startTime = System.currentTimeMillis();
        this.active = true;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        broadcastTitle("<white>" + getDisplayName());
        broadcast("<light_purple><bold>⚡ Ивент: </bold><white>" + getDisplayName());
        if (getRewardDescription() != null) broadcast("<gray>Награда: <green>" + getRewardDescription());
        broadcast("<gray>Длительность: <yellow>" + getDurationSeconds() + " сек");

        onStart();

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

            if (elapsed >= getDurationSeconds()) {
                end();
                return;
            }

            onTick(elapsed);
        }, 20L, 20L);
    }

    public final void end() {
        if (!active) return;
        active = false;

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        onEnd();
        unregisterListener();

        broadcast("<light_purple><bold>⚡ Ивент завершён:</bold> <white>" + getDisplayName());
    }

    public final void forceEnd() {
        if (!active) return;
        active = false;

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        onForceEnd();
        unregisterListener();
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
    }

    public long getRemainingSeconds() {
        if (!active) return 0;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return Math.max(0, getDurationSeconds() - elapsed);
    }

    protected void broadcast(String miniMessageText) {
        Component component = mm.deserialize(
                plugin.getConfig().getString("messages.prefix", "") + miniMessageText);
        for (UUID uuid : session.getPlayers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.sendMessage(component);
        }
    }

    protected void broadcastTitle(String subtitleText) {
        Title title = Title.title(
                mm.deserialize("<light_purple><bold>⚡ ИВЕНТ!</bold></light_purple>"),
                mm.deserialize(subtitleText),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))
        );
        for (UUID uuid : session.getPlayers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.showTitle(title);
        }
    }

    protected java.util.List<Player> getOnlinePlayers() {
        java.util.List<Player> result = new java.util.ArrayList<>();
        for (UUID uuid : session.getPlayers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return result;
    }

    protected void giveDetectors(Player player, int amount) {
        var detector = plugin.getGameManager().createDetectorItem();
        detector.setAmount(amount);
        var leftover = player.getInventory().addItem(detector);
        if (!leftover.isEmpty())
            for (var item : leftover.values())
                player.getWorld().dropItemNaturally(player.getLocation(), item);


    }
}