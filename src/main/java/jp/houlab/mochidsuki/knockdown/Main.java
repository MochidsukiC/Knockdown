package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class Main extends JavaPlugin {
    public static Plugin plugin;
    public static ProtocolManager manager;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Listener(), this);

        new EveryTicks().runTaskTimer(this, 1L, 1L);
        plugin = this;

        manager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

class V{
    static HashMap<Player, ItemStack[]> knockDownBU = new HashMap<>();
}