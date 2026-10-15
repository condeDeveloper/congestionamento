package br.com.conde.congestionamento;

/**
 * O SORTEIO DA CASA, escrito aqui em vez de usar o da biblioteca.
 *
 * <p>E um gerador congruente linear com as constantes de Knuth. A razao de ser proprio e
 * medida: o teste roda com semente fixa em tres sistemas operacionais e precisa dar o mesmo
 * numero nos tres, e a implementacao da biblioteca nao e contrato entre versoes.
 *
 * <h2>O deslocamento de 32</h2>
 *
 * <p>O estado tem 64 bits e o que sai sao os 32 DE CIMA. Nos bits de baixo, o bit zero
 * alterna a cada passo, os dois ultimos tem periodo quatro, e assim por diante: a fracao
 * montada com eles sobe em degraus regulares em vez de pular.
 *
 * <p>E aqui isso seria quase indetectavel e completamente fatal. A perda sorteada viraria
 * perda PERIODICA, com o mesmo intervalo entre perdas sempre, que e exatamente a hipotese
 * da formula de Mathis. A tabela diria que a formula acerta a vazao com perda aleatoria, e
 * o que ela estaria medindo seria a formula contra ela mesma. Ha um teste so sobre isso.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente;
    }

    /** Um inteiro qualquer, dos 32 bits de cima do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;

        return (int) (estado >>> 32);
    }

    /** Um numero entre 0 inclusive e 1 exclusive. */
    public double fracao() {
        return (proximo() & 0x7fffffff) / (double) 0x80000000L;
    }

    /** Verdadeiro com esta probabilidade. */
    public boolean com(double probabilidade) {
        return fracao() < probabilidade;
    }

    /** Um valor de 0 ate quantos menos um. */
    public int ate(int quantos) {
        return Math.floorMod(proximo(), quantos);
    }

    /** Uma divisao qualquer da capacidade entre tantos fluxos, nenhum zerado. */
    public double[] divisaoQualquer(int fluxos, double capacidade) {
        double[] taxas = new double[fluxos];

        for (int i = 0; i < fluxos; i++) taxas[i] = Lei.MINIMO + fracao() * capacidade;

        return taxas;
    }
}
