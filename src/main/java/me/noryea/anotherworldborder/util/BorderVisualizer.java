package me.noryea.anotherworldborder.util;

import me.noryea.anotherworldborder.data.BorderManager;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;

public class BorderVisualizer {
    private static final int MAX_DISTANCE = 64; // 最大可见距离
    private static final ParticleEffect PARTICLE = ParticleTypes.CLOUD;

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
        float tickOffset = (world.getTime() % 20) / 20.0f;

        // 计算玩家视角指向圆心的角度
        double cameraAngle = Math.atan2(dz, dx);

        // 根据视角角度和距离计算最佳分段角度，略微降低粒子密度
        double segmentAngle = Math.max(0.12, Math.min(0.25, 1.5 / (radius / 8)));

        // 将起始角度向下取整到步长的倍数
        double startAngle = Math.floor(cameraAngle / segmentAngle) * segmentAngle;

        LinkedList<Packet<? super ClientPlayPacketListener>> bundle = new LinkedList<>();
        double visibleRangeAngle = 0.75 * Math.PI; // 半圆范围

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

            // 在段上生成粒子
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
            if (playerPos.squaredDistanceTo(segmentCenter) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }

            // 在段上生成粒子
            renderParticlesOnSegment(playerPos, x1, z1, x2, z2, bundle);
        }

        // 发送最后一批粒子
        if (!bundle.isEmpty()) {
            player.networkHandler.sendPacket(new BundleS2CPacket(bundle));
        }
    }

    // 辅助方法：在线段上渲染粒子
    private static void renderParticlesOnSegment(Vec3d playerPos,
                                                 double x1, double z1, double x2, double z2,
                                                 LinkedList<Packet<? super ClientPlayPacketListener>> bundle) {
        // 计算段长度和需要的粒子数（降低粒子密度）
        double segmentLength = Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        int pointsOnSegment = Math.max(2, (int) (segmentLength * 0.2)); // 控制密度

        // 在线段上均匀分布粒子
        for (int j = 0; j < pointsOnSegment; j++) {
            double t = j / (double) (pointsOnSegment - 1);
            double x = x1 + (x2 - x1) * t;
            double z = z1 + (z2 - z1) * t;
            int blockY = (int) playerPos.y;
            // 垂直方向的粒子
            var packet = new ParticleS2CPacket(
                    PARTICLE,
                    true,
                    false,
                    x, blockY, z,
                    0, 24, 0, 0,
                    12
            );
            bundle.add(packet);
        }
    }
}
