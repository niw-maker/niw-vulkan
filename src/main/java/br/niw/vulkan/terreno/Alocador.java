package br.niw.vulkan.terreno;

import java.util.Arrays;

/** Divide um buffer em trechos: primeiro que couber, juntando vizinhos livres na devolução. */
final class Alocador {
    private int[] ini = new int[16], tam = new int[16];
    private int n;
    int capacidade;

    Alocador(int capacidade) {
        this.capacidade = capacidade;
        ini[0] = 0;
        tam[0] = capacidade;
        n = 1;
    }

    /** Devolve o início do trecho, ou -1 se não couber. */
    int pega(int quanto) {
        for (int i = 0; i < n; i++) {
            if (tam[i] < quanto) continue;
            int r = ini[i];
            ini[i] += quanto;
            tam[i] -= quanto;
            if (tam[i] == 0) tira(i);
            return r;
        }
        return -1;
    }

    void devolve(int onde, int quanto) {
        if (quanto == 0) return;
        int i = Arrays.binarySearch(ini, 0, n, onde);
        i = i >= 0 ? i : -i - 1;
        boolean antes = i > 0 && ini[i - 1] + tam[i - 1] == onde;
        boolean depois = i < n && onde + quanto == ini[i];
        if (antes && depois) {
            tam[i - 1] += quanto + tam[i];
            tira(i);
        } else if (antes) {
            tam[i - 1] += quanto;
        } else if (depois) {
            ini[i] = onde;
            tam[i] += quanto;
        } else {
            if (n == ini.length) {
                ini = Arrays.copyOf(ini, n * 2);
                tam = Arrays.copyOf(tam, n * 2);
            }
            System.arraycopy(ini, i, ini, i + 1, n - i);
            System.arraycopy(tam, i, tam, i + 1, n - i);
            ini[i] = onde;
            tam[i] = quanto;
            n++;
        }
    }

    void cresce(int nova) {
        int velha = capacidade;
        capacidade = nova;
        devolve(velha, nova - velha);
    }

    boolean vazio() {
        return n == 1 && tam[0] == capacidade;
    }

    private void tira(int i) {
        System.arraycopy(ini, i + 1, ini, i, n - i - 1);
        System.arraycopy(tam, i + 1, tam, i, n - i - 1);
        n--;
    }
}
