package br.com.conde.congestionamento;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("convergencia") || comando.equals("tudo")) convergencia();
        if (comando.equals("eficiencia") || comando.equals("tudo")) eficiencia();
        if (comando.equals("formula") || comando.equals("tudo")) formula();
        if (comando.equals("ida-e-volta") || comando.equals("tudo")) idaEVolta();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("AS QUATRO LEIS CONTRA O TEOREMA DE CHIU E JAIN");
        System.out.println(Medidas.CASOS + " divisoes iniciais sorteadas por lei, de 2 a 6"
                + " fluxos, " + Medidas.RODADAS + " rodadas cada.");
        System.out.println("A coluna que importa e a do meio: quantas vezes a equidade"
                + " PIOROU no caminho.");
        System.out.println();

        System.out.printf("  %-26s %12s %10s %14s %14s %14s%n",
                "lei", "convergiram", "pioraram", "indice medio", "pior indice",
                "maior variacao");

        for (Medidas.Correcao linha : Medidas.correcao())
            System.out.printf(Locale.ROOT, "  %-26s %12d %10d %14.4f %14.4f %14.6f%n",
                    linha.lei(), linha.convergiram(), linha.pioraram(),
                    linha.indiceMedioFinal(), linha.menorIndiceFinal(),
                    linha.maiorVariacao());

        System.out.println();
        System.out.println("  Multiplicar tudo pelo mesmo numero nao muda o indice de Jain:"
                + " a lei que so multiplica");
        System.out.println("  congela a divisao que encontrou, seja ela qual for.");
        System.out.println();
    }

    private static void convergencia() {
        System.out.println();
        System.out.println("QUANTAS RODADAS ATE DIVIDIR IGUAL");
        System.out.println("Aumento somado com queda multiplicada, " + Medidas.CASOS_POR_LINHA
                + " divisoes iniciais sorteadas por linha.");
        System.out.println();

        System.out.printf("  %-10s %18s %14s %18s %18s%n",
                "fluxos", "rodadas ate justo", "nunca ficou", "indice sincrono",
                "indice com perda em um");

        for (Medidas.Convergencia linha : Medidas.convergencia())
            System.out.printf(Locale.ROOT, "  %-10d %18.1f %14d %18.4f %18.4f%n",
                    linha.fluxos(), linha.rodadasAteJusto(), linha.nuncaFicaramJustos(),
                    linha.indiceSincrono(), linha.indiceComPerdaEmUm());

        System.out.println();
        System.out.println("  A ultima coluna troca a hipotese mais forte do modelo: em vez"
                + " de todos caiam juntos,");
        System.out.println("  um pacote se perde e so o dono dele cai.");
        System.out.println();
    }

    private static void eficiencia() {
        System.out.println();
        System.out.println("O QUE O FATOR DE QUEDA CUSTA");
        System.out.println("Dois fluxos comecando em 90 e 10, enlace de "
                + (int) Medidas.CAPACIDADE + ", 500 rodadas.");
        System.out.println();

        System.out.printf("  %-14s %12s %14s %14s %18s%n",
                "fator", "uso medio", "menor soma", "maior soma", "rodadas ate justo");

        for (Medidas.Eficiencia linha : Medidas.eficiencia())
            System.out.printf(Locale.ROOT, "  %-14.3f %11.1f%% %14.1f %14.1f %18d%n",
                    linha.fatorDeQueda(), 100 * linha.usoMedio(), linha.menorSoma(),
                    linha.maiorSoma(), linha.rodadasAteJusto());

        System.out.println();
    }

    private static void formula() {
        System.out.println();
        System.out.println("O SERROTE CONTRA A FORMULA DA VAZAO");
        System.out.println("vazao = raiz(3/2) / (ida e volta * raiz da perda), de Mathis e"
                + " outros, 1997.");
        System.out.println("Ida e volta igual a 1, entao a vazao sai em pacotes por ida e"
                + " volta.");
        System.out.println();

        System.out.println("  Com perda certa: a janela sobe ate o teto e cai pela metade.");
        System.out.println();

        System.out.printf("  %-14s %14s %12s %12s %10s%n",
                "janela maxima", "perda", "vazao", "formula", "razao");

        for (Medidas.Formula linha : Medidas.comPerdaCerta())
            System.out.printf(Locale.ROOT, "  %-14d %14.6f %12.3f %12.3f %10.4f%n",
                    linha.janelaMaxima(), linha.perdaMedida(), linha.vazao(),
                    linha.formula(), linha.razao());

        System.out.println();
        System.out.println("  Com perda sorteada: cada pacote se perde com a probabilidade"
                + " pedida.");
        System.out.println();

        System.out.printf("  %-14s %14s %12s %12s %10s%n",
                "probabilidade", "perda medida", "vazao", "formula", "razao");

        for (Medidas.Formula linha : Medidas.comPerdaSorteada())
            System.out.printf(Locale.ROOT, "  %-14.4f %14.6f %12.3f %12.3f %10.4f%n",
                    linha.probabilidade(), linha.perdaMedida(), linha.vazao(),
                    linha.formula(), linha.razao());

        System.out.println();
    }

    private static void idaEVolta() {
        System.out.println();
        System.out.println("DOIS FLUXOS COM TEMPOS DE IDA E VOLTA DIFERENTES");
        System.out.println("A mesma lei, o mesmo enlace, a mesma taxa inicial. So a demora"
                + " da confirmacao muda.");
        System.out.println();

        System.out.printf("  %-18s %12s %12s %16s %12s %12s%n",
                "razao das demoras", "rapido", "lento", "razao das taxas", "1/r", "1/r2");

        for (Medidas.IdaEVolta linha : Medidas.idaEVolta())
            System.out.printf(Locale.ROOT, "  %-18d %12.3f %12.3f %16.3f %12.3f %12.3f%n",
                    linha.razao(), linha.taxaDoRapido(), linha.taxaDoLento(),
                    linha.razaoDasTaxas(), 1.0 / linha.razao(),
                    1.0 / (linha.razao() * linha.razao()));

        System.out.println();
        System.out.println("  A razao das taxas acompanha a razao das demoras, nao o"
                + " quadrado dela.");
        System.out.println();
    }
}
