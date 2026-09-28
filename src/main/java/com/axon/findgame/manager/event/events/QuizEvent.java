package com.axon.findgame.manager.event.events;

import com.axon.findgame.FindGame;
import com.axon.findgame.manager.event.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class QuizEvent extends GameEvent {

    private int currentQuestion = 0;
    private final int totalQuestions = 5;
    private QuizQuestion activeQuestion = null;
    private boolean questionAnswered = false;
    private long questionStartTime = 0;
    private final Set<UUID> winners = new HashSet<>();

    private static final List<QuizQuestion> QUESTIONS = List.of(
            new QuizQuestion("Сколько блоков обсидиана нужно для портала в Нижний мир?", List.of("10", "десять")),
            new QuizQuestion("Какой моб дропает жемчуг Эндера?", List.of("эндермен", "эндерман", "странник края", "enderman")),
            new QuizQuestion("Сколько XP стоит зачарование на 30 уровне?", List.of("3", "три")),
            new QuizQuestion("Из чего крафтится компас?", List.of("железо и редстоун", "железо редстоун", "iron redstone")),
            new QuizQuestion("Какой блок бесконечно генерирует воду?", List.of("бесконечный источник", "источник воды", "2 ведра воды")),
            new QuizQuestion("Максимальный уровень зачарования Острота?", List.of("5", "пять", "v")),
            new QuizQuestion("Какой моб боится кошек?", List.of("крипер", "creeper")),
            new QuizQuestion("Сколько слитков нужно для полного сета брони?", List.of("24", "двадцать четыре")),
            new QuizQuestion("Какой блок нельзя добыть в выживании?", List.of("бедрок", "коренная порода", "bedrock")),
            new QuizQuestion("Высота мира в 1.21?", List.of("384", "от -64 до 320")),
            new QuizQuestion("Что дропает Визер при смерти?", List.of("звезда незера", "nether star", "звезда нижнего мира")),
            new QuizQuestion("Какой биом самый редкий?", List.of("грибной остров", "mushroom", "грибной", "грибные поля")),
            new QuizQuestion("Сколько здоровья у Эндер Дракона?", List.of("200", "двести")),
            new QuizQuestion("Из чего крафтится ведро?", List.of("железо", "3 железных слитка", "iron")),
            new QuizQuestion("Какой эффект даёт золотое яблоко?", List.of("поглощение", "регенерация", "absorption"))
    );

    public QuizEvent(FindGame plugin) {
        super(plugin);
    }

    @Override
    public String getDisplayName() {
        return "🧠 Викторина (" + currentQuestion + "/" + totalQuestions + ")";
    }

    @Override
    public int getDurationSeconds() {
        return 120;
    }

    @Override
    public String getRewardDescription() {
        return "+3 Детектора за правильный ответ";
    }

    @Override
    protected void onStart() {
        broadcast("<yellow>🧠 Викторина! 5 вопросов о Minecraft!");
        broadcast("<gray>Пишите ответ в чат. Первый правильный ответ — <green>+3 детектора</green>!");

        askNextQuestion();
    }

    @Override
    protected void onEnd() {
        broadcast("<gray>Викторина завершена! Победителей: <white>" + winners.size());
    }

    @Override
    protected void onTick(long elapsedSeconds) {
        if (activeQuestion != null && !questionAnswered) {
            long questionElapsed = (System.currentTimeMillis() - questionStartTime) / 1000;
            if (questionElapsed >= 20) {
                broadcast("<red>Время вышло! Ответ: <white>" + activeQuestion.answers.getFirst());
                questionAnswered = true;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive()) askNextQuestion();
                }, 60L);
            }
        }
    }

    private void askNextQuestion() {
        currentQuestion++;
        if (currentQuestion > totalQuestions) {
            end();
            return;
        }

        List<QuizQuestion> shuffled = new ArrayList<>(QUESTIONS);
        Collections.shuffle(shuffled);
        activeQuestion = shuffled.getFirst();
        questionAnswered = false;
        questionStartTime = System.currentTimeMillis();

        broadcast("<yellow><bold>❓ Вопрос #" + currentQuestion + ":</bold></yellow>");
        broadcast("<white>" + activeQuestion.question);
        broadcast("<gray>Пишите ответ в чат! (20 сек)");

        for (Player p : getOnlinePlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isActive() || activeQuestion == null || questionAnswered) return;

        Player player = event.getPlayer();
        if (!session.getPlayers().containsKey(player.getUniqueId())) return;

        String answer = event.getMessage().trim().toLowerCase();

        boolean correct = false;
        for (String validAnswer : activeQuestion.answers) {
            if (answer.contains(validAnswer.toLowerCase())) {
                correct = true;
                break;
            }
        }

        if (correct) {
            questionAnswered = true;
            event.setCancelled(true);

            winners.add(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                broadcast("<green><bold>✔ " + player.getName() + " ответил правильно!</bold> +3 детектора");
                giveDetectors(player, 3);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive()) askNextQuestion();
                }, 60L);
            });
        }
    }

    private record QuizQuestion(String question, List<String> answers) {}
}