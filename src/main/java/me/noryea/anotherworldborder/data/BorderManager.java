package me.noryea.anotherworldborder.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.noryea.anotherworldborder.AnotherWorldBorder;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.network.ServerPlayerEntity;

public class BorderManager extends PersistentState {
    private static final Codec<BorderManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("centerX").forGetter(state -> state.centerX),
                    Codec.DOUBLE.fieldOf("centerZ").forGetter(state -> state.centerZ),
                    Codec.DOUBLE.fieldOf("radius").forGetter(state -> state.radius),
                    Codec.DOUBLE.fieldOf("damagePerSecond").forGetter(state -> state.damagePerSecond)
            ).apply(instance, BorderManager::new)
    );
    public static final PersistentStateType<BorderManager> TYPE = new PersistentStateType<>(
            "another-world-border",
            BorderManager::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX
    );

    // 简化的成员变量 - 每个维度只有一个毒圈
    private double centerX;
    private double centerZ;
    private double radius;
    private double damagePerSecond;

    // 默认构造函数
    public BorderManager() {
        this(0, 0, 1000, 1.0);
    }

    // 带参数的构造函数
    public BorderManager(double centerX, double centerZ, double radius, double damagePerSecond) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.damagePerSecond = damagePerSecond;
    }

    // 从ServerWorld获取对应的BorderManager实例
    public static BorderManager getBorderState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    // 设置边界参数
    public void setBorder(double centerX, double centerZ, double radius) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        markDirty();
    }

    // 设置边界中心
    public void setCenterPosition(double centerX, double centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        markDirty();
    }

    // 调整边界半径
    public void addBorderRadius(double distance) {
        this.radius = Math.max(1.0, this.radius + distance);
        markDirty();
    }

    // 检查坐标是否在边界内
    public boolean isInside(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return (dx * dx + dz * dz) <= (radius * radius);
    }

    // 获取到边界的距离
    public double getDistanceFromBorder(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        return Math.max(distance - radius, 0);
    }

    // 处理服务器tick
    public void tick(ServerWorld world) {
        // 检查并伤害在边界外的玩家
        if (world.getServer().getTicks() % AnotherWorldBorder.getConfig().getDamageInterval() == 0) { // 每秒检查一次
            applyDamageToPlayersOutsideBorder(world);
        }
    }

    // 对边界外的玩家造成伤害
    private void applyDamageToPlayersOutsideBorder(ServerWorld world) {

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;

            Vec3d pos = player.getPos();
            if (!isInside(pos.x, pos.z)) {
                double damage = damagePerSecond;
                // 距离边界越远伤害越高
                double distance = getDistanceFromBorder(pos.x, pos.z);
                damage += (distance / 16.0) * AnotherWorldBorder.getConfig().getDamageMultiplier();
                player.damage(world, player.getDamageSources().outsideBorder(), (float)damage);
                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.literal("你因为不在安全区中扣血了!")));
            }
        }
    }

    // Getter方法
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }
}
