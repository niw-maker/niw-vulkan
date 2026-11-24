package br.niw.vulkan.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteCoordinateExpander.class)
public interface ModeloSpriteAcesso {
    @Accessor("delegate") VertexConsumer niw$dentro();
    @Accessor("sprite") TextureAtlasSprite niw$sprite();
}
