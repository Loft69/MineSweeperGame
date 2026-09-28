package com.axon.findgame.command;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.GameManager;
import com.axon.findgame.manager.event.EventManager;
import com.axon.findgame.manager.event.GameEvent;
import com.axon.findgame.manager.event.GameEventType;
import com.axon.findgame.model.GameSession;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FindGameCommand implements CommandExecutor, TabCompleter {

    private final FindGame plugin;
    private final MiniMessage mm;

    public FindGameCommand(FindGame plugin) {
        this.plugin = plugin;
        this.mm = plugin.mm();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!sender.hasPermission("findgame.admin")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                if (plugin.getGameManager().isGameActive()) {
                    sender.sendMessage(plugin.msg("already-running"));
                    return true;
                }
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    sender.sendMessage(plugin.msg("no-players"));
                    return true;
                }
                plugin.getGameManager().startGame();
            }
            case "stop" -> {
                if (!sender.hasPermission("findgame.admin")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                if (!plugin.getGameManager().isGameActive()) {
                    sender.sendMessage(plugin.msg("not-running"));
                    return true;
                }
                plugin.getGameManager().endGame(false);
                sender.sendMessage(mm.deserialize("<yellow>Игра принудительно остановлена."));
            }
            case "reload" -> {
                if (!sender.hasPermission("findgame.admin")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(plugin.msg("reloaded"));
            }
            case "help" -> sendHelp(sender);
            case "event" -> {
                if (args.length != 2) {
                    return false;
                }

                GameManager gameManager = plugin.getGameManager();
                GameSession gameSession = gameManager.getCurrentSession();
                if (gameSession == null || !gameSession.isActive()) {
                    sender.sendMessage("Игра не запущена");
                    return false;
                }

                String arg = args[1];

                EventManager eventManager = plugin.getEventManager();

                GameEvent gameEvent = eventManager.getActiveEvent();
                if (gameEvent != null && gameEvent.isActive()) {
                    gameEvent.forceEnd();
                }

                GameEventType event = eventManager.getRegisteredType(arg);
                if (event == null) {
                    sender.sendMessage("Такого ивента нет");
                    return false;
                }

                eventManager.startEvent(gameSession, event);
            }
            default -> sender.sendMessage(mm.deserialize("<red>Неизвестная подкоманда! /fg help"));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize("<gradient:gold:red><bold>═══ FindGame v3.0 ═══</bold></gradient>"));
        sender.sendMessage(mm.deserialize("<gold>/fg start</gold> <gray>— Запустить <red>(admin)"));
        sender.sendMessage(mm.deserialize("<gold>/fg stop</gold> <gray>— Остановить <red>(admin)"));
        sender.sendMessage(mm.deserialize("<gold>/fg reload</gold> <gray>— Перезагрузить <red>(admin)"));
        sender.sendMessage(mm.deserialize("<gold>/fg help</gold> <gray>— Справка"));
        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize("<yellow><bold>Как играть:</bold></yellow>"));
        sender.sendMessage(mm.deserialize("<gray>1. Админ запускает <white>/fg start"));
        sender.sendMessage(mm.deserialize("<gray>2. ПКМ детектором — подсказка + направление"));
        sender.sendMessage(mm.deserialize("<gray>3. Крафт детекторов: <white>Redstone+Iron+Diamond"));
        sender.sendMessage(mm.deserialize("<gray>4. Все игроки <white>≤15 блоков</white> друг от друга для проверки"));
        sender.sendMessage(mm.deserialize("<gray>5. Случайные <light_purple>ивенты</light_purple> дают бонусы!"));
        sender.sendMessage(mm.deserialize("<red><bold>⚠</bold> Не ломайте бомбу!"));
        sender.sendMessage(mm.deserialize(""));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("help");
            if (sender.hasPermission("findgame.admin")) subs.addAll(List.of("start", "stop", "reload", "event"));
            for (String sub : subs) if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            EventManager eventManager = plugin.getEventManager();
            List<String> ids = eventManager.getRegisteredTypesId();
            for (String id : ids) if (id.startsWith(args[1].toLowerCase())) completions.add(id);
        }
        return completions;
    }
}