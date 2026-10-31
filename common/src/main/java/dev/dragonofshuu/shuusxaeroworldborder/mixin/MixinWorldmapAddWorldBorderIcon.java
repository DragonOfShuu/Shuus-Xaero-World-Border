package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.dragonofshuu.shuusxaeroworldborder.gui.WorldmapWorldBorderToggleButton;

import xaero.map.gui.GuiMap;

/**
 * Adds the world border show/hide button to the world map.
 *
 * <p>Injected at the end of {@code GuiMap.init}, after {@code super.init} has rebuilt the widget list,
 * so the button is a first class widget: it is drawn on the same layer as the icons beside it, takes
 * input through the same path, gets its tooltip from Xaero's own widget tooltip loop, and disappears
 * with the rest of the interface when the interface is hidden.</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinWorldmapAddWorldBorderIcon {

    /**
     * {@code GuiMap.init} overrides {@code Screen.init}, so its name is obfuscated by whichever
     * mapping the running loader uses. {@code remap = true} makes the Mixin annotation processor write
     * the resolved name into the refmap, which the runtime then applies. The mixin class itself stays
     * {@code remap = false} because everything else it touches is Xaero's own, unmapped code.
     */
    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void shuusxaerowb$addWorldmapWorldBorderIcon(CallbackInfo ci) {
        ((GuiMap) (Object) this).addButton(new WorldmapWorldBorderToggleButton());
    }
}
