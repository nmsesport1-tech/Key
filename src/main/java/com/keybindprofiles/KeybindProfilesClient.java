package com.keybindprofiles;

import com.keybindprofiles.mixin.OptionsSubScreenAccessor;
import com.keybindprofiles.mixin.ScreenInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindProfilesClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("keybindprofiles");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ProfileManager.load());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ProfileManager.saveActive());

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof KeyBindsScreen) {
                addButtons(client, screen);
                ScreenEvents.remove(screen).register(s -> ProfileManager.saveActive());
            }
        });
    }

    private static void reopen(Minecraft mc, Screen old) {
        Screen parent = ((OptionsSubScreenAccessor) old).kp$getLastScreen();
        mc.setScreen(new KeyBindsScreen(parent, mc.options));
    }

    private static void addButtons(Minecraft mc, Screen s) {
        ScreenInvoker in = (ScreenInvoker) s;
        int y = 6;
        int x = 6;

        in.kp$addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            ProfileManager.cycle(-1);
            reopen(mc, s);
        }).bounds(x, y, 16, 20).build());
        x += 18;

        Button name = Button.builder(Component.literal(ProfileManager.active()), b -> {
            ProfileManager.cycle(1);
            reopen(mc, s);
        }).bounds(x, y, 96, 20).build();
        in.kp$addRenderableWidget(name);
        x += 98;

        in.kp$addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            ProfileManager.cycle(1);
            reopen(mc, s);
        }).bounds(x, y, 16, 20).build());
        x += 20;

        in.kp$addRenderableWidget(Button.builder(Component.literal("Neu"), b -> {
            ProfileManager.create();
            reopen(mc, s);
        }).bounds(x, y, 34, 20).build());
        x += 36;

        in.kp$addRenderableWidget(Button.builder(Component.literal("Kopie"), b -> {
            ProfileManager.duplicate();
            reopen(mc, s);
        }).bounds(x, y, 40, 20).build());
        x += 42;

        Button del = Button.builder(Component.literal("Löschen"), b -> {
            ProfileManager.deleteActive();
            reopen(mc, s);
        }).bounds(x, y, 50, 20).build();
        del.active = ProfileManager.count() > 1;
        in.kp$addRenderableWidget(del);
    }
}
