package org.rexi.hunters_Runners.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.rexi.hunters_Runners.Hunters_Runners;

import java.util.*;

public class GameListener implements Listener {

    private final Hunters_Runners plugin;

    public GameListener(Hunters_Runners plugin) {
        this.plugin = plugin;
    }

    // Daño por caída cancelado para runners
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.started) return;
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
        }
    }

    // Control de daño entre jugadores
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!plugin.started) return;
        if (!plugin.huntersReleased) return;
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        boolean isAttackerHunter = plugin.hunters.contains(attacker.getName());
        boolean isVictimRunner = plugin.runners.contains(victim.getName());

        if (!isAttackerHunter) {
            // Si no es cazador, no puede dañar a nadie
            event.setCancelled(true);
            return;
        }

        if (!isVictimRunner) {
            // Un cazador no puede dañar a otro cazador
            event.setCancelled(true);
            return;
        }

        // Cazador daña a runner: "mata" al runner
        victim.setGameMode(GameMode.SPECTATOR);
        plugin.runnersCount--;
        plugin.HunterKills.put(attacker.getName(), plugin.HunterKills.getOrDefault(attacker.getName(), 0) + 1);

        victim.sendMessage(ChatColor.RED + "¡Has sido cazado!");
        attacker.sendMessage(ChatColor.GREEN + "¡Has cazado a " + victim.getName() + "!");

        plugin.getServer().broadcastMessage(ChatColor.RED + "¡" + victim.getName() + " fue cazado por " + attacker.getName() + "!");

        plugin.runServerCommand("tab nametag show "+ victim.getName() +" -s");

        if (plugin.runnersCount != 0) {
            plugin.getServer().broadcastMessage(ChatColor.AQUA + "Quedan " + plugin.runnersCount + " corredores.");
        } else {
            plugin.RunnerMVP.add(victim.getName());
            plugin.RunnerTime = (int) ((System.currentTimeMillis() - plugin.gameStartTime) / 1000);
            plugin.endGame("hunters");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        if (!plugin.started) return;
        Player player = event.getPlayer();
        String name = player.getName();

        if (plugin.runners.contains(name)) {
            plugin.runners.remove(name);
            plugin.runnersCount--;
            plugin.getServer().broadcastMessage(ChatColor.RED + "¡ " + name + " se ha desconectado!");

            if (plugin.runnersCount != 0) {
                plugin.getServer().broadcastMessage(ChatColor.AQUA + "Quedan " + plugin.runnersCount + " corredores.");
            } else {
                plugin.endGame("hunters");
            }
        } else if (plugin.hunters.contains(name)){
            plugin.hunters.remove(name);
            plugin.getServer().broadcastMessage(ChatColor.RED + "¡ El cazador " + name + " se ha desconectado!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.started) return;
        Player player = event.getPlayer();

        Location spawn = plugin.getGameSpawn();
        if (spawn == null) {
            player.kick(LegacyComponentSerializer.legacyAmpersand().deserialize("&cError: coordenadas del spawn no válidas."));
            return;
        }
        player.teleport(spawn);
        player.setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onShift(PlayerToggleSneakEvent event) {
        if (!plugin.started) return;
        if (!plugin.huntersReleased) return;

        Player player = event.getPlayer();
        String name = player.getName();

        if (plugin.runners.contains(name)) {
            if (event.isSneaking()) {
                plugin.runServerCommand("eglow set " +name+ " aqua");
            } else {
                plugin.runServerCommand("eglow set " +name+ " off");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.started) return;
        if (!plugin.huntersReleased) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.runners.contains(player.getName())) return;

        UUID uuid = player.getUniqueId();

        plugin.lastMove.put(uuid, System.currentTimeMillis());

        // Si se mueve y tenía glow por quieto, se lo quitamos
        if (plugin.glowing.contains(uuid)) {
            plugin.runServerCommand("eglow set " + player.getName() + " off");
            plugin.glowing.remove(uuid);
        }
    }
}