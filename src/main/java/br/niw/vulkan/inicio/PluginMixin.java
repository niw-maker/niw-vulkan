package br.niw.vulkan.inicio;

import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Primeiro código do mod a rodar: sobe o driver antes da janela e decide quais mixins entram. */
public class PluginMixin implements IMixinConfigPlugin {
    public static boolean temMod(String id) {
        return LoadingModList.get() != null && LoadingModList.get().getModFileById(id) != null;
    }

    @Override
    public void onLoad(String pacote) {
        Mixins.registerErrorHandlerClass("br.niw.vulkan.inicio.ErroMixin");
        if (FMLEnvironment.dist == Dist.CLIENT && Chaves.ligada("vulkan")) DriverVulkan.carrega();
    }

    /** Chave do módulo de um mixin, pelo começo do nome. */
    public static String chaveDe(String nome) {
        if (nome.startsWith("Terreno")) return "terreno";
        if (nome.startsWith("Lote")) return "lotes";
        if (nome.startsWith("Modelo")) return "modelos";
        if (nome.startsWith("Textura")) return "texturas";
        if (nome.startsWith("Mundo")) return "mundo";
        if (nome.startsWith("Oclusao")) return "oclusao";
        if (nome.startsWith("Prioridade")) return "prioridade";
        if (nome.startsWith("Geracao")) return "geracao";
        if (nome.startsWith("Palladium")) return "palladium";
        if (nome.startsWith("Dados")) return "entidades";
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String alvo, String mixin) {
        String chave = chaveDe(mixin.substring(mixin.lastIndexOf('.') + 1));
        if (chave == null) return true;
        if (!Chaves.ligada(chave)) return false;
        boolean renderTrocado = temMod("embeddium") || temMod("rubidium") || temMod("optifine") || temMod("sodium") || temMod("vulkanmod");
        return switch (chave) {
            case "terreno", "modelos", "texturas", "oclusao" -> !renderTrocado;
            case "lotes" -> !temMod("immediatelyfast") && !temMod("oculus") && !temMod("iris") && !temMod("optifine") && !temMod("vulkanmod");
            case "palladium" -> temMod("palladium") && temMod("kubejs");
            default -> true;
        };
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> a, Set<String> b) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String a, ClassNode b, String c, IMixinInfo d) { }
    @Override public void postApply(String a, ClassNode b, String c, IMixinInfo d) { }
}
