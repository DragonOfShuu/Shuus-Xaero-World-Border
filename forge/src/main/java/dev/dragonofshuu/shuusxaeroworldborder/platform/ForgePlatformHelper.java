package dev.dragonofshuu.shuusxaeroworldborder.platform;

import dev.dragonofshuu.shuusxaeroworldborder.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        ModList modList = ModList.get();
        if (modList != null) {
            return modList.isLoaded(modId);
        }
        // During early mixin preparation, ModList isn't initialized yet.
        // LoadingModList is populated before mixins run and can be used as a fallback.
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }
}