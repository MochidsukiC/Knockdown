package jp.houlab.mochidsuki.knockdown.scoreCounterAPI;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;

import static jp.houlab.mochidsuki.knockdown.Main.plugin;

public class VictimProfile {
    /**
     * 各プレイヤーのプロファイルを登録する
     * Key - 対象プレイヤー
     * Value - プロファイル
     */
    static final public HashMap<Player, VictimProfile> victimProfiles = new HashMap<>();


    private Player knocker;
    private final HashSet<Player> damager = new HashSet<>();
    private HashSet<Player> assistant = new HashSet<>();
    private final HashMap<Player,BukkitTask> damagerSchedulerList = new HashMap<>();

    public void addDamager(Player player) {
        damager.add(player);
        BukkitTask damagerScheduler = new BukkitRunnable() {
                public void run() {
                    damager.remove(player);
                }
            }.runTaskLater(plugin,400);

        if(damagerSchedulerList.containsKey(player)) {
            damagerSchedulerList.get(player).cancel();
            damagerSchedulerList.remove(player);
            damagerSchedulerList.put(player,damagerScheduler);
        }
    }
    public HashSet<Player> getDamager() {
        return damager;
    }


    public Player getKnocker() {
        return knocker;
    }

    public void setKnocker(Player knocker) {
        this.knocker = knocker;
    }

    public HashSet<Player> getAssistant() {
        return assistant;
    }

    public void setAssistant(HashSet<Player> assistant) {
        this.assistant = assistant;
    }

}
