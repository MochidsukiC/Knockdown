package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
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

    // サーバーバージョンが1.21以上かどうか
    private static boolean isVersion1_21OrHigher = false;

    // サーバーバージョンが1.21.2以上かどうか (ENTITY_POSITION_SYNC 使用)
    public static boolean isVersion1_21_2OrHigher = false;

    // NMS の Pose クラス (キャッシュ)
    private static Class<?> nmsPoseClass = null;

    // Pose の enum 定数をキャッシュ (STANDING=0, SWIMMING=3, etc.)
    private static Object[] poseEnumConstants = null;

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

        // バージョン検出
        detectServerVersion();

        // ノックダウン中のプレイヤーの姿勢を傍受して変更
        registerPoseInterceptor();
    }

    /**
     * サーバーバージョンを検出し、NMS Pose クラスを初期化
     */
    private void detectServerVersion() {
        String version = Bukkit.getBukkitVersion();
        // 例: "1.21.4-R0.1-SNAPSHOT" や "1.20.4-R0.1-SNAPSHOT"
        try {
            String[] parts = version.split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            isVersion1_21OrHigher = (major > 1) || (major == 1 && minor >= 21);
            isVersion1_21_2OrHigher = (major > 1) || (major == 1 && (minor > 21 || (minor == 21 && patch >= 2)));

            getLogger().info("Detected server version: " + version + " (1.21+: " + isVersion1_21OrHigher + ", 1.21.2+: " + isVersion1_21_2OrHigher + ")");

            // NMS Pose クラスを取得してキャッシュ
            initNmsPoseClass();

        } catch (Exception e) {
            getLogger().warning("Failed to detect server version, assuming 1.20.x");
            isVersion1_21OrHigher = false;
            isVersion1_21_2OrHigher = false;
        }
    }

    /**
     * NMS の Pose クラスを初期化
     */
    private void initNmsPoseClass() {
        try {
            // 1.21+ では Mojang mappings を使用するため "world.entity.Pose"
            // 1.20.x 以前では "EntityPose"
            if (isVersion1_21OrHigher) {
                try {
                    nmsPoseClass = MinecraftReflection.getMinecraftClass("world.entity.Pose");
                } catch (Exception e) {
                    nmsPoseClass = MinecraftReflection.getMinecraftClass("Pose");
                }
            } else {
                nmsPoseClass = MinecraftReflection.getMinecraftClass("EntityPose");
            }
            poseEnumConstants = nmsPoseClass.getEnumConstants();
            getLogger().info("NMS Pose class loaded: " + nmsPoseClass.getName());
        } catch (Exception e) {
            getLogger().warning("Failed to load NMS Pose class: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * EntityPose を NMS 互換の値に変換
     * STANDING=0, FALL_FLYING=1, SLEEPING=2, SWIMMING=3, SPIN_ATTACK=4, SNEAKING=5, etc.
     */
    private static Object convertPoseForVersion(EnumWrappers.EntityPose pose) {
        if (isVersion1_21OrHigher && poseEnumConstants != null) {
            // 1.21+ では NMS の Pose enum 定数を直接使用
            int ordinal = pose.ordinal();
            if (ordinal < poseEnumConstants.length) {
                return poseEnumConstants[ordinal];
            }
        }
        // 1.20.x では ProtocolLib の EntityPose をそのまま使用
        return pose;
    }

    /**
     * バージョンに応じた Pose Serializer を取得
     */
    private static WrappedDataWatcher.Serializer getPoseSerializer() {
        if (isVersion1_21OrHigher && nmsPoseClass != null) {
            return WrappedDataWatcher.Registry.get(nmsPoseClass);
        }
        return WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());
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

                    // バージョンに応じた Pose Serializer を取得
                    WrappedDataWatcher.Serializer poseSerializer = getPoseSerializer();

                    // バージョンに応じた Pose 値を取得
                    Object swimmingPose = convertPoseForVersion(EnumWrappers.EntityPose.SWIMMING);

                    // Index 6 (Pose)のデータがあるか確認し、あれば書き換え、なければ追加
                    boolean poseFound = false;
                    for (int i = 0; i < dataValues.size(); i++) {
                        WrappedDataValue value = dataValues.get(i);
                        if (value.getIndex() == 6) {
                            // Poseを上書き
                            dataValues.set(i, new WrappedDataValue(
                                    6,
                                    poseSerializer,
                                    swimmingPose
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
                                swimmingPose
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

            // バージョンに応じた Pose Serializer を取得
            WrappedDataWatcher.Serializer poseSerializer = getPoseSerializer();

            // バージョンに応じた Pose 値を取得
            Object convertedPose = convertPoseForVersion(pose);

            // メタデータリストの作成
            List<WrappedDataValue> dataValues = new java.util.ArrayList<>();
            dataValues.add(new WrappedDataValue(
                    6, // Entity Pose Index
                    poseSerializer,
                    convertedPose
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