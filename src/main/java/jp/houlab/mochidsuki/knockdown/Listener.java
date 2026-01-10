package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.wrappers.EnumWrappers;
import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.ScoreProfile;
import jp.houlab.mochidsuki.knockdown.scoreCounterAPI.VictimProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.jetbrains.annotations.NotNull;

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

    // ★★★ 改善点1: イベントハンドラを1つに統合し、処理の重複と競合を完全に防ぐ ★★★
    // PriorityをMONITORにすることで、他のプラグインの計算がすべて終わった後の最終的なダメージ値で判定できる。
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        // ダメージを受けたのがプレイヤーでなければ無視
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player damager = null;

        // ダメージの原因がエンティティの場合、攻撃者を取得する
        if (event instanceof EntityDamageByEntityEvent) {
            Entity damagerEntity = ((EntityDamageByEntityEvent) event).getDamager();

            // 発射物の場合、撃ったプレイヤーを取得
            if (damagerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) damagerEntity;
                if (projectile.getShooter() instanceof Player) {
                    damager = (Player) projectile.getShooter();
                }
            } else if (damagerEntity instanceof Player) {
                damager = (Player) damagerEntity;
            }
        }

        // 攻撃者がノックダウン状態なら、攻撃をキャンセル
        if (damager != null && damager.hasPotionEffect(PotionEffectType.UNLUCK)) {
            event.setCancelled(true);
            return;
        }

        // 攻撃されたプレイヤーの透明化を解除
        victim.removePotionEffect(PotionEffectType.INVISIBILITY);

        // 既にノックダウン状態、または無敵のトーテムを持っている場合は処理しない
        if (victim.hasPotionEffect(PotionEffectType.UNLUCK) ||
                victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return;
        }

        // スコア計算
        double finalDamage = event.getFinalDamage();
        if (damager != null && !damager.getUniqueId().equals(victim.getUniqueId())) {
            victimProfiles.get(victim.getUniqueId()).addDamager(damager);
            scoreProfiles.get(damager.getUniqueId()).addDamageScore(finalDamage);
        }

        // ★★★ 改善点2: 吸収ハートを考慮した正確な致死判定 ★★★
        if ((victim.getHealth() + victim.getAbsorptionAmount()) <= finalDamage) {
            // イベントをキャンセルして、自前のノックダウン処理に切り替える
            event.setCancelled(true);
            executeKnockdown(victim, damager);
        }
    }

    /**
     * プレイヤーをノックダウンさせる処理
     * @param victim ノックダウンされるプレイヤー
     * @param damager 最後にダメージを与えたプレイヤー (nullの場合あり)
     */
    private void executeKnockdown(Player victim, @Nullable Player damager) {
        // インベントリのバックアップ
        ItemStack[] itemStacks = new ItemStack[41];
        for (int i = 0; i < 40; i++) { // 0-39: メインインベントリ+防具スロット
            itemStacks[i] = victim.getInventory().getItem(i);
        }
        itemStacks[40] = victim.getInventory().getItemInOffHand(); // 40: オフハンド
        V.knockDownBU.put(victim, itemStacks);

        // ノックダウン状態にする
        victim.getInventory().clear();
        victim.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, Integer.MAX_VALUE, 0, true, true));
        victim.setFoodLevel(0);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 4, true, true));
        victim.setHealth(victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()); // 体力を最大にする

        // 姿勢をSWIMMINGに変更（全プレイヤーに通知）
        Main.sendPosePacket(victim, EnumWrappers.EntityPose.SWIMMING);

        // ノックダウンメッセージの送信とサウンド再生
        sendKnockdownMessages(victim, damager);

        // アシストとノッカーの記録
        UUID knockerUuid = null;
        if (damager != null) {
            knockerUuid = damager.getUniqueId();
        } else {
            // damagerがnullの場合、getDamager()から取得を試みる
            Set<UUID> damagers = victimProfiles.get(victim.getUniqueId()).getDamager();
            if (!damagers.isEmpty()) {
                knockerUuid = damagers.iterator().next();
            }
        }

        if (knockerUuid != null) {
            victimProfiles.get(victim.getUniqueId()).setKnocker(knockerUuid);
        }
        victimProfiles.get(victim.getUniqueId()).setAssistant(victimProfiles.get(victim.getUniqueId()).getDamager());

        // 部隊全滅判定
        checkTeamWipe(victim);
    }

    private void sendKnockdownMessages(Player victim, @Nullable Player damager) {
        UUID knockerUuid = null;
        if (damager != null) {
            knockerUuid = damager.getUniqueId();
        } else {
            // damagerがnullの場合、getDamager()から取得を試みる
            Set<UUID> damagers = victimProfiles.get(victim.getUniqueId()).getDamager();
            if (!damagers.isEmpty()) {
                knockerUuid = damagers.iterator().next();
            }
        }

        if (knockerUuid == null) return;

        OfflinePlayer knocker = Bukkit.getOfflinePlayer(knockerUuid);
        Team kTeam = victim.getScoreboard().getPlayerTeam(knocker);
        TextColor kTeamColor = (kTeam != null && kTeam.color() != null) ? kTeam.color() : NamedTextColor.WHITE;

        Team vTeam = victim.getScoreboard().getPlayerTeam(victim);
        TextColor vTeamColor = (vTeam != null && vTeam.color() != null) ? vTeam.color() : NamedTextColor.WHITE;

        Component knockerName = Component.text(knocker.getName()).color(kTeamColor);
        Component victimName = Component.text(victim.getName()).color(vTeamColor);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(knocker.getPlayer())) {
                onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_SAND_BREAK, 1, 0.4f);
                onlinePlayer.playSound(onlinePlayer, Sound.ITEM_TOTEM_USE, 0.1f, 2);
                onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 10, 1);
                float pitch = Math.min(1.0f, scoreProfiles.get(onlinePlayer.getUniqueId()).getKnockScore() * 0.2f);
                onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 1 + pitch);
                onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_ANVIL_PLACE, 1, 0.5f + pitch);
                onlinePlayer.sendMessage(victimName.append(Component.text("をノックダウン!!")).decorate(TextDecoration.BOLD));
            } else if (onlinePlayer.equals(victim)) {
                onlinePlayer.sendMessage(knockerName.append(Component.text("にノックダウンされた!!")).decorate(TextDecoration.BOLD));
            } else if ((vTeam != null && vTeam.hasEntry(onlinePlayer.getName())) || (kTeam != null && kTeam.hasEntry(onlinePlayer.getName()))) {
                onlinePlayer.sendMessage(knockerName.append(Component.text("が")).append(victimName).append(Component.text("をノックダウン!!")).decorate(TextDecoration.BOLD));
            } else {
                onlinePlayer.sendMessage(knockerName.append(Component.text("が")).append(victimName).append(Component.text("をノックダウン")));
            }
        }
    }

    private void checkTeamWipe(Player victim) {
        Team playerTeam = victim.getScoreboard().getEntryTeam(victim.getName());
        if (playerTeam == null) {
            victim.setHealth(0);
            victim.sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
            if (scoreProfiles.containsKey(victim.getUniqueId())) {
                scoreProfiles.get(victim.getUniqueId()).setRankScore(jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());
            }
            return;
        }

        boolean isTeamWiped = true;
        for (String entry : playerTeam.getEntries()) {
            OfflinePlayer offlineTeammate = Bukkit.getOfflinePlayer(entry);
            if (offlineTeammate.isOnline()) {
                Player teammate = offlineTeammate.getPlayer();
                if (teammate != null && ( teammate.getGameMode() == GameMode.ADVENTURE || teammate.getGameMode() == GameMode.SURVIVAL ) && !teammate.hasPotionEffect(PotionEffectType.UNLUCK)) {
                    isTeamWiped = false;
                    break;
                }
            }
        }

        if (isTeamWiped) {
            for (String entry : playerTeam.getEntries()) {
                OfflinePlayer offlineTeammate = Bukkit.getOfflinePlayer(entry);
                if (offlineTeammate.isOnline()) {
                    Player teammate = offlineTeammate.getPlayer();
                    if (teammate != null && (teammate.getGameMode() == GameMode.SURVIVAL || teammate.getGameMode() == GameMode.ADVENTURE)) {
                        teammate.setHealth(0);
                    }
                    if (teammate != null) {
                        teammate.sendTitle(ChatColor.RED + "部隊全滅", "", 20, 40, 10);
                        teammate.addScoreboardTag("Rank" + jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());
                    }
                }
                if (scoreProfiles.containsKey(offlineTeammate.getUniqueId())) {
                    scoreProfiles.get(offlineTeammate.getUniqueId()).setRankScore(jp.houlab.mochidsuki.battleroyalecore3.V.getTeamCount());
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
        UUID knockerUuid = victimProfiles.get(event.getPlayer().getUniqueId()).getKnocker();

        // ★ 攻撃者(knocker)のスコアを加算
        if (knockerUuid != null) {
            // scoreProfilesに存在するか確認してから加算
            ScoreProfile knockerProfile = scoreProfiles.get(knockerUuid);
            if (knockerProfile != null) {
                knockerProfile.addKillScore();
            }
        }

        // ★ アシストしたプレイヤーのスコアを加算
        for (UUID assistantUuid : victimProfiles.get(event.getEntity().getUniqueId()).getAssistant()) {
            // アシスト者が攻撃者自身でないことを確認
            if (knockerUuid == null || !assistantUuid.equals(knockerUuid)) {
                // scoreProfilesに存在するか確認してから加算
                ScoreProfile assistantProfile = scoreProfiles.get(assistantUuid);
                if (assistantProfile != null) {
                    assistantProfile.addAssistScore();
                }
            }
        }

        // ★ 死亡したプレイヤーのスコアを加算
        ScoreProfile victimProfile = scoreProfiles.get(event.getEntity().getUniqueId());
        if (victimProfile != null) {
            victimProfile.addDeathScore();
        }

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
