package jp.houlab.mochidsuki.knockdown;

import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile;
import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.VictimProfile;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nullable;
import java.util.*;

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

            if ((team == null || ((Player)event.getRightClicked()).getScoreboard().getPlayerTeam((Player) event.getRightClicked()) == null ||  team.hasPlayer((OfflinePlayer) event.getRightClicked())) && event.getPlayer().getLocation().distance(event.getRightClicked().getLocation()) < 2 && ((Player)event.getRightClicked()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer()).hasPotionEffect(PotionEffectType.UNLUCK) && !(event.getPlayer().hasPotionEffect(PotionEffectType.SLOW))) {
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
     * @param damagedEntity 攻撃者
     */
    private void knockDown(EntityDamageEvent event,@Nullable Entity damagedEntity){
        Player damager = null;
        //ノックダウン対象か判断

        if (damagedEntity != null && damagedEntity.getType().equals(EntityType.PLAYER)) {
            if (((Player) damagedEntity).hasPotionEffect(PotionEffectType.UNLUCK)) {
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

            if (damagedEntity != null && (damagedEntity.getType() == EntityType.PLAYER || damagedEntity.getType() == EntityType.ARROW || damagedEntity.getType() == EntityType.FIREBALL || damagedEntity.getType() == EntityType.SPECTRAL_ARROW || damagedEntity.getType() == EntityType.TRIDENT)) {
                switch (damagedEntity.getType()){
                    case PLAYER:{
                        damager = (Player) damagedEntity;
                        break;
                    }
                    case ARROW:{
                        if(((Arrow) damagedEntity).getShooter() instanceof Player) {
                            damager = (Player) ((Arrow) damagedEntity).getShooter();
                        }
                        break;
                    }
                    case FIREBALL:{
                        if(((Fireball) damagedEntity).getShooter() instanceof Player) {
                            damager = (Player) ((Fireball) damagedEntity).getShooter();
                        }
                        break;
                    }
                    case SPECTRAL_ARROW:{
                        if(((SpectralArrow) damagedEntity).getShooter() instanceof Player){
                            damager = (Player) ((SpectralArrow) damagedEntity).getShooter();
                        }
                        break;
                    }
                    case TRIDENT:{
                        if(((Trident) damagedEntity).getShooter() instanceof Player){
                            damager = (Player) ((Trident) damagedEntity).getShooter();
                        }
                        break;
                    }
                }
            }

            double damage = event.getFinalDamage();
            if (damager != null && !damager.getUniqueId().equals(victim.getUniqueId())) {
                victimProfiles.get(victim.getUniqueId()).addDamager(damager);
                scoreProfiles.get(damager.getUniqueId()).addDamageScore(damage);
            }
            if (!(victim.hasPotionEffect(PotionEffectType.UNLUCK))) {
                if ((victim.getHealth() <= damage)) {
                    if (((Player) event.getEntity()).getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && ((Player) event.getEntity()).getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                        //ノックダウン
                        ItemStack[] itemStacks = new ItemStack[41];
                        for (int i = 0; i < itemStacks.length; i++) {
                            itemStacks[i] = victim.getInventory().getItem(i);
                        }
                        itemStacks[40] = victim.getInventory().getItemInOffHand();
                        itemStacks[36] = victim.getInventory().getItem(config.getInt("HeadSlot"));
                        itemStacks[37] = victim.getInventory().getItem(config.getInt("ChestSlot"));
                        itemStacks[39] = victim.getInventory().getItem(config.getInt("BootsSlot"));
                        V.knockDownBU.put(victim, itemStacks);
                        victim.updateInventory();
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 999999999, 0, true, true));
                        victim.setFoodLevel(0);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 999999999, 4, true, true));
                        victim.setHealth(40);
                        event.setCancelled(true);
                        victim.getInventory().clear();
                        if(damager != null){
                            damager.sendMessage(victim.getName() + "をノックダウン!");
                            damager.playSound(damager, Sound.BLOCK_ANVIL_PLACE, 100, 0);
                        }

                        if(damager != null) {
                            victimProfiles.get(victim.getUniqueId()).setKnocker(damager.getUniqueId());
                        }else {
                            List<UUID> damagers  = new ArrayList<>(victimProfiles.get(victim.getUniqueId()).getDamager());
                            if(!damagers.isEmpty()) {
                                victimProfiles.get(victim.getUniqueId()).setKnocker(damagers.get(victimProfiles.get(victim.getUniqueId()).getDamager().size() - 1));
                            }
                        }
                        victimProfiles.get(victim.getUniqueId()).setAssistant(victimProfiles.get(victim.getUniqueId()).getDamager());



                        //部隊全滅
                        Team playerTeam = victim.getScoreboard().getEntryTeam(victim.getName());

                        int livers = 0;
                        if(playerTeam != null) {
                            for (String entry : playerTeam.getEntries()) {
                                if (plugin.getServer().getOfflinePlayer(entry).isOnline()) {
                                    Player teammate = Bukkit.getPlayer(entry);
                                    if (teammate.getGameMode().equals(GameMode.SURVIVAL) && !teammate.hasPotionEffect(PotionEffectType.UNLUCK)) {
                                        livers++;
                                    }
                                }
                            }
                        }
                        if (livers == 0) {
                            if(playerTeam != null) {
                                for (String entry : playerTeam.getEntries()) {
                                    if (plugin.getServer().getOfflinePlayer(entry).isOnline()) {
                                        if (Bukkit.getPlayer(entry).getGameMode().equals(GameMode.SURVIVAL) || Bukkit.getPlayer(entry).getGameMode().equals(GameMode.ADVENTURE)) {
                                            Bukkit.getPlayer(entry).setHealth(0);
                                        }
                                        Bukkit.getPlayer(entry).sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
                                    }
                                    if(scoreProfiles.containsKey(Bukkit.getOfflinePlayer(entry).getUniqueId())) {
                                        scoreProfiles.get(Bukkit.getOfflinePlayer(entry).getUniqueId()).setRankScore(jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());
                                    }
                                }
                            }else {
                                victim.setHealth(0);
                                victim.sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
                                scoreProfiles.get(victim.getUniqueId()).setRankScore(jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());
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
            int num = allowList.get(i);
            if(num == -106){
                num = 40;
            }
            if (V.knockDownBU.get(event.getEntity())[num] != null &&V.knockDownBU.get(event.getEntity())[num].getType() != Material.FILLED_MAP) {
                deathCart.getInventory().setItem(i , V.knockDownBU.get(event.getEntity())[num]);
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
        event.getEntity().getInventory().clear();

        try {
            Block block = new Location(event.getEntity().getServer().getWorld(Main.config.getString("deathCardPlusItemLocation.world")),Main.config.getInt("deathCardPlusItemLocation.x"),Main.config.getInt("deathCardPlusItemLocation.y"),Main.config.getInt("deathCardPlusItemLocation.z")).getBlock();
            Material material = block.getType();
            Chest chest = (Chest) block.getState();
            int r = new Random().nextInt(chest.getInventory().getSize());
            deathCart.getInventory().setItem(23,chest.getInventory().getItem(r));

        }catch (Exception e){
            e.printStackTrace();
        }


        //Victim->Scoreスコア移行
        // 変更後
        //Victim->Scoreスコア移行
        Player knocker = null;
        if(victimProfiles.get(event.getPlayer().getUniqueId()).getKnocker() != null && Bukkit.getOfflinePlayer(victimProfiles.get(event.getPlayer().getUniqueId()).getKnocker()).isOnline()) {
            knocker = Bukkit.getPlayer(victimProfiles.get(event.getPlayer().getUniqueId()).getKnocker());
        }

        // ★ 攻撃者(knocker)がnullでないことを確認してからキルスコアを加算
        if (knocker != null) {
            scoreProfiles.get(knocker.getUniqueId()).addKillScore();
        }

        for (UUID assistant : victimProfiles.get(event.getEntity().getUniqueId()).getAssistant()) {
            // ★ アシストしたプレイヤーがnullでないことを確認
            if (assistant == null) {
                continue;
            }
            // ★ 攻撃者がいない場合、または攻撃者とアシスト者が違う場合にスコアを加算
            if (knocker == null || !assistant.equals(knocker.getUniqueId())) {
                scoreProfiles.get(assistant).addAssistScore();
            }
        }
        scoreProfiles.get(event.getEntity().getUniqueId()).addDeathScore();


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
        victimProfiles.put(event.getPlayer().getUniqueId(),new VictimProfile());
        scoreProfiles.put(event.getPlayer().getUniqueId(),new ScoreProfile());
    }

}
