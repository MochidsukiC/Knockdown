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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static jp.houlab.mochidsuki.knockdown.Main.manager;
import static jp.houlab.mochidsuki.knockdown.Main.plugin;

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


                player.setSneaking(true);

                /*
                PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                packet.getIntegers().write(0, player.getEntityId());

                WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Integer.class);


                List<WrappedDataValue> values = Lists.newArrayList(
                        new WrappedDataValue(6, serializer, EnumWrappers.EntityPose.SWIMMING.ordinal())
                );
                packet.getDataValueCollectionModifier().write(0,values);

                for (Player player1: plugin.getServer().getOnlinePlayers()) {
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player1, packet);
                    } catch (Exception e) {
                        System.out.println("There was an issue with one of the glowing enchants!");
                    }
                }

                 */
            }
        }
    }
}
