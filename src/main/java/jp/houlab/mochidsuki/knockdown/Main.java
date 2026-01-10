package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;

/**
 * メインクラス
 */
public final class Main extends JavaPlugin {
    public static Plugin plugin;
    public static ProtocolManager manager;
    public static FileConfiguration config;

    /**
     * 起動時の初期化処理
     */
    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Listener(), this);

        new EveryTicks().runTaskTimer(this, 1L, 1L);
        plugin = this;

        saveDefaultConfig();
        config = getConfig();

        getCommand("watchscore").setExecutor(new CommandListener() );

        manager = ProtocolLibrary.getProtocolManager();

        // ノックダウン中のプレイヤーの姿勢を傍受して変更
        registerPoseInterceptor();
    }

    /**
     * Entity Metadataパケットを傍受してノックダウン中のプレイヤーの姿勢をSWIMMINGに変更
     */
    private void registerPoseInterceptor() {
        manager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_METADATA
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    // パケットから対象エンティティIDを取得
                    int entityId = event.getPacket().getIntegers().read(0);

                    // エンティティIDからプレイヤーを特定
                    Player target = null;
                    for (Player player : getServer().getOnlinePlayers()) {
                        if (player.getEntityId() == entityId) {
                            target = player;
                            break;
                        }
                    }

                    // 対象がノックダウン中のプレイヤーでない場合はスキップ
                    if (target == null || !target.hasPotionEffect(PotionEffectType.UNLUCK)) {
                        return;
                    }

                    // 水中や飛行中は自然な姿勢にするためスキップ
                    if (target.isInWater() || target.isGliding()) {
                        return;
                    }

                    // メタデータリストを取得
                    List<WrappedDataValue> dataValues = event.getPacket().getDataValueCollectionModifier().read(0);

                    // Pose Serializerの取得
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer poseSerializer =
                            com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());

                    // Index 6 (Pose)のデータがあるか確認し、あれば書き換え、なければ追加
                    boolean poseFound = false;
                    for (int i = 0; i < dataValues.size(); i++) {
                        WrappedDataValue value = dataValues.get(i);
                        if (value.getIndex() == 6) {
                            // Poseを上書き
                            dataValues.set(i, new WrappedDataValue(
                                    6,
                                    poseSerializer,
                                    EnumWrappers.EntityPose.SWIMMING
                            ));
                            poseFound = true;
                            break;
                        }
                    }

                    // Poseデータがなければ追加
                    if (!poseFound) {
                        dataValues.add(new WrappedDataValue(
                                6,
                                poseSerializer,
                                EnumWrappers.EntityPose.SWIMMING
                        ));
                    }

                    // 書き換えたメタデータをパケットに戻す
                    event.getPacket().getDataValueCollectionModifier().write(0, dataValues);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 終了
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * プレイヤーの姿勢を強制的に変更するパケットを全プレイヤーに送信
     * @param target 対象プレイヤー
     * @param pose 変更する姿勢
     */
    public static void sendPosePacket(Player target, EnumWrappers.EntityPose pose) {
        try {
            com.comphenix.protocol.events.PacketContainer posePacket =
                    manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            posePacket.getIntegers().write(0, target.getEntityId());

            // Pose Serializerの取得
            com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer poseSerializer =
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());

            // メタデータリストの作成
            List<WrappedDataValue> dataValues = new java.util.ArrayList<>();
            dataValues.add(new WrappedDataValue(
                    6, // Entity Pose Index (1.20.4)
                    poseSerializer,
                    pose
            ));

            posePacket.getDataValueCollectionModifier().write(0, dataValues);

            // 全プレイヤーに送信
            manager.broadcastServerPacket(posePacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * ノックダウン中のプレイヤーのインベントリをバックアップする。
 */
class V{
    static HashMap<Player, ItemStack[]> knockDownBU = new HashMap<>();
}