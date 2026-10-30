package dev.dragonofshuu.shuusxaeroworldborder.mixin.config;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import dev.dragonofshuu.shuusxaeroworldborder.platform.Services;

import org.objectweb.asm.tree.ClassNode;
import java.util.List;
import java.util.Set;

public class OptionalWorldmapMixinPlugin implements IMixinConfigPlugin {
 
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!Services.PLATFORM.isModLoaded("xaeroworldmap")) {
            return false;
        }
        return true; 
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }   
}
