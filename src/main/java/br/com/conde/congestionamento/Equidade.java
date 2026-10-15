package br.com.conde.congestionamento;

/**
 * COMO MEDIR SE A DIVISAO E JUSTA.
 *
 * <p>Usar a diferenca entre o maior e o menor nao serve: ela depende da escala, e dois
 * fluxos com 10 e 20 seriam tao injustos quanto dois com 1010 e 1020.
 *
 * <p>O indice de Jain, de 1984, nao tem esse defeito. Ele vale 1 quando todos recebem o
 * mesmo, vale k/n quando k fluxos dividem tudo igualmente e n-k ficam com zero, nao muda
 * quando tudo e multiplicado pela mesma constante, e nao depende de quantos fluxos ha.
 */
public final class Equidade {

    private Equidade() {}

    /** O indice de Jain: a soma ao quadrado dividida por n vezes a soma dos quadrados. */
    public static double indice(double[] taxas) {
        double soma = 0;
        double somaDosQuadrados = 0;

        for (double taxa : taxas) {
            soma += taxa;
            somaDosQuadrados += taxa * taxa;
        }

        if (somaDosQuadrados == 0) return 1;

        return soma * soma / (taxas.length * somaDosQuadrados);
    }

    /** Quanto da capacidade esta sendo usado, que e a outra metade da pergunta. */
    public static double eficiencia(double[] taxas, double capacidade) {
        double soma = 0;

        for (double taxa : taxas) soma += taxa;

        return soma / capacidade;
    }

    public static double soma(double[] taxas) {
        double soma = 0;

        for (double taxa : taxas) soma += taxa;

        return soma;
    }

    public static double maior(double[] taxas) {
        double maior = Double.NEGATIVE_INFINITY;

        for (double taxa : taxas) maior = Math.max(maior, taxa);

        return maior;
    }

    public static double menor(double[] taxas) {
        double menor = Double.POSITIVE_INFINITY;

        for (double taxa : taxas) menor = Math.min(menor, taxa);

        return menor;
    }
}
