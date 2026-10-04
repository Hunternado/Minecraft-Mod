package com.hunternado.frieren.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state for every humanoid in the mod; filled by {@link FrierenHumanoidRenderer}. */
public class FrierenHumanoidRenderState extends LivingEntityRenderState {
    public float limbSwing;
    public float limbSwingAmount;
    public float headYaw;
    public float headPitch;
    public float time;
    public boolean casting;
}
