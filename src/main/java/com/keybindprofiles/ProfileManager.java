package com.keybindprofiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("keybind_profiles.json");

    private static final class Data {
        String active;
        LinkedHashMap<String, LinkedHashMap<String, String>> profiles = new LinkedHashMap<>();
    }

    private static Data data = new Data();

    private ProfileManager() {}

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null && d.profiles != null && !d.profiles.isEmpty()) {
                    data = d;
                }
            } catch (Exception e) {
                KeybindProfilesClient.LOGGER.error("Konnte Profile nicht laden", e);
            }
        }
        if (data.profiles.isEmpty()) {
            data.profiles.put("Profil 1", snapshot());
            data.active = "Profil 1";
            write();
        } else if (data.active == null || !data.profiles.containsKey(data.active)) {
            data.active = data.profiles.keySet().iterator().next();
        }
        applyActive();
    }

    private static Options options() {
        return Minecraft.getInstance().options;
    }

    private static LinkedHashMap<String, String> snapshot() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (KeyMapping km : options().keyMappings) {
            map.put(km.getName(), km.saveString());
        }
        return map;
    }

    private static void apply(Map<String, String> map) {
        Options o = options();
        for (KeyMapping km : o.keyMappings) {
            String saved = map == null ? null : map.get(km.getName());
            km.setKey(saved != null ? InputConstants.getKey(saved) : km.getDefaultKey());
        }
        KeyMapping.resetMapping();
        o.save();
    }

    private static void applyActive() {
        apply(data.profiles.get(data.active));
    }

    private static void write() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            KeybindProfilesClient.LOGGER.error("Konnte Profile nicht speichern", e);
        }
    }

    /** Aktuelle Tasten in das aktive Profil schreiben. */
    public static void saveActive() {
        if (data.active == null) return;
        data.profiles.put(data.active, snapshot());
        write();
    }

    public static List<String> names() {
        return new ArrayList<>(data.profiles.keySet());
    }

    public static String active() {
        return data.active;
    }

    public static void switchTo(String name) {
        if (!data.profiles.containsKey(name)) return;
        saveActive();
        data.active = name;
        applyActive();
        write();
    }

    public static void cycle(int dir) {
        List<String> n = names();
        int i = n.indexOf(data.active);
        switchTo(n.get(Math.floorMod(i + dir, n.size())));
    }

    private static String freeName(String base) {
        int i = 1;
        String name;
        do {
            name = base + " " + i++;
        } while (data.profiles.containsKey(name));
        return name;
    }

    /** Neues Profil mit Standard-Tasten. */
    public static void create() {
        saveActive();
        String name = freeName("Profil");
        data.profiles.put(name, new LinkedHashMap<>());
        data.active = name;
        applyActive();
        saveActive();
    }

    /** Aktives Profil duplizieren und zur Kopie wechseln. */
    public static void duplicate() {
        saveActive();
        String name = freeName(data.active + " Kopie");
        data.profiles.put(name, new LinkedHashMap<>(data.profiles.get(data.active)));
        data.active = name;
        write();
    }

    /** Aktives Profil löschen (das letzte bleibt bestehen). */
    public static void deleteActive() {
        if (data.profiles.size() <= 1) return;
        data.profiles.remove(data.active);
        data.active = data.profiles.keySet().iterator().next();
        applyActive();
        write();
    }

    public static int count() {
        return data.profiles.size();
    }
}
