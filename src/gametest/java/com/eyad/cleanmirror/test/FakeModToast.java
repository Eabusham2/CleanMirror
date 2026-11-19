package com.eyad.cleanmirror.test;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;

/** A toast from "a mod": a solid magenta block. Must not reach the feed unless modded toasts are allowed. */
public class FakeModToast implements Toast {
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    @Override public Toast.Visibility getVisibility() { return visibility; }

    @Override public void update(ToastManager manager, long time) {
        if (time > 600_000L) visibility = Toast.Visibility.HIDE;
    }

    @Override public void draw(DrawContext ctx, TextRenderer text, long startTime) {
        ctx.fill(0, 0, getWidth(), getHeight(), 0xFFFF00FF);
    }
}
