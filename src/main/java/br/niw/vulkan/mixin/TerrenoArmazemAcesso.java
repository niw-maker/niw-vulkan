package br.niw.vulkan.mixin;

import java.util.LinkedHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer$RenderChunkStorage")
public interface TerrenoArmazemAcesso {
    @Accessor("renderChunks") LinkedHashSet<?> niw$chunks();
}
