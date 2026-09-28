package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class FreezeTagEvent extends GameEvent {

    private final Random random = new Random();
    private UUID frozenPlayer = null;
    private long frozenUntil = 0;

    public FreezeTagEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        Player frozen = frozenPlayer != null ? Bukkit.getPlayer(frozenPlayer) : null;
        if (frozen != null && System.currentTimeMillis() < frozenUntil) return "🧊 Заморозка (" + frozen.getName() + " заморожен!)";
        return "🧊 Случайная заморозка";
    }

    @Override
    public int getDurationSeconds() {
        return 75;
    }

    @Override
    public String getRewardDescription() {
        return "+1 Детектор каждому";
    }

    @Override
    protected void onStart() {
        broadcast("<aqua>🧊 Каждые 15 секунд случайного игрока заморозит!");
        broadcast("<gray>Замороженный не может двигаться 5 секунд.");

        session.getArenaWorld().setTime(18000);
    }

    @Override
    protected void onEnd() {
        for (Player p : getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            p.setFreezeTicks(0);
        }

        session.getArenaWorld().setTime(6000); // день

        broadcast("<green>Все выжили! +1 детектор каждому!");
        for (Player p : getOnlinePlayers()) {
            giveDetectors(p, 1);
            p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
        }
    }

    @Override
    protected void onForceEnd() {
        for (Player p : getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            p.setFreezeTicks(0);
        }
        session.getArenaWorld().setTime(6000);
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (frozenPlayer != null && System.currentTimeMillis() >= frozenUntil) {
            Player p = Bukkit.getPlayer(frozenPlayer);
            if (p != null && p.isOnline()) {
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                p.setFreezeTicks(0);
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
                broadcast("<aqua>" + p.getName() + " разморожен!");
            }
            frozenPlayer = null;
        }

        if (elapsedSeconds > 0 && elapsedSeconds % 15 == 0) freezeRandomPlayer();

        if (frozenPlayer != null) {
            Player p = Bukkit.getPlayer(frozenPlayer);
            if (p != null && p.isOnline()) {
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);
                p.setFreezeTicks(p.getMaxFreezeTicks());
            }
        }
    }

    private void freezeRandomPlayer() {
        List<Player> players = getOnlinePlayers();
        if (players.isEmpty()) return;

        Player target = players.get(random.nextInt(players.size()));
        frozenPlayer = target.getUniqueId();
        frozenUntil = System.currentTimeMillis() + 5000; // 5 секунд

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 255, false, false, true));
        target.setFreezeTicks(target.getMaxFreezeTicks());

        target.playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.5f);
        broadcast("<aqua>🧊 " + target.getName() + " заморожен на 5 секунд!");
    }
}