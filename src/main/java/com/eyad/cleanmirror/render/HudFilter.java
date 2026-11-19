package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The whitelist. Every HUD piece in 1.21.11 goes through Fabric's element registry with a name like
 * "minecraft:hotbar" or "voicechat:hud" - and the part before the colon is the mod that made it.
 *
 * Vanilla pieces show unless you untick them. Mod pieces are hidden unless you tick them. So a mod
 * you install next month is already off the feed without you doing anything.
 */
public final class HudFilter {

    private static final Set<String> discovered = Collections.synchronizedSet(new HashSet<>());

    private HudFilter() {}

    /** Remember a piece so F7 and the settings screen can list it. Called for both windows. */
    public static void note(Identifier id) {
        if (id != null) discovered.add(id.toString());
    }

    /** Your own window is never filtered. On the clean feed, only allowed pieces draw. */
    public static boolean allow(Identifier id) {
        if (!CleanMirror.renderingClean) return true;
        if (id == null) return false;
        return CleanMirrorConfig.get().allows(id.getNamespace(), id.getPath());
    }

    public static boolean wouldAllow(String fullName) {
        int i = fullName.indexOf(':');
        if (i <= 0) return false;
        return CleanMirrorConfig.get().allows(fullName.substring(0, i), fullName.substring(i + 1));
    }

    public static Set<String> discovered() {
        synchronized (discovered) { return Set.copyOf(discovered); }
    }

    public static Set<String> discoveredMods() {
        Set<String> out = new HashSet<>();
        for (String s : discovered()) {
            int i = s.indexOf(':');
            if (i > 0) out.add(s.substring(0, i));
        }
        out.remove("minecraft");
        return out;
    }
}
