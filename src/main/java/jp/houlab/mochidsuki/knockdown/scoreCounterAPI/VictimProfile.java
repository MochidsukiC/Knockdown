package jp.houlab.mochidsuki.knockdown.scoreCounterAPI;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;

import static jp.houlab.mochidsuki.knockdown.Main.plugin;

public class VictimProfile {
    /**
     * 各プレイヤーのプロファイルを登録する
     * Key - 対象プレイヤー
     * Value - プロファイル
     */
    static final public HashMap<UUID, VictimProfile> victimProfiles = new HashMap<>();


    private UUID knocker;
    private Set<UUID> damager = new LinkedHashSet<>();
    private Set<UUID> assistant = new HashSet<>();
    private HashMap<UUID,BukkitTask> damagerSchedulerList = new HashMap<>();

    public void addDamager(Player player) {
        damager.remove(player.getUniqueId());
        damager.add(player.getUniqueId());
        BukkitTask damagerScheduler = new BukkitRunnable() {
                public void run() {
                    damager.remove(player.getUniqueId());
                }
            }.runTaskLater(plugin,400);

        if(damagerSchedulerList.containsKey(player.getUniqueId())) {
            damagerSchedulerList.get(player.getUniqueId()).cancel();
            damagerSchedulerList.remove(player.getUniqueId());
        }
        damagerSchedulerList.put(player.getUniqueId(),damagerScheduler);
    }
    public Set<UUID> getDamager() {
        return damager;
    }


    public @Nullable UUID getKnocker() {
        return knocker;
    }

    public void setKnocker(UUID knocker) {
        this.knocker = knocker;
    }

    public Set<UUID> getAssistant() {
        return assistant;
    }

    public void setAssistant(Set<UUID> assistant) {
        this.assistant = assistant;
    }

    public void reset(){
        knocker = null;
        damager = new LinkedHashSet<>();
        assistant = new HashSet<>();
        damagerSchedulerList = new HashMap<>();

    }
}
