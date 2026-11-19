package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.config.CleanMirrorConfig.MirrorMode;
import com.eyad.cleanmirror.config.CleanMirrorConfig.NametagMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

/**
 * ===================== HOW MODDED NAMETAGS GET CLEANED =====================
 *
 * In 1.21.11 a nametag is decided in two steps: first each entity's details are copied into a
 * "render state" (that's when the name text is chosen), then labels are drawn from that state.
 * Health-bar mods change the TEXT in step one, and often also force names to show on every mob.
 *
 * So after step one finishes - after every mod has had its say - we overwrite the result with what
 * vanilla would have done: the entity's real name (team colours kept), shown only where vanilla's
 * own rules would show it. Those rules are copied from the 1.21.11 source, not called, so a mod
 * that changes them can't change ours.
 *
 * REUSE mode: there's one world picture, so this applies to both windows.
 * REDRAW mode: it applies only while the clean world is being drawn. You keep your health bars.
 */
public final class NametagRules {

    private NametagRules() {}


    public static boolean active() {
        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        if (cfg.nametags == NametagMode.AS_IS) return false;
        return cfg.mode == MirrorMode.REUSE || CleanMirror.renderingClean;
    }

    public static boolean strict() {
        return CleanMirrorConfig.get().strictNametags && active();
    }

    public static void sanitize(Entity entity, EntityRenderState state, Entity targeted, boolean cameraKnown, float tickProgress) {
        CleanMirrorConfig cfg = CleanMirrorConfig.get();

        if (cfg.nametags == NametagMode.OFF || !cameraKnown) {
            state.displayName = null;
            if (state instanceof PlayerEntityRenderState p && cfg.nametags == NametagMode.OFF) p.playerName = null;
            return;
        }

        if (entity instanceof LivingEntity living) {
            boolean show = state.squaredDistanceToCamera < 4096.0
                    && vanillaHasLabel(living, state.squaredDistanceToCamera, targeted);
            if (show) {
                state.displayName = vanillaName(entity, cfg);
                if (state.nameLabelPos == null) {
                    state.nameLabelPos = entity.getAttachments()
                            .getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickProgress));
                }
            } else {
                state.displayName = null;
            }
        } else if (state.displayName != null) {
            // Item frames, displays, etc.: keep vanilla's show/hide, but use the real name.
            state.displayName = vanillaName(entity, cfg);
        }
    }

    private static Text vanillaName(Entity entity, CleanMirrorConfig cfg) {
        Text name = entity.getName();
        return cfg.keepTeamColors ? Team.decorateName(entity.getScoreboardTeam(), name) : name;
    }

    /** LivingEntityRenderer / MobEntityRenderer / PlayerEntityRenderer / ArmorStandEntityRenderer .hasLabel, 1.21.11. */
    private static boolean vanillaHasLabel(LivingEntity e, double distSq, Entity targeted) {
        if (e instanceof ArmorStandEntity stand) return stand.isCustomNameVisible();

        if (!livingRule(e, distSq)) return false;

        if (e instanceof MobEntity || e instanceof PlayerLikeEntity) {
            boolean shouldRenderName = e instanceof PlayerEntity || e.isCustomNameVisible();
            return shouldRenderName || (e.getCustomName() != null && e == targeted);
        }
        return true;
    }

    private static boolean livingRule(LivingEntity e, double distSq) {
        if (e.isSneaky() && distSq >= 1024.0) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity me = mc.player;
        if (me == null) return false;
        boolean visible = !e.isInvisibleTo(me);

        if (e != me) {
            AbstractTeam team = e.getScoreboardTeam();
            AbstractTeam mine = me.getScoreboardTeam();
            if (team != null) {
                switch (team.getNameTagVisibilityRule()) {
                    case ALWAYS:
                        return visible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return mine == null ? visible : team.isEqual(mine) && (team.shouldShowFriendlyInvisibles() || visible);
                    case HIDE_FOR_OWN_TEAM:
                        return mine == null ? visible : !team.isEqual(mine) && visible;
                    default:
                        return true;
                }
            }
        }
        return MinecraftClient.isHudEnabled() && e != mc.getCameraEntity() && visible && !e.hasPassengers();
    }
}
