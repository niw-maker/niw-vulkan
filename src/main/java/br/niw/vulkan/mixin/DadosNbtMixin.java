package br.niw.vulkan.mixin;

import br.niw.vulkan.entidades.NbtLembrado;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NbtPredicate.class)
public class DadosNbtMixin {
    @Redirect(method = "matches(Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/NbtPredicate;getEntityTagToCompare(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag niw$lembra(Entity e) {
        CompoundTag t = NbtLembrado.de(e);
        if (t == null) NbtLembrado.guarda(e, t = NbtPredicate.getEntityTagToCompare(e));
        return t;
    }
}
