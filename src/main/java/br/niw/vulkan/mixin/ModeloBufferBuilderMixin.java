package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.EscritorDireto;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Os mesmos bytes do original, escritos de uma vez na memória. Só nos formatos rápidos do próprio jogo. */
@Mixin(BufferBuilder.class)
public abstract class ModeloBufferBuilderMixin extends DefaultedVertexConsumer implements EscritorDireto {
    @Shadow private ByteBuffer buffer;
    @Shadow private int nextElementByte;
    @Shadow private int vertices;
    @Shadow private int elementIndex;
    @Shadow private boolean fastFormat;
    @Shadow private boolean fullFormat;
    @Shadow private VertexFormat format;
    @Shadow private VertexFormat.Mode mode;

    @Shadow protected abstract void ensureCapacity(int bytes);

    private long niw$abre(int n, int passo) {
        // um a mais, como o original, que sempre deixa lugar para o próximo vértice
        int preciso = (n + 1) * passo;
        if (nextElementByte + preciso > buffer.capacity()) ensureCapacity(preciso);
        return MemoryUtil.memAddress0(buffer) + nextElementByte;
    }

    @Override
    public long niw$reserva(int n) {
        if (!fullFormat || defaultColorSet || elementIndex != 0 || mode != VertexFormat.Mode.QUADS || format.getVertexSize() != 36) return 0;
        return niw$abre(n, 36);
    }

    @Override
    public void niw$avanca(int n) {
        nextElementByte += n * 36;
        vertices += n;
    }

    @Inject(method = "vertex(FFFFFFFFFIIFFF)V", at = @At("HEAD"), cancellable = true)
    private void niw$vertice(float x, float y, float z, float r, float g, float b, float a, float u, float v, int sobre, int luz, float nx, float ny, float nz, CallbackInfo ci) {
        if (!fastFormat || defaultColorSet || elementIndex != 0 || mode != VertexFormat.Mode.QUADS || format.getVertexSize() != (fullFormat ? 36 : 32)) return;
        int passo = fullFormat ? 36 : 32;
        long m = niw$abre(1, passo);
        niw$escreve(m, x, y, z, r, g, b, a, u, v, sobre, luz, nx, ny, nz);
        nextElementByte += passo;
        vertices++;
        ci.cancel();
    }

    private void niw$escreve(long m, float x, float y, float z, float r, float g, float b, float a, float u, float v, int sobre, int luz, float nx, float ny, float nz) {
        MemoryUtil.memPutFloat(m, x);
        MemoryUtil.memPutFloat(m + 4, y);
        MemoryUtil.memPutFloat(m + 8, z);
        MemoryUtil.memPutInt(m + 12, (int) (r * 255f) & 255 | ((int) (g * 255f) & 255) << 8 | ((int) (b * 255f) & 255) << 16 | ((int) (a * 255f) & 255) << 24);
        MemoryUtil.memPutFloat(m + 16, u);
        MemoryUtil.memPutFloat(m + 20, v);
        long k = m + 24;
        if (fullFormat) {
            MemoryUtil.memPutInt(k, sobre);
            k += 4;
        }
        MemoryUtil.memPutInt(k, luz);
        MemoryUtil.memPutInt(k + 4, niw$n(nx) | niw$n(ny) << 8 | niw$n(nz) << 16);
    }

    private static int niw$n(float f) {
        return (int) ((f < -1f ? -1f : Math.min(f, 1f)) * 127f) & 255;
    }

    /** Blocos e itens: mesma conta do VertexConsumer (com a luz e as normais prontas do Forge), sem objetos no caminho. */
    @Override
    public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brilho, float r, float g, float b, float alfa, int[] luzes, int sobre, boolean comCor) {
        int[] d = quad.getVertices();
        int passo = fullFormat ? 36 : 32;
        if (!fastFormat || defaultColorSet || elementIndex != 0 || mode != VertexFormat.Mode.QUADS || format.getVertexSize() != passo || d.length != 32) {
            super.putBulkData(pose, quad, brilho, r, g, b, alfa, luzes, sobre, comCor);
            return;
        }
        Vec3i dir = quad.getDirection().getNormal();
        Matrix4f t = pose.pose();
        Matrix3f tn = pose.normal();
        float dx = dir.getX(), dy = dir.getY(), dz = dir.getZ();
        float nx = tn.m00 * dx + (tn.m10 * dy + tn.m20 * dz);
        float ny = tn.m01 * dx + (tn.m11 * dy + tn.m21 * dz);
        float nz = tn.m02 * dx + (tn.m12 * dy + tn.m22 * dz);
        float m00 = t.m00(), m01 = t.m01(), m02 = t.m02(), m10 = t.m10(), m11 = t.m11(), m12 = t.m12();
        float m20 = t.m20(), m21 = t.m21(), m22 = t.m22(), m30 = t.m30(), m31 = t.m31(), m32 = t.m32();
        long m = niw$abre(4, passo);
        for (int k = 0, i = 0; k < 4; k++, i += 8, m += passo) {
            float x = Float.intBitsToFloat(d[i]), y = Float.intBitsToFloat(d[i + 1]), z = Float.intBitsToFloat(d[i + 2]);
            float cr = brilho[k] * r, cg = brilho[k] * g, cb = brilho[k] * b, ca = alfa;
            if (comCor) {
                int c = d[i + 3];
                cr = (c & 255) / 255f * brilho[k] * r;
                cg = (c >> 8 & 255) / 255f * brilho[k] * g;
                cb = (c >> 16 & 255) / 255f * brilho[k] * b;
                ca = alfa * (float) (c >>> 24) / 255f;
            }
            int pronta = d[i + 6], luz = luzes[k];
            if (pronta != 0) luz = Math.max(luz & 0xFFFF, pronta & 0xFFFF) | Math.max(luz >>> 16, pronta >>> 16) << 16;
            int no = d[i + 7];
            if ((no & 0xFFFFFF) != 0) {
                float ax = (byte) no / 127f, ay = (byte) (no >> 8) / 127f, az = (byte) (no >> 16) / 127f;
                nx = tn.m00 * ax + (tn.m10 * ay + tn.m20 * az);
                ny = tn.m01 * ax + (tn.m11 * ay + tn.m21 * az);
                nz = tn.m02 * ax + (tn.m12 * ay + tn.m22 * az);
            }
            niw$escreve(m, m00 * x + (m10 * y + (m20 * z + m30)), m01 * x + (m11 * y + (m21 * z + m31)), m02 * x + (m12 * y + (m22 * z + m32)),
                cr, cg, cb, ca, Float.intBitsToFloat(d[i + 4]), Float.intBitsToFloat(d[i + 5]), sobre, luz, nx, ny, nz);
        }
        nextElementByte += 4 * passo;
        vertices += 4;
    }
}
