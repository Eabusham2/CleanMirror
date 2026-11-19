package com.eyad.cleanmirror.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Every field here is wired to something real. If a switch is in the menu, it does a thing.
 */
public class CleanMirrorConfig {

    /**
     * How the clean picture's WORLD gets made. (The HUD is always rebuilt clean either way.)
     *
     * REUSE  - copy the world picture the game already drew. Costs almost nothing.
     *          Nametags are part of the world, so nametag cleaning applies to BOTH windows.
     *
     * REDRAW - draw the world a second time, just for the clean window. Nametag cleaning
     *          applies to the clean window only (you keep your health bars), and it unlocks
     *          freecam-proofing, its own FOV, and hide-hand. Costs a second world render, so use
     *          the frame-rate limit.
     */
    public enum MirrorMode { REUSE, REDRAW }

    /**
     * VANILLA - names are rebuilt from the entity's real name, and shown only where vanilla
     *           would show them. Health bars / HP numbers / "names on every mob" all go away.
     * OFF     - no nametags at all.
     * AS_IS   - don't touch them.
     */
    public enum NametagMode { AS_IS, VANILLA, OFF }

    public enum ScalingMode { STRETCH, LETTERBOX, CROP }

    // ---------------------------------------------------------------- mode
    public MirrorMode mode = MirrorMode.REUSE;

    // ---------------------------------------------------------------- window
    public String windowTitle = "Clean Feed";
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public boolean borderless = false;
    public boolean alwaysOnTop = false;
    public boolean vsync = false;
    /** Clean-window frames per second. 0 = every game frame. Skipped frames cost nothing at all. */
    public int fpsCap = 60;
    public boolean openOnLaunch = false;
    public ScalingMode scaling = ScalingMode.LETTERBOX;

    // ---------------------------------------------------------------- REDRAW-only
    /** Clean-feed field of view. 0 = same as yours. Your sprint/speed FOV changes still apply on top. */
    public double fovOverride = 0;
    /** Hide your held item on the clean feed only. */
    public boolean hideHand = false;
    /** The clean feed always shows the view from your body, never from a freecam. */
    public boolean freecamProof = true;

    // ---------------------------------------------------------------- nametags
    public NametagMode nametags = NametagMode.VANILLA;
    /** Vanilla colours names by scoreboard team. Keeping that is still vanilla. */
    public boolean keepTeamColors = true;
    /**
     * Also skip anything other mods add while a name label is being drawn (extra lines, icons).
     * Off by default because Simple Voice Chat draws its speaker icon there.
     */
    public boolean strictNametags = false;

    // ---------------------------------------------------------------- HUD
    /** Vanilla HUD pieces to HIDE on the clean feed. Empty = full vanilla HUD. */
    public Set<String> hiddenVanillaHud = new LinkedHashSet<>();

    /** Mods allowed to draw HUD pieces on the clean feed, by mod name ("voicechat") or exact piece id. */
    public Set<String> allowedMods = new LinkedHashSet<>(List.of("voicechat"));

    /**
     * Fabric's old-style HUD callback has no names on anything, so it can't be filtered per mod.
     * true = never run it on the clean feed (safe). false = run all of it.
     */
    public boolean hideUnnamedDrawing = true;

    /** The F3 screen (and its charts / pie chart), whenever you have it open. */
    public boolean showDebugScreen = true;

    // ---------------------------------------------------------------- screens & popups
    public boolean showScreens = true;
    public boolean showModdedScreens = false;
    public boolean showConnectionScreens = true;
    /** Advancement / recipe / tutorial toasts. */
    public boolean showToasts = true;
    /** Toasts created by mods (e.g. a client announcing a module toggled). */
    public boolean showModdedToasts = false;

    // ---------------------------------------------------------------- saving
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cleanmirror.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CleanMirrorConfig instance;

    public static CleanMirrorConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static CleanMirrorConfig load() {
        try {
            if (Files.exists(PATH)) {
                CleanMirrorConfig c = GSON.fromJson(Files.readString(PATH), CleanMirrorConfig.class);
                if (c != null) {
                    if (c.hiddenVanillaHud == null) c.hiddenVanillaHud = new LinkedHashSet<>();
                    if (c.allowedMods == null) c.allowedMods = new LinkedHashSet<>();
                    if (c.nametags == null) c.nametags = NametagMode.VANILLA;
                    if (c.scaling == null) c.scaling = ScalingMode.LETTERBOX;
                    if (c.mode == null) c.mode = MirrorMode.REUSE;
                    return c;
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[CleanMirror] Config unreadable, starting fresh: " + e);
        }
        return new CleanMirrorConfig();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[CleanMirror] Could not save config: " + e);
        }
    }

    /** May this HUD piece reach the clean window? */
    public boolean allows(String namespace, String path) {
        String full = namespace + ":" + path;
        if ("minecraft".equals(namespace)) return !hiddenVanillaHud.contains(full);
        return allowedMods.contains(namespace) || allowedMods.contains(full);
    }
}
