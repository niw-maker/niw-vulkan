package br.niw.vulkan.entidades;

import br.niw.vulkan.mixin.LoteEstadoAcesso;
import br.niw.vulkan.mixin.LoteTipoAcesso;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import java.util.Set;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Diz o que pode esperar para ser desenhado em lote. Só entra tipo original do jogo (mesma classe), com shader, teste de
 * profundidade e texturas originais: o resultado dele não depende de estado que outro mod troque no meio do caminho.
 */
final class Tipos extends RenderStateShard {
    static final byte OPACO = 1, TRANSLUCIDO = 2, NAO = 3, NAO_E_MISTURA = 4;
    private static final Reference2ByteOpenHashMap<RenderType> CACHE = new Reference2ByteOpenHashMap<>();
    private static final Set<ShaderStateShard> SHADERS = Set.of(RENDERTYPE_ENTITY_SOLID_SHADER, RENDERTYPE_ENTITY_CUTOUT_SHADER,
        RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER, RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER, RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER,
        RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER, RENDERTYPE_ENTITY_TRANSLUCENT_SHADER, RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER,
        RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER, RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER, RENDERTYPE_EYES_SHADER,
        RENDERTYPE_ENERGY_SWIRL_SHADER, RENDERTYPE_ENTITY_SHADOW_SHADER, RENDERTYPE_ENTITY_NO_OUTLINE_SHADER,
        RENDERTYPE_TEXT_SHADER, RENDERTYPE_TEXT_INTENSITY_SHADER);

    private Tipos() {
        super("niw", () -> { }, () -> { });
    }

    static byte de(RenderType t) {
        byte b = CACHE.getByte(t);
        if (b == 0) {
            if (CACHE.size() > 4096) CACHE.clear();
            CACHE.put(t, b = olha(t));
        }
        return b;
    }

    private static byte olha(RenderType t) {
        if (!(t instanceof LoteTipoAcesso a)) return NAO_E_MISTURA;
        LoteEstadoAcesso e = (LoteEstadoAcesso) (Object) a.niw$estado();
        boolean opaco = e.niw$transparencia() == NO_TRANSPARENCY;
        Class<?> tex = e.niw$textura().getClass();
        boolean serve = t.canConsolidateConsecutiveGeometry() && SHADERS.contains(e.niw$shader()) && e.niw$profundidade() == LEQUAL_DEPTH_TEST
            && (tex == TextureStateShard.class || tex == MultiTextureStateShard.class || tex == EmptyTextureStateShard.class);
        if (serve) return opaco ? OPACO : TRANSLUCIDO;
        return opaco && e.niw$profundidade() == LEQUAL_DEPTH_TEST ? NAO : NAO_E_MISTURA;
    }
}
