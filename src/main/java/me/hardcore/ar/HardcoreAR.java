package me.hardcore.ar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public class HardcoreAR extends JavaPlugin {

    private StatsService statsService;
    private HudScoreboard hudScoreboard;
    private BukkitTask progressTask;
    private BukkitTask saveTask;
    private boolean isResetting = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        statsService = new StatsService(this);
        hudScoreboard = new HudScoreboard(this, statsService);

        getServer().getPluginManager().registerEvents(
            new StatsListener(this, statsService, hudScoreboard), this
        );

        hudScoreboard.start();

        progressTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            hudScoreboard.updateProgress();
        }, 100L, 100L);

        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            statsService.saveAll();
        }, 6000L, 6000L);

        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsService.PlayerStats stats = statsService.getStats(player);
            if (stats.isHudEnabled()) {
                hudScoreboard.createBoard(player);
            }
        }

        getLogger().info("HardcoreAR 1HP Challenge запущен!");
    }

    @Override
    public void onDisable() {
        if (progressTask != null) progressTask.cancel();
        if (saveTask != null) saveTask.cancel();
        hudScoreboard.stop();

        // Не сохраняем если идёт reset
        if (!isResetting) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                StatsService.PlayerStats stats = statsService.getStats(player);
                stats.updatePlayTime();
                hudScoreboard.removeBoard(player);
            }
            statsService.saveAll();
        }

        getLogger().info("HardcoreAR выключен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        // /stats
        if (cmd.equals("stats")) {
            return handleStatsCommand(sender, args);
        }

        // /reset
        if (cmd.equals("reset")) {
            return handleResetCommand(sender);
        }
        
        // /1xp - помощь
        if (cmd.equals("1xp")) {
            return handleHelpCommand(sender);
        }
        
        // /dead - телепорт на место смерти
        if (cmd.equals("dead")) {
            return handleDeadCommand(sender);
        }
        
        // /rs - сброс статистики игрока
        if (cmd.equals("rs")) {
            return handleRsCommand(sender);
        }
        
        // /quest - описание заданий
        if (cmd.equals("quest")) {
            return handleQuestCommand(sender);
        }

        return false;
    }
    
    private boolean handleQuestCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("en", "only_players"));
            return true;
        }
        
        boolean ru = player.getLocale().toLowerCase().startsWith("ru");
        
        player.sendMessage("§6§l═══ " + (ru ? "ЗАДАНИЯ" : "QUESTS") + " §6§l═══");
        player.sendMessage("");
        player.sendMessage("§e1. " + (ru ? "Деревня" : "Village"));
        player.sendMessage("   §7" + (ru ? "Найди деревню и поторгуй с жителем" : "Find a village and trade with a villager"));
        player.sendMessage("");
        player.sendMessage("§e2. " + (ru ? "Незер" : "Nether"));
        player.sendMessage("   §7" + (ru ? "Построй портал и зайди в Незер" : "Build a portal and enter the Nether"));
        player.sendMessage("");
        player.sendMessage("§e3. " + (ru ? "Стержни" : "Blaze Rods"));
        player.sendMessage("   §7" + (ru ? "Убей ифрита и получи огненный стержень" : "Kill a Blaze and get a Blaze Rod"));
        player.sendMessage("");
        player.sendMessage("§e4. " + (ru ? "Жемчуг" : "Ender Pearls"));
        player.sendMessage("   §7" + (ru ? "Убей эндермена и получи жемчуг эндера" : "Kill an Enderman and get an Ender Pearl"));
        player.sendMessage("");
        player.sendMessage("§e5. " + (ru ? "Око эндера" : "Eye of Ender"));
        player.sendMessage("   §7" + (ru ? "Скрафти око эндера из стержня и жемчуга" : "Craft an Eye of Ender from rod and pearl"));
        player.sendMessage("");
        player.sendMessage("§e6. " + (ru ? "Посетил Энд" : "Visited End"));
        player.sendMessage("   §7" + (ru ? "Активируй портал и зайди в Энд" : "Activate the portal and enter the End"));
        player.sendMessage("");
        player.sendMessage("§e7. " + (ru ? "Убил дракона" : "Killed Dragon"));
        player.sendMessage("   §7" + (ru ? "Победи Эндер Дракона!" : "Defeat the Ender Dragon!"));
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════");
        
        return true;
    }
    
    private boolean handleRsCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("en", "only_players"));
            return true;
        }
        
        StatsService.PlayerStats stats = statsService.getStats(player);
        
        // Сбрасываем всё
        stats.setAttempts(0);
        stats.setPlayTimeSeconds(0);
        stats.resetSessionTime();
        stats.setProgress(0);
        stats.setLastProgress(0);
        stats.setFoundVillage(false);
        stats.setVisitedNether(false);
        stats.setGotBlazeRods(false);
        stats.setGotPearls(false);
        stats.setNetherComplete(false);
        stats.setGotArrows(false);
        stats.setFoundStronghold(false);
        stats.setVisitedEnd(false);
        stats.setKilledDragon(false);
        
        statsService.saveStats(player.getUniqueId());
        
        // Обновляем скорборд
        hudScoreboard.removeBoard(player);
        hudScoreboard.createBoard(player);
        
        player.sendMessage(Lang.get(player, "stats_reset"));
        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Lang.get("en", "only_players"));
                return true;
            }

            hudScoreboard.toggleBoard(player);
            StatsService.PlayerStats stats = statsService.getStats(player);
            String message = stats.isHudEnabled() ? Lang.get(player, "hud_on") : Lang.get(player, "hud_off");
            player.sendMessage(message);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("hardcorear.admin")) {
                if (sender instanceof Player p) {
                    sender.sendMessage(Lang.get(p, "no_permission"));
                } else {
                    sender.sendMessage(Lang.get("en", "no_permission"));
                }
                return true;
            }
            reloadConfig();
            if (sender instanceof Player p) {
                sender.sendMessage(Lang.get(p, "config_reloaded"));
            } else {
                sender.sendMessage(Lang.get("en", "config_reloaded"));
            }
            return true;
        }

        return true;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            player.sendMessage(Lang.get(player, "help_header"));
            player.sendMessage(Lang.get(player, "help_stats"));
            player.sendMessage(Lang.get(player, "help_stats_reload"));
            player.sendMessage(Lang.get(player, "help_rs"));
            player.sendMessage(Lang.get(player, "help_dead"));
            player.sendMessage(Lang.get(player, "help_quest"));
            player.sendMessage(Lang.get(player, "help_reset"));
            player.sendMessage(Lang.get(player, "help_1xp"));
        } else {
            sender.sendMessage(Lang.get("en", "help_header"));
            sender.sendMessage(Lang.get("en", "help_stats"));
            sender.sendMessage(Lang.get("en", "help_stats_reload"));
            sender.sendMessage(Lang.get("en", "help_rs"));
            sender.sendMessage(Lang.get("en", "help_dead"));
            sender.sendMessage(Lang.get("en", "help_quest"));
            sender.sendMessage(Lang.get("en", "help_reset"));
            sender.sendMessage(Lang.get("en", "help_1xp"));
        }
        return true;
    }
    
    private boolean handleDeadCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("en", "only_players"));
            return true;
        }
        
        StatsService.PlayerStats stats = statsService.getStats(player);
        
        // Если в спектаторе — возвращаем в survival на спавн
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(Lang.get(player, "returned_to_game"));
            return true;
        }
        
        // Если не в спектаторе — телепортируем на место смерти
        if (!stats.hasDeathLocation()) {
            player.sendMessage(Lang.get(player, "no_death_location"));
            return true;
        }
        
        World world = Bukkit.getWorld(stats.getDeathWorld());
        if (world == null) {
            player.sendMessage(Lang.get(player, "world_not_found"));
            return true;
        }
        
        Location deathLoc = new Location(world, stats.getDeathX(), stats.getDeathY(), stats.getDeathZ());
        player.teleport(deathLoc);
        player.sendMessage(Lang.get(player, "teleported_to_death"));
        return true;
    }

    private boolean handleResetCommand(CommandSender sender) {
        isResetting = true;
        Bukkit.broadcastMessage(colorize("&c&l⚠ СЕРВЕР ПЕРЕЗАПУСКАЕТСЯ! Мир будет пересоздан..."));

        // Очищаем кэш статистики (чтобы не сохранилось обратно)
        statsService.clearCache();

        // Кикаем всех игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(colorize("&cСервер перезапускается!\n&eМир пересоздаётся..."));
        }

        // Удаляем миры и данные
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Удаляем папки миров
            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                Bukkit.unloadWorld(world, false);
                deleteFolder(new File(Bukkit.getWorldContainer(), worldName));
            }
            
            // Удаляем данные игроков плагина
            File playerData = new File(getDataFolder(), "playerdata");
            if (playerData.exists()) {
                deleteFolder(playerData);
            }

            getLogger().info("Миры и данные удалены. Перезапуск сервера...");
            
            // Перезапуск сервера
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.shutdown();
            }, 20L);
            
        }, 40L);

        return true;
    }

    private void deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            folder.delete();
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public StatsService getStatsService() {
        return statsService;
    }
}
