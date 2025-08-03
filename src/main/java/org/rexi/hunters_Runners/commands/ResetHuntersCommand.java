package org.rexi.hunters_Runners.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.rexi.hunters_Runners.Hunters_Runners;

public class ResetHuntersCommand implements CommandExecutor {

    private final Hunters_Runners plugin;

    public ResetHuntersCommand(Hunters_Runners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.started = false;
        plugin.hunters.clear();
        plugin.runners.clear();
        plugin.runnersCount = 0;
        plugin.huntersReleased= false;

        if (plugin.hunterReleaseTask != null) {
            plugin.hunterReleaseTask.cancel();
            plugin.hunterReleaseTask = null;
        }
        if (plugin.gameEndTask != null) {
            plugin.gameEndTask.cancel();
            plugin.gameEndTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            Location lobby = plugin.getGameLobby();
            if (lobby != null) player.teleport(lobby);
        }
        return true;
    }
}
