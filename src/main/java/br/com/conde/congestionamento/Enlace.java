package br.com.conde.congestionamento;

import java.util.ArrayList;
import java.util.List;

/**
 * O ENLACE E AS RODADAS: um bit de realimentacao, igual para todos.
 *
 * <p>A cada rodada o enlace soma as taxas. Se a soma passa da capacidade, todos recebem
 * "nao cabe"; se nao passa, todos recebem "cabe". E so isso: nenhum fluxo sabe quantos
 * outros existem, nenhum sabe a capacidade, e nenhum fala com os outros.
 *
 * <p>Essa pobreza de informacao e o ponto. Se cada fluxo soubesse a capacidade e o numero de
 * fluxos, dividir por n seria trivial. A pergunta e se a divisao justa aparece do nada, com
 * um bit por rodada.
 */
public final class Enlace {

    private final double capacidade;

    public Enlace(double capacidade) {
        this.capacidade = capacidade;
    }

    public double capacidade() {
        return capacidade;
    }

    public boolean cabe(double[] taxas) {
        return Equidade.soma(taxas) <= capacidade;
    }

    /** O que aconteceu numa execucao. */
    public record Execucao(Lei lei, double capacidade, double[] inicio, double[] fim,
            List<double[]> historia, int rodadas) {

        public double indiceFinal() {
            return Equidade.indice(fim);
        }

        public double indiceInicial() {
            return Equidade.indice(inicio);
        }

        public double eficienciaFinal() {
            return Equidade.soma(fim) / capacidade;
        }

        public double[] em(int rodada) {
            return historia.get(rodada);
        }
    }

    /**
     * Roda tantas rodadas e guarda a historia inteira.
     *
     * <p>A historia e guardada porque as perguntas interessantes nao sao sobre o fim: sao
     * sobre o caminho. Quantas rodadas ate ficar justo, quanto a soma oscila em volta da
     * capacidade, e se a equidade chega a piorar em algum momento.
     */
    public Execucao rodar(Lei lei, double[] inicio, int rodadas) {
        double[] taxas = inicio.clone();

        List<double[]> historia = new ArrayList<>();

        historia.add(taxas.clone());

        for (int rodada = 0; rodada < rodadas; rodada++) {
            boolean cabe = cabe(taxas);

            for (int i = 0; i < taxas.length; i++) taxas[i] = lei.aplicar(taxas[i], cabe);

            historia.add(taxas.clone());
        }

        return new Execucao(lei, capacidade, inicio.clone(), taxas, List.copyOf(historia),
                rodadas);
    }

    /**
     * O mesmo enlace, mas o sinal de congestionamento vai para UM fluxo so, sorteado.
     *
     * <p>A realimentacao sincrona e uma hipotese forte: ela supoe que todo mundo percebe o
     * congestionamento na mesma rodada. Uma fila que descarta por excesso nao faz isso; ela
     * descarta um pacote, e o dono daquele pacote e quem cai.
     *
     * <p>A pergunta que esta execucao responde e se a convergencia para a divisao justa
     * sobrevive a essa troca, ou se ela era um efeito de todo mundo cair junto.
     */
    public Execucao rodarComPerdaEmUm(Lei lei, double[] inicio, int rodadas, Sorteio sorteio) {
        double[] taxas = inicio.clone();

        List<double[]> historia = new ArrayList<>();

        historia.add(taxas.clone());

        for (int rodada = 0; rodada < rodadas; rodada++) {
            if (cabe(taxas))
                for (int i = 0; i < taxas.length; i++) taxas[i] = lei.subir(taxas[i]);
            else {
                int azarado = sorteio.ate(taxas.length);

                taxas[azarado] = lei.cair(taxas[azarado]);
            }

            historia.add(taxas.clone());
        }

        return new Execucao(lei, capacidade, inicio.clone(), taxas, List.copyOf(historia),
                rodadas);
    }

    /**
     * O mesmo enlace com fluxos de tempo de ida e volta diferente.
     *
     * <p>A SUBIDA depende da confirmacao voltar, e e ai que o tempo de ida e volta entra: o
     * fluxo com ida e volta 4 sobe uma vez a cada quatro rodadas, e o com ida e volta 1 sobe
     * em todas. A QUEDA nao: o pacote se perde na fila, e se perde para todo mundo na mesma
     * rodada.
     *
     * <p>Essa assimetria e o modelo certo e nao uma conveniencia. Fazer a queda tambem ser
     * mais rara para o fluxo lento compensa exatamente a subida mais rara, e a medida sai
     * dizendo que o tempo de ida e volta nao importa, que e falso.
     *
     * <p>Nada na lei de controle fala de tempo. A diferenca de taxa que sai daqui nao foi
     * programada por ninguem: ela aparece porque quem reage mais vezes sobe mais vezes.
     */
    public Execucao rodarComIdaEVolta(Lei lei, double[] inicio, int[] idaEVolta, int rodadas) {
        double[] taxas = inicio.clone();

        List<double[]> historia = new ArrayList<>();

        historia.add(taxas.clone());

        for (int rodada = 0; rodada < rodadas; rodada++) {
            boolean cabe = cabe(taxas);

            for (int i = 0; i < taxas.length; i++) {
                if (!cabe) {
                    taxas[i] = lei.cair(taxas[i]);
                    continue;
                }

                if (rodada % idaEVolta[i] == 0) taxas[i] = lei.subir(taxas[i]);
            }

            historia.add(taxas.clone());
        }

        return new Execucao(lei, capacidade, inicio.clone(), taxas, List.copyOf(historia),
                rodadas);
    }

    /** A media de cada taxa nas ultimas rodadas, que e o que se mede num serrote. */
    public static double[] mediaDasUltimas(Execucao execucao, int ultimas) {
        List<double[]> historia = execucao.historia();

        double[] media = new double[execucao.fim().length];

        for (int rodada = historia.size() - ultimas; rodada < historia.size(); rodada++)
            for (int i = 0; i < media.length; i++) media[i] += historia.get(rodada)[i];

        for (int i = 0; i < media.length; i++) media[i] /= ultimas;

        return media;
    }

    /** Em que rodada a equidade passa deste valor e nao volta mais, ou -1 se nunca. */
    public static int rodadaEmQueFicaJusto(Execucao execucao, double alvo) {
        List<double[]> historia = execucao.historia();

        for (int rodada = 0; rodada < historia.size(); rodada++) {
            boolean ficou = true;

            for (int depois = rodada; depois < historia.size(); depois++)
                if (Equidade.indice(historia.get(depois)) < alvo) {
                    ficou = false;
                    break;
                }

            if (ficou) return rodada;
        }

        return -1;
    }
}
