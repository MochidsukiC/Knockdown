package jp.houlab.mochidsuki.knockdown;

import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import javax.imageio.IIOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static jp.houlab.mochidsuki.knockdown.Main.plugin;
import static jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile.outputScoreCsv;

public class CommandListener implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("watchscore")) {
            Player player = (Player) sender;
            ScoreProfile.scoreProfiles.get(player.getUniqueId()).sendScore(player);
            outputScoreCsv();
        }



        return false;
    }
}
