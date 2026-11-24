package br.niw.medidor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Roda um roteiro fixo e grava um JSON. Liga só com -Dniwmedidor.saida=arquivo.json.
 * Fases: carga (chunks à vista prontos), giro (parado, 360°), horda (animais + baús no campo de visão), voo (chunks novos).
 */
@Mod("niwmedidor")
public class Medidor {
    static final Logger LOG = LoggerFactory.getLogger("niwmedidor");
    final String saida = System.getProperty("niwmedidor.saida");
    final String mundo = System.getProperty("niwmedidor.mundo", "medicao");
    final String[] fases = System.getProperty("niwmedidor.fases", "carga,giro,horda,voo").split(",");
    final double seg = Double.parseDouble(System.getProperty("niwmedidor.segundos", "25"));
    final double cargaMax = Double.parseDouble(System.getProperty("niwmedidor.cargaMax", "150"));
    static final long LENTO_NS = (long) (Double.parseDouble(System.getProperty("niwmedidor.lento", "15")) * 1e6);
    final java.util.List<String> lentos = new java.util.ArrayList<>();
    static final double CARGA_MIN = Double.parseDouble(System.getProperty("niwmedidor.cargaMin", "15"));
    final int animais = Integer.getInteger("niwmedidor.animais", 250);
    final boolean fotos = Boolean.getBoolean("niwmedidor.fotos");
    final Cenas cenas = new Cenas(this);
    final JsonObject json = new JsonObject();
    final long inicioJvm = ManagementFactory.getRuntimeMXBean().getStartTime();

    boolean pediuMundo, acabou;
    int fase = -1;
    double tInicio, x0, y0, z0;
    long ultimoNs, cpuFase;
    long[] quadros = new long[1 << 20];
    int n, nTick, ultimoTickServidor, esperaTicks;
    double somaTick, maxTick, estavelDesde = -1;
    Map<Long, Long> cpuThreads = new HashMap<>();
    long gcMs, gcN;
    int secAntes, chkAntes;

    public Medidor() {
        if (saida == null || FMLEnvironment.dist != Dist.CLIENT) return;
        MinecraftForge.EVENT_BUS.register(this);
        LOG.info("medidor ligado: {}", String.join(",", fases));
    }

    static double agora() { return System.nanoTime() / 1e9; }

    @SubscribeEvent
    public void tickCliente(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || acabou) return;
        Minecraft mc = Minecraft.getInstance();
        Screen tela = mc.screen;
        if (!pediuMundo && mc.level == null && mc.getOverlay() == null && tela != null) {
            // alguns mods põem telas próprias antes da tela de título
            if (esperaTicks++ < 60) return;
            if (!(tela instanceof TitleScreen)) tela = new TitleScreen();
            pediuMundo = true;
            json.addProperty("menu_s", (System.currentTimeMillis() - inicioJvm) / 1000.0);
            File dir = mc.gameDirectory.toPath().resolve("saves").resolve(mundo).toFile();
            if (new File(dir, "level.dat").exists()) {
                mc.createWorldOpenFlows().loadLevel(tela, mundo);
            } else {
                LevelSettings ls = new LevelSettings(mundo, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(mundo, ls, new WorldOptions(20260711L, true, false), WorldPresets::createNormalWorldDimensions);
            }
            return;
        }
        // confirma sozinho os avisos ao abrir o mundo (experimental, registros)
        if (pediuMundo && mc.level == null && (tela instanceof ConfirmScreen || tela instanceof BackupConfirmScreen)) {
            tela.children().stream().filter(c -> c instanceof Button).skip(tela instanceof BackupConfirmScreen ? 1 : 0).findFirst()
                .ifPresent(c -> ((Button) c).onPress());
        }
        if (mc.level != null && mc.player != null && mc.screen != null && fase >= 0 && mc.getOverlay() == null) mc.setScreen(null);
        MinecraftServer s = mc.getSingleplayerServer();
        if (s != null && s.getTickCount() != ultimoTickServidor) {
            ultimoTickServidor = s.getTickCount();
            double ms = s.tickTimes[(s.getTickCount() + 99) % 100] / 1e6;
            somaTick += ms;
            nTick++;
            maxTick = Math.max(maxTick, ms);
        }
    }

