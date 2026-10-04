package com.hunternado.frieren.client.render;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.client.model.geom.ModelLayerLocation;

/** Model layers owned by this mod (no reliance on vanilla layer constants). */
public final class ModModelLayers {
    public static final ModelLayerLocation HUMANOID = new ModelLayerLocation(FrierenIds.id("humanoid"), "main");
    public static final ModelLayerLocation MIMIC = new ModelLayerLocation(FrierenIds.id("mimic"), "main");
    public static final ModelLayerLocation STILLE = new ModelLayerLocation(FrierenIds.id("stille"), "main");

    private ModModelLayers() {}
}
