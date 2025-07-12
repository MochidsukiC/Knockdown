package jp.houlab.mochidsuki.knockdown.scoreCounterAPI;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

import static jp.houlab.mochidsuki.battleroyalecore3.Main.plugin;

public class ScoreProfile {
    /**
     * 各プレイヤーのプロファイルを登録する
     * Key - 対象プレイヤー
     * Value - プロファイル
     */
    static final public HashMap<UUID, ScoreProfile> scoreProfiles = new HashMap<>();

    private int killScore = 0;
    private int deathScore = 0;
    private int assistScore = 0;
    private double damageScore = 0;
    private int rankScore = 0;



    public int getKillScore() {
        return killScore;
    }

    public void addKillScore() {
        if(killScore != 0) {
            this.killScore = this.killScore + 1;
        }else {
            this.killScore = 1;
        }
    }

    public int getDeathScore() {
        return deathScore;
    }

    public void addDeathScore() {
        if(deathScore != 0) {
            this.deathScore = this.deathScore + 1;
        }else {
            this.deathScore = 1;
        }
    }

    public int getAssistScore() {
        return assistScore;
    }

    public void addAssistScore() {
        if (assistScore != 0) {
            this.assistScore = this.assistScore + 1;
        }else {
            this.assistScore = 1;
        }

    }

    public double getDamageScore() {
        return damageScore;
    }

    public void addDamageScore(double damageScore) {
        this.damageScore = this.damageScore + damageScore;
    }

    public int getRankScore() {
        return rankScore;
    }

    public void setRankScore(int rankScore) {
        this.rankScore = rankScore;
    }

    public void reset(){
        this.killScore = 0;
        this.deathScore = 0;
        this.assistScore = 0;
        this.damageScore = 0;
        this.rankScore = 0;
    }

    public void sendScore(Player player){
        int rank = getRankScore();
        if(rank == 0){
            rank = 1;
        }

        player.sendMessage("戦績====================");
        player.sendMessage("順位　　　 : "+ rank);
        player.sendMessage("キル数　　 : "+ getKillScore());
        player.sendMessage("アシスト数 : "+ getAssistScore());
        player.sendMessage("ダメージ数 : "+ (int)getDamageScore());
        player.sendMessage("デス数　　 : "+ getDeathScore());
        player.sendMessage("=======================");


    }
}
