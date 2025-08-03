package org.rexi.hunters_Runners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.rexi.hunters_Runners.commands.HuntersCommand;
import org.rexi.hunters_Runners.commands.ResetHuntersCommand;
import org.rexi.hunters_Runners.commands.StartHuntersCommand;
import org.rexi.hunters_Runners.listeners.GameListener;

import java.util.ArrayList;
import java.util.List;

public final class Hunters_Runners extends JavaPlugin {

    public boolean started = false;
    public List<String> hunters = new ArrayList<>();
    public List<String> runners = new ArrayList<>();
    public int runnersCount = 0;
    public boolean huntersReleased = false;
    public BukkitTask gameEndTask = null;
    public BukkitTask hunterReleaseTask = null;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Carga config.yml si no existe

        getCommand("starthunters").setExecutor(new StartHuntersCommand(this));
        getCommand("hunters").setExecutor(new HuntersCommand(this));
        getCommand("resethunters").setExecutor(new ResetHuntersCommand(this));

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Location getGameSpawn() {
        String raw = getConfig().getString("game.game_spawn");
        if (raw == null) return null;

        String[] parts = raw.split(";");
        if (parts.length < 6) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getGameLobby() {
        String raw = getConfig().getString("game.lobby");
        if (raw == null) return null;

        String[] parts = raw.split(";");
        if (parts.length < 6) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void endGame(String winner) {
        if (winner.equalsIgnoreCase("hunters")) {
            getServer().broadcastMessage(ChatColor.GREEN + "Han ganado los cazadores!");

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (hunters.contains(player.getName())) {
                    Bukkit.broadcastMessage(ChatColor.AQUA + "▶ " + player.getName());
                }
            }
        } else if (winner.equalsIgnoreCase("runners")) {
            getServer().broadcastMessage(ChatColor.GREEN + "Han ganado los corredores!");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (runners.contains(player.getName())) {
                    Bukkit.broadcastMessage(ChatColor.AQUA + "▶ " + player.getName());
                }
            }
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Se ha intentado terminar una partida, pero el substring es incorrecto");
            return;
        }

        Location lobby = getGameLobby();
        if (lobby == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error: coordenadas del lobby no válidas");
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(lobby);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        started = false;
        huntersReleased = false;
        hunters.clear();
        runners.clear();
        if (hunterReleaseTask != null) {
            hunterReleaseTask.cancel();
            hunterReleaseTask = null;
        }
        if (gameEndTask != null) {
            gameEndTask.cancel();
            gameEndTask = null;
        }
    }
}
