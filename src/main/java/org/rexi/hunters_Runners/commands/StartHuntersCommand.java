package org.rexi.hunters_Runners.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
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

    private final Hunters_Runners  plugin;

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
        if (posiblesHunters.size() < HuntersCount) {
            sender.sendMessage(ChatColor.RED + "Se necesitan al menos 3 jugadores sin permiso de admin para comenzar.");
            return true;
        }

        Collections.shuffle(posiblesHunters);
        List<Player> huntersSeleccionados = posiblesHunters.subList(0, HuntersCount);

        plugin.hunters.clear(); // Limpiamos la lista de cazadores
        // Anunciamos los cazadores
        Bukkit.broadcastMessage(ChatColor.GOLD + "¡Los cazadores han sido elegidos!");
        for (Player hunter : huntersSeleccionados) {
            Bukkit.broadcastMessage(ChatColor.RED + "▶ " + hunter.getName());
            plugin.hunters.add(hunter.getName()); // Guardamos el nombre del cazador
        }

        int countdown = plugin.getConfig().getInt("game.countdown", 5);
        plugin.started = true; // Marcamos que el juego ha comenzado

        Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego comenzará en " + countdown + " segundos!");

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

                    // Teleportar SOLO a los runners
                    plugin.runnersCount = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!plugin.hunters.contains(p.getName()) && !p.hasPermission("hunters.admin")) {
                            p.teleport(spawn);
                            plugin.runnersCount++;
                            plugin.runners.add(p.getName());
                            p.setGameMode(GameMode.SURVIVAL);
                        }
                    }

                    int hideTime = plugin.getConfig().getInt("game.hide_time", 30);

                    Bukkit.broadcastMessage(ChatColor.YELLOW + "¡Los corredores han sido teletransportados! Tienen " + hideTime + " segundos para esconderse...");

                    // Esperar x segundos y teletransportar a los cazadores
                    plugin.hunterReleaseTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.huntersReleased = true;
                            Bukkit.broadcastMessage(ChatColor.RED + "¡Los cazadores han sido liberados!");

                            if (gameTime >= 60) {
                                Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego durará " + (gameTime / 60) + " minutos!");
                            } else {
                                Bukkit.broadcastMessage(ChatColor.GREEN + "¡El juego durará " + gameTime + " segundos!");
                            }

                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (plugin.hunters.contains(p.getName())) {
                                    p.teleport(spawn);
                                    p.setGameMode(GameMode.SURVIVAL);
                                }
                            }

                            // Temporizador del fin de la partida
                            plugin.gameEndTask = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (plugin.started) {
                                        Bukkit.broadcastMessage(ChatColor.AQUA + "¡El tiempo se ha acabado!");
                                        plugin.endGame("runners");
                                    }
                                }
                            }.runTaskLater(plugin, gameTime * 20L);
                        }
                    }.runTaskLater(plugin, hideTime * 20L); // segundos

                } else {
                    if (timeLeft <= 5 || timeLeft % 10 == 0)
                        Bukkit.broadcastMessage(ChatColor.GRAY + "Comienza en " + timeLeft + "...");
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0, 20L);

        return true;
    }
}