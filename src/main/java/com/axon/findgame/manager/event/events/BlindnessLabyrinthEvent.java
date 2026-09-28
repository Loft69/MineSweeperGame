package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlindnessLabyrinthEvent extends GameEvent {

    private boolean freeChecks = false;

    public BlindnessLabyrinthEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        return "🌑 Слепое испытание";
    }

    @Override
    public int getDurationSeconds() {
        return 60;
    }

    @Override
    public String getRewardDescription() {
        return "Бесплатные проверки!";
    }

    @Override
    protected void onStart() {
        freeChecks = true;

        broadcast("<dark_purple>🌑 Тьма окутывает вас...");
        broadcast("<green><bold>Все проверки детектором БЕСПЛАТНЫЕ следующие 60 сек!</bold>");
        broadcast("<gray>Но вы <red>ослеплены</red>. Ориентируйтесь на подсказки!");

        for (Player player : getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, getDurationSeconds() * 20 + 20, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, getDurationSeconds() * 20 + 20, 0, false, false, true));
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }
    }

    @Override
    protected void onEnd() {
        freeChecks = false;

        for (Player player : getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        }

        broadcast("<green>🌑 Зрение восстановлено! Бесплатные проверки закончились.");
    }

    @Override
    protected void onForceEnd() {
        freeChecks = false;

        for (Player player : getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (elapsedSeconds % 8 == 0) {
            for (Player player : getOnlinePlayers()) player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.4f, 0.7f + (float) (Math.random() * 0.6));
        }

        if (elapsedSeconds == 40) broadcast("<yellow>🌑 20 секунд до конца! Проверяйте блоки!");
        if (elapsedSeconds == 50) broadcast("<red>🌑 10 секунд! Последний шанс!");

        long remaining = getDurationSeconds() - elapsedSeconds;
        for (Player p : getOnlinePlayers()) p.sendActionBar(mm.deserialize("<green>🌑 Бесплатные проверки: <white>" + remaining + " сек</white> " + "<dark_gray>| <gray>Проверено: <white>" + (session.getPlayerData(p.getUniqueId()) != null ? session.getPlayerData(p.getUniqueId()).getChecksUsed() : 0)));
    }

    public boolean isFreeChecks() {
        return freeChecks && isActive();
    }
}