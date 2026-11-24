package br.niw.vulkan.entidades;

import br.niw.vulkan.terreno.Visao;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

/**
 * Não desenha entidade que está inteira em seções que o jogo já sabe que não dá para ver (atrás de paredes, em cavernas).
 * Fica de fora tudo que pode aparecer através de paredes ou longe do próprio corpo.
 */
public final class Oclusao {
    /** Preenchida só durante o passe do mundo principal. */
    public static Visao visao;
    private static final Reference2BooleanOpenHashMap<Class<?>> COMUM = new Reference2BooleanOpenHashMap<>();

    private Oclusao() { }

    public static boolean esconde(Entity e, EntityRenderer<?> r) {
        Visao v = visao;
        if (v == null || e.noCulling || e instanceof Player || e instanceof Projectile || e instanceof Display || e.hasCustomName()) return false;
        if (e instanceof Mob m && m.isLeashed()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.getCameraEntity() || !comum(r)) return false;
        AABB b = e.getBoundingBoxForCulling();
        if (b.hasNaN() || b.getSize() == 0 || b.getSize() > 12) return false;
        int x0 = (int) Math.floor(b.minX - 1.5) >> 4, x1 = (int) Math.floor(b.maxX + 1.5) >> 4;
        int y0 = (int) Math.floor(b.minY - 1.5) >> 4, y1 = (int) Math.floor(b.maxY + 1.5) >> 4;
        int z0 = (int) Math.floor(b.minZ - 1.5) >> 4, z1 = (int) Math.floor(b.maxZ + 1.5) >> 4;
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = y0; y <= y1; y++)
                    if (v.alcanca(x, y, z)) return false;
        return !mc.shouldEntityAppearGlowing(e);
    }

    /** Desenhista que tem regra própria de visibilidade (feixes, cabos, cristais) nunca é escondido. */
    private static boolean comum(EntityRenderer<?> r) {
        Class<?> c = r.getClass();
        if (COMUM.containsKey(c)) return COMUM.getBoolean(c);
        boolean ok = true;
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            if (k == EntityRenderer.class || k == MobRenderer.class) continue;
            for (Method m : k.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == boolean.class && p.length == 5 && Entity.class.isAssignableFrom(p[0])
                    && p[1] == Frustum.class && p[2] == double.class && p[3] == double.class && p[4] == double.class) ok = false;
            }
        }
        COMUM.put(c, ok);
        return ok;
    }
}
