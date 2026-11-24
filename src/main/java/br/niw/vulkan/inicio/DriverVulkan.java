package br.niw.vulkan.inicio;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

/**
 * Carrega o OpenGL do Mesa (Zink, que executa em Vulkan) só dentro deste processo, antes de a janela existir.
 * Como a DLL já está na memória com o nome opengl32.dll, o GLFW e o LWJGL usam essa no lugar da do sistema.
 */
public final class DriverVulkan {
    public static volatile boolean ativo;
    private static final String[] DLLS = {"libgallium_wgl.dll", "opengl32.dll"};

    private DriverVulkan() { }

    public static void carrega() {
        if (!System.getProperty("os.name").startsWith("Windows")) return;
        try {
            SharedLibrary k32 = Library.loadNative(DriverVulkan.class, "org.lwjgl", "kernel32");
            if (jaCarregada(k32)) {
                // A janela de carregamento do Forge abre o OpenGL antes dos mods. Desliga só ela, a partir da próxima vez.
                FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL, false);
                Chaves.LOG.warn("A janela antecipada do Forge já tinha aberto o OpenGL. Ela foi desligada em config/fml.toml: "
                    + "feche e abra o jogo mais uma vez para rodar em Vulkan.");
                return;
            }
            Path marca = marca();
            if (Files.exists(marca)) {
                Files.delete(marca);
                Chaves.grava("vulkan", "false");
                Chaves.LOG.error("Na última vez o jogo não passou da abertura com Vulkan; ele foi desligado em config/niwvulkan.properties (vulkan=false).");
                return;
            }
            if (!temVulkan()) {
                Chaves.LOG.warn("Este PC não tem Vulkan 1.1 ou mais novo; o jogo segue em OpenGL normal");
                return;
            }
            Path dir = pasta();
            if (dir == null) return;
            env(k32, "GALLIUM_DRIVER", "zink");
            env(k32, "mesa_glthread", Chaves.texto("glthread", "true"));
            env(k32, "MESA_SHADER_CACHE_DIR", FMLPaths.GAMEDIR.get().resolve(".niwvulkan/shaders").toString());
            for (String kv : Chaves.texto("mesa", "").split(";"))
                if (kv.contains("=")) env(k32, kv.substring(0, kv.indexOf('=')), kv.substring(kv.indexOf('=') + 1));
            for (String dll : DLLS) System.load(dir.resolve(dll).toAbsolutePath().toString());
            Configuration.OPENGL_LIBRARY_NAME.set(dir.resolve("opengl32.dll").toAbsolutePath().toString());
            Files.createDirectories(marca.getParent());
            Files.writeString(marca, "apagado quando a janela abre");
            ativo = true;
            Chaves.LOG.info("OpenGL em Vulkan (Zink) carregado de {}", dir);
        } catch (Throwable t) {
            Chaves.LOG.error("não deu para carregar o Vulkan; o jogo segue em OpenGL normal", t);
        }
    }

    private static Path marca() {
        return FMLPaths.GAMEDIR.get().resolve(".niwvulkan/abrindo");
    }

    /** A janela abriu e o contexto está de pé. */
    public static void abriu() {
        if (!ativo) return;
        try {
            Files.deleteIfExists(marca());
        } catch (Exception e) {
            Chaves.LOG.warn("não consegui apagar a marca de abertura", e);
        }
    }

    private static boolean temVulkan() {
        try {
            SharedLibrary vk = Library.loadNative(DriverVulkan.class, "org.lwjgl", "vulkan-1");
            long f = vk.getFunctionAddress("vkEnumerateInstanceVersion");
            if (f == 0) return false;
            try (MemoryStack s = MemoryStack.stackPush()) {
                java.nio.IntBuffer v = s.callocInt(1);
                return JNI.callPI(MemoryUtil.memAddress(v), f) == 0 && v.get(0) >= (1 << 22 | 1 << 12);
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean jaCarregada(SharedLibrary k32) {
        long f = k32.getFunctionAddress("GetModuleHandleW");
        try (MemoryStack s = MemoryStack.stackPush()) {
            return JNI.callPP(MemoryUtil.memAddress(s.UTF16("opengl32.dll")), f) != 0;
        }
    }

    /** A DLL tem o próprio CRT e lê o ambiente do processo quando é carregada. */
    private static void env(SharedLibrary k32, String k, String v) {
        try (MemoryStack s = MemoryStack.stackPush()) {
            JNI.callPPI(MemoryUtil.memAddress(s.UTF16(k)), MemoryUtil.memAddress(s.UTF16(v)), k32.getFunctionAddress("SetEnvironmentVariableW"));
        }
    }

    private static Path pasta() throws Exception {
        String fora = System.getProperty("niwvulkan.dlls");
        if (fora != null) return Path.of(fora);
        Path dir = FMLPaths.GAMEDIR.get().resolve(".niwvulkan/mesa");
        Files.createDirectories(dir);
        for (String dll : DLLS) {
            try (InputStream in = DriverVulkan.class.getResourceAsStream("/niwvulkan/win64/" + dll)) {
                if (in == null) {
                    Chaves.LOG.warn("este jar não traz {}; o jogo segue em OpenGL normal", dll);
                    return null;
                }
                byte[] d = in.readAllBytes();
                Path alvo = dir.resolve(dll);
                if (Files.exists(alvo) && Arrays.equals(Files.readAllBytes(alvo), d)) continue;
                Path tmp = dir.resolve(dll + ".novo");
                Files.write(tmp, d);
                Files.move(tmp, alvo, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dir;
    }
}
