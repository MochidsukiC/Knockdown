package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static jp.houlab.mochidsuki.knockdown.Main.manager;
import static jp.houlab.mochidsuki.knockdown.Main.plugin;

/**
 * ノックダウン中のプレイヤーの姿勢を制御し、行動を制限する。
 * @author Mochidsuki
 */
public class EveryTicks extends BukkitRunnable {
    @Override
    public void run() {
        for(Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                for (int i = 0; i <= 2; i++) {
                    for (int ii = 0; ii <= 2; ii++) {
                        for (int iii = 0; iii <= 2; iii++) {
                            player.sendBlockChange(player.getLocation().add(i - 1, iii, ii - 1), player.getLocation().add(i - 1, iii, ii - 1).getBlock().getBlockData());
                        }
                    }
                }

                if (player.getLocation().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                    player.sendBlockChange(player.getLocation().add(0, 1, 0), Material.BARRIER.createBlockData());
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,3,1,false,false));

                player.setSneaking(true);

            }
        }
    }
}
