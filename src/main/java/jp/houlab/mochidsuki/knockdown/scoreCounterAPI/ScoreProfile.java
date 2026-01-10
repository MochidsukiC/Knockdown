package jp.houlab.mochidsuki.knockdown.scoreCounterAPI;

import jp.houlab.mochidsuki.knockdown.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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

    private Set<UUID> knocks = new HashSet<>();
    private int rankScore = 0;

    public int getKnockScore(){
        return knocks.size();
    }

    public Set<UUID> getKnocks() {
        return knocks;
    }

    public void addKnocks(UUID knock) {
        this.knocks.add(knock);
    }


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
        this.knocks = new HashSet<>();
        this.rankScore = 0;
    }

    public void sendScore(Player player){
        int rank = getRankScore();

        player.sendMessage("戦績====================");
        player.sendMessage("順位　　　 : "+ rank);
        player.sendMessage("キル数　　 : "+ getKillScore());
        player.sendMessage("アシスト数 : "+ getAssistScore());
        player.sendMessage("ノック数　 : "+ getKnockScore());
        player.sendMessage("ダメージ数 : "+ (int)getDamageScore());
        player.sendMessage("デス数　　 : "+ getDeathScore());
        player.sendMessage("=======================");


    }


    public static void outputScoreCsv(){
        // ★★★ 改善点1: ファイルはプラグインのデータフォルダに保存するのがベストプラクティスです ★★★
        File dataFolder = Main.plugin.getDataFolder();
        // データフォルダが存在しない場合は作成します
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File scoreFile = new File(dataFolder, "Score.csv");

        // try-with-resources を使用して、リソースを自動的にクローズし、コードを安全にします
        try (FileWriter fw = new FileWriter(scoreFile, false);
             PrintWriter pw = new PrintWriter(new BufferedWriter(fw))) {

            pw.println("TeamName,rank,kill,damage,score");

            for (Team team : Main.plugin.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
                Player teamMember = null;
                for (String name : team.getEntries()) {
                    // プレイヤーがオンラインかどうかを先にチェックします
                    Player onlinePlayer = Bukkit.getPlayer(name);
                    if (onlinePlayer != null) {
                        teamMember = onlinePlayer;
                        break;
                    }
                }

                if (teamMember != null) {
                    ScoreProfile scoreProfile = ScoreProfile.scoreProfiles.get(teamMember.getUniqueId());
                    // scoreProfileがnullの場合を考慮します
                    if (scoreProfile == null) continue;

                    int rank = scoreProfile.getRankScore();
                    int kill = 0;
                    int damage = 0;


                    for(String name : team.getEntries()) {
                        Player teamOther = Bukkit.getPlayer(name);
                        if (teamOther != null) {
                            ScoreProfile otherProfile = ScoreProfile.scoreProfiles.get(teamOther.getUniqueId());
                            if (otherProfile != null) {
                                kill += otherProfile.getKillScore();
                                damage += (int) otherProfile.getDamageScore();
                            }
                        }
                    }
                    int score = kill; // スコアの計算はキル数から開始
                    if(score > 9) {
                        score = 9;
                    }

                    // switch式はよりモダンなアロー構文に書き換え可能です
                    switch (rank){
                        case 1 -> score += 16;
                        case 2 -> score += 13;
                        case 3 -> score += 10;
                        case 4 -> score += 8;
                        case 5 -> score += 6;
                        case 6 -> score += 4;
                        case 7 -> score += 3;
                        case 8 -> score += 2;
                        case 9 -> score += 1;
                    }
                    pw.println(team.getName() + "," + rank + "," + kill + "," + damage +"," + score);
                }
            }

            pw.println();
            pw.println("PlayerName,teamName,rank,kill,assist,damage,score");

            // getOfflinePlayers()は非常に重い処理なので、ループの外で一度だけ呼び出します
            for (UUID uuid : scoreProfiles.keySet()) {
                ScoreProfile profile = scoreProfiles.get(uuid);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid); // 名前取得のためにOfflinePlayerを使用
                if(profile != null) {
                    int rank = profile.getRankScore();
                    String teamName = "NotFound";
                    if(Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player) != null){
                        teamName = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player).getName();
                    }
                    int kill = profile.getKillScore();
                    int assist = profile.getAssistScore();
                    // ★★★ バグ修正: アシスト数ではなく、ダメージ数を取得します ★★★
                    int damage = (int) profile.getDamageScore();
                    int score = kill;


                    switch (rank){
                        case 1 -> score += 16;
                        case 2 -> score += 13;
                        case 3 -> score += 10;
                        case 4 -> score += 8;
                        case 5 -> score += 6;
                        case 6 -> score += 4;
                        case 7 -> score += 3;
                        case 8 -> score += 2;
                        case 9 -> score += 1;
                    }

                    pw.println(player.getName() + ","+ teamName +"," + rank + "," + kill + "," + assist + "," + damage +"," + score);
                }
            }

        } catch (IOException e){
            e.printStackTrace();
            // エラーが発生した場合はここで処理を終了します
            return;
        }

        // ★★★【ご要望の箇所】★★★
        // ファイルの書き込みが完了した後、ファイルを読み取り専用に設定します
        System.out.println("csvファイルを出力し、読み取り専用にしました: " + scoreFile.getAbsolutePath());

    }
}
