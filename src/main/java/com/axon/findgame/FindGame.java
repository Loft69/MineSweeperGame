package com.axon.findgame;

import com.axon.findgame.command.FindGameCommand;
import com.axon.findgame.listener.CraftListener;
import com.axon.findgame.listener.GameListener;
import com.axon.findgame.listener.PortalListener;
import com.axon.findgame.listener.ProtectionListener;
import com.axon.findgame.manager.*;
import com.axon.findgame.manager.event.EventManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class FindGame extends JavaPlugin {

    private static FindGame instance;
    private GameManager gameManager;
    private WorldManager worldManager;
    private HologramManager hologramManager;
    private ProximityManager proximityManager;
    private ScoreboardManager scoreboardManager;
    private EventManager eventManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.worldManager = new WorldManager(this);
        this.hologramManager = new HologramManager(this);
        this.proximityManager = new ProximityManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.eventManager = new EventManager(this);
        this.gameManager = new GameManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new GameListener(this), this);
        pm.registerEvents(new CraftListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new PortalListener(this), this);

        var command = getCommand("findgame");
        if (command != null) {
            var fgCommand = new FindGameCommand(this);
            command.setExecutor(fgCommand);
            command.setTabCompleter(fgCommand);
        }

        getLogger().info("FindGame v3.0 загружен!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceEndGame();
        getLogger().info("FindGame v3.0 выключен!");
    }

    public static FindGame getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public ProximityManager getProximityManager() { return proximityManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public EventManager getEventManager() { return eventManager; }
    public MiniMessage mm() { return miniMessage; }

    public Component msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + key, "<red>Missing: " + key);
        return miniMessage.deserialize(prefix + msg);
    }

    public Component msg(String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + key, "<red>Missing: " + key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return miniMessage.deserialize(prefix + msg);
    }

    public Component msgRaw(String key) {
        String msg = getConfig().getString("messages." + key, "<red>Missing: " + key);
        return miniMessage.deserialize(msg);
    }

    public String msgStr(String key) {
        return getConfig().getString("messages." + key, "");
    }
}