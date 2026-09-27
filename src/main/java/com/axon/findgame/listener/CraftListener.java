package com.axon.findgame.listener;

import com.axon.findgame.FindGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class CraftListener implements Listener {

    private final FindGame plugin;
    private final NamespacedKey recipeKey;

    public CraftListener(FindGame plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "detector_craft");
        registerRecipe();
    }

    private void registerRecipe() {
        int amount = plugin.getConfig().getInt("craft-result-amount", 4);

        ItemStack result = plugin.getGameManager().createDetectorItem();
        result.setAmount(amount);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("RIR", "IDI", "RIR");
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('D', Material.DIAMOND);

        Bukkit.removeRecipe(recipeKey);
        Bukkit.addRecipe(recipe);

        plugin.getLogger().info("Рецепт детектора зарегистрирован (результат: " + amount + " шт.)");
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!plugin.getGameManager().isGameActive() || !plugin.getGameManager().isPlayerInGame(player.getUniqueId())) return;

        ItemStack result = event.getRecipe().getResult();
        if (plugin.getGameManager().isDetector(result)) {
            int amount = result.getAmount();
            player.sendMessage(plugin.msg("detector-crafted", "%amount%", String.valueOf(amount)));
        }
    }
}