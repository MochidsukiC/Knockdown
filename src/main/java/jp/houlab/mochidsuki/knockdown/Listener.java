package jp.houlab.mochidsuki.knockdown;

import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile;
import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.VictimProfile;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
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
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;

import static jp.houlab.mochidsuki.battleinventory.Main.config;
import static jp.houlab.mochidsuki.knockdown.Main.plugin;
import static jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile.scoreProfiles;
import static jp.houlab.mochidsuki.knockdown.scoreCounterAPI.VictimProfile.victimProfiles;


/**
 * イベントリスナー
 */
public class Listener implements org.bukkit.event.Listener {

    /**
     * 蘇生を開始する
     * @param event イベント
     */
    @EventHandler
    public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event){
        if(event.getRightClicked().getType() == EntityType.PLAYER) {
            Team team = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());

            if (team.hasPlayer((OfflinePlayer) event.getRightClicked()) && event.getPlayer().getLocation().distance(event.getRightClicked().getLocation()) < 2 && ((Player)event.getRightClicked()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer().hasPotionEffect(PotionEffectType.SLOW))) {
                new LongPress(event.getPlayer(), null, 100, (Player) event.getRightClicked()).runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    /**
     * ノックダウンメソッドを呼び出す
     * @param event イベント
     */
    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent event){
        knockDown(event, null);
    }

    /**
     * ノックダウンメソッドを呼び出す
     * @param event イベント
     */
    @EventHandler
    public void EntityDamagedEntity(EntityDamageByEntityEvent event) {
        knockDown(event,event.getDamager());
    }

    /**
     * プレイヤーがノックダウンするか判断したのち、する場合はノックダウンさせる
     * @param event イベント
     * @param entity 攻撃者
     */
    private void knockDown(EntityDamageEvent event,@Nullable Entity entity){
        Player damager = null;
        //ノックダウン対象か判断

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
            Player victim = (Player) event.getEntity();
            if (entity != null && (entity.getType() == EntityType.PLAYER || entity.getType() == EntityType.ARROW)) {
                if (entity.getType() == EntityType.PLAYER) {
                    damager = (Player) entity;
                } else {
                    damager = (Player) ((Arrow) entity).getShooter();
                }
            }
            victimProfiles.get(victim).addDamager(damager);


            double damage = event.getFinalDamage();
            scoreProfiles.get(victim).addDamageScore(damage);
            if (!(victim.hasPotionEffect(PotionEffectType.UNLUCK))) {
                if ((victim.getHealth() <= damage)) {
                    if (((Player) event.getEntity()).getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && ((Player) event.getEntity()).getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                        //ノックダウン
                        ItemStack[] itemStacks = new ItemStack[41];
                        for (int i = 0; i < itemStacks.length; i++) {
                            itemStacks[i] = victim.getInventory().getItem(i);
                        }
                        V.knockDownBU.put(victim, itemStacks);
                        victim.updateInventory();
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 999999999, 0, true, true));
                        victim.setFoodLevel(0);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 999999999, 4, true, true));
                        victim.setHealth(40);
                        event.setCancelled(true);
                        victim.getInventory().clear();
                        if(entity != null){
                            damager.sendMessage(victim.getName() + "をノックダウン!");
                            damager.playSound(entity, Sound.BLOCK_ANVIL_PLACE, 100, 0);
                        }

                        victimProfiles.get(victim).setKnocker(damager);
                        victimProfiles.get(victim).setAssistant(victimProfiles.get(victim).getDamager());


                        //部隊全滅
                        Team playerTeam = victim.getScoreboard().getEntryTeam(victim.getName());
                        int livers = 0;
                        for (String entry : playerTeam.getEntries()) {
                            if (victim.getServer().getOnlinePlayers().contains(Bukkit.getPlayer(entry))) {
                                Player teammate = Bukkit.getPlayer(entry);
                                if (teammate.getGameMode().equals(GameMode.SURVIVAL) && !teammate.hasPotionEffect(PotionEffectType.UNLUCK)) {
                                    livers++;
                                }
                            }
                        }
                        if (livers == 0) {
                            for (String entry : playerTeam.getEntries()) {
                                if (plugin.getServer().getOfflinePlayer(entry).isOnline()) {
                                    if(Bukkit.getPlayer(entry).getGameMode().equals(GameMode.SURVIVAL)) {
                                        Bukkit.getPlayer(entry).setHealth(0);
                                    }
                                    Bukkit.getPlayer(entry).sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
                                }
                                scoreProfiles.get(Bukkit.getPlayer(entry)).setRankScore(jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());

                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * 確殺とデスカートの生成
     * @param event イベント
     */
    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent event){
        event.getEntity().setGameMode(GameMode.SPECTATOR);

        Entity entity = event.getEntity().getWorld().spawn(event.getEntity().getLocation(),EntityType.MINECART_CHEST.getEntityClass());
        entity.setGlowing(true);
        entity.setInvulnerable(true);
        StorageMinecart deathCart = (StorageMinecart)entity;


        List<Integer> allowList = config.getIntegerList("AllowSlot");
        for(int i = 0; i < allowList.size(); i++){
            if (V.knockDownBU.get(event.getEntity())[allowList.get(i)].getType() != Material.FILLED_MAP) {
                deathCart.getInventory().setItem(i , V.knockDownBU.get(event.getEntity())[allowList.get(i)]);
            }
        }

        deathCart.getInventory().setItem(24, V.knockDownBU.get(event.getEntity())[config.getInt("HeadSlot")]);
        deathCart.setCustomName(event.getEntity().getName());
        try {
            ItemStack chest = V.knockDownBU.get(event.getEntity())[config.getInt("ChestSlot")];
            Damageable chestD = (Damageable) chest.getItemMeta();
            chestD.setDamage(0);
            chest.setItemMeta(chestD);
            deathCart.getInventory().setItem(25, chest);
        }catch (Exception e){}
        deathCart.getInventory().setItem(26, V.knockDownBU.get(event.getEntity())[config.getInt("BootsSlot")]);
        if(V.knockDownBU.get(event.getEntity())[40].getType() != Material.FILLED_MAP) {
            deathCart.getInventory().setItem(23, V.knockDownBU.get(event.getEntity())[40]);
        }
        event.getEntity().getInventory().clear();

        //Victim->Scoreスコア移行
        scoreProfiles.get(victimProfiles.get(event.getPlayer()).getKnocker()).addKillScore();
        for (Player player : victimProfiles.get(event.getEntity()).getAssistant()) {
            if(!player.getName().equals(victimProfiles.get(event.getPlayer()).getKnocker().getName())){
                scoreProfiles.get(player).addAssistScore();
            }
        }
        scoreProfiles.get(event.getEntity()).addDeathScore();


        event.getEntity().sendMessage("死んでしまった!!");
        event.getEntity().sendMessage("数字ボタンを押すとほかの人のところにTPできるぞ!!");
    }

    /**
     * プレイヤーの姿勢を泳がせる
     * @param event
     */
    @EventHandler
    public void EntityToggleSwimEvent(EntityToggleSwimEvent event){
        event.setCancelled(true);
    }

    /**
     *プレイヤーがログインした場合トリガーされる
     * @param event
     */
    @EventHandler
    public void PlayerLoginEvent(PlayerLoginEvent event){
        victimProfiles.put(event.getPlayer(),new VictimProfile());
        scoreProfiles.put(event.getPlayer(),new ScoreProfile());
    }

}
