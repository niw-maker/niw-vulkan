package br.niw.vulkan.inicio;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Se outro mod mexeu no mesmo lugar e uma parte nossa não encaixa, o jogo não cai: aquela parte fica de fora e o módulo
 * dela é desligado para a próxima abertura.
 */
public class ErroMixin implements IMixinErrorHandler {
    @Override
    public ErrorAction onPrepareError(IMixinConfig c, Throwable t, IMixinInfo m, ErrorAction a) {
        return trata(m, t, a);
    }

    @Override
    public ErrorAction onApplyError(String alvo, Throwable t, IMixinInfo m, ErrorAction a) {
        return trata(m, t, a);
    }

    private static ErrorAction trata(IMixinInfo m, Throwable t, ErrorAction a) {
        if (m == null || !m.getClassName().startsWith("br.niw.vulkan.")) return a;
        String nome = m.getClassName().substring(m.getClassName().lastIndexOf('.') + 1);
        String chave = PluginMixin.chaveDe(nome);
        Chaves.LOG.error("{} não encaixou com outro mod ({}). O módulo '{}' foi desligado a partir da próxima abertura; feche e abra o jogo.",
            nome, t.toString(), chave);
        if (chave != null) Chaves.grava(chave, "false");
        return ErrorAction.WARN;
    }
}
