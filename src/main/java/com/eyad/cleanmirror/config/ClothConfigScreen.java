package com.eyad.cleanmirror.config;

import com.eyad.cleanmirror.config.CleanMirrorConfig.MirrorMode;
import com.eyad.cleanmirror.config.CleanMirrorConfig.NametagMode;
import com.eyad.cleanmirror.config.CleanMirrorConfig.ScalingMode;
import com.eyad.cleanmirror.render.HudFilter;
import com.eyad.cleanmirror.window.MirrorWindow;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

/** The settings screen (Cloth Config). Only loaded when Cloth Config is installed. */
final class ClothConfigScreen {

    private ClothConfigScreen() {}

    /** Every vanilla HUD piece in 1.21.11, in the order the game draws them. */
    private static final List<Identifier> VANILLA_HUD = List.of(
            VanillaHudElements.MISC_OVERLAYS, VanillaHudElements.CROSSHAIR, VanillaHudElements.SPECTATOR_MENU,
            VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
            VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
            VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP,
            VanillaHudElements.SPECTATOR_TOOLTIP, VanillaHudElements.STATUS_EFFECTS, VanillaHudElements.BOSS_BAR,
            VanillaHudElements.SLEEP, VanillaHudElements.DEMO_TIMER, VanillaHudElements.SCOREBOARD,
            VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.TITLE_AND_SUBTITLE, VanillaHudElements.CHAT,
            VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES);

    static Screen create(Screen parent) {
        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.translatable("cleanmirror.title"));
        ConfigEntryBuilder e = builder.entryBuilder();

