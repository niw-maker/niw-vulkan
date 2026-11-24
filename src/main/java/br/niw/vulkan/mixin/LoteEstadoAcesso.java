package br.niw.vulkan.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface LoteEstadoAcesso {
    @Accessor("shaderState") RenderStateShard.ShaderStateShard niw$shader();
    @Accessor("transparencyState") RenderStateShard.TransparencyStateShard niw$transparencia();
    @Accessor("depthTestState") RenderStateShard.DepthTestStateShard niw$profundidade();
    @Accessor("textureState") RenderStateShard.EmptyTextureStateShard niw$textura();
}
