
package ru.w1nlord.w1nhealthindicators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("w1nhealthindicators.json");
    private static Data data = new Data();

    private Config() {
    }

    public enum HealthSource {
        SCOREBOARD, FORMATTED_TEXT, NONE
    }

    public static final class Data {
        public boolean renderHearts = true;
        public boolean renderArmor = true;
        public HealthSource healthSource = HealthSource.FORMATTED_TEXT;
        public float scale = 1.0F;
    }
    public static float getScale() { return Math.max(0.5F, Math.min(2.0F, data.scale)); }
    public static void setScale(float value) { data.scale = Math.max(0.5F, Math.min(2.0F, value)); }

    public static boolean shouldRenderHearts() { return data.renderHearts; }

    public static boolean shouldRenderArmor() { return data.renderArmor; }

    public static HealthSource getHealthSource() { return data.healthSource; }

    public static void setRenderHearts(boolean value) { data.renderHearts = value; }

    public static void setRenderArmor(boolean value) { data.renderArmor = value; }

    public static void setHealthSource(HealthSource value) { data.healthSource = value; }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
                if (loaded != null) data = loaded;
            }
        } catch (Exception e) {
            System.err.println("Failed to load W1nHealthIndicators config: " + e.getMessage());
        }

        if (data.healthSource == null) data.healthSource = HealthSource.FORMATTED_TEXT;
        if (!Float.isFinite(data.scale)) data.scale = 1.0F;
        data.scale = Math.max(0.5F, Math.min(2.0F, data.scale));
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("Failed to save W1nHealthIndicators config: " + e.getMessage());
        }
    }
}