        // ---------------------------------------------------------------- Mode
        ConfigCategory mode = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.mode"));
        mode.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.mode")).build());
        mode.addEntry(e.startEnumSelector(Text.translatable("cleanmirror.o.mode"), MirrorMode.class, cfg.mode)
                .setDefaultValue(MirrorMode.REUSE)
                .setTooltip(Text.translatable("cleanmirror.o.mode.t1"), Text.translatable("cleanmirror.o.mode.t2"))
                .setSaveConsumer(v -> cfg.mode = v).build());
        mode.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.redrawonly")).build());
        mode.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.freecam"), cfg.freecamProof)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.freecam.t"))
                .setSaveConsumer(v -> cfg.freecamProof = v).build());
        mode.addEntry(e.startDoubleField(Text.translatable("cleanmirror.o.fov"), cfg.fovOverride)
                .setMin(0).setMax(170).setDefaultValue(0).setTooltip(Text.translatable("cleanmirror.o.fov.t"))
                .setSaveConsumer(v -> cfg.fovOverride = v).build());
        mode.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.hidehand"), cfg.hideHand)
                .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.o.hidehand.t"))
                .setSaveConsumer(v -> cfg.hideHand = v).build());

        // ---------------------------------------------------------------- Window
        ConfigCategory window = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.window"));
        window.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.window")).build());
        window.addEntry(e.startStrField(Text.translatable("cleanmirror.o.title"), cfg.windowTitle)
                .setDefaultValue("Clean Feed").setTooltip(Text.translatable("cleanmirror.o.title.t"))
                .setSaveConsumer(v -> cfg.windowTitle = v).build());
        window.addEntry(e.startIntField(Text.translatable("cleanmirror.o.width"), cfg.windowWidth)
                .setMin(320).setMax(7680).setDefaultValue(1280).setSaveConsumer(v -> cfg.windowWidth = v).build());
        window.addEntry(e.startIntField(Text.translatable("cleanmirror.o.height"), cfg.windowHeight)
                .setMin(240).setMax(4320).setDefaultValue(720).setSaveConsumer(v -> cfg.windowHeight = v).build());
        window.addEntry(e.startIntSlider(Text.translatable("cleanmirror.o.fps"), cfg.fpsCap, 0, 240)
                .setDefaultValue(60)
                .setTextGetter(v -> v == 0 ? Text.translatable("cleanmirror.o.fps.match") : Text.literal(v + " FPS"))
                .setTooltip(Text.translatable("cleanmirror.o.fps.t"))
                .setSaveConsumer(v -> cfg.fpsCap = v).build());
        window.addEntry(e.startEnumSelector(Text.translatable("cleanmirror.o.scaling"), ScalingMode.class, cfg.scaling)
                .setDefaultValue(ScalingMode.LETTERBOX).setTooltip(Text.translatable("cleanmirror.o.scaling.t"))
                .setSaveConsumer(v -> cfg.scaling = v).build());
        window.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.borderless"), cfg.borderless)
                .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.o.borderless.t"))
                .setSaveConsumer(v -> cfg.borderless = v).build());
        window.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.ontop"), cfg.alwaysOnTop)
                .setDefaultValue(false).setSaveConsumer(v -> cfg.alwaysOnTop = v).build());
        window.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.vsync"), cfg.vsync)
                .setDefaultValue(false).setSaveConsumer(v -> cfg.vsync = v).build());
        window.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.autoopen"), cfg.openOnLaunch)
                .setDefaultValue(false).setSaveConsumer(v -> cfg.openOnLaunch = v).build());

        // ---------------------------------------------------------------- Nametags
        ConfigCategory tags = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.nametags"));
        tags.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.nametags")).build());
        tags.addEntry(e.startEnumSelector(Text.translatable("cleanmirror.o.nametags"), NametagMode.class, cfg.nametags)
                .setDefaultValue(NametagMode.VANILLA)
                .setTooltip(Text.translatable("cleanmirror.o.nametags.t1"), Text.translatable("cleanmirror.o.nametags.t2"),
                        Text.translatable("cleanmirror.o.nametags.t3"))
                .setSaveConsumer(v -> cfg.nametags = v).build());
        tags.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.teamcolors"), cfg.keepTeamColors)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.teamcolors.t"))
                .setSaveConsumer(v -> cfg.keepTeamColors = v).build());
        tags.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.strict"), cfg.strictNametags)
                .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.o.strict.t"))
                .setSaveConsumer(v -> cfg.strictNametags = v).build());

        // ---------------------------------------------------------------- Vanilla HUD
        ConfigCategory hud = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.hud"));
        hud.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.hud")).build());
        hud.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.debug"), cfg.showDebugScreen)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.debug.t"))
                .setSaveConsumer(v -> cfg.showDebugScreen = v).build());
        for (Identifier id : VANILLA_HUD) {
            String key = id.toString();
            hud.addEntry(e.startBooleanToggle(Text.literal(pretty(id.getPath())), !cfg.hiddenVanillaHud.contains(key))
                    .setDefaultValue(true).setTooltip(Text.literal(key))
                    .setSaveConsumer(v -> { if (v) cfg.hiddenVanillaHud.remove(key); else cfg.hiddenVanillaHud.add(key); })
                    .build());
        }

        // ---------------------------------------------------------------- Mods
        ConfigCategory mods = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.mods"));
        mods.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.mods")).build());
        TreeSet<String> seen = new TreeSet<>(HudFilter.discoveredMods());
        for (String s : cfg.allowedMods) if (!s.contains(":")) seen.add(s);
        if (seen.isEmpty()) {
            mods.addEntry(e.startTextDescription(Text.translatable("cleanmirror.mods.empty")).build());
        }
        for (String modName : seen) {
            mods.addEntry(e.startBooleanToggle(Text.literal(modName), cfg.allowedMods.contains(modName))
                    .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.mods.tip", modName))
                    .setSaveConsumer(v -> { if (v) cfg.allowedMods.add(modName); else cfg.allowedMods.remove(modName); })
                    .build());
        }
        List<String> exact = new ArrayList<>();
        for (String s : cfg.allowedMods) if (s.contains(":")) exact.add(s);
        mods.addEntry(e.startStrList(Text.translatable("cleanmirror.o.allowlist"), exact)
                .setDefaultValue(List.of()).setTooltip(Text.translatable("cleanmirror.o.allowlist.t"))
                .setSaveConsumer(v -> {
                    cfg.allowedMods.removeIf(s -> s.contains(":"));
                    for (String s : v) if (s != null && s.contains(":")) cfg.allowedMods.add(s.trim());
                }).build());
        mods.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.hideunnamed"), cfg.hideUnnamedDrawing)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.hideunnamed.t"))
                .setSaveConsumer(v -> cfg.hideUnnamedDrawing = v).build());

        // ---------------------------------------------------------------- Menus & popups
        ConfigCategory screens = builder.getOrCreateCategory(Text.translatable("cleanmirror.cat.screens"));
        screens.addEntry(e.startTextDescription(Text.translatable("cleanmirror.help.screens")).build());
        screens.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.screens"), cfg.showScreens)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.screens.t"))
                .setSaveConsumer(v -> cfg.showScreens = v).build());
        screens.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.modscreens"), cfg.showModdedScreens)
                .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.o.modscreens.t"))
                .setSaveConsumer(v -> cfg.showModdedScreens = v).build());
        screens.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.connscreens"), cfg.showConnectionScreens)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.connscreens.t"))
                .setSaveConsumer(v -> cfg.showConnectionScreens = v).build());
        screens.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.toasts"), cfg.showToasts)
                .setDefaultValue(true).setTooltip(Text.translatable("cleanmirror.o.toasts.t"))
                .setSaveConsumer(v -> cfg.showToasts = v).build());
        screens.addEntry(e.startBooleanToggle(Text.translatable("cleanmirror.o.modtoasts"), cfg.showModdedToasts)
                .setDefaultValue(false).setTooltip(Text.translatable("cleanmirror.o.modtoasts.t"))
                .setSaveConsumer(v -> cfg.showModdedToasts = v).build());

        builder.setSavingRunnable(() -> {
            cfg.allowedMods = new LinkedHashSet<>(cfg.allowedMods);
            cfg.save();
            if (MirrorWindow.isOpen()) {   // re-create so size / border / always-on-top apply now
                MirrorWindow.close();
                MirrorWindow.open();
            }
        });
        return builder.build();
    }

    private static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
