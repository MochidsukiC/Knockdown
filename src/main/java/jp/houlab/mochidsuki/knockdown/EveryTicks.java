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

import static jp.houlab.mochidsuki.knockdown.Main.manager;
import static jp.houlab.mochidsuki.knockdown.Main.plugin;

public class EveryTicks extends BukkitRunnable {

    // ゴーストシュルカーのEntityID管理用
    private static final Map<UUID, Integer> shulkerMap = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void run() {
        Set<UUID> onlineUuids = new HashSet<>();

        for(Player player : plugin.getServer().getOnlinePlayers()) {
            onlineUuids.add(player.getUniqueId());

            if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                // --- ノックダウン中 ---

                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3, 128, false, false));

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

    /**
     * ゴーストシュルカー移動 (本人にのみ送信)
     */
    private void teleportGhostShulker(Player player, int entityId, Location loc) {
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
            e.printStackTrace();
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