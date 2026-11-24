package br.niw.vulkan.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface TerrenoFrustumAcesso {
    @Accessor("intersection") FrustumIntersection niw$intersecao();
    @Accessor("camX") double niw$x();
    @Accessor("camY") double niw$y();
    @Accessor("camZ") double niw$z();
}
