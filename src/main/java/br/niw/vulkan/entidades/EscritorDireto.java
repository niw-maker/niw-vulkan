package br.niw.vulkan.entidades;

/** Acesso direto à memória de um BufferBuilder, só quando o formato é o de entidade e nada fora do comum está ligado. */
public interface EscritorDireto {
    /** Garante espaço e devolve o endereço do próximo vértice, ou 0 se não dá para escrever direto. */
    long niw$reserva(int vertices);

    void niw$avanca(int vertices);
}
