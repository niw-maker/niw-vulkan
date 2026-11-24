package br.niw.vulkan.mixin;

import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ViewArea.class)
public interface TerrenoGradeAcesso {
    @Accessor("chunkGridSizeX") int niw$gx();
    @Accessor("chunkGridSizeY") int niw$gy();
    @Accessor("chunkGridSizeZ") int niw$gz();
}
