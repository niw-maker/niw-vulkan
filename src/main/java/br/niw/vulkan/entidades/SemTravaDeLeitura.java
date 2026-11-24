package br.niw.vulkan.entidades;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Os dados de uma entidade só ganham chaves novas na construção; ler um valor não precisa de trava.
 * A escrita continua travada como no original.
 */
public final class SemTravaDeLeitura extends ReentrantReadWriteLock {
    private final ReadLock leitura = new ReadLock(this) {
        @Override public void lock() { }
        @Override public void unlock() { }
    };

    @Override
    public ReadLock readLock() {
        return leitura;
    }
}