    void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer s = mc.getSingleplayerServer();
        s.execute(() -> s.getCommands().performPrefixedCommand(
            s.getPlayerList().getPlayer(mc.player.getUUID()).createCommandSourceStack().withPermission(4).withSuppressedOutput(), c));
    }

    @SubscribeEvent
    public void quadro(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (acabou || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (e.phase == TickEvent.Phase.START) {
            if (System.nanoTime() > proximaJanela && Boolean.getBoolean("niwmedidor.janela")) {
                proximaJanela = System.nanoTime() + 2_000_000_000L;
                confereJanela(mc);
            }
            move(mc);
            return;
        }
        long t = System.nanoTime();
        if (fase < 0) {
            if (mc.getOverlay() != null || mc.player.tickCount < 20) return;
            json.addProperty("no_mundo_s", (System.currentTimeMillis() - inicioJvm) / 1000.0);
            json.addProperty("gl", GlUtil.getVendor() + " | " + GlUtil.getRenderer() + " | " + GlUtil.getOpenGLVersion());
            json.addProperty("tela", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
            json.addProperty("rd", mc.options.getEffectiveRenderDistance());
            mc.options.pauseOnLostFocus = false;
            mc.getWindow().setTitle(rotulo);
            cmd("gamerule doMobSpawning false");
            cmd("gamerule doDaylightCycle false");
            cmd("gamerule doWeatherCycle false");
            cmd("time set 6000");
            cmd("weather clear");
            cmd("kill @e[type=!player]");
            cmd("gamemode creative");
            // lugar fixo: todas as rodadas, com e sem o mod, olham exatamente para a mesma coisa
            x0 = Double.parseDouble(System.getProperty("niwmedidor.x", "9.5"));
            z0 = Double.parseDouble(System.getProperty("niwmedidor.z", "-6.5"));
            y0 = Double.parseDouble(System.getProperty("niwmedidor.y", "94"));
            cmd("tp @s %s %s %s".formatted(x0, y0, z0));
            json.addProperty("pos", x0 + " " + y0 + " " + z0);
            proxima(mc);
            return;
        }
        if (n < quadros.length && ultimoNs != 0) quadros[n++] = t - ultimoNs;
        if (ultimoNs != 0 && t - ultimoNs > LENTO_NS && lentos.size() < 400)
            lentos.add(java.time.LocalTime.now() + " " + (t - ultimoNs) / 100000 / 10.0);
        ultimoNs = t;
        double dt = agora() - tInicio;
        boolean fim;
        if (fases[fase].equals("carga")) {
            int sec = mc.levelRenderer.countRenderedChunks(), chk = mc.getSingleplayerServer().overworld().getChunkSource().getLoadedChunksCount();
            boolean parado = sec == secAntes && chk == chkAntes && mc.levelRenderer.getChunkRenderDispatcher().getToBatchCount() == 0;
            secAntes = sec;
            chkAntes = chk;
            if (!parado) estavelDesde = -1;
            else if (estavelDesde < 0) estavelDesde = dt;
            fim = dt > cargaMax || (estavelDesde >= 0 && dt - estavelDesde > 6 && dt > CARGA_MIN);
            if (fim) json.addProperty("carga_pronta_s", estavelDesde);
        } else {
            if (fases[fase].equals("robustez")) robustez(mc, dt);
            fim = dt > (fases[fase].equals("robustez") ? 138 : seg);
        }
        if (fim) {
            fecha(mc);
            proxima(mc);
        }
    }

    void move(Minecraft mc) {
        if (fase < 0) return;
        double dt = agora() - tInicio;
        double x = x0, y = y0, z = z0;
        float yaw = -35, pitch = 18;
        switch (fases[fase]) {
            case "giro" -> yaw = (float) (dt / seg * 720.0);
            case "estatua" -> { pitch = 30; yaw = 0; }
            case "bestiario" -> { pitch = 25; yaw = (float) (Math.sin(dt * 0.7) * 35); }
            case "efeitos" -> { pitch = 22; yaw = (float) (Math.sin(dt * 0.7) * 20); z = z0 + 4; y = y0 - 2; cenas.quadro(dt, x0, y0, z0); }
            case "horda" -> { pitch = 35; yaw = (float) (Math.sin(dt) * 10); }
            case "voo" -> { x = x0 + dt * 20; pitch = 12; yaw = -90; y = y0 + 30; }
            default -> { }
        }
        mc.player.getAbilities().flying = true;
        mc.player.setDeltaMovement(0, 0, 0);
        mc.player.setPos(x, y, z);
        mc.player.xo = x; mc.player.yo = y; mc.player.zo = z;
        mc.player.xOld = x; mc.player.yOld = y; mc.player.zOld = z;
        mc.player.setYRot(yaw); mc.player.yRotO = yaw;
        mc.player.setXRot(pitch); mc.player.xRotO = pitch;
        mc.player.yHeadRot = yaw; mc.player.yHeadRotO = yaw;
        mc.player.yBodyRot = yaw; mc.player.yBodyRotO = yaw;
    }

    void proxima(Minecraft mc) {
        fase++;
        if (fase >= fases.length) {
            termina(mc);
            return;
        }
        if (fases[fase].equals("horda")) {
            // o mundo-base pode guardar animais de outra rodada; a contagem tem de ser sempre a mesma
            cmd("gamerule doMobLoot false");
            cmd("kill @e[type=!player]");
            cmd("kill @e[type=item]");
            int bx = (int) x0, by = (int) y0, bz = (int) z0;
            cmd("fill %d %d %d %d %d %d minecraft:grass_block".formatted(bx - 22, by - 10, bz + 2, bx + 22, by - 10, bz + 44));
            for (int k = 1; k < 9; k++)
                cmd("fill %d %d %d %d %d %d minecraft:air".formatted(bx - 22, by - 10 + k, bz + 2, bx + 22, by - 10 + k, bz + 44));
            String[] tipos = {"cow", "sheep", "pig", "chicken", "villager", "horse"};
            for (int i = 0; i < animais; i++)
                cmd("summon minecraft:%s %d %d %d".formatted(tipos[i % tipos.length], bx - 18 + (i * 7) % 37, by - 9, bz + 5 + (i * 13) % 36));
            for (int i = 0; i < 150; i++)
                cmd("setblock %d %d %d minecraft:chest".formatted(bx - 20 + (i % 15) * 3 - 1, by - 9, bz + 4 + (i / 15) * 4));
        }
        if (fase > 0 && fases[fase - 1].equals("efeitos")) cenas.fim();
        if (fases[fase].equals("bestiario")) cenas.bestiario((int) x0, (int) y0, (int) z0);
        if (fases[fase].equals("efeitos")) cenas.efeitos((int) x0, (int) y0, (int) z0);
        if (fases[fase].equals("estatua")) estatua();
        if (fases[fase].equals("robustez")) passo = 0;
        tInicio = agora();
        n = 0;
        ultimoNs = 0;
        somaTick = 0;
        nTick = 0;
        maxTick = 0;
        estavelDesde = -1;
        cpuFase = cpuProcesso();
        cpuThreads = threads();
        gcMs = gcTempo();
        gcN = gcCont();
        LOG.info("fase {}", fases[fase]);
    }

    /** Cena parada com tipos de desenho variados, para comparar imagens com e sem o mod. */
    void estatua() {
        int bx = (int) x0, by = (int) y0, bz = (int) z0;
        cmd("kill @e[type=!player]");
        cmd("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(bx - 20, by - 8, bz + 2, bx + 20, by - 8, bz + 30));
        for (int k = 1; k < 8; k++) cmd("fill %d %d %d %d %d %d minecraft:air".formatted(bx - 20, by - 8 + k, bz + 2, bx + 20, by - 8 + k, bz + 30));
        String[] m = {"cow", "sheep", "horse", "villager", "zombie", "skeleton", "spider", "enderman", "slime", "magma_cube", "creeper", "pig",
            "llama", "fox", "wolf", "cat", "iron_golem", "shulker", "witch", "blaze", "piglin", "warden", "allay", "axolotl"};
        for (int i = 0; i < m.length; i++)
            cmd("summon minecraft:%s %d %d %d {NoAI:1b,Silent:1b,Rotation:[180f,0f],NoGravity:1b,Size:2,HandItems:[{id:\"minecraft:diamond_sword\",Count:1b,tag:{Enchantments:[{id:\"minecraft:sharpness\",lvl:1s}]}},{}],ArmorItems:[{id:\"minecraft:iron_boots\",Count:1b},{id:\"minecraft:gold_leggings\" ,Count:1b},{id:\"minecraft:leather_chestplate\",Count:1b},{id:\"minecraft:chainmail_helmet\",Count:1b}]}"
                .formatted(m[i], bx - 16 + (i % 8) * 4, by - 7, bz + 8 + (i / 8) * 5));
        String[] b = {"chest", "ender_chest", "shulker_box", "red_bed", "white_banner", "oak_sign", "enchanting_table", "conduit", "bell", "skeleton_skull",
            "decorated_pot", "lectern", "red_stained_glass", "ice", "slime_block", "honey_block", "campfire", "oak_leaves", "poppy", "water"};
        for (int i = 0; i < b.length; i++) cmd("setblock %d %d %d minecraft:%s".formatted(bx - 19 + i * 2, by - 7, bz + 5, b[i]));
        for (int i = 0; i < 5; i++) {
            cmd("summon minecraft:armor_stand %d %d %d {Rotation:[180f,0f],ShowArms:1b,ArmorItems:[{id:\"minecraft:diamond_boots\",Count:1b,tag:{Enchantments:[{}]}},{},{},{id:\"minecraft:player_head\",Count:1b}]}".formatted(bx - 10 + i * 5, by - 7, bz + 24));
            cmd("summon minecraft:glow_item_frame %d %d %d {Facing:1b,Fixed:1b,Item:{id:\"minecraft:%s\",Count:1b}}".formatted(bx - 10 + i * 5, by - 7, bz + 6, i % 2 == 0 ? "clock" : "glass"));
        }
    }

    final String rotulo = System.getProperty("niwmedidor.rotulo", "TESTE");
    long proximaJanela;
    int fpsContados, fpsAgora;
    long fpsSegundo;
    final com.google.gson.JsonArray janela = new com.google.gson.JsonArray();

    /** Etiqueta na tela: quem está olhando sabe se o teste é o normal ou o otimizado. */
    @SubscribeEvent
    public void etiqueta(net.minecraftforge.client.event.RenderGuiEvent.Post e) {
        long seg = System.nanoTime() / 1_000_000_000L;
        fpsContados++;
        if (seg != fpsSegundo) {
            fpsSegundo = seg;
            fpsAgora = fpsContados;
            fpsContados = 0;
        }
        Minecraft mc = Minecraft.getInstance();
        String t = rotulo + "  |  cena: " + (fase >= 0 && fase < fases.length ? fases[fase] : "abrindo") + "  |  " + fpsAgora + " fps";
        var g = e.getGuiGraphics();
        int w = mc.font.width(t) + 8, x = mc.getWindow().getGuiScaledWidth() - w - 4;
        g.fill(x, 4, x + w, 18, 0xC0000000);
        g.drawString(mc.font, t, x + 4, 7, rotulo.startsWith("OTIMIZADO") ? 0xFF55FF55 : 0xFFFFAA00);
    }

    /** Olha o que foi apresentado na janela do próprio jogo (não a tela do PC): claro demais = janela branca. */
    void confereJanela(Minecraft mc) {
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w < 64 || h < 64) return;
        int antes = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
        java.nio.ByteBuffer b = org.lwjgl.system.MemoryUtil.memAlloc(w * 4);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, 0);
        org.lwjgl.opengl.GL11.glReadBuffer(org.lwjgl.opengl.GL11.GL_FRONT);
        long soma = 0;
        int branco = 0, n = 0;
        for (int k = 1; k < 8; k++) {
            org.lwjgl.opengl.GL11.glReadPixels(0, h * k / 8, w, 1, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, b);
            for (int i = 0; i < w; i += 8) {
                int c = b.getInt(i * 4), m = Math.min(c & 255, Math.min(c >> 8 & 255, c >> 16 & 255));
                soma += (c & 255) + (c >> 8 & 255) + (c >> 16 & 255);
                if (m > 245) branco++;
                n++;
            }
        }
        org.lwjgl.opengl.GL11.glReadBuffer(org.lwjgl.opengl.GL11.GL_BACK);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, antes);
        org.lwjgl.system.MemoryUtil.memFree(b);
        while (org.lwjgl.opengl.GL11.glGetError() != 0) { }
        janela.add(java.time.LocalTime.now().withNano(0) + " " + (fase >= 0 && fase < fases.length ? fases[fase] : "-") + " media=" + soma / (3 * n) + " branco=" + branco * 100 / n + "%");
        json.add("janela", janela);
    }

    int passo;

    /** Mexe em tudo que costuma derrubar um renderizador: recarga, distância, dimensões, tamanho da janela, opções. */
    void robustez(Minecraft mc, double dt) {
        int p = (int) (dt / 6);
        if (p < passo) return;
        passo = p + 1;
        LOG.info("robustez passo {}", p);
        switch (p) {
            case 0 -> mc.levelRenderer.allChanged();
            case 1 -> mc.options.renderDistance().set(20);
            case 2 -> mc.options.renderDistance().set(5);
            case 3 -> mc.options.renderDistance().set(12);
            case 4 -> { mc.options.graphicsMode().set(net.minecraft.client.GraphicsStatus.FAST); mc.levelRenderer.allChanged(); }
            case 5 -> { mc.options.graphicsMode().set(net.minecraft.client.GraphicsStatus.FABULOUS); mc.levelRenderer.allChanged(); }
            case 6 -> { mc.options.graphicsMode().set(net.minecraft.client.GraphicsStatus.FANCY); mc.levelRenderer.allChanged(); }
            case 7 -> mc.getWindow().setWindowed(1280, 720);
            case 8 -> mc.getWindow().setWindowed(1920, 1080);
            case 9 -> mc.reloadResourcePacks();
            case 11 -> { x0 = 100.5; z0 = 0.5; y0 = 80; cmd("execute in minecraft:the_nether run tp @s 100 80 0"); }
            case 13 -> { y0 = 90; cmd("execute in minecraft:the_end run tp @s 100 90 0"); }
            case 15 -> { x0 = 9.5; z0 = -6.5; y0 = 94; cmd("execute in minecraft:overworld run tp @s 9.5 94 -6.5"); }
            case 17 -> { cmd("summon minecraft:lightning_bolt ~ ~ ~20"); cmd("weather thunder"); cmd("time set 18000"); }
            case 18 -> { cmd("effect give @e minecraft:glowing 30"); estatua(); }
            case 19 -> { cmd("weather clear"); cmd("time set 6000"); mc.options.ambientOcclusion().set(false); mc.levelRenderer.allChanged(); }
            case 20 -> { mc.options.ambientOcclusion().set(true); mc.options.mipmapLevels().set(0); mc.updateMaxMipLevel(0); mc.delayTextureReload(); }
            default -> { }
        }
        int erro = org.lwjgl.opengl.GL11C.glGetError();
        if (erro != 0) LOG.error("robustez: erro GL {} no passo {}", erro, p);
        if (fotos && p >= 1) {
            NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            try { img.writeToFile(Path.of(saida + ".rob%02d.png".formatted(p))); } catch (Exception ex) { LOG.error("foto", ex); }
            img.close();
        }
    }

    static long cpuProcesso() {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
    }

    static Map<Long, Long> threads() {
        ThreadMXBean b = ManagementFactory.getThreadMXBean();
        Map<Long, Long> m = new HashMap<>();
        for (long id : b.getAllThreadIds()) m.put(id, b.getThreadCpuTime(id));
        return m;
    }

    static long gcTempo() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum(); }

    static long gcCont() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum(); }

    void fecha(Minecraft mc) {
        double dt = agora() - tInicio;
        JsonObject o = new JsonObject();
        long[] q = Arrays.copyOf(quadros, n);
        Arrays.sort(q);
        double soma = 0;
        for (long v : q) soma += v;
        o.addProperty("segundos", dt);
        o.addProperty("quadros", n);
        if (n > 0) {
            o.addProperty("fps", n / (soma / 1e9));
            o.addProperty("p50_ms", q[n / 2] / 1e6);
            o.addProperty("p99_ms", q[(int) (n * 0.99)] / 1e6);
            o.addProperty("pior_ms", q[n - 1] / 1e6);
            o.addProperty("fps_1pc", piores(q, 0.01));
            o.addProperty("fps_01pc", piores(q, 0.001));
            o.addProperty("travadas_50ms", Arrays.stream(q).filter(v -> v > 50_000_000L).count());
        }
        o.addProperty("mspt", nTick > 0 ? somaTick / nTick : 0);
        o.addProperty("mspt_max", maxTick);
        o.addProperty("ticks_servidor", nTick);
        o.addProperty("nucleos_usados", (cpuProcesso() - cpuFase) / 1e9 / dt);
        ThreadMXBean b = ManagementFactory.getThreadMXBean();
        Map<String, Double> por = new HashMap<>();
        threads().forEach((id, v) -> {
            var info = b.getThreadInfo(id);
            if (info == null || v < 0) return;
            por.merge(info.getThreadName().replaceAll("[-#]?\\d+$", ""), (v - cpuThreads.getOrDefault(id, 0L)) / 1e7 / dt, Double::sum);
        });
        JsonObject th = new JsonObject();
        por.entrySet().stream().sorted((a, c) -> Double.compare(c.getValue(), a.getValue())).limit(8)
            .forEach(x -> th.addProperty(x.getKey(), Math.round(x.getValue() * 10) / 10.0));
        o.add("threads_pct", th);
        o.addProperty("gc_ms", gcTempo() - gcMs);
        o.addProperty("gc_n", gcCont() - gcN);
        Runtime r = Runtime.getRuntime();
        o.addProperty("heap_mb", (r.totalMemory() - r.freeMemory()) >> 20);
        o.addProperty("secoes", mc.levelRenderer.countRenderedChunks());
        o.addProperty("entidades", mc.levelRenderer.getEntityStatistics());
        o.addProperty("chunks_txt", mc.levelRenderer.getChunkStatistics());
        o.addProperty("chunks_servidor", mc.getSingleplayerServer().overworld().getChunkSource().getLoadedChunksCount());
        com.google.gson.JsonArray la = new com.google.gson.JsonArray();
        lentos.forEach(la::add);
        o.add("lentos", la);
        lentos.clear();
        json.add(fases[fase], o);
        if (fotos) {
            NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            try { img.writeToFile(Path.of(saida + "." + fases[fase] + ".png")); } catch (Exception ex) { LOG.error("foto", ex); }
            img.close();
        }
        LOG.info("fase {} -> {}", fases[fase], o);
        grava();
    }

    static double piores(long[] q, double f) {
        int k = Math.max(1, (int) (q.length * f));
        double s = 0;
        for (int i = q.length - k; i < q.length; i++) s += q[i];
        return 1e9 / (s / k);
    }

    void grava() {
        try { Files.writeString(Path.of(saida), new GsonBuilder().setPrettyPrinting().create().toJson(json)); }
        catch (Exception ex) { LOG.error("gravar", ex); }
    }

    void termina(Minecraft mc) {
        acabou = true;
        json.addProperty("completo", true);
        grava();
        LOG.info("medidor terminou");
        mc.stop();
    }
}
