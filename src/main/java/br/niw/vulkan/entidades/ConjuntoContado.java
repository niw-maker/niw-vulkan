package br.niw.vulkan.entidades;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Predicate;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

/** HashSet que sabe se mudou desde a última conversão para NBT. */
public final class ConjuntoContado extends HashSet<ResourceLocation> {
    private transient ListTag pronto;

    /** Lista na mesma ordem de iteração, refeita só se o conjunto mudou. Devolve cópia: quem recebe pode alterar. */
    public ListTag nbt() {
        ListTag l = pronto;
        if (l == null) {
            l = new ListTag();
            for (ResourceLocation r : this) l.add(StringTag.valueOf(r.toString()));
            pronto = l;
        }
        return l.copy();
    }

    @Override public boolean add(ResourceLocation r) {
        boolean m = super.add(r);
        if (m) pronto = null;
        return m;
    }

    @Override public boolean remove(Object o) {
        boolean m = super.remove(o);
        if (m) pronto = null;
        return m;
    }

    @Override public void clear() {
        pronto = null;
        super.clear();
    }

    @Override public boolean removeAll(Collection<?> c) {
        pronto = null;
        return super.removeAll(c);
    }

    @Override public boolean retainAll(Collection<?> c) {
        pronto = null;
        return super.retainAll(c);
    }

    @Override public boolean removeIf(Predicate<? super ResourceLocation> p) {
        pronto = null;
        return super.removeIf(p);
    }

    @Override public Iterator<ResourceLocation> iterator() {
        Iterator<ResourceLocation> i = super.iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return i.hasNext(); }
            @Override public ResourceLocation next() { return i.next(); }
            @Override public void remove() {
                i.remove();
                pronto = null;
            }
        };
    }
}
