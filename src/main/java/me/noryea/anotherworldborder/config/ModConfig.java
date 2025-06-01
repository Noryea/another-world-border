package me.noryea.anotherworldborder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("anotherworldborder.json");

    private int damageInterval = 20; // 伤害检查间隔（ticks）
    private double damageMultiplier = 0.5; // 伤害倍率
    private boolean showParticles = true; // 是否显示粒子

    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ModConfig.class);
                this.damageInterval = loaded.damageInterval;
                this.damageMultiplier = loaded.damageMultiplier;
                this.showParticles = loaded.showParticles;
            } else {
                save(); // 创建默认配置
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter 方法
    public int getDamageInterval() {
        return damageInterval;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public boolean isShowParticles() {
        return showParticles;
    }
}
