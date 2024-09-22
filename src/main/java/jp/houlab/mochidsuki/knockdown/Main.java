package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

/**
 * メインクラス
 */
public final class Main extends JavaPlugin {
    public static Plugin plugin;
    public static ProtocolManager manager;

    /**
     * 起動時の初期化処理
     */
    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Listener(), this);

        new EveryTicks().runTaskTimer(this, 1L, 1L);
        plugin = this;

        manager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * 終了
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

/**
 * ノックダウン中のプレイヤーのインベントリをバックアップする。
 */
class V{
    static HashMap<Player, ItemStack[]> knockDownBU = new HashMap<>();
}