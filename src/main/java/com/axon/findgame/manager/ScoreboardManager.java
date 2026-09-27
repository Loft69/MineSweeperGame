package com.axon.findgame.manager;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.EventManager;
import com.axon.findgame.manager.event.GameEvent;
import com.axon.findgame.model.GameSession;
import com.axon.findgame.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.UUID;

public class ScoreboardManager {

    private final FindGame plugin;
    private final MiniMessage mm;
    private BukkitTask updateTask;

    public ScoreboardManager(FindGame plugin) {
        this.plugin = plugin;
        this.mm = plugin.mm();
    }

    public void startUpdating() {
        if (updateTask != null) updateTask.cancel();

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameSession session = plugin.getGameManager().getCurrentSession();
            if (session == null || !session.isActive()) return;

            for (UUID uuid : session.getPlayers().keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                updatePlayerScoreboard(player, session);
            }
        }, 0L, 20L);
    }

    public void stopUpdating() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updatePlayerScoreboard(Player player, GameSession session) {
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("findgame", Criteria.DUMMY, mm.deserialize("<gradient:gold:red><bold>💣 FindGame</bold></gradient>"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;

        setLine(objective, line--, mm.deserialize("<gray>⏱ Время: <white>" + session.getFormattedElapsedTime()));

        setLine(objective, line--, Component.empty());

        setLine(objective, line--, mm.deserialize("<yellow><bold>Игроки:</bold></yellow>"));

        for (var entry : session.getPlayers().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            int detectors = countDetectors(p);
            String status = p.isDead() ? "<red>💀" : "<green>❤";
            setLine(objective, line--, mm.deserialize(status + " <white>" + p.getName() + " <gray>🔍" + detectors));

            if (line <= 3) break;
        }

        setLine(objective, line--, Component.text("  "));

        EventManager em = plugin.getEventManager();
        GameEvent activeEvent = em.getActiveEvent();

        if (activeEvent != null && activeEvent.isActive()) {
            setLine(objective, line--, mm.deserialize("<light_purple><bold>⚡ Ивент:</bold></light_purple>"));
            setLine(objective, line--, mm.deserialize("<white>" + activeEvent.getDisplayName()));

            long remaining = activeEvent.getRemainingSeconds();
            String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
            setLine(objective, line--, mm.deserialize("<gray>Осталось: <yellow>" + timeStr));

            String reward = activeEvent.getRewardDescription();
            if (reward != null && !reward.isEmpty()) setLine(objective, line--, mm.deserialize("<gray>Награда: <green>" + reward));
        } else {
            long nextIn = em.getSecondsUntilNextEvent();
            if (nextIn > 0) setLine(objective, line--, mm.deserialize("<gray>Ивент через: <yellow>" + String.format("%d:%02d", nextIn / 60, nextIn % 60)));
        }

        setLine(objective, line--, Component.text("   "));
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd != null) setLine(objective, line--, mm.deserialize("<gray>Проверок: <white>" + pd.getChecksUsed()));

        player.setScoreboard(scoreboard);
    }

    public void clearAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void setLine(Objective objective, int score, Component text) {
        String entry = generateUniqueEntry(score);
        Scoreboard scoreboard = objective.getScoreboard();
        if (scoreboard != null) {
            Team team = objective.getScoreboard().registerNewTeam("line_" + score);
            team.addEntry(entry);
            team.prefix(text);
            objective.getScore(entry).setScore(score);
        }
    }

    private String generateUniqueEntry(int line) {
        return "§r".repeat(Math.max(0, line)) + "§" + Integer.toHexString(line % 16);
    }

    private int countDetectors(Player player) {
        int count = 0;
        for (var item : player.getInventory().getContents())
            if (item != null && plugin.getGameManager().isDetector(item)) count += item.getAmount();
        return count;
    }
}