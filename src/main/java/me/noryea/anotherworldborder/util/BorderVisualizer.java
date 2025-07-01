package me.noryea.anotherworldborder.util;

import me.noryea.anotherworldborder.data.BorderManager;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;

public class BorderVisualizer {
    private static final int MAX_DISTANCE = 64; // 最大可见距离
    private static final int WHITE_COLOR = ColorHelper.fromFloats(1f, 1f, 1f, 1f);
    private static final ParticleEffect PARTICLE = new DustColorTransitionParticleEffect(WHITE_COLOR, WHITE_COLOR, 4.0f);
    // 粒子密度：每多少个方块长度放置一个粒子生成点
    private static final double BLOCKS_PER_PARTICLE_POINT = 8.5;
    // 每个分段的目标粒子数
    private static final int PARTICLES_PER_SEGMENT = 2;

    public static void visualizeBorders(ServerWorld world) {
        BorderManager borderState = BorderManager.getBorderState(world);

        // 为每个玩家生成边界粒子
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (shouldShowBorder(player, borderState)) {
                showBorderParticles(player, borderState, world);
            }
        }
    }

    private static boolean shouldShowBorder(ServerPlayerEntity player, BorderManager borderState) {
        if (player.isSpectator()) return true;
        double dx = player.getX() - borderState.getCenterX();
        double dz = player.getZ() - borderState.getCenterZ();
        double playerDistanceSqr = dx * dx + dz * dz;
        double radius = borderState.getRadius() * 0.5;
        return playerDistanceSqr > radius * radius;
    }

    private static void showBorderParticles(ServerPlayerEntity player, BorderManager border, ServerWorld world) {
        Vec3d playerPos = player.getPos();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double radius = border.getRadius();

        double dx = playerPos.x - centerX;
        double dz = playerPos.z - centerZ;
        double cameraAngle = Math.atan2(dz, dx);

        // 目标弧长 = BLOCKS_PER_PARTICLE_POINT * PARTICLES_PER_SEGMENT
        double targetArcLength = BLOCKS_PER_PARTICLE_POINT * PARTICLES_PER_SEGMENT;
        double segmentAngle = targetArcLength / radius;
        // 限制角度在合理范围内
        segmentAngle = MathHelper.clamp(segmentAngle, 0.1, 1.2);

        double startAngle = Math.floor(cameraAngle / segmentAngle) * segmentAngle;

        float tickOffset = (world.getTime() % 30) / 30f;
        LinkedList<Packet<? super ClientPlayPacketListener>> bundle = new LinkedList<>();
        double visibleRangeAngle = 0.7 * Math.PI; // 半圆范围

        // 向右渲染部分圆周
        for (double angle = startAngle; angle < startAngle + visibleRangeAngle/2; angle += segmentAngle) {
            // 应用tickOffset到角度上实现动画效果
            double animatedAngle = angle + tickOffset * segmentAngle;
            // 计算段的两个端点
            double x1 = centerX + radius * Math.cos(animatedAngle);
            double z1 = centerZ + radius * Math.sin(animatedAngle);
            double x2 = centerX + radius * Math.cos(animatedAngle + segmentAngle);
            double z2 = centerZ + radius * Math.sin(animatedAngle + segmentAngle);

            // 检查这个段是否在玩家可见范围内
            Vec3d segmentCenter = new Vec3d((x1 + x2) / 2, playerPos.y, (z1 + z2) / 2);
            if (playerPos.squaredDistanceTo(segmentCenter) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            renderParticlesOnSegment(playerPos, x1, z1, x2, z2, bundle);
        }

        // 向左渲染部分圆周
        for (double angle = startAngle; angle > startAngle - visibleRangeAngle/2; angle -= segmentAngle) {
            // 应用tickOffset到角度上实现动画效果
            double animatedAngle = angle + tickOffset * segmentAngle;
            // 计算段的两个端点
            double x1 = centerX + radius * Math.cos(animatedAngle);
            double z1 = centerZ + radius * Math.sin(animatedAngle);
            double x2 = centerX + radius * Math.cos(animatedAngle - segmentAngle);
            double z2 = centerZ + radius * Math.sin(animatedAngle - segmentAngle);

            // 检查这个段是否在玩家可见范围内
            Vec3d segmentCenter = new Vec3d((x1 + x2) / 2, playerPos.y, (z1 + z2) / 2);
            if (playerPos.squaredDistanceTo(segmentCenter) > MAX_DISTANCE * MAX_DISTANCE)
                continue;
            renderParticlesOnSegment(playerPos, x1, z1, x2, z2, bundle);
        }

        // 发包
        if (!bundle.isEmpty())
            player.networkHandler.sendPacket(new BundleS2CPacket(bundle));
    }

    /**
     * 辅助方法：在线段上渲染粒子
     */
    private static void renderParticlesOnSegment(Vec3d playerPos,
                                                 double x1, double z1, double x2, double z2,
                                                 LinkedList<Packet<? super ClientPlayPacketListener>> bundle) {
        double segmentLength = Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        int pointsOnSegment = Math.max(1, (int) (segmentLength / BLOCKS_PER_PARTICLE_POINT));

        // 在线段上均匀分布粒子
        for (int j = 0; j < pointsOnSegment; j++) {
            double t = j / (double) (pointsOnSegment - 1);
            // 防止除以零的情况
            if (pointsOnSegment == 1) t = 0.5;

            double x = x1 + (x2 - x1) * t;
            double z = z1 + (z2 - z1) * t;
            int blockY = (int) playerPos.y;
            // 垂直方向的粒子
            var packet = new ParticleS2CPacket(
                    PARTICLE,
                    true,
                    false,
                    x, blockY, z,
                    0, 20, 0,
                    0,
                    4
            );
            bundle.add(packet);
        }
    }
}
