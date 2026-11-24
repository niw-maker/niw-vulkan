package br.niw.vulkan.mixin;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderChunkRegion.class)
public interface TerrenoRegiaoAcesso {
    @Accessor("chunks") RenderChunk[][] niw$chunks();
}
