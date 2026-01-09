package me.hardcore.ar;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class Lang {
    
    private static final Map<String, Map<String, String>> messages = new HashMap<>();
    
    static {
        // Русский (по умолчанию)
        Map<String, String> ru = new HashMap<>();
        ru.put("stats_header", "§eСтатистика:");
        ru.put("attempt", "§f▸ Попытка: §c#");
        ru.put("timer", "§f▸ Таймер: §e");
        ru.put("progress", "§f▸ Прогресс: §a");
        ru.put("achievements_header", "§6Достижения:");
        ru.put("visited_nether", "§f▸ Посетил ад: ");
        ru.put("blaze_rods", "§f▸ Стержни: ");
        ru.put("pearls", "§f▸ Жемчуг: ");
        ru.put("nether_complete", "§f▸ Ад пройден: ");
        ru.put("arrows", "§f▸ Стрелы: ");
        ru.put("stronghold", "§f▸ Крепость: ");
        ru.put("visited_end", "§f▸ Посетил Энд: ");
        ru.put("killed_dragon", "§f▸ Убил дракона: ");
        ru.put("death_message", "§c§l☠ Ты умер! §7Напиши §e/dead §7чтобы вернуться в игру.");
        ru.put("returned_to_game", "§a✔ Возвращён в игру!");
        ru.put("no_death_location", "§cУ тебя нет сохранённого места смерти!");
        ru.put("world_not_found", "§cМир не найден!");
        ru.put("teleported_to_death", "§a✔ Телепортирован на место смерти!");
        ru.put("stats_reset", "§a✔ Статистика сброшена!");
        ru.put("hud_on", "§aHUD включен!");
        ru.put("hud_off", "§cHUD выключен!");
        ru.put("only_players", "§cТолько для игроков!");
        ru.put("no_permission", "§cНет прав!");
        ru.put("config_reloaded", "§aКонфиг перезагружен!");
        ru.put("help_header", "§6§l═══ Survival1XP Команды ═══");
        ru.put("help_stats", "§e/stats §7- Вкл/выкл HUD статистики");
        ru.put("help_stats_reload", "§e/stats reload §7- Перезагрузить конфиг");
        ru.put("help_rs", "§e/rs §7- Сбросить свою статистику");
        ru.put("help_dead", "§e/dead §7- Вернуться в игру (из спектатора)");
        ru.put("help_reset", "§e/reset §7- Полный сброс сервера");
        ru.put("help_1xp", "§e/1xp §7- Показать эту справку");
        ru.put("welcome_header", "§6§l═══════════════════════════");
        ru.put("welcome_message", "§e§lДобро пожаловать в Survival1XP!");
        ru.put("welcome_hint", "§7Напиши §e/1xp §7чтобы увидеть все команды");
        ru.put("spectator_only", "§cЭта команда доступна только в режиме спектатора!");
        messages.put("ru", ru);
        
        // Английский
        Map<String, String> en = new HashMap<>();
        en.put("stats_header", "§eStatistics:");
        en.put("attempt", "§f▸ Attempt: §c#");
        en.put("timer", "§f▸ Timer: §e");
        en.put("progress", "§f▸ Progress: §a");
        en.put("achievements_header", "§6Achievements:");
        en.put("visited_nether", "§f▸ Visited Nether: ");
        en.put("blaze_rods", "§f▸ Blaze Rods: ");
        en.put("pearls", "§f▸ Pearls: ");
        en.put("nether_complete", "§f▸ Nether Done: ");
        en.put("arrows", "§f▸ Arrows: ");
        en.put("stronghold", "§f▸ Stronghold: ");
        en.put("visited_end", "§f▸ Visited End: ");
        en.put("killed_dragon", "§f▸ Killed Dragon: ");
        en.put("death_message", "§c§l☠ You died! §7Type §e/dead §7to return to game.");
        en.put("returned_to_game", "§a✔ Returned to game!");
        en.put("no_death_location", "§cYou have no saved death location!");
        en.put("world_not_found", "§cWorld not found!");
        en.put("teleported_to_death", "§a✔ Teleported to death location!");
        en.put("stats_reset", "§a✔ Stats reset!");
        en.put("hud_on", "§aHUD enabled!");
        en.put("hud_off", "§cHUD disabled!");
        en.put("only_players", "§cPlayers only!");
        en.put("no_permission", "§cNo permission!");
        en.put("config_reloaded", "§aConfig reloaded!");
        en.put("help_header", "§6§l═══ Survival1XP Commands ═══");
        en.put("help_stats", "§e/stats §7- Toggle HUD");
        en.put("help_stats_reload", "§e/stats reload §7- Reload config");
        en.put("help_rs", "§e/rs §7- Reset your stats");
        en.put("help_dead", "§e/dead §7- Return to game (from spectator)");
        en.put("help_reset", "§e/reset §7- Full server reset");
        en.put("help_1xp", "§e/1xp §7- Show this help");
        en.put("welcome_header", "§6§l═══════════════════════════");
        en.put("welcome_message", "§e§lWelcome to Survival1XP!");
        en.put("welcome_hint", "§7Type §e/1xp §7to see all commands");
        en.put("spectator_only", "§cThis command is only available in spectator mode!");
        messages.put("en", en);
    }
    
    public static String get(Player player, String key) {
        String locale = player.getLocale().toLowerCase();
        String lang = locale.startsWith("ru") ? "ru" : "en";
        
        Map<String, String> langMap = messages.getOrDefault(lang, messages.get("en"));
        return langMap.getOrDefault(key, key);
    }
    
    public static String get(String locale, String key) {
        String lang = locale.toLowerCase().startsWith("ru") ? "ru" : "en";
        Map<String, String> langMap = messages.getOrDefault(lang, messages.get("en"));
        return langMap.getOrDefault(key, key);
    }
}
