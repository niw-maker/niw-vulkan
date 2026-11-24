package br.niw.vulkan.mundo;

/**
 * Escolha do canto de bioma com o mesmo resultado do original. O sorteio desloca cada eixo em no máximo 0,45, então
 * dá para descartar sem conta os cantos que não teriam como ganhar do mais próximo.
 */
public final class Biomas {
    private static final long A = 6364136223846793005L, C = 1442695040888963407L;
    /** [fração x,y,z em quartos][canto] -> menor distância possível. */
    private static final double[] MINIMA = new double[64 * 8];

    static {
        for (int f = 0; f < 64; f++)
            for (int c = 0; c < 8; c++) {
                double x = (f >> 4) / 4.0 - (c >> 2), y = (f >> 2 & 3) / 4.0 - (c >> 1 & 1), z = (f & 3) / 4.0 - (c & 1);
                MINIMA[f * 8 + c] = folga(x) + folga(y) + folga(z) - 1e-9;
            }
    }

    private Biomas() { }

    private static double folga(double d) {
        double m = Math.max(0, Math.abs(d) - 0.45);
        return m * m;
    }

    private static long proximo(long s, long v) {
        return s * (s * A + C) + v;
    }

    /** Canto escolhido (0 a 7, bits x=4, y=2, z=1) para o bloco dado. */
    public static int canto(long semente, int bx, int by, int bz) {
        int i = bx - 2, j = by - 2, k = bz - 2;
        int l = i >> 2, m = j >> 2, n = k >> 2;
        int f = (i & 3) << 4 | (j & 3) << 2 | (k & 3);
        double fx = (i & 3) / 4.0, fy = (j & 3) / 4.0, fz = (k & 3) / 4.0;
        int melhor = (i & 2) << 1 | (j & 2) | (k & 2) >> 1;
        double d = distancia(semente, melhor, l, m, n, fx, fy, fz);
        for (int c = 0; c < 8; c++) {
            if (c == melhor || MINIMA[f * 8 + c] > d) continue;
            double e = distancia(semente, c, l, m, n, fx, fy, fz);
            if (e < d || e == d && c < melhor) {
                d = e;
                melhor = c;
            }
        }
        return melhor;
    }

    private static double distancia(long s, int c, int l, int m, int n, double fx, double fy, double fz) {
        int x = l + (c >> 2), y = m + (c >> 1 & 1), z = n + (c & 1);
        long v = proximo(proximo(proximo(proximo(proximo(proximo(s, x), y), z), x), y), z);
        double dx = fx - (c >> 2) + desvio(v);
        v = proximo(v, s);
        double dy = fy - (c >> 1 & 1) + desvio(v);
        v = proximo(v, s);
        double dz = fz - (c & 1) + desvio(v);
        return dz * dz + dy * dy + dx * dx;
    }

    private static double desvio(long v) {
        return ((v >> 24 & 1023) / 1024.0 - 0.5) * 0.9;
    }
}
