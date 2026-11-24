package br.niw.vulkan.inicio;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Chaves de cada módulo: arquivo config/niwvulkan.properties, com -Dniwvulkan.CHAVE=valor por cima. */
public final class Chaves {
    public static final Logger LOG = LoggerFactory.getLogger("NiwVulkan");
    private static final Map<String, String> PADRAO = new LinkedHashMap<>();
    private static final Properties P = new Properties();

    static {
        PADRAO.put("vulkan", "true");
        PADRAO.put("terreno", "true");
        PADRAO.put("lotes", "true");
        PADRAO.put("modelos", "true");
        PADRAO.put("entidades", "true");
        PADRAO.put("oclusao", "true");
        PADRAO.put("prioridade", "true");
        PADRAO.put("geracao", "true");
        PADRAO.put("palladium", "true");
        PADRAO.put("mundo", "true");
        PADRAO.put("texturas", "true");
        Path arq = FMLPaths.CONFIGDIR.get().resolve("niwvulkan.properties");
        PADRAO.forEach(P::setProperty);
        try {
            if (Files.exists(arq)) {
                try (Reader r = Files.newBufferedReader(arq)) { P.load(r); }
            }
            try (Writer w = Files.newBufferedWriter(arq)) {
                w.write("# NiwVulkan: true liga, false desliga cada modulo (vale na proxima abertura do jogo)\n");
                for (String k : PADRAO.keySet()) w.write(k + "=" + P.getProperty(k) + "\n");
            }
        } catch (Exception e) {
            LOG.warn("não consegui ler ou gravar {}", arq, e);
        }
    }

    private Chaves() { }

    private static void escreve(Path arq) throws java.io.IOException {
        try (Writer w = Files.newBufferedWriter(arq)) {
            w.write("# NiwVulkan: true liga, false desliga cada modulo (vale na proxima abertura do jogo)\n");
            for (String k : PADRAO.keySet()) w.write(k + "=" + P.getProperty(k) + "\n");
        }
    }

    public static void grava(String chave, String valor) {
        P.setProperty(chave, valor);
        try {
            escreve(FMLPaths.CONFIGDIR.get().resolve("niwvulkan.properties"));
        } catch (Exception e) {
            LOG.warn("não consegui gravar a chave {}", chave, e);
        }
    }

    public static boolean ligada(String chave) {
        return Boolean.parseBoolean(System.getProperty("niwvulkan." + chave, P.getProperty(chave, "true")));
    }

    public static String texto(String chave, String padrao) {
        return System.getProperty("niwvulkan." + chave, P.getProperty(chave, padrao));
    }
}
