package jp.houlab.mochidsuki.knockdown;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static jp.houlab.mochidsuki.knockdown.Main.*;

public class EveryTicks extends BukkitRunnable {

    // ゴーストシュルカーのEntityID管理用
    private static final Map<UUID, Integer> shulkerMap = new HashMap<>();
    private final Random random = new Random();

    private static int tick;


    @Override
    public void run() {
        tick++;
        if(tick % 200 == 0) tick = 0;

        Set<UUID> onlineUuids = new HashSet<>();

        for(Player player : plugin.getServer().getOnlinePlayers()) {
            onlineUuids.add(player.getUniqueId());

            if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                // --- ノックダウン中 ---

                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3, 128, false, false));

                if(config.getBoolean("allowDownPlayerGlowing") && tick == 0) player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 1, false, false));


                // 水中や飛行中は自然な姿勢になるのでゴースト処理は行わない
                if (player.isInWater() || player.isGliding()) {
                    removeShulker(player);
                    continue;
                }

                // ---------------------------------------------------------
                // 【本人向け】ゴーストシュルカーによる物理的這いずり化
                // ---------------------------------------------------------
                // 本人のクライアントには「物理的な天井がある」と認識させる必要がある。
                // 姿勢の変更はPacketAdapterで傍受して行うため、ここでは送信しない。

                int entityId;
                if (!shulkerMap.containsKey(player.getUniqueId())) {
                    // 新規生成
                    entityId = random.nextInt(100000) + 100000000;
                    shulkerMap.put(player.getUniqueId(), entityId);
                    spawnGhostShulker(player, entityId);
                } else {
                    entityId = shulkerMap.get(player.getUniqueId());
                }

                // シュルカーを「頭の位置(Y+1.0)」に配置する
                // これにより高さ1.0mの空間ができ、立位(1.8m)もスニーク(1.5m)もできないため這いずり(0.6m)になる
                Location shulkerLoc = player.getLocation().add(0, 1.0, 0);
                teleportGhostShulker(player, entityId, shulkerLoc);

            } else {
                // --- ノックダウン解除 ---
                removeShulker(player);
            }
        }

        // ログアウトしたプレイヤーのデータ整理
        shulkerMap.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    /**
     * ゴーストシュルカー生成 (本人にのみ送信)
     */
    private void spawnGhostShulker(Player player, int entityId) {
        try {
            PacketContainer spawnPacket = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, UUID.randomUUID());
            spawnPacket.getEntityTypeModifier().write(0, EntityType.SHULKER);

            // 初期位置
            Location loc = player.getLocation().add(0, 1.0, 0);
            spawnPacket.getDoubles()
                    .write(0, loc.getX())
                    .write(1, loc.getY())
                    .write(2, loc.getZ());

            // 透明化メタデータ (0x20 = Invisible)
            PacketContainer metaPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metaPacket.getIntegers().write(0, entityId);
            List<WrappedDataValue> metadata = new ArrayList<>();
            metadata.add(new WrappedDataValue(
                    0, // Entity Status Index
                    WrappedDataWatcher.Registry.get(Byte.class),
                    (byte) 0x20 // Invisible Flag
            ));
            metaPacket.getDataValueCollectionModifier().write(0, metadata);

            // 本人にのみ送信
            manager.sendServerPacket(player, spawnPacket);
            manager.sendServerPacket(player, metaPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 1.21.2+ 用のパケットタイプをキャッシュ
    private static PacketType positionSyncPacketType = null;
    private static boolean positionSyncChecked = false;

    /**
     * ゴーストシュルカー移動 (本人にのみ送信)
     * 1.21.2+ では ENTITY_POSITION_SYNC を使用
     */
    private void teleportGhostShulker(Player player, int entityId, Location loc) {
        try {
            // 1.21.2+ 用パケットタイプを動的に取得
            if (!positionSyncChecked) {
                positionSyncChecked = true;
                try {
                    // リフレクションで ENTITY_POSITION_SYNC を取得
                    positionSyncPacketType = (PacketType) PacketType.Play.Server.class
                            .getField("ENTITY_POSITION_SYNC").get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // 存在しない場合は null のまま
                    positionSyncPacketType = null;
                }
            }

            if (Main.isVersion1_21_2OrHigher && positionSyncPacketType != null) {
                // 1.21.2+ 用パケット構造
                // PositionMoveRotation という内部クラスを使用する構造に変更された
                try {
                    PacketContainer teleportPacket = manager.createPacket(positionSyncPacketType);

                    // VarInt でエンティティID
                    teleportPacket.getIntegers().write(0, entityId);

                    // 1.21.2+ では位置情報が内部クラスでラップされている可能性
                    // StructureModifier で全フィールドを確認
                    var modifier = teleportPacket.getModifier();

                    // Vec3D (位置) を書き込み - インデックス1
                    if (modifier.size() > 1) {
                        Object positionObj = modifier.read(1);
                        if (positionObj != null) {
                            // Vec3D の場合、リフレクションで設定
                            Class<?> vec3Class = positionObj.getClass();
                            try {
                                var constructor = vec3Class.getConstructor(double.class, double.class, double.class);
                                Object newPos = constructor.newInstance(loc.getX(), loc.getY(), loc.getZ());
                                modifier.write(1, newPos);
                            } catch (Exception e) {
                                // フォールバック: フィールドを直接設定
                            }
                        }
                    }

                    // Vec3D (速度) を書き込み - インデックス2
                    if (modifier.size() > 2) {
                        Object velocityObj = modifier.read(2);
                        if (velocityObj != null) {
                            Class<?> vec3Class = velocityObj.getClass();
                            try {
                                var constructor = vec3Class.getConstructor(double.class, double.class, double.class);
                                Object newVel = constructor.newInstance(0.0, 0.0, 0.0);
                                modifier.write(2, newVel);
                            } catch (Exception e) {
                                // フォールバック
                            }
                        }
                    }

                    // Yaw, Pitch (Float)
                    var floatModifier = teleportPacket.getModifier().withType(Float.class);
                    floatModifier.writeSafely(0, 0.0f);
                    floatModifier.writeSafely(1, 0.0f);

                    // On Ground
                    teleportPacket.getBooleans().writeSafely(0, false);

                    manager.sendServerPacket(player, teleportPacket);
                } catch (Exception e) {
                    // フォールバック: ENTITY_TELEPORT を試す
                    sendLegacyTeleportPacket(player, entityId, loc);
                }
            } else {
                sendLegacyTeleportPacket(player, entityId, loc);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 1.20.x 用のテレポートパケットを送信
     */
    private void sendLegacyTeleportPacket(Player player, int entityId, Location loc) {
        try {
            PacketContainer teleportPacket = manager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles()
                    .write(0, loc.getX())
                    .write(1, loc.getY())
                    .write(2, loc.getZ());
            teleportPacket.getBytes()
                    .write(0, (byte) 0)
                    .write(1, (byte) 0);
            teleportPacket.getBooleans().write(0, false);

            manager.sendServerPacket(player, teleportPacket);
        } catch (Exception e) {
            // 無視 (パケット構造が異なる場合)
        }
    }

    /**
     * ゴーストシュルカー削除
     */
    private void removeShulker(Player player) {
        if (shulkerMap.containsKey(player.getUniqueId())) {
            int entityId = shulkerMap.get(player.getUniqueId());
            try {
                // Destroy Packet
                PacketContainer destroyPacket = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                // ProtocolLib 5.0.0+ 向けの書き方 (List<Integer>)
                destroyPacket.getIntLists().write(0, Collections.singletonList(entityId));
                manager.sendServerPacket(player, destroyPacket);
            } catch (Exception e) {
                // フォールバック (int[])
                try {
                    PacketContainer destroyPacket = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    destroyPacket.getIntegerArrays().write(0, new int[]{entityId});
                    manager.sendServerPacket(player, destroyPacket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            shulkerMap.remove(player.getUniqueId());
        }
    }
}