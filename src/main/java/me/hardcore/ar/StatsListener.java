package me.hardcore.ar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class StatsListener implements Listener {

    private final HardcoreAR plugin;
    private final StatsService statsService;
    private final HudScoreboard hudScoreboard;

    public StatsListener(HardcoreAR plugin, StatsService statsService, HudScoreboard hudScoreboard) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.hudScoreboard = hudScoreboard;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        StatsService.PlayerStats stats = statsService.getStats(player);
        stats.resetSessionTime();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                setHalfHeart(player);
                if (stats.isHudEnabled()) {
                    hudScoreboard.createBoard(player);
                }
                
                // Показываем подсказку при первом входе
                if (stats.isFirstJoin()) {
                    stats.setFirstJoin(false);
                    statsService.saveStats(player.getUniqueId());
                    
                    // Показываем toast-уведомление
                    plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "advancement grant " + player.getName() + " only survival1xp:welcome"
                    );
                    
                    // Отправляем сообщение в чат
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        boolean ru = player.getLocale().toLowerCase().startsWith("ru");
                        player.sendMessage(Lang.get(player, "welcome_header"));
                        player.sendMessage(Lang.get(player, "welcome_message"));
                        player.sendMessage(Lang.get(player, "welcome_hint"));
                        if (ru) {
                            player.sendMessage("§7Для связи или ошибки — §3T§be§3l§be§3g§br§3a§bm§7: §5@moorya4ok");
                        } else {
                            player.sendMessage("§7For contact or bugs — §3T§be§3l§be§3g§br§3a§bm§7: §5@moorya4ok");
                        }
                        player.sendMessage(Lang.get(player, "welcome_header"));
                    }, 20L);
                }
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        StatsService.PlayerStats stats = statsService.getStats(player);
        stats.updatePlayTime();

        hudScoreboard.removeBoard(player);
        statsService.unloadStats(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        StatsService.PlayerStats stats = statsService.getStats(player);
        stats.incrementAttempts();
        
        // Сохраняем место смерти
        Location deathLoc = player.getLocation();
        stats.setDeathLocation(
            deathLoc.getWorld().getName(),
            deathLoc.getX(),
            deathLoc.getY(),
            deathLoc.getZ()
        );
        
        // НЕ сбрасываем достижения — они сохраняются!
        stats.setProgress(0);
        stats.setLastProgress(0);
        
        statsService.saveStats(player.getUniqueId());
        
        // Автоматически переключаем в спектатор и телепортируем на место смерти
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.spigot().respawn();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                        player.teleport(deathLoc);
                        player.sendMessage(Lang.get(player, "death_message"));
                    }
                }, 1L);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                setHalfHeart(player);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        StatsService.PlayerStats stats = statsService.getStats(player);
        World.Environment env = player.getWorld().getEnvironment();

        if (env == World.Environment.NETHER && !stats.isVisitedNether()) {
            stats.setVisitedNether(true);
            statsService.saveStats(player.getUniqueId());
            checkAllTasksComplete(player, stats);
        } else if (env == World.Environment.THE_END && !stats.isVisitedEnd()) {
            stats.setVisitedEnd(true);
            statsService.saveStats(player.getUniqueId());
            checkAllTasksComplete(player, stats);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        StatsService.PlayerStats stats = statsService.getStats(killer);

        if (entity instanceof EnderDragon) {
            stats.setKilledDragon(true);
            statsService.saveStats(killer.getUniqueId());
            checkAllTasksComplete(killer, stats);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        checkItems(player, item.getType());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Проверяем весь инвентарь при закрытии
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                checkItems(player, item.getType());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Material type = event.getRecipe().getResult().getType();
        if (type == Material.ENDER_EYE) {
            StatsService.PlayerStats stats = statsService.getStats(player);
            if (!stats.isNetherComplete()) {
                stats.setNetherComplete(true);
                statsService.saveStats(player.getUniqueId());
                checkAllTasksComplete(player, stats);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseEnderEye(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.ENDER_EYE) return;
        
        // Око эндера используется - ничего не делаем, задание крепости убрано
    }
    
    // Когда получает достижение "Недреманное око" (Eye Spy) - нашёл крепость
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        String key = event.getAdvancement().getKey().getKey();
        StatsService.PlayerStats stats = statsService.getStats(player);
        
        // adventure/trade = торговля с жителем (нашёл деревню)
        if (key.equals("adventure/trade") || key.equals("adventure/voluntary_exile")) {
            if (!stats.hasFoundVillage()) {
                stats.setFoundVillage(true);
                statsService.saveStats(player.getUniqueId());
                checkAllTasksComplete(player, stats);
            }
        }
    }
    
    private void checkAllTasksComplete(Player player, StatsService.PlayerStats stats) {
        if (stats.getCompletedCount() == 7) {
            boolean ru = player.getLocale().toLowerCase().startsWith("ru");
            
            // Красивое сообщение о победе
            player.sendMessage("");
            player.sendMessage("§6§l✦ §e§l" + (ru ? "ПОЗДРАВЛЯЕМ" : "CONGRATULATIONS") + "§6§l ✦");
            player.sendMessage("§a" + (ru ? "Ты выполнил все задания!" : "You completed all tasks!"));
            player.sendMessage("§7" + (ru ? "Попыток: " : "Attempts: ") + "§c" + stats.getAttempts());
            player.sendMessage("§7" + (ru ? "Время: " : "Time: ") + "§e" + TimeUtils.formatTime(stats.getTotalPlayTime()));
            player.sendMessage("");
            
            // Звук победы
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Фейерверк эффект через title
            player.sendTitle("§6§l★ " + (ru ? "ПОБЕДА" : "VICTORY") + " §6§l★", 
                           "§a" + (ru ? "Все задания выполнены!" : "All tasks completed!"), 
                           10, 70, 20);
        }
    }
    
    private void checkItems(Player player, Material type) {
        StatsService.PlayerStats stats = statsService.getStats(player);
        boolean changed = false;

        if (type == Material.BLAZE_ROD && !stats.hasGotBlazeRods()) {
            stats.setGotBlazeRods(true);
            changed = true;
        }
        
        if (type == Material.ENDER_PEARL && !stats.hasGotPearls()) {
            stats.setGotPearls(true);
            changed = true;
        }
        
        if (type == Material.ARROW && !stats.hasGotArrows()) {
            stats.setGotArrows(true);
            changed = true;
        }
        
        if (type == Material.ENDER_EYE && !stats.isNetherComplete()) {
            stats.setNetherComplete(true);
            changed = true;
        }
        
        if (changed) {
            statsService.saveStats(player.getUniqueId());
            checkAllTasksComplete(player, stats);
        }
    }

    private void setHalfHeart(Player player) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1.0);
        player.setHealth(1.0);
    }
}
