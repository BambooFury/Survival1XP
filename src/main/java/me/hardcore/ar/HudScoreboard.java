package me.hardcore.ar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HudScoreboard {

    private final HardcoreAR plugin;
    private final StatsService statsService;
    private final Map<UUID, PlayerBoard> playerBoards = new HashMap<>();
    private BukkitTask updateTask;
    private int animFrame = 0;

    private static final String CHECK = "§a■";
    private static final String CROSS = "§8■";

    public HudScoreboard(HardcoreAR plugin, StatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("update-interval", 20);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            animFrame++;
            updateAll();
        }, 20L, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        playerBoards.clear();
    }

    public void createBoard(Player player) {
        StatsService.PlayerStats stats = statsService.getStats(player);
        if (!stats.isHudEnabled()) return;

        PlayerBoard pb = new PlayerBoard(player);
        playerBoards.put(player.getUniqueId(), pb);
        updateBoard(player, pb, stats);
    }

    public void removeBoard(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void toggleBoard(Player player) {
        StatsService.PlayerStats stats = statsService.getStats(player);
        stats.toggleHud();

        if (stats.isHudEnabled()) {
            createBoard(player);
        } else {
            removeBoard(player);
        }
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBoard pb = playerBoards.get(player.getUniqueId());
            if (pb == null) continue;

            StatsService.PlayerStats stats = statsService.getStats(player);
            if (!stats.isHudEnabled()) continue;

            updateBoard(player, pb, stats);
        }
    }
    
    private String getAnimatedTitle() {
        String[] frames = {
            "§x§F§F§6§B§0§0§lS§x§F§F§8§5§0§0§lu§x§F§F§9§F§0§0§lr§x§F§F§B§9§0§0§lv§x§F§F§D§3§0§0§li§x§F§F§E§D§0§0§lv§x§E§D§F§F§0§0§la§x§D§3§F§F§0§0§ll §x§F§F§0§0§0§0§l1§x§F§F§3§3§0§0§lX§x§F§F§6§6§0§0§lP",
            "§x§F§F§8§5§0§0§lS§x§F§F§9§F§0§0§lu§x§F§F§B§9§0§0§lr§x§F§F§D§3§0§0§lv§x§F§F§E§D§0§0§li§x§E§D§F§F§0§0§lv§x§D§3§F§F§0§0§la§x§B§9§F§F§0§0§ll §x§F§F§3§3§0§0§l1§x§F§F§6§6§0§0§lX§x§F§F§9§9§0§0§lP",
            "§x§F§F§9§F§0§0§lS§x§F§F§B§9§0§0§lu§x§F§F§D§3§0§0§lr§x§F§F§E§D§0§0§lv§x§E§D§F§F§0§0§li§x§D§3§F§F§0§0§lv§x§B§9§F§F§0§0§la§x§9§F§F§F§0§0§ll §x§F§F§6§6§0§0§l1§x§F§F§9§9§0§0§lX§x§F§F§C§C§0§0§lP"
        };
        return frames[animFrame % frames.length];
    }

    private void updateBoard(Player player, PlayerBoard pb, StatsService.PlayerStats stats) {
        boolean ru = player.getLocale().toLowerCase().startsWith("ru");
        
        // Прогресс бар
        int progress = (int) Math.min(stats.getProgress(), 100);
        String progressBar = createProgressBar(progress);
        
        pb.setLine(15, "§8« " + (ru ? "§e§lПопытка §c#" : "§e§lAttempt §c#") + stats.getAttempts() + " §8»");
        pb.setLine(14, "  §7⏱ §f" + TimeUtils.formatTime(stats.getTotalPlayTime()));
        pb.setLine(13, "");
        pb.setLine(12, "§8▪ " + (ru ? "§f§lПРОГРЕСС" : "§f§lPROGRESS") + " §8▪");
        pb.setLine(11, "  " + progressBar + " §f" + progress + "%");
        pb.setLine(10, "");
        pb.setLine(9, "§8▪ " + (ru ? "§f§lЗАДАНИЯ" : "§f§lTASKS") + " §8▪");
        pb.setLine(8, "  " + getTaskLine(stats.hasFoundVillage(), ru ? "Деревня" : "Village"));
        pb.setLine(7, "  " + getTaskLine(stats.isVisitedNether(), ru ? "Незер" : "Nether"));
        pb.setLine(6, "  " + getTaskLine(stats.hasGotBlazeRods(), ru ? "Стержни" : "Rods"));
        pb.setLine(5, "  " + getTaskLine(stats.hasGotPearls(), ru ? "Жемчуг" : "Pearls"));
        pb.setLine(4, "  " + getTaskLine(stats.isNetherComplete(), ru ? "Око эндера" : "Ender Eye"));
        pb.setLine(3, "  " + getTaskLine(stats.isVisitedEnd(), ru ? "Посетил Энд" : "Visited End"));
        pb.setLine(2, "  " + getTaskLine(stats.hasKilledDragon(), ru ? "Убил дракона" : "Killed Dragon"));
        pb.setLine(1, "§5by BambooFury");
        pb.setLine(0, "");
    }
    
    private String createProgressBar(int percent) {
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§8█");
            }
        }
        return bar.toString();
    }
    
    private String getTaskLine(boolean done, String name) {
        if (done) {
            return "§a✓ §7" + name;
        } else {
            return "§8○ §8" + name;
        }
    }

    private String formatProgress(double progress) {
        return String.format("%.0f", Math.min(progress, 100.0));
    }

    public void updateProgress() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsService.PlayerStats stats = statsService.getStats(player);
            int completed = stats.getCompletedCount();
            double progress = (completed / 7.0) * 100.0;
            stats.setProgress(progress);
        }
    }

    private class PlayerBoard {
        private final Scoreboard board;
        private final Objective objective;
        private final Map<Integer, Team> teams = new HashMap<>();
        private final Map<Integer, String> entries = new HashMap<>();

        public PlayerBoard(Player player) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = board.registerNewObjective("hud", Criteria.DUMMY, "§6§lSurvival§c§l1XP");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            
            try {
                objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
            } catch (Exception ignored) {}

            for (int i = 0; i <= 15; i++) {
                String entry = getUniqueEntry(i);
                entries.put(i, entry);
                
                Team team = board.registerNewTeam("line" + i);
                team.addEntry(entry);
                teams.put(i, team);
                
                objective.getScore(entry).setScore(i);
            }

            player.setScoreboard(board);
        }

        public void setLine(int line, String text) {
            Team team = teams.get(line);
            if (team == null) return;
            
            if (text.length() > 64) {
                text = text.substring(0, 64);
            }
            team.setPrefix(text);
        }

        private String getUniqueEntry(int i) {
            return ChatColor.values()[i % 16].toString() + ChatColor.RESET;
        }
    }
}
