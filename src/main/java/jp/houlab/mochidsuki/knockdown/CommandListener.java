package jp.houlab.mochidsuki.knockdown;

import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandListener implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("watchscore")){
            Player player = (Player) sender;
            ScoreProfile.scoreProfiles.get(player.getUniqueId()).sendScore(player);
        }


        return false;
    }
}
