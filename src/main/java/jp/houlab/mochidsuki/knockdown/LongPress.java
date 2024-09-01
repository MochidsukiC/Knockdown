package jp.houlab.mochidsuki.knockdown;


import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.time.Duration;
import java.util.Collections;


public class LongPress extends BukkitRunnable {
    double use = 0;
    Player player;
    String type;
    Material item;
    double time;
    Player fenixPlayer;

    public LongPress(Player p, String t, Material i, double ti, Player player1){
        player = p;
        type = t;
        item = i;
        time = ti;
        fenixPlayer = player1;
        player.getLocation().getWorld().playSound(fenixPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,1f, (float) 2);
    }

    @Override
    public void run() {
        if (player.isSneaking()) {
            use = use + 1;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 10, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2, 200, true, false));
            fenixPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 10, true, false));
            fenixPlayer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2, 200, true, false));
            player.getLocation().getWorld().playSound(fenixPlayer.getLocation(), Sound.BLOCK_BEACON_AMBIENT,0.5f, (float) 2);
            String bar = String.join("", Collections.nCopies((int) (use / time * 10), "■"));
            String barM = String.join("", Collections.nCopies((int) ((time - use) / time * 10), "-"));
            String half;
            if (use % (time / 10) != 0) {
                half = "□";
            } else {
                half = "";
            }

            TextComponent textComponent = Component.text("[").append(Component.text(bar + half + barM,NamedTextColor.LIGHT_PURPLE)).append(Component.text("]"));
            Title title = Title.title(Component.text(""), textComponent,Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(100),Duration.ofSeconds(1)));
            player.showTitle(title);
            fenixPlayer.showTitle(title);
            fenixPlayer.getWorld().spawnParticle(Particle.COMPOSTER,fenixPlayer.getLocation(),10,0.5,1,0.5,0);
            if (use >= time) {
                use = 0;
                fenixPlayer.removePotionEffect(PotionEffectType.UNLUCK);
                fenixPlayer.removePotionEffect(PotionEffectType.HEALTH_BOOST);
                fenixPlayer.setHealth(2);
                fenixPlayer.setFoodLevel(10);
                player.getLocation().getWorld().stopSound(SoundStop.named(Sound.BLOCK_BEACON_AMBIENT));
                player.getLocation().getWorld().playSound(fenixPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT,1f, (float) 2);
                for(int i = 0; i < V.knockDownBU.get(fenixPlayer).length; i++){
                    fenixPlayer.getInventory().setItem(i,V.knockDownBU.get(fenixPlayer)[i]);
                }
                for(int i =0;i<=2;i++){
                    for(int ii = 0;ii<=2;ii++){
                        for(int iii = 0; iii <= 2; iii++) {
                            fenixPlayer.sendBlockChange(player.getLocation().add(i - 1, iii, ii - 1), player.getLocation().add(i - 1, iii, ii - 1).getBlock().getBlockData());
                        }
                    }
                }
                fenixPlayer.setSneaking(false);
                cancel();
            }
        } else {
            player.getLocation().getWorld().stopSound(SoundStop.named(Sound.BLOCK_BEACON_AMBIENT));
            player.getLocation().getWorld().playSound(fenixPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,1f, (float) 2);
            use = 0;
            cancel();
        }
    }
}
