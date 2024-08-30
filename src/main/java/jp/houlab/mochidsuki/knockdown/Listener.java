package jp.houlab.mochidsuki.knockdown;

import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nullable;
import java.util.HashSet;

import static jp.houlab.mochidsuki.knockdown.Main.plugin;

public class Listener implements org.bukkit.event.Listener {

    @EventHandler
    public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event){
        if(event.getRightClicked().getType() == EntityType.PLAYER) {
            Team team = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());

            if (team.hasPlayer((OfflinePlayer) event.getRightClicked()) && event.getPlayer().getLocation().distance(event.getRightClicked().getLocation()) < 2 && ((Player)event.getRightClicked()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer().hasPotionEffect(PotionEffectType.SLOW))) {
                new LongPress(event.getPlayer(), "fenix", null, 100, (Player) event.getRightClicked()).runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent event){
        knockDown(event, null);
    }

    @EventHandler
    public void EntityDamagedEntity(EntityDamageByEntityEvent event) {
        knockDown(event,event.getDamager());
    }

    private void knockDown(EntityDamageEvent event,@Nullable Entity entity){
        Player damager = null;


        if (entity != null && entity.getType().equals(EntityType.PLAYER)) {
            if (((Player) entity).hasPotionEffect(PotionEffectType.UNLUCK)) {
                event.setCancelled(true);
                return;
            }
            try {
                ((Player) event.getEntity()).removePotionEffect(PotionEffectType.INVISIBILITY);
            } catch (Exception e) {
            }
        }

        if (event.getEntity().getType().equals(EntityType.PLAYER)) {


            Player player = (Player) event.getEntity();
            if (entity != null && (entity.getType() == EntityType.PLAYER || entity.getType() == EntityType.ARROW)) {
                if (entity.getType() == EntityType.PLAYER) {
                    damager = (Player) entity;
                } else {
                    damager = (Player) ((Arrow) entity).getShooter();
                }
            }

            double damage = event.getFinalDamage();
            if (!(player.hasPotionEffect(PotionEffectType.UNLUCK))) {
                if ((player.getHealth() <= damage)) {
                    if (((Player) event.getEntity()).getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && ((Player) event.getEntity()).getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                        //ノックダウン
                        ItemStack[] itemStacks = new ItemStack[41];
                        for (int i = 0; i < itemStacks.length; i++) {
                            itemStacks[i] = player.getInventory().getItem(i);
                        }
                        V.knockDownBU.put(player, itemStacks);
                        player.updateInventory();
                        player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 999999999, 0, true, true));
                        player.setFoodLevel(0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 999999999, 4, true, true));
                        player.setHealth(40);
                        event.setCancelled(true);
                        player.getInventory().clear();
                        if(entity != null){
                            damager.sendMessage(player.getName() + "をノックダウン!");
                            damager.playSound(entity, Sound.BLOCK_ANVIL_PLACE, 100, 0);
                        }

                        //部隊全滅
                        Team playerTeam = player.getScoreboard().getEntryTeam(player.getName());
                        int livers = 0;
                        for (String entry : playerTeam.getEntries()) {
                            if (player.getServer().getOnlinePlayers().contains(Bukkit.getPlayer(entry))) {
                                Player teammate = Bukkit.getPlayer(entry);
                                if (teammate.getGameMode().equals(GameMode.SURVIVAL) && !teammate.hasPotionEffect(PotionEffectType.UNLUCK)) {
                                    livers++;
                                }
                            }
                        }
                        if (livers == 0) {
                            for (String entry : playerTeam.getEntries()) {
                                if (player.getServer().getOnlinePlayers().contains(Bukkit.getPlayer(entry)) && Bukkit.getPlayer(entry).getGameMode().equals(GameMode.SURVIVAL)) {
                                    Bukkit.getPlayer(entry).setHealth(0);
                                }
                                Bukkit.getPlayer(entry).sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
                            }
                        }
                    }

                }
            }
        }
    }


    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent event){
        Entity entity = event.getEntity().getWorld().spawn(event.getEntity().getLocation(),EntityType.MINECART_CHEST.getEntityClass());
        entity.setGlowing(true);
        entity.setInvulnerable(true);
        StorageMinecart deathCart = (StorageMinecart)entity;
        for (int i = 0; i <= 10;i++){
            if(V.knockDownBU.get(event.getEntity())[i] != null) {
                if (V.knockDownBU.get(event.getEntity())[i].getType() != Material.FILLED_MAP) {
                    deathCart.getInventory().setItem(i, V.knockDownBU.get(event.getEntity())[i]);
                }
            }
        }
        deathCart.getInventory().setItem(11, V.knockDownBU.get(event.getEntity())[18]);
        deathCart.getInventory().setItem(18, V.knockDownBU.get(event.getEntity())[19]);
        deathCart.getInventory().setItem(19, V.knockDownBU.get(event.getEntity())[27]);
        deathCart.getInventory().setItem(20, V.knockDownBU.get(event.getEntity())[28]);

        deathCart.getInventory().setItem(24, V.knockDownBU.get(event.getEntity())[21]);
        deathCart.setCustomName(event.getEntity().getName());
        try {
            ItemStack chest = V.knockDownBU.get(event.getEntity())[22];
            Damageable chestD = (Damageable) chest.getItemMeta();
            chestD.setDamage(0);
            chest.setItemMeta(chestD);
            deathCart.getInventory().setItem(25, chest);
        }catch (Exception e){}
        deathCart.getInventory().setItem(26, V.knockDownBU.get(event.getEntity())[23]);
        deathCart.getInventory().setItem(21,V.knockDownBU.get(event.getEntity())[40]);
        event.getEntity().getInventory().clear();




        event.getEntity().sendMessage("死んでしまった!!");
        event.getEntity().sendMessage("数字ボタンを押すとほかの人のところにTPできるぞ!!");
    }

    @EventHandler
    public void EntityToggleSwimEvent(EntityToggleSwimEvent event){
        event.setCancelled(true);
    }


}
