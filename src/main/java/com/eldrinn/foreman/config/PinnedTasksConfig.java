package com.eldrinn.foreman.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PinnedTasksConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final int MAX_PINS = 5;

    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    private static class Data {

        @SerializedName("pinnedTasks")
        List<String> pinnedTasks = new ArrayList<>();

        @SerializedName("hud")
        HudPosition hud = new HudPosition();
    }

    private static class HudPosition {

        @SerializedName("anchor")
        String anchor = Anchor.TOP_RIGHT.name();

        @SerializedName("offsetX")
        int offsetX = 0;

        @SerializedName("offsetY")
        int offsetY = 0;

        @SerializedName("scale")
        double scale = 1.0;

        @SerializedName("showBackground")
        boolean showBackground = true;

        @SerializedName("hudVisible")
        boolean hudVisible = true;
    }

    private Data data = new Data();

    public void load() {
        File file = configFile();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                data = loaded;
                if (data.hud == null) data.hud = new HudPosition();
                if (data.pinnedTasks == null) data.pinnedTasks = new ArrayList<>();
            }
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger("foreman")
                .warn("Failed to load foreman_pins.json: {}", e.getMessage());
        }
    }

    public void save() {
        File file = configFile();
        file.getParentFile()
            .mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger("foreman")
                .warn("Failed to save foreman_pins.json: {}", e.getMessage());
        }
    }

    public List<UUID> getPinnedIds() {
        List<UUID> result = new ArrayList<>();
        for (String s : data.pinnedTasks) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    public boolean isPinned(UUID id) {
        return data.pinnedTasks.contains(id.toString());
    }

    public boolean pin(UUID id) {
        String s = id.toString();
        if (data.pinnedTasks.contains(s)) return false;
        if (data.pinnedTasks.size() >= MAX_PINS) return false;
        data.pinnedTasks.add(s);
        save();
        return true;
    }

    public void unpin(UUID id) {
        if (data.pinnedTasks.remove(id.toString())) {
            save();
        }
    }

    public void removeStale(java.util.Set<UUID> existing) {
        boolean changed = data.pinnedTasks.removeIf(s -> {
            try {
                return !existing.contains(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                return true; // remove malformed entries too
            }
        });
        if (changed) save();
    }

    public Anchor getAnchor() {
        try {
            return Anchor.valueOf(data.hud.anchor);
        } catch (IllegalArgumentException e) {
            return Anchor.TOP_RIGHT;
        }
    }

    public int getOffsetX() {
        return data.hud.offsetX;
    }

    public int getOffsetY() {
        return data.hud.offsetY;
    }

    public double getScale() {
        return data.hud.scale;
    }

    public void setScale(double scale) {
        data.hud.scale = Math.max(0.5, Math.min(2.0, scale));
        save();
    }

    public boolean isShowBackground() {
        return data.hud.showBackground;
    }

    public void setShowBackground(boolean showBackground) {
        data.hud.showBackground = showBackground;
        save();
    }

    public boolean isHudVisible() {
        return data.hud.hudVisible;
    }

    public void setHudVisible(boolean hudVisible) {
        data.hud.hudVisible = hudVisible;
        save();
    }

    /** Writes offsetX to memory only — caller must call save() when drag ends. */
    public void setOffsetXRaw(int offsetX) {
        data.hud.offsetX = offsetX;
    }

    /** Writes offsetY to memory only — caller must call save() when drag ends. */
    public void setOffsetYRaw(int offsetY) {
        data.hud.offsetY = offsetY;
    }

    public void setAnchor(Anchor anchor) {
        data.hud.anchor = anchor.name();
    }

    public void resetToDefaults() {
        data.hud.anchor = Anchor.TOP_RIGHT.name();
        data.hud.offsetX = 0;
        data.hud.offsetY = 0;
        data.hud.scale = 1.0;
        data.hud.showBackground = true;
        data.hud.hudVisible = true;
        save();
    }

    public static int getMaxPins() {
        return MAX_PINS;
    }

    private static File configFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/foreman_pins.json");
    }
}
