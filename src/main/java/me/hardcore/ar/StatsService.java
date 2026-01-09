package me.hardcore.ar;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsService {

    private final HardcoreAR plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public StatsService(HardcoreAR plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerStats getStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, this::loadStats);
    }

    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    private PlayerStats loadStats(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return new PlayerStats(uuid);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerStats stats = new PlayerStats(uuid);
        stats.setAttempts(config.getInt("attempts", 0));
        stats.setPlayTimeSeconds(config.getLong("play-time", 0));
        stats.setProgress(config.getDouble("progress", 0));
        stats.setLastProgress(config.getDouble("last-progress", 0));
        stats.setHudEnabled(config.getBoolean("hud-enabled", true));
        
        // Достижения прохождения
        stats.setFoundVillage(config.getBoolean("found-village", false));
        stats.setVisitedNether(config.getBoolean("visited-nether", false));
        stats.setGotBlazeRods(config.getBoolean("got-blaze-rods", false));
        stats.setGotPearls(config.getBoolean("got-pearls", false));
        stats.setNetherComplete(config.getBoolean("nether-complete", false));
        stats.setGotArrows(config.getBoolean("got-arrows", false));
        stats.setFoundStronghold(config.getBoolean("found-stronghold", false));
        stats.setVisitedEnd(config.getBoolean("visited-end", false));
        stats.setKilledDragon(config.getBoolean("killed-dragon", false));
        stats.setFirstJoin(config.getBoolean("first-join", true));
        
        // Место смерти
        if (config.contains("death-world")) {
            stats.setDeathWorld(config.getString("death-world"));
            stats.setDeathX(config.getDouble("death-x"));
            stats.setDeathY(config.getDouble("death-y"));
            stats.setDeathZ(config.getDouble("death-z"));
        }
        
        return stats;
    }

    public void saveStats(UUID uuid) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = getPlayerFile(uuid);
            FileConfiguration config = new YamlConfiguration();
            config.set("attempts", stats.getAttempts());
            config.set("play-time", stats.getPlayTimeSeconds());
            config.set("progress", stats.getProgress());
            config.set("last-progress", stats.getLastProgress());
            config.set("hud-enabled", stats.isHudEnabled());
            
            // Достижения
            config.set("found-village", stats.hasFoundVillage());
            config.set("visited-nether", stats.isVisitedNether());
            config.set("got-blaze-rods", stats.hasGotBlazeRods());
            config.set("got-pearls", stats.hasGotPearls());
            config.set("nether-complete", stats.isNetherComplete());
            config.set("got-arrows", stats.hasGotArrows());
            config.set("found-stronghold", stats.hasFoundStronghold());
            config.set("visited-end", stats.isVisitedEnd());
            config.set("killed-dragon", stats.hasKilledDragon());
            config.set("first-join", stats.isFirstJoin());
            
            // Место смерти
            if (stats.hasDeathLocation()) {
                config.set("death-world", stats.getDeathWorld());
                config.set("death-x", stats.getDeathX());
                config.set("death-y", stats.getDeathY());
                config.set("death-z", stats.getDeathZ());
            }

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось сохранить статистику для " + uuid);
            }
        });
    }

    public void resetStats(UUID uuid) {
        PlayerStats stats = new PlayerStats(uuid);
        stats.setHudEnabled(true);
        statsCache.put(uuid, stats);
        saveStats(uuid);
    }

    public void unloadStats(UUID uuid) {
        saveStats(uuid);
        statsCache.remove(uuid);
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    public void saveAll() {
        statsCache.keySet().forEach(this::saveStats);
    }
    
    public void clearCache() {
        statsCache.clear();
    }

    public static class PlayerStats {
        private final UUID uuid;
        private int attempts;
        private long playTimeSeconds;
        private double progress;
        private double lastProgress;
        private boolean hudEnabled = true;
        private long sessionStartTime;
        
        // Достижения прохождения
        private boolean foundVillage;
        private boolean visitedNether;
        private boolean gotBlazeRods;
        private boolean gotPearls;
        private boolean netherComplete;
        private boolean gotArrows;
        private boolean foundStronghold;
        private boolean visitedEnd;
        private boolean killedDragon;
        
        // Место смерти
        private String deathWorld;
        private double deathX, deathY, deathZ;
        
        // Первый вход
        private boolean firstJoin = true;

        public PlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.sessionStartTime = System.currentTimeMillis();
        }

        public UUID getUuid() { return uuid; }
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        public void incrementAttempts() { this.attempts++; }

        public long getPlayTimeSeconds() { return playTimeSeconds; }
        public void setPlayTimeSeconds(long playTimeSeconds) { this.playTimeSeconds = playTimeSeconds; }

        public long getTotalPlayTime() {
            return playTimeSeconds + (System.currentTimeMillis() - sessionStartTime) / 1000;
        }

        public void updatePlayTime() {
            long now = System.currentTimeMillis();
            playTimeSeconds += (now - sessionStartTime) / 1000;
            sessionStartTime = now;
        }

        public void resetSessionTime() {
            sessionStartTime = System.currentTimeMillis();
        }

        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }

        public double getLastProgress() { return lastProgress; }
        public void setLastProgress(double lastProgress) { this.lastProgress = lastProgress; }

        public double getDelta() { return progress - lastProgress; }

        public boolean isHudEnabled() { return hudEnabled; }
        public void setHudEnabled(boolean hudEnabled) { this.hudEnabled = hudEnabled; }
        public void toggleHud() { this.hudEnabled = !this.hudEnabled; }
        
        // Достижения
        public boolean hasFoundVillage() { return foundVillage; }
        public void setFoundVillage(boolean v) { this.foundVillage = v; }
        
        public boolean isVisitedNether() { return visitedNether; }
        public void setVisitedNether(boolean v) { this.visitedNether = v; }
        
        public boolean hasGotBlazeRods() { return gotBlazeRods; }
        public void setGotBlazeRods(boolean v) { this.gotBlazeRods = v; }
        
        public boolean hasGotPearls() { return gotPearls; }
        public void setGotPearls(boolean v) { this.gotPearls = v; }
        
        public boolean isNetherComplete() { return netherComplete; }
        public void setNetherComplete(boolean v) { this.netherComplete = v; }
        
        public boolean hasGotArrows() { return gotArrows; }
        public void setGotArrows(boolean v) { this.gotArrows = v; }
        
        public boolean hasFoundStronghold() { return foundStronghold; }
        public void setFoundStronghold(boolean v) { this.foundStronghold = v; }
        
        public boolean isVisitedEnd() { return visitedEnd; }
        public void setVisitedEnd(boolean v) { this.visitedEnd = v; }
        
        public boolean hasKilledDragon() { return killedDragon; }
        public void setKilledDragon(boolean v) { this.killedDragon = v; }
        
        public int getCompletedCount() {
            int count = 0;
            if (foundVillage) count++;
            if (visitedNether) count++;
            if (gotBlazeRods) count++;
            if (gotPearls) count++;
            if (netherComplete) count++;
            if (visitedEnd) count++;
            if (killedDragon) count++;
            return count;
        }
        
        // Место смерти
        public String getDeathWorld() { return deathWorld; }
        public void setDeathWorld(String w) { this.deathWorld = w; }
        public double getDeathX() { return deathX; }
        public void setDeathX(double x) { this.deathX = x; }
        public double getDeathY() { return deathY; }
        public void setDeathY(double y) { this.deathY = y; }
        public double getDeathZ() { return deathZ; }
        public void setDeathZ(double z) { this.deathZ = z; }
        
        public void setDeathLocation(String world, double x, double y, double z) {
            this.deathWorld = world;
            this.deathX = x;
            this.deathY = y;
            this.deathZ = z;
        }
        
        public boolean hasDeathLocation() {
            return deathWorld != null;
        }
        
        public void clearDeathLocation() {
            this.deathWorld = null;
        }
        
        // Первый вход
        public boolean isFirstJoin() { return firstJoin; }
        public void setFirstJoin(boolean v) { this.firstJoin = v; }
    }
}
