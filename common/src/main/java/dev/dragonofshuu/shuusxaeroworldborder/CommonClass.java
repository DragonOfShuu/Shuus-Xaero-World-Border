package dev.dragonofshuu.shuusxaeroworldborder;

import dev.dragonofshuu.shuusxaeroworldborder.platform.Services;

public class CommonClass {
    public static void init() {

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {

            Constants.LOG.info("Hello to {}", Constants.MOD_ID);
        }
    }
}