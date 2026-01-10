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
    public void onLoad() {
        // Пусто
    }

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
        
        // /winez - секретная команда победы
        if (cmd.equals("winez")) {
            return handleWinezCommand(sender);
        }

        return false;
    }
    
    private boolean handleWinezCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        
        StatsService.PlayerStats stats = statsService.getStats(player);
        
        // Выполняем все задания
        stats.setFoundVillage(true);
        stats.setVisitedNether(true);
        stats.setGotBlazeRods(true);
        stats.setGotPearls(true);
        stats.setNetherComplete(true);
        stats.setVisitedEnd(true);
        stats.setKilledDragon(true);
        
        statsService.saveStats(player.getUniqueId());
        
        // Даём предметы
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 64));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAZE_ROD, 16));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL, 16));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_EYE, 12));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE, 1));
        
        // Обновляем скорборд
        hudScoreboard.removeBoard(player);
        hudScoreboard.createBoard(player);
        
        // Показываем победу
        boolean ru = player.getLocale().toLowerCase().startsWith("ru");
        
        player.sendMessage("");
        player.sendMessage("§6§l✦ §e§l" + (ru ? "ПОЗДРАВЛЯЕМ" : "CONGRATULATIONS") + "§6§l ✦");
        player.sendMessage("§a" + (ru ? "Ты выполнил все задания!" : "You completed all tasks!"));
        player.sendMessage("§7" + (ru ? "Попыток: " : "Attempts: ") + "§c" + stats.getAttempts());
        player.sendMessage("§7" + (ru ? "Время: " : "Time: ") + "§e" + TimeUtils.formatTime(stats.getTotalPlayTime()));
        player.sendMessage("");
        
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        player.sendTitle("§6§l★ " + (ru ? "ПОБЕДА" : "VICTORY") + " §6§l★", 
                       "§a" + (ru ? "Все задания выполнены!" : "All tasks completed!"), 
                       10, 70, 20);
        
        // Падающие алмазы
        spawnDiamondRain(player);
        
        return true;
    }
    
    private void spawnDiamondRain(Player player) {
        org.bukkit.Location loc = player.getLocation().add(0, 2, 0);
        World world = player.getWorld();
        
        for (int wave = 0; wave < 5; wave++) {
            final int w = wave;
            getServer().getScheduler().runTaskLater(this, () -> {
                for (int i = 0; i < 8; i++) {
                    double offsetX = (Math.random() - 0.5) * 2;
                    double offsetZ = (Math.random() - 0.5) * 2;
                    org.bukkit.Location spawnLoc = loc.clone().add(offsetX, Math.random(), offsetZ);
                    
                    org.bukkit.entity.Item item = world.dropItem(spawnLoc, new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND));
                    item.setPickupDelay(Integer.MAX_VALUE);
                    item.setVelocity(new org.bukkit.util.Vector(
                        (Math.random() - 0.5) * 0.3,
                        0.2,
                        (Math.random() - 0.5) * 0.3
                    ));
                    
                    getServer().getScheduler().runTaskLater(this, item::remove, 60L);
                }
                
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }, wave * 10L);
        }
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
        
        // Если в спектаторе — возвращаем в survival на безопасную точку
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            
            // Находим безопасную точку на земле
            Location spawnLoc = player.getWorld().getSpawnLocation();
            Location safeLoc = findSafeLocation(spawnLoc);
            
            player.teleport(safeLoc);
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
        Location safeLoc = findSafeLocation(deathLoc);
        
        player.teleport(safeLoc);
        player.sendMessage(Lang.get(player, "teleported_to_death"));
        return true;
    }
    
    private Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        
        // Находим самый высокий твёрдый блок
        int highestY = world.getHighestBlockYAt(x, z);
        
        // Проверяем что это не воздух и не жидкость
        Location safeLoc = new Location(world, x + 0.5, highestY + 1, z + 0.5);
        
        // Если высота слишком низкая (void), ставим на 64
        if (highestY < 1) {
            safeLoc.setY(65);
        }
        
        return safeLoc;
    }

    private boolean handleResetCommand(CommandSender sender) {
        isResetting = true;
        Bukkit.broadcastMessage(colorize("&c&l⚠ СБРОС МИРА! Сервер перезапускается..."));

        // Удаляем данные игроков плагина (задания сбросятся)
        File playerData = new File(getDataFolder(), "playerdata");
        if (playerData.exists()) {
            deleteFolder(playerData);
        }
        
        // Создаём маркер сброса - при входе игроки получат очистку инвентаря
        try {
            File marker = new File(getDataFolder(), "reset_players");
            getDataFolder().mkdirs();
            marker.createNewFile();
        } catch (Exception e) {
            getLogger().warning("Не удалось создать маркер: " + e.getMessage());
        }

        // Кикаем всех игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(colorize("&cСервер перезапускается!\n&eМир сбрасывается..."));
        }

        // Перезапуск сервера
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.shutdown();
        }, 20L);

        return true;
    }
    
    private void randomizeSeed() {
        try {
            File propsFile = new File(Bukkit.getWorldContainer(), "server.properties");
            if (!propsFile.exists()) return;
            
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(propsFile)) {
                props.load(fis);
            }
            
            // Генерируем новый случайный сид
            long newSeed = new java.util.Random().nextLong();
            props.setProperty("level-seed", String.valueOf(newSeed));
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
                props.store(fos, null);
            }
            
            getLogger().info("Новый сид мира: " + newSeed);
        } catch (Exception e) {
            getLogger().warning("Не удалось изменить сид: " + e.getMessage());
        }
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
    
    private void checkAndDeleteWorlds() {
        File marker = new File(getDataFolder(), "reset_marker");
        if (!marker.exists()) return;
        
        getLogger().info("Обнаружен маркер сброса. Удаляем миры...");
        marker.delete();
        
        // Удаляем папки миров
        File serverDir = Bukkit.getWorldContainer();
        String[] worldFolders = {"world", "world_nether", "world_the_end"};
        
        for (String worldName : worldFolders) {
            File worldFolder = new File(serverDir, worldName);
            if (worldFolder.exists()) {
                deleteFolder(worldFolder);
                getLogger().info("Удалён мир: " + worldName);
            }
        }
        
        // Удаляем данные игроков плагина
        File playerData = new File(getDataFolder(), "playerdata");
        if (playerData.exists()) {
            deleteFolder(playerData);
        }
        
        // Генерируем новый сид
        randomizeSeed();
        
        getLogger().info("Миры удалены. Новый мир будет создан!");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public StatsService getStatsService() {
        return statsService;
    }
}
