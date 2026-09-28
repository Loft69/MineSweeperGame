package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemDeliveryEvent extends GameEvent {

    private Material requiredMaterial;
    private int requiredAmount;
    private final Set<UUID> completedPlayers = new HashSet<>();

    private static final Material[] POSSIBLE_ITEMS = {
            Material.COBBLESTONE, Material.DIRT, Material.OAK_LOG,
            Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.SAND,
            Material.GRAVEL, Material.COAL, Material.RAW_IRON,
            Material.RAW_COPPER, Material.WHEAT_SEEDS, Material.DANDELION,
            Material.POPPY, Material.SUGAR_CANE, Material.CACTUS,
            Material.CLAY_BALL, Material.KELP, Material.BAMBOO,
            Material.APPLE, Material.STICK, Material.FLINT,
            Material.FEATHER, Material.ROTTEN_FLESH, Material.BONE,
            Material.STRING, Material.GUNPOWDER, Material.SPIDER_EYE
    };

    private final Random random = new Random();

    public ItemDeliveryEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        String itemName = requiredMaterial != null ? formatName(requiredMaterial.name()) : "???";
        return "📦 Доставка: " + itemName + " x" + requiredAmount;
    }

    @Override
    public int getDurationSeconds() {
        return 120;
    }

    @Override
    public String getRewardDescription() {
        return "+2 Детектора";
    }

    @Override
    protected void onStart() {
        requiredMaterial = POSSIBLE_ITEMS[random.nextInt(POSSIBLE_ITEMS.length)];
        requiredAmount = 8 + random.nextInt(25); // 8-32

        String itemName = formatName(requiredMaterial.name());

        broadcast("<yellow>Соберите и выбросьте (Q) <white>" + requiredAmount + "x " + itemName + "</white> чтобы получить детекторы!");
        broadcast("<gray>Каждый игрок может сдать индивидуально.");
    }

    @Override
    protected void onEnd() {
        int completed = completedPlayers.size();
        broadcast("<gray>Ивент завершён. Выполнили: <white>" + completed + "</white> игроков.");
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (elapsedSeconds % 30 == 0 && elapsedSeconds > 0) {
            String itemName = formatName(requiredMaterial.name());
            broadcast("<gray>Напоминание: нужно <white>" + requiredAmount + "x " + itemName + "</white>. Выбросьте (Q) чтобы сдать.");
        }
    }

    @EventHandler
    public void onPlayerDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!isActive()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!session.getPlayers().containsKey(uuid)) return;
        if (completedPlayers.contains(uuid)) {
            player.sendMessage(mm.deserialize("<yellow>Вы уже сдали предмет в этом ивенте!"));
            return;
        }

        ItemStack dropped = event.getItemDrop().getItemStack();

        if (dropped.getType() != requiredMaterial) return;

        if (dropped.getAmount() >= requiredAmount) {
            event.getItemDrop().remove();

            int excess = dropped.getAmount() - requiredAmount;
            if (excess > 0) {
                ItemStack returning = new ItemStack(requiredMaterial, excess);
                player.getInventory().addItem(returning);
            }

            completedPlayers.add(uuid);

            giveDetectors(player, 2);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            broadcast("<green>" + player.getName() + " сдал предмет и получил +2 детектора!");
        } else
            player.sendMessage(mm.deserialize("<red>Нужно <white>" + requiredAmount + "</white>, а вы выбросили <white>" + dropped.getAmount() + "</white>. Выбросьте нужное кол-во за раз!"));
    }

    private String formatName(String name) {
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}