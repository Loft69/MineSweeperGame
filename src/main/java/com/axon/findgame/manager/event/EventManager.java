package com.axon.findgame.manager.event;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.events.*;
import com.axon.findgame.model.GameSession;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private final FindGame plugin;
    private final List<GameEventType> registeredTypes = new ArrayList<>();
    private final Random random = new Random();

    @Getter
    private GameEvent activeEvent = null;
    private BukkitTask schedulerTask = null;
    private long nextEventTime = 0;

    public EventManager(FindGame plugin) {
        this.plugin = plugin;
        registerDefaultEvents();
    }

    private void registerDefaultEvents() {
        // Обычные ивенты (частые)
        registerEventType(new GameEventType(
                "item_delivery",
                "📦 Доставка предметов",
                "Сдайте нужный предмет за награду!",
                5,
                () -> new ItemDeliveryEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "treasure_hunt",
                "🗺️ Охота за сокровищами",
                "Найдите сундуки с лутом!",
                4,
                () -> new TreasureHuntEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "quiz",
                "🧠 Викторина",
                "Ответь на вопросы о Minecraft!",
                5,
                () -> new QuizEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "supply_drop",
                "🪂 Сброс припасов",
                "Добеги до точки сброса!",
                4,
                () -> new SupplyDropEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "freeze_tag",
                "🧊 Случайная заморозка",
                "Случайного игрока замораживает!",
                4,
                () -> new FreezeTagEvent(plugin)
        ));

        // Средней редкости
        registerEventType(new GameEventType(
                "speed_race",
                "⚡ Скоростной забег",
                "Добеги до контрольной точки!",
                3,
                () -> new SpeedBoostRaceEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "blindness",
                "🌑 Слепое испытание",
                "Бесплатные проверки, но вы слепы!",
                3,
                () -> new BlindnessLabyrinthEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "hot_potato",
                "🥔 Горячая картошка",
                "Передай картошку другому!",
                3,
                () -> new HotPotatoEvent(plugin)
        ));

        // Сложные ивенты (редкие)
        registerEventType(new GameEventType(
                "meteor_shower",
                "☄️ Метеоритный дождь",
                "Уворачивайтесь от метеоритов!",
                2,
                () -> new MeteorShowerEvent(plugin)
        ));

        registerEventType(new GameEventType(
                "zombie_apocalypse",
                "🧟 Зомби-апокалипсис",
                "Выживите в волнах зомби!",
                1,
                () -> new ZombieApocalypseEvent(plugin)
        ));
    }

    public void registerEventType(GameEventType type) {
        registeredTypes.add(type);
        plugin.getLogger().info("Зарегистрирован тип ивента: " + type.id());
    }

    public void startScheduler(GameSession session) {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) return;

        int firstDelay = plugin.getConfig().getInt("events.first-event-delay", 120);
        nextEventTime = System.currentTimeMillis() + (firstDelay * 1000L);

        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (session == null || !session.isActive()) {
                stopScheduler();
                return;
            }

            if (activeEvent != null && activeEvent.isActive()) return;
            if (activeEvent != null) activeEvent = null;

            if (System.currentTimeMillis() >= nextEventTime) {
                startRandomEvent(session);
                scheduleNextEvent();
            }

        }, 20L, 20L);
    }

    public void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        if (activeEvent != null) {
            activeEvent.forceEnd();
            activeEvent = null;
        }
    }

    public void startRandomEvent(GameSession session) {
        if (registeredTypes.isEmpty()) return;

        GameEventType chosen = pickWeightedRandom();
        if (chosen == null) return;

        activeEvent = chosen.factory().get();
        activeEvent.start(session);

        plugin.getLogger().info("[EVENT] Запущен ивент: " + chosen.displayName());
    }

    public void startEvent(@NonNull GameSession session, @NonNull GameEventType eventType) {
        activeEvent = eventType.factory().get();
        activeEvent.start(session);

        plugin.getLogger().info("[EVENT] Принудительно запущен ивент: " + eventType.displayName());
    }

    private GameEventType pickWeightedRandom() {
        int totalWeight = 0;
        for (GameEventType type : registeredTypes) {
            totalWeight += type.weight();
        }

        if (totalWeight <= 0) return registeredTypes.getFirst();

        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (GameEventType type : registeredTypes) {
            current += type.weight();
            if (roll < current) return type;
        }

        return registeredTypes.getLast();
    }

    private void scheduleNextEvent() {
        int minInterval = plugin.getConfig().getInt("events.min-interval", 180);
        int maxInterval = plugin.getConfig().getInt("events.max-interval", 420);
        int interval = minInterval + random.nextInt(maxInterval - minInterval + 1);
        nextEventTime = System.currentTimeMillis() + (interval * 1000L);
    }

    public long getSecondsUntilNextEvent() {
        if (nextEventTime <= 0) return 0;
        long remaining = (nextEventTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public List<GameEventType> getRegisteredTypes() {
        return Collections.unmodifiableList(registeredTypes);
    }

    public GameEventType getRegisteredType(String id) {
        Optional<GameEventType> optEvent = getRegisteredTypes().stream().filter(event -> event.id().equalsIgnoreCase(id)).findFirst();
        return optEvent.orElse(null);
    }

    public List<String> getRegisteredTypesId() {
        return getRegisteredTypes().stream().map(GameEventType::id).toList();
    }
}