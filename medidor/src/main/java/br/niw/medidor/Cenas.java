package br.niw.medidor;

import com.mojang.brigadier.ParseResults;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cenas que põem na tela o que os mods trazem: todos os mobs, armaduras, poderes do Palladium e partículas. */
final class Cenas {
    static final Logger LOG = LoggerFactory.getLogger("niwmedidor");
    private final Medidor m;
    private final List<SimpleParticleType> particulas = new ArrayList<>();
    private final List<String> poderes = new ArrayList<>();
    private int proximoPoder;
    private double ultimoPoder;

    Cenas(Medidor m) {
        this.m = m;
    }

    private void chao(int bx, int by, int bz) {
        m.cmd("gamerule doMobLoot false");
        m.cmd("gamerule mobGriefing false");
        m.cmd("kill @e[type=!player]");
        m.cmd("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(bx - 30, by - 8, bz + 2, bx + 30, by - 8, bz + 50));
        for (int k = 1; k < 10; k++) m.cmd("fill %d %d %d %d %d %d minecraft:air".formatted(bx - 30, by - 8 + k, bz + 2, bx + 30, by - 8 + k, bz + 50));
    }

    /** Um de cada mob dos mods (até 150), parados, em grade. */
    void bestiario(int bx, int by, int bz) {
        chao(bx, by, bz);
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer s = mc.getSingleplayerServer();
        List<String> tipos = new ArrayList<>();
        s.execute(() -> {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                if (id.startsWith("minecraft:") || t.getCategory() == MobCategory.MISC || id.contains("boss") || id.contains("dragon")) continue;
                try {
                    if (t.create(s.overworld()) instanceof Mob) tipos.add(id);
                } catch (Throwable e) {
                    LOG.warn("mob {} não pôde ser criado: {}", id, e.toString());
                }
            }
            int n = Math.min(150, tipos.size());
            LOG.info("bestiario: {} tipos de mob, usando {}", tipos.size(), n);
            for (int i = 0; i < n; i++)
                m.cmd("summon %s %d %d %d {NoAI:1b,Silent:1b,NoGravity:1b,PersistenceRequired:1b,Rotation:[180f,0f]}".formatted(
                    tipos.get(i * tipos.size() / n), bx - 28 + (i % 15) * 4, by - 7, bz + 6 + (i / 15) * 4));
        });
    }

    /** Zumbis vestidos com as armaduras dos mods, poderes no jogador e partículas de todos os mods. */
    void efeitos(int bx, int by, int bz) {
        chao(bx, by, bz);
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer s = mc.getSingleplayerServer();
        particulas.clear();
        for (var t : BuiltInRegistries.PARTICLE_TYPE) if (t instanceof SimpleParticleType p) particulas.add(p);
        s.execute(() -> {
            List<String>[] pecas = new List[4];
            for (int i = 0; i < 4; i++) pecas[i] = new ArrayList<>();
            for (Item it : BuiltInRegistries.ITEM)
                if (it instanceof ArmorItem a && !BuiltInRegistries.ITEM.getKey(it).getNamespace().equals("minecraft"))
                    pecas[a.getType().getSlot().getIndex()].add(BuiltInRegistries.ITEM.getKey(it).toString());
            for (int i = 0; i < 60; i++) {
                StringBuilder ar = new StringBuilder();
                for (int k = 0; k < 4; k++)
                    ar.append(k > 0 ? "," : "").append(pecas[k].isEmpty() ? "{}" : "{id:\"" + pecas[k].get(i * 7 % pecas[k].size()) + "\",Count:1b}");
                m.cmd("summon minecraft:zombie %d %d %d {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],ArmorItems:[%s]}".formatted(
                    bx - 20 + (i % 10) * 4, by - 7, bz + 8 + (i / 10) * 5, ar));
            }
            LOG.info("efeitos: {} tipos de partícula, armaduras {}/{}/{}/{}", particulas.size(), pecas[0].size(), pecas[1].size(), pecas[2].size(), pecas[3].size());
            achaPoderes(s);
        });
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
    }

    private void achaPoderes(MinecraftServer s) {
        try {
            CommandSourceStack c = s.getPlayerList().getPlayers().get(0).createCommandSourceStack().withPermission(4);
            var d = s.getCommands().getDispatcher();
            var raiz = d.getRoot().getChild("superpower");
            if (raiz == null) {
                LOG.info("poderes: sem Palladium");
                return;
            }
            for (String u : d.getAllUsage(raiz, c, false)) LOG.info("poderes: uso /superpower {}", u);
            for (String verbo : new String[]{"set", "add", "replace"}) {
                String linha = "superpower " + verbo + " ";
                ParseResults<CommandSourceStack> p = d.parse(linha, c);
                d.getCompletionSuggestions(p).get().getList().forEach(x -> poderes.add("superpower " + verbo + " " + x.getText() + " %s"));
                if (!poderes.isEmpty()) break;
            }
            LOG.info("poderes: {} encontrados, ex.: {}", poderes.size(), poderes.isEmpty() ? "-" : poderes.get(0));
        } catch (Throwable t) {
            LOG.warn("poderes", t);
        }
    }

    /** A cada quadro da cena de efeitos. */
    void quadro(double dt, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        Random r = new Random((long) (dt * 1000));
        if (particulas.isEmpty() || mc.level == null) return;
        if (dt - ultimoPoder > 1 || dt < ultimoPoder) {
            ultimoPoder = dt;
            if (!poderes.isEmpty()) {
                m.cmd(poderes.get(proximoPoder++ * 37 % poderes.size()).formatted("@a"));
            }
        }
        if (mc.level.getGameTime() == ultimoTick) return;
        ultimoTick = mc.level.getGameTime();
        for (int i = 0; i < 120; i++) {
            SimpleParticleType p = particulas.get(r.nextInt(particulas.size()));
            try {
                mc.level.addParticle(p, true, x + (r.nextDouble() - 0.5) * 24, y - 6 + r.nextDouble() * 8, z + 4 + r.nextDouble() * 24,
                    (r.nextDouble() - 0.5) * 0.1, r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.1);
            } catch (Throwable t) {
                particulas.remove(p);
                return;
            }
        }
    }

    private long ultimoTick;

    void fim() {
        Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
    }
}
