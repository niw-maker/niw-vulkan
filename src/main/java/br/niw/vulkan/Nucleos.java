package br.niw.vulkan;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Divide contas curtas do quadro entre núcleos, com a própria thread de desenho ajudando. */
public final class Nucleos {
    private static final int EXTRAS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 3));
    private static final ExecutorService POOL = Executors.newFixedThreadPool(EXTRAS, r -> {
        Thread t = new Thread(r, "NiwVulkan nucleo");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY + 2);
        return t;
    });

    private Nucleos() { }

    /** Roda todas e só volta quando a última acabar. */
    public static void todas(List<Runnable> tarefas) {
        int n = tarefas.size();
        if (n <= 2) {
            for (Runnable r : tarefas) r.run();
            return;
        }
        AtomicInteger prox = new AtomicInteger();
        CountDownLatch fim = new CountDownLatch(n);
        Throwable[] erro = new Throwable[1];
        Runnable puxa = () -> {
            int i;
            while ((i = prox.getAndIncrement()) < n) {
                try {
                    tarefas.get(i).run();
                } catch (Throwable t) {
                    erro[0] = t;
                } finally {
                    fim.countDown();
                }
            }
        };
        for (int k = 0; k < Math.min(EXTRAS, n - 1); k++) POOL.execute(puxa);
        puxa.run();
        boolean interrompida = false;
        while (true) {
            try {
                fim.await();
                break;
            } catch (InterruptedException e) {
                interrompida = true;
            }
        }
        if (interrompida) Thread.currentThread().interrupt();
        if (erro[0] != null) throw new RuntimeException(erro[0]);
    }
}
