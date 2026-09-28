package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class HotPotatoEvent extends GameEvent {

    private UUID potatoHolder = null;
    private final String POTATO_KEY = "hot_potato";
    private final Random random = new Random();

    public HotPotatoEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        Player holder = potatoHolder != null ? Bukkit.getPlayer(potatoHolder) : null;
        String holderName = holder != null ? holder.getName() : "???";
        return "🥔 Горячая картошка (" + holderName + ")";
    }

    @Override
    public int getDurationSeconds() {
        return 45;
    }

    @Override
    public String getRewardDescription() {
        return "+2 Детектора (всем кроме держателя)";
    }

    @Override
    protected void onStart() {
        List<Player> players = getOnlinePlayers();
        if (players.isEmpty()) {
            end();
            return;
        }

        Player chosen = players.get(random.nextInt(players.size()));
        potatoHolder = chosen.getUniqueId();

        givePotato(chosen);

        broadcast("<gold>🥔 " + chosen.getName() + " получил ГОРЯЧУЮ КАРТОШКУ!");
        broadcast("<gray>Ударьте другого игрока (ЛКМ) чтобы передать!");
        broadcast("<gray>У кого картошка в конце — получит <red>урон</red>!");
    }

    @Override
    protected void onEnd() {
        Player holder = potatoHolder != null ? Bukkit.getPlayer(potatoHolder) : null;

        if (holder != null && holder.isOnline()) {
            holder.damage(10.0);
            holder.playSound(holder.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            holder.getWorld().spawnParticle(Particle.EXPLOSION, holder.getLocation(), 5);
            broadcast("<red>💥 " + holder.getName() + " не избавился от картошки!");

            removePotato(holder);
        }

        for (Player p : getOnlinePlayers()) {
            if (potatoHolder != null && p.getUniqueId().equals(potatoHolder)) continue;
            giveDetectors(p, 2);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        broadcast("<green>Остальные получили +2 детектора!");
    }

    @Override
    protected void onForceEnd() {
        if (potatoHolder != null) {
            Player holder = Bukkit.getPlayer(potatoHolder);
            if (holder != null) removePotato(holder);
        }
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        Player holder = potatoHolder != null ? Bukkit.getPlayer(potatoHolder) : null;
        if (holder == null || !holder.isOnline()) {
            List<Player> players = getOnlinePlayers();
            if (!players.isEmpty()) {
                Player next = players.get(random.nextInt(players.size()));
                transferPotato(holder, next);
            }
            return;
        }

        holder.getWorld().spawnParticle(Particle.FLAME, holder.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);

        if (elapsedSeconds % 5 == 0) holder.playSound(holder.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

        long remaining = getDurationSeconds() - elapsedSeconds;
        if (remaining <= 10 && remaining > 0) broadcast("<red>🥔 " + remaining + "...");
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!isActive()) return;

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (!attacker.getUniqueId().equals(potatoHolder)) return;
        if (!session.getPlayers().containsKey(victim.getUniqueId())) return;

        event.setCancelled(true);

        transferPotato(attacker, victim);
    }

    private void transferPotato(Player from, Player to) {
        if (from != null) removePotato(from);
        potatoHolder = to.getUniqueId();
        givePotato(to);

        broadcast("<gold>🥔 " + to.getName() + " теперь держит картошку!");
        to.playSound(to.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.5f);
    }

    private void givePotato(Player player) {
        ItemStack potato = new ItemStack(Material.MAGMA_CREAM, 1);
        ItemMeta meta = potato.getItemMeta();
        meta.displayName(mm.deserialize("<red><bold>🥔 ГОРЯЧАЯ КАРТОШКА</bold></red>"));
        meta.lore(List.of(mm.deserialize("<gray>Ударь другого игрока чтобы передать!"), mm.deserialize("<red>В конце таймера — ВЗРЫВ!")));
        var key = new NamespacedKey(plugin, POTATO_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
        potato.setItemMeta(meta);

        player.getInventory().setItemInOffHand(potato);
    }

    private void removePotato(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.hasItemMeta()) {
            var key = new NamespacedKey(plugin, POTATO_KEY);
            if (offHand.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) player.getInventory().setItemInOffHand(null);
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.MAGMA_CREAM && item.hasItemMeta()) {
                var key = new NamespacedKey(plugin, POTATO_KEY);
                if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) player.getInventory().setItem(i, null);
            }
        }
    }
}