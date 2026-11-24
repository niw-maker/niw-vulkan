package br.niw.vulkan.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Busca de entidades numa caixa pequena: o original percorre todas as seções da fatia inteira do mundo para achar umas
 * poucas. Aqui vai direto nelas, na mesma ordem.
 */
@Mixin(EntitySectionStorage.class)
public class MundoEntidadesMixin<T extends EntityAccess> {
    @Shadow @Final private Long2ObjectMap<EntitySection<T>> sections;

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("HEAD"), cancellable = true)
    private void niw$direto(AABB b, AbortableIterationConsumer<EntitySection<T>> c, CallbackInfo ci) {
        int x0 = SectionPos.posToSectionCoord(b.minX - 2.0), x1 = SectionPos.posToSectionCoord(b.maxX + 2.0);
        int y0 = SectionPos.posToSectionCoord(b.minY - 4.0), y1 = SectionPos.posToSectionCoord(b.maxY);
        int z0 = SectionPos.posToSectionCoord(b.minZ - 2.0), z1 = SectionPos.posToSectionCoord(b.maxZ + 2.0);
        if (x1 < x0 || y1 < y0 || z1 < z0 || ((long) x1 - x0 + 1) * ((long) y1 - y0 + 1) * ((long) z1 - z0 + 1) > 128 || (long) x1 - x0 > 64) return;
        ci.cancel();
        for (int x = x0; x <= x1; x++) {
            // a ordem original é a da chave: z e y sem sinal, então os negativos vêm depois
            if (niw$z(x, y0, y1, Math.max(z0, 0), z1, c) || niw$z(x, y0, y1, z0, Math.min(z1, -1), c)) return;
        }
    }

    @Unique
    private boolean niw$z(int x, int y0, int y1, int z0, int z1, AbortableIterationConsumer<EntitySection<T>> c) {
        for (int z = z0; z <= z1; z++)
            if (niw$y(x, Math.max(y0, 0), y1, z, c) || niw$y(x, y0, Math.min(y1, -1), z, c)) return true;
        return false;
    }

    @Unique
    private boolean niw$y(int x, int y0, int y1, int z, AbortableIterationConsumer<EntitySection<T>> c) {
        for (int y = y0; y <= y1; y++) {
            EntitySection<T> s = sections.get(SectionPos.asLong(x, y, z));
            if (s != null && !s.isEmpty() && s.getStatus().isAccessible() && c.accept(s).shouldAbort()) return true;
        }
        return false;
    }
}
