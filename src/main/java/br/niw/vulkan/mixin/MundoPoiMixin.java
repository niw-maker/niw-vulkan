package br.niw.vulkan.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aldeões procuram camas e postos o tempo todo; quase toda seção é vazia e não precisa virar stream. */
@Mixin(PoiManager.class)
public abstract class MundoPoiMixin extends SectionStorage<PoiSection> {
    private MundoPoiMixin(Path a, Function<Runnable, Codec<PoiSection>> b, Function<Runnable, PoiSection> c, DataFixer d, DataFixTypes e, boolean f, RegistryAccess g, LevelHeightAccessor h) {
        super(a, b, c, d, e, f, g, h);
    }

    @Inject(method = "getInChunk", at = @At("HEAD"), cancellable = true)
    private void niw$direto(Predicate<Holder<PoiType>> tipo, ChunkPos p, PoiManager.Occupancy ocupacao, CallbackInfoReturnable<Stream<PoiRecord>> cir) {
        Stream<PoiRecord> s = null;
        for (int y = levelHeightAccessor.getMinSection(), f = levelHeightAccessor.getMaxSection(); y < f; y++) {
            Optional<PoiSection> o = getOrLoad(SectionPos.asLong(p.x, y, p.z));
            if (o.isEmpty()) continue;
            Stream<PoiRecord> r = o.get().getRecords(tipo, ocupacao);
            s = s == null ? r : Stream.concat(s, r);
        }
        cir.setReturnValue(s == null ? Stream.empty() : s);
    }
}
