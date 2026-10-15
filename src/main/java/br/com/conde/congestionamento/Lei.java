package br.com.conde.congestionamento;

import java.util.List;

/**
 * UMA LEI DE CONTROLE LINEAR, no formato de Chiu e Jain.
 *
 * <p>Cada fluxo recebe um bit por rodada: cabe ou nao cabe. Com um bit so, e sem saber
 * quantos outros fluxos existem nem qual e a capacidade, ele atualiza a propria taxa por
 *
 * <pre>x <- a + b * x</pre>
 *
 * <p>com um par (a, b) para quando cabe e outro para quando nao cabe. Aumento SOMADO e b=1
 * com a positivo; aumento MULTIPLICADO e a=0 com b maior que 1. Queda SOMADA e b=1 com a
 * negativo; queda MULTIPLICADA e a=0 com b menor que 1.
 *
 * <p>Sao quatro combinacoes, e o artigo de 1989 mostra que apenas UMA delas converge para a
 * divisao justa. Este repositorio nao cita isso: ele mede.
 *
 * <p>O sinal e o mesmo para todos no mesmo instante, que e a hipotese de realimentacao
 * sincrona. Ela e forte e esta aqui de proposito: e exatamente sob ela que o teorema vale, e
 * medir sob outra hipotese nao testaria o teorema.
 */
public record Lei(String nome, double somaAoSubir, double produtoAoSubir, double somaAoCair,
        double produtoAoCair) {

    /** A taxa nunca cai abaixo disto: um fluxo parado nao volta a crescer com multiplicacao. */
    public static final double MINIMO = 0.01;

    public double subir(double taxa) {
        return Math.max(MINIMO, somaAoSubir + produtoAoSubir * taxa);
    }

    public double cair(double taxa) {
        return Math.max(MINIMO, somaAoCair + produtoAoCair * taxa);
    }

    public double aplicar(double taxa, boolean cabe) {
        return cabe ? subir(taxa) : cair(taxa);
    }

    /** Aumento somado, queda multiplicada: a do TCP. */
    public static Lei aimd() {
        return new Lei("soma e multiplica", 1.0, 1.0, 0.0, 0.5);
    }

    /** Aumento somado, queda somada. */
    public static Lei aiad() {
        return new Lei("soma e soma", 1.0, 1.0, -1.0, 1.0);
    }

    /** Aumento multiplicado, queda multiplicada. */
    public static Lei mimd() {
        return new Lei("multiplica e multiplica", 0.0, 1.2, 0.0, 0.5);
    }

    /** Aumento multiplicado, queda somada. */
    public static Lei miad() {
        return new Lei("multiplica e soma", 0.0, 1.2, -1.0, 1.0);
    }

    /** Aumento somado com fator de queda escolhido, para a tabela da eficiencia. */
    public static Lei comQueda(double fator) {
        return new Lei("queda " + fator, 1.0, 1.0, 0.0, fator);
    }

    /** As quatro, na ordem das tabelas. */
    public static List<Lei> todas() {
        return List.of(aimd(), aiad(), mimd(), miad());
    }
}
