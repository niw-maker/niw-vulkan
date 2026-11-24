package br.niw.vulkan.texturas;

import br.niw.vulkan.Nucleos;
import java.util.ArrayList;
import java.util.List;

/** Junta as misturas de um ciclo de animação, calcula nos núcleos e só então envia, na ordem original. */
public final class FilaAnimacao {
    private static final List<Runnable> CONTAS = new ArrayList<>(), ENVIOS = new ArrayList<>();
    private static boolean aberta;

    private FilaAnimacao() { }

    public static void abre() {
        aberta = true;
    }

    public static void faz(Runnable conta, Runnable envio) {
        if (aberta) {
            CONTAS.add(conta);
            ENVIOS.add(envio);
        } else {
            conta.run();
            envio.run();
        }
    }

    public static void fecha() {
        aberta = false;
        if (CONTAS.isEmpty()) return;
        try {
            Nucleos.todas(CONTAS);
            for (Runnable r : ENVIOS) r.run();
        } finally {
            CONTAS.clear();
            ENVIOS.clear();
        }
    }
}
