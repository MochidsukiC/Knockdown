package jp.houlab.mochidsuki.knockdown.scoreCounterAPI;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class ScoreProfile {
    /**
     * 各プレイヤーのプロファイルを登録する
     * Key - 対象プレイヤー
     * Value - プロファイル
     */
    static final public HashMap<Player, ScoreProfile> scoreProfiles = new HashMap<>();

    private int killScore;
    private int deathScore;
    private int assistScore;
    private double damageScore;
    private int rankScore;



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
        if(damageScore != 0) {
            this.damageScore = this.damageScore + damageScore;
        }else {
            this.damageScore = damageScore;
        }
    }

    public int getRankScore() {
        return rankScore;
    }

    public void setRankScore(int rankScore) {
        this.rankScore = rankScore;
    }
}
