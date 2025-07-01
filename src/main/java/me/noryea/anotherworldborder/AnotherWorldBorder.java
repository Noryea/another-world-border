package me.noryea.anotherworldborder;

import me.noryea.anotherworldborder.command.AnotherWorldBorderCommand;
import me.noryea.anotherworldborder.config.ModConfig;
import me.noryea.anotherworldborder.data.BorderManager;
import me.noryea.anotherworldborder.util.BorderVisualizer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnotherWorldBorder implements ModInitializer {
    public static final String MOD_ID = "anotherworldborder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;

    @Override
    public void onInitialize() {
        // 初始化配置
        config = new ModConfig();
        config.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AnotherWorldBorderCommand.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 处理每个世界的边界
            server.getWorlds().forEach(world -> {
                BorderManager.getBorderState(world).tick(world);
                BorderVisualizer.visualizeBorders(world);
            });
        });

        LOGGER.info("AnotherWorldBorder mod initialized");
    }

    public static ModConfig getConfig() {
        return config;
    }
}
