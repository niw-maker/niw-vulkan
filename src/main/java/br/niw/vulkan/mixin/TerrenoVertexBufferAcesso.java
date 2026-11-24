package br.niw.vulkan.mixin;

import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexBuffer.class)
public interface TerrenoVertexBufferAcesso {
    @Accessor("vertexBufferId") int niw$vbo();
    @Accessor("vertexBufferId") void niw$vbo(int id);
    @Accessor("indexBufferId") int niw$ibo();
    @Accessor("indexBufferId") void niw$ibo(int id);
    @Accessor("arrayObjectId") int niw$vao();
    @Accessor("arrayObjectId") void niw$vao(int id);
}
