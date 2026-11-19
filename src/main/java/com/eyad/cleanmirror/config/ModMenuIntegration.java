package com.eyad.cleanmirror.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.text.Text;

/**
 * Mod Menu's gear button. The real screen lives in ClothConfigScreen and is only touched when Cloth
 * Config is installed - so having Mod Menu without Cloth shows a friendly note instead of crashing.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config") || FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return ClothConfigScreen::create;
        }
        return parent -> new NoticeScreen(
                () -> MinecraftClient.getInstance().setScreen(parent),
                Text.translatable("cleanmirror.title"),
                Text.translatable("cleanmirror.nocloth"));
    }
}
