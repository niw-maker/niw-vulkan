package br.niw.vulkan.mixin;

import br.niw.vulkan.terreno.Desenhista;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A busca por seções a remontar passa só pelas que estão marcadas, em vez de todas as visíveis a cada quadro. */
@Mixin(LevelRenderer.class)
public class TerrenoSujosMixin {
    @Unique private final ObjectArrayList<Object> niw$sujos = new ObjectArrayList<>();
    @Unique private int niw$vVisao = -1, niw$vSujos = -1, niw$tam;
    @Unique private Object niw$de;

    @Redirect(method = "compileChunks", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;", remap = false))
    private ObjectListIterator<?> niw$soSujos(ObjectArrayList<?> lista) {
        int s = Desenhista.versaoSujos(), v = Desenhista.versaoVisao();
        if (s != niw$vSujos || v != niw$vVisao || lista.size() != niw$tam || lista != niw$de) {
            niw$vSujos = s;
            niw$vVisao = v;
            niw$tam = lista.size();
            niw$de = lista;
            niw$sujos.clear();
            for (int i = 0, n = lista.size(); i < n; i++)
                if (((TerrenoInfoAcesso) lista.get(i)).niw$chunk().isDirty()) niw$sujos.add(lista.get(i));
        }
        return niw$sujos.iterator();
    }
}
