package org.rexi.hunters_Runners.commands;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.rexi.hunters_Runners.Hunters_Runners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartHuntersCommand implements CommandExecutor {

    private final Hunters_Runners plugin;

    public StartHuntersCommand(Hunters_Runners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hunters.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para ejecutar este comando.");
            return true;
        }

        if (plugin.started) {
            sender.sendMessage(ChatColor.RED + "El juego ya está empezado");
            return false;
        }

        List<Player> posiblesHunters = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("hunters.admin")) {
                posiblesHunters.add(player);
            }
        }

        int HuntersCount = plugin.getConfig().getInt("game.hunters", 3);
        if (posiblesHunters.size() < HuntersCount + 1) {
            sender.sendMessage(ChatColor.RED + "Se necesitan al menos " + (HuntersCount + 1) + " jugadores sin permiso de admin para comenzar.");
            return true;
        }

        Collections.shuffle(posiblesHunters);
        List<Player> huntersSeleccionados = posiblesHunters.subList(0, HuntersCount);

        plugin.hunters.clear();
        plugin.runServerCommand("svar reset hunters *");
        plugin.runServerCommand("eglow set * off");
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.runServerCommand("tab nametag show "+ player.getName() +" -s");
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "¡Los cazadores han sido elegidos!");
        for (Player hunter : huntersSeleccionados) {
            Bukkit.broadcastMessage(ChatColor.RED + "▶ " + hunter.getName());
            plugin.hunters.add(hunter.getName());
            plugin.runServerCommand("svar set hunters &c "+hunter.getName());
            plugin.runServerCommand("eglow set "+hunter.getName()+ " red");
        }

        int countdown = plugin.getConfig().getInt("game.countdown", 5);
        plugin.started = true;

        Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego comenzará en " + countdown + " segundos!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&aLos cazadores han sido elegidos"), ChatColor.translateAlternateColorCodes('&', "&6¡El juego comenzará en " + countdown + " segundos!"), 10, 40, 10);
        }

        Location spawn = plugin.getGameSpawn();
        if (spawn == null) {
            sender.sendMessage(ChatColor.RED + "Error: coordenadas del spawn no válidas.");
            plugin.started = false;
            return false;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("hunters.admin")) {
                p.teleport(spawn);
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        int gameTime = plugin.getConfig().getInt("game.game_time", 300);

        new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                if (timeLeft == 0) {
                    cancel();

                    int hideTime = plugin.getConfig().getInt("game.hide_time", 30);

                    plugin.runnersCount = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&aCorredores teletransportados"), ChatColor.translateAlternateColorCodes('&', "&6Tienen " + hideTime + " segundos para esconderse"), 10, 40, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                        if (!plugin.hunters.contains(p.getName()) && !p.hasPermission("hunters.admin")) {
                            p.teleport(spawn);
                            plugin.runnersCount++;
                            plugin.runners.add(p.getName());
                            p.setGameMode(GameMode.SURVIVAL);
                            plugin.runServerCommand("svar set hunters &b "+p.getName());
                            plugin.runServerCommand("tab nametag hide "+ p.getName() +" -s");
                        }
                    }

                    Bukkit.broadcastMessage(ChatColor.YELLOW + "¡Los corredores han sido teletransportados! Tienen " + hideTime + " segundos para esconderse...");

                    plugin.hideBossBar = Bukkit.createBossBar("¡Escóndete!", BarColor.YELLOW, BarStyle.SOLID);
                    for (Player p : Bukkit.getOnlinePlayers()) plugin.hideBossBar.addPlayer(p);

                    new BukkitRunnable() {
                        int hideTimeLeft = hideTime;

                        @Override
                        public void run() {
                            if (hideTimeLeft <= 0) {
                                plugin.hideBossBar.removeAll();
                                cancel();

                                plugin.huntersReleased = true;
                                Bukkit.broadcastMessage(ChatColor.RED + "¡Los cazadores han sido liberados!");

                                if (gameTime >= 60) {
                                    Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego durará " + (gameTime / 60) + " minutos!");
                                } else {
                                    Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego durará " + gameTime + " segundos!");
                                }

                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&cCazadores liberados"), ChatColor.translateAlternateColorCodes('&', "&6Tienen " + gameTime/60 + " minutos para encontrar a los corredores"), 10, 40, 10);
                                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                                    if (plugin.hunters.contains(p.getName())) {
                                        p.teleport(spawn);
                                        p.setGameMode(GameMode.SURVIVAL);
                                    }
                                }

                                plugin.gameStartTime = (int) System.currentTimeMillis();

                                plugin.gameBossBar = Bukkit.createBossBar("¡Encuentra a los corredores!", BarColor.RED, BarStyle.SOLID);
                                for (Player p : Bukkit.getOnlinePlayers()) plugin.gameBossBar.addPlayer(p);

                                new BukkitRunnable() {
                                    int gameTimeLeft = gameTime;

                                    @Override
                                    public void run() {
                                        if (!plugin.started || gameTimeLeft <= 0) {
                                            if (plugin.gameBossBar != null) {
                                                plugin.gameBossBar.removeAll();
                                                plugin.gameBossBar = null;
                                            }
                                            cancel();
                                            Bukkit.broadcastMessage(ChatColor.AQUA + "¡El tiempo se ha acabado!");
                                            for (String player : plugin.runners) {
                                                plugin.RunnerMVP.add(player);
                                            }
                                            plugin.RunnerTime = (int) ((System.currentTimeMillis() - plugin.gameStartTime) / 1000);
                                            plugin.endGame("runners");
                                            return;
                                        }

                                        double progress = (double) gameTimeLeft / gameTime;
                                        plugin.gameBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                                        if (gameTimeLeft > 60) {
                                            int minutes = gameTimeLeft / 60;
                                            int seconds = gameTimeLeft % 60;
                                            plugin.gameBossBar.setTitle(ChatColor.RED + "Tiempo restante: " + minutes + "m " + seconds + "s");
                                        } else {
                                            plugin.gameBossBar.setTitle(ChatColor.RED + "Tiempo restante: " + gameTimeLeft + "s");
                                        }
                                        gameTimeLeft--;
                                    }
                                }.runTaskTimer(plugin, 0L, 20L);

                            } else {
                                double progress = (double) hideTimeLeft / hideTime;
                                plugin.hideBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                                plugin.hideBossBar.setTitle(ChatColor.YELLOW + "Tiempo para esconderse: " + hideTimeLeft + "s");
                                hideTimeLeft--;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L);

                } else {
                    if (timeLeft <= 5 || timeLeft % 10 == 0)
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&7" + timeLeft), "", 0, 20, 0);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        }

                    Bukkit.broadcastMessage(ChatColor.GRAY + "Comienza en " + timeLeft + "...");
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0, 20L);

        return true;
    }
}