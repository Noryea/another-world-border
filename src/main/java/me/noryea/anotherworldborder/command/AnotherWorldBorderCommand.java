package me.noryea.anotherworldborder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.noryea.anotherworldborder.data.BorderManager;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AnotherWorldBorderCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("anotherworldborder")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("radius")
                        .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                .executes(context -> setBorder(context, null))
                                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                        .executes(context -> setBorder(context,
                                                DimensionArgumentType.getDimensionArgument(context, "dimension"))))))

                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg(-1000.0, 1000.0))
                                .executes(context -> addBorder(context, null))
                                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                        .executes(context -> addBorder(context,
                                                DimensionArgumentType.getDimensionArgument(context, "dimension"))))))

                .then(CommandManager.literal("center")
                        .executes(context -> centerBorder(context, null))
                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                .executes(context -> centerBorder(context,
                                        DimensionArgumentType.getDimensionArgument(context, "dimension")))))

                .then(CommandManager.literal("info")
                        .executes(context -> showBorderInfo(context, null))
                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                .executes(context -> showBorderInfo(context,
                                        DimensionArgumentType.getDimensionArgument(context, "dimension")))))

                .then(CommandManager.literal("distanceFromBorder")
                        .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                .executes(context -> getDistanceFromBorder(context.getSource(), EntityArgumentType.getEntity(context, "entity")) )))
        );
    }

    private static int getDistanceFromBorder(ServerCommandSource source, Entity entity) {
        ServerWorld world = source.getWorld();
        BorderManager borderState = BorderManager.getBorderState(world);
        if (entity.getWorld() != world) return 0;
        int distance = (int) (borderState.getDistanceFromBorder(entity.getX(), entity.getZ()) + 0.5);
        source.sendFeedback(() -> Text.literal("distance: " + distance), false);
        return distance;
    }

    private static int setBorder(CommandContext<ServerCommandSource> context, ServerWorld specifiedWorld) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = specifiedWorld != null ? specifiedWorld : source.getWorld();
        double radius = DoubleArgumentType.getDouble(context, "radius");
        String dimensionId = world.getRegistryKey().getValue().toString();

        BorderManager borderState = BorderManager.getBorderState(world);
        borderState.setBorder(borderState.getCenterX(), borderState.getCenterZ(), radius);
        source.sendFeedback(() -> Text.literal("Set border in dimension " + dimensionId + " to radius " + radius), true);
        return 1;
    }

    private static int addBorder(CommandContext<ServerCommandSource> context, ServerWorld specifiedWorld) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = specifiedWorld != null ? specifiedWorld : source.getWorld();
        double distance = DoubleArgumentType.getDouble(context, "distance");

        String dimensionId = world.getRegistryKey().getValue().toString();
        BorderManager borderState = BorderManager.getBorderState(world);

        borderState.addBorderRadius(distance);

        source.sendFeedback(() -> Text.literal("Changed border in dimension " + dimensionId + " to radius " + borderState.getRadius()), true);
        return 1;
    }

    private static int centerBorder(CommandContext<ServerCommandSource> context, ServerWorld specifiedWorld) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = specifiedWorld != null ? specifiedWorld : source.getWorld();

        String dimensionId = world.getRegistryKey().getValue().toString();
        BorderManager borderState = BorderManager.getBorderState(world);

        Vec3d pos = source.getPosition();

        borderState.setCenterPosition(pos.x, pos.z);

        source.sendFeedback(() -> Text.literal("Set border center in dimension " + dimensionId + " to " + pos.x + ", " + pos.z), true);
        return 1;
    }

    private static int showBorderInfo(CommandContext<ServerCommandSource> context, ServerWorld specifiedWorld) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = specifiedWorld != null ? specifiedWorld : source.getWorld();

        String dimensionId = world.getRegistryKey().getValue().toString();
        BorderManager borderState = BorderManager.getBorderState(world);

        double centerX = borderState.getCenterX();
        double centerZ = borderState.getCenterZ();
        double radius = borderState.getRadius();

        source.sendFeedback(() -> Text.literal("Border in dimension " + dimensionId + ":")
                .append("\nCenter: " + centerX + ", " + centerZ)
                .append("\nRadius: " + radius), false);

        return 1;
    }
}
