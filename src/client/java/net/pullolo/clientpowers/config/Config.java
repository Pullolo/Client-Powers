package net.pullolo.clientpowers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("clientpowers.json");

    public static Config INSTANCE = new Config();

    // Power
    public String activePower = "NONE";

    // Cosmetics
    public boolean particlesEnabled = true;
    public boolean auraEnabled = true;
    public boolean trailEnabled = false;
    public boolean wingsEnabled = false;

    // Modules
    public boolean dynamicLightEnabled = false;
    public int dynamicLightRadius = 8;
    public boolean playerGlowEnabled = false;
    public float playerGlowRange = 32.0f;
    public boolean toggleSprintEnabled = true;

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) INSTANCE = loaded;
        } catch (IOException ignored) {}
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException ignored) {}
    }
}
