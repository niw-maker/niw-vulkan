package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.ConjuntoContado;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBook.class)
public class DadosLivroMixin {
    @Shadow @Final @Mutable protected Set<ResourceLocation> known;
    @Shadow @Final @Mutable protected Set<ResourceLocation> highlight;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void niw$conta(CallbackInfo ci) {
        if (known.isEmpty()) known = new ConjuntoContado();
        if (highlight.isEmpty()) highlight = new ConjuntoContado();
    }
}
