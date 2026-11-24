package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.EscritorDireto;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mesma conta do original, escrita direto na memória em vez de vértice a vértice por interface. */
@Mixin(value = ModelPart.Cube.class, priority = 1100)
public class ModeloCuboMixin {
    @Shadow @Final private ModelPart.Polygon[] polygons;
    /** Cópia corrida dos vértices (x, y, z, u, v) e normais; refeita se algum mod trocar um polígono. */
    @Unique private float[] niw$dados;
    @Unique private ModelPart.Polygon[] niw$feitoDe;

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void niw$direto(PoseStack.Pose pose, VertexConsumer vc, int luz, int sobre, float r, float g, float b, float a, CallbackInfo ci) {
        TextureAtlasSprite sp = null;
        if (vc.getClass() == SpriteCoordinateExpander.class) {
            ModeloSpriteAcesso s = (ModeloSpriteAcesso) vc;
            vc = s.niw$dentro();
            sp = s.niw$sprite();
        }
        if (vc.getClass() != BufferBuilder.class) return;
        EscritorDireto e = (EscritorDireto) vc;
        if (niw$dados == null || niw$trocaram()) {
            if (!niw$copia()) return;
        }
        float[] d = niw$dados;
        int polis = polygons.length;
        long m = e.niw$reserva(polis * 4);
        if (m == 0) return;
        Matrix4f t = pose.pose();
        Matrix3f tn = pose.normal();
        float m00 = t.m00(), m01 = t.m01(), m02 = t.m02(), m10 = t.m10(), m11 = t.m11(), m12 = t.m12();
        float m20 = t.m20(), m21 = t.m21(), m22 = t.m22(), m30 = t.m30(), m31 = t.m31(), m32 = t.m32();
        int cor = (int) (r * 255f) & 255 | ((int) (g * 255f) & 255) << 8 | ((int) (b * 255f) & 255) << 16 | ((int) (a * 255f) & 255) << 24;
        for (int p = 0, i = 0; p < polis; p++) {
            float x = d[i], y = d[i + 1], z = d[i + 2];
            i += 3;
            int normal = niw$n(tn.m00 * x + (tn.m10 * y + tn.m20 * z)) | niw$n(tn.m01 * x + (tn.m11 * y + tn.m21 * z)) << 8
                | niw$n(tn.m02 * x + (tn.m12 * y + tn.m22 * z)) << 16;
            for (int k = 0; k < 4; k++, i += 5, m += 36) {
                float vx = d[i], vy = d[i + 1], vz = d[i + 2], u = d[i + 3], v = d[i + 4];
                if (sp != null) {
                    u = sp.getU(u * 16f);
                    v = sp.getV(v * 16f);
                }
                MemoryUtil.memPutFloat(m, m00 * vx + (m10 * vy + (m20 * vz + m30)));
                MemoryUtil.memPutFloat(m + 4, m01 * vx + (m11 * vy + (m21 * vz + m31)));
                MemoryUtil.memPutFloat(m + 8, m02 * vx + (m12 * vy + (m22 * vz + m32)));
                MemoryUtil.memPutInt(m + 12, cor);
                MemoryUtil.memPutFloat(m + 16, u);
                MemoryUtil.memPutFloat(m + 20, v);
                MemoryUtil.memPutInt(m + 24, sobre);
                MemoryUtil.memPutInt(m + 28, luz);
                MemoryUtil.memPutInt(m + 32, normal);
            }
        }
        e.niw$avanca(polis * 4);
        ci.cancel();
    }

    @Unique
    private boolean niw$trocaram() {
        if (niw$feitoDe.length != polygons.length) return true;
        for (int i = 0; i < polygons.length; i++) if (niw$feitoDe[i] != polygons[i]) return true;
        return false;
    }

    @Unique
    private boolean niw$copia() {
        float[] d = new float[polygons.length * 23];
        int i = 0;
        for (ModelPart.Polygon p : polygons) {
            if (p == null || p.vertices.length != 4) return false;
            Vector3f n = p.normal;
            d[i++] = n.x;
            d[i++] = n.y;
            d[i++] = n.z;
            for (ModelPart.Vertex v : p.vertices) {
                d[i++] = v.pos.x / 16f;
                d[i++] = v.pos.y / 16f;
                d[i++] = v.pos.z / 16f;
                d[i++] = v.u;
                d[i++] = v.v;
            }
        }
        niw$feitoDe = polygons.clone();
        niw$dados = d;
        return true;
    }

    @Unique
    private static int niw$n(float f) {
        return (int) ((f < -1f ? -1f : Math.min(f, 1f)) * 127f) & 255;
    }
}
