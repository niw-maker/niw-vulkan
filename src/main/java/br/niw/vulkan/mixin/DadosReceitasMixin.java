package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.ConjuntoContado;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Datapacks que testam os dados do jogador pedem esta lista, com milhares de receitas, várias vezes por tick. */
@Mixin(ServerRecipeBook.class)
public class DadosReceitasMixin extends RecipeBook {
    @Inject(method = "toNbt", at = @At("HEAD"), cancellable = true)
    private void niw$guardado(CallbackInfoReturnable<CompoundTag> cir) {
        if (!(known instanceof ConjuntoContado k) || !(highlight instanceof ConjuntoContado h)) return;
        CompoundTag c = new CompoundTag();
        getBookSettings().write(c);
        c.put("recipes", k.nbt());
        c.put("toBeDisplayed", h.nbt());
        cir.setReturnValue(c);
    }
}
