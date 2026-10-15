package br.com.conde.congestionamento;

import java.util.ArrayList;
import java.util.List;

/**
 * AS MEDIDAS: tudo o que o repositorio afirma, calculado aqui e em nenhum outro lugar.
 *
 * <p>O Medidor imprime o que sai daqui e o teste confere o que sai daqui. Os dois leem a
 * MESMA funcao, entao nao existe a situacao de o texto dizer uma coisa e o teste conferir
 * outra: se um numero mudar, a tabela muda e o teste quebra junto.
 */
public final class Medidas {

    /** A capacidade do enlace, em unidades de taxa. */
    public static final double CAPACIDADE = 100;

    /** Quantos inicios sorteados cada lei enfrenta. */
    public static final int CASOS = 500;

    /** Quantas rodadas cada execucao dura. */
    public static final int RODADAS = 300;

    /** A equidade a partir da qual a divisao e considerada justa. */
    public static final double JUSTO = 0.99;

    public static final long SEMENTE = 1;

    private Medidas() {}

    private static final Enlace ENLACE = new Enlace(CAPACIDADE);

    /* ===================== 1. correcao ===================== */

    public record Correcao(String lei, int casos, int convergiram, int pioraram,
            double indiceMedioFinal, double menorIndiceFinal, double maiorVariacao) {}

    private static List<Correcao> correcao;

    /**
     * AS QUATRO LEIS CONTRA O TEOREMA DE CHIU E JAIN.
     *
     * <p>O artigo de 1989 diz que so o aumento somado com a queda multiplicada converge para
     * a divisao justa. A medida nao cita o teorema: ela sorteia quinhentas divisoes iniciais
     * para cada lei e olha o que acontece.
     *
     * <p>A coluna que importa nao e a do indice final: e a de quantas vezes o indice PIOROU
     * no caminho. Uma lei pode terminar justa por ter comecado justa, e o teorema fala sobre
     * a direcao do movimento, nao sobre o ponto de chegada.
     */
    public static List<Correcao> correcao() {
        if (correcao != null) return correcao;

        List<Correcao> saida = new ArrayList<>();

        for (Lei lei : Lei.todas()) {
            Sorteio sorteio = new Sorteio(SEMENTE);

            int convergiram = 0;
            int pioraram = 0;

            double soma = 0;
            double menor = 1;
            double maiorVariacao = 0;

            for (int caso = 0; caso < CASOS; caso++) {
                double[] inicio = sorteio.divisaoQualquer(2 + sorteio.ate(5), CAPACIDADE);

                Enlace.Execucao execucao = ENLACE.rodar(lei, inicio, RODADAS);

                double antes = Equidade.indice(execucao.em(0));

                boolean piorou = false;

                for (int rodada = 1; rodada < execucao.historia().size(); rodada++) {
                    double agora = Equidade.indice(execucao.em(rodada));

                    maiorVariacao = Math.max(maiorVariacao, Math.abs(agora - antes));

                    if (agora < antes - 1e-9) piorou = true;

                    antes = agora;
                }

                if (piorou) pioraram++;
                if (execucao.indiceFinal() > JUSTO) convergiram++;

                soma += execucao.indiceFinal();
                menor = Math.min(menor, execucao.indiceFinal());
            }

            saida.add(new Correcao(lei.nome(), CASOS, convergiram, pioraram, soma / CASOS,
                    menor, maiorVariacao));
        }

        return correcao = List.copyOf(saida);
    }

    /* ===================== 2. convergencia ===================== */

    public record Convergencia(int fluxos, double rodadasAteJusto, int nuncaFicaramJustos,
            double indiceSincrono, double indiceComPerdaEmUm) {}

    /** Quantos casos entram em cada linha da tabela da convergencia. */
    public static final int CASOS_POR_LINHA = 100;

    /** Quantas rodadas do fim entram na media do indice. */
    public static final int ULTIMAS = 500;

    private static List<Convergencia> convergencia;

    /**
     * QUANTAS RODADAS ATE DIVIDIR IGUAL, E O QUE A PERDA EM UM FLUXO SO MUDA.
     *
     * <p>A coluna da perda em um fluxo so troca a hipotese mais forte do modelo. Na
     * realimentacao sincrona todo mundo recebe o sinal na mesma rodada; numa fila que
     * descarta por excesso, um pacote se perde e so o dono dele cai.
     */
    public static List<Convergencia> convergencia() {
        if (convergencia != null) return convergencia;

        List<Convergencia> saida = new ArrayList<>();

        for (int fluxos : new int[] {2, 3, 4, 6, 8}) {
            Sorteio sorteio = new Sorteio(SEMENTE);

            long soma = 0;
            int nunca = 0;

            double sincrono = 0;
            double emUm = 0;

            for (int caso = 0; caso < CASOS_POR_LINHA; caso++) {
                double[] inicio = sorteio.divisaoQualquer(fluxos, CAPACIDADE);

                Enlace.Execucao execucao = ENLACE.rodar(Lei.aimd(), inicio, 2000);

                int quando = Enlace.rodadaEmQueFicaJusto(execucao, JUSTO);

                if (quando < 0) nunca++;
                else soma += quando;

                sincrono += mediaDoIndice(execucao, ULTIMAS);

                emUm += mediaDoIndice(
                        ENLACE.rodarComPerdaEmUm(Lei.aimd(), inicio, 2000, sorteio), ULTIMAS);
            }

            saida.add(new Convergencia(fluxos,
                    soma / (double) (CASOS_POR_LINHA - nunca), nunca,
                    sincrono / CASOS_POR_LINHA, emUm / CASOS_POR_LINHA));
        }

        return convergencia = List.copyOf(saida);
    }

    /** A media do indice de equidade nas ultimas rodadas de uma execucao. */
    public static double mediaDoIndice(Enlace.Execucao execucao, int ultimas) {
        double soma = 0;

        for (int rodada = execucao.historia().size() - ultimas;
                rodada < execucao.historia().size(); rodada++)
            soma += Equidade.indice(execucao.em(rodada));

        return soma / ultimas;
    }

    /* ===================== 3. eficiencia ===================== */

    public record Eficiencia(double fatorDeQueda, double usoMedio, double menorSoma,
            double maiorSoma, int rodadasAteJusto) {

        public double oscilacao() {
            return maiorSoma - menorSoma;
        }
    }

    /** Os fatores de queda medidos. */
    public static final double[] FATORES = {0.3, 0.5, 0.7, 0.875, 0.95};

    private static List<Eficiencia> eficiencia;

    /**
     * O QUE O FATOR DE QUEDA CUSTA.
     *
     * <p>Cair pela metade devolve o enlace depressa e deixa o cano meio vazio. Cair pouco
     * mantem o cano cheio e demora muito mais para dividir igual. Nao ha escolha certa: ha
     * uma troca, e esta tabela e o tamanho dela.
     */
    public static List<Eficiencia> eficiencia() {
        if (eficiencia != null) return eficiencia;

        List<Eficiencia> saida = new ArrayList<>();

        for (double fator : FATORES) {
            Enlace.Execucao execucao =
                    ENLACE.rodar(Lei.comQueda(fator), new double[] {90, 10}, 500);

            double soma = 0;
            double menor = Double.MAX_VALUE;
            double maior = 0;

            for (int rodada = execucao.historia().size() - ULTIMAS / 5;
                    rodada < execucao.historia().size(); rodada++) {

                double total = Equidade.soma(execucao.em(rodada));

                soma += total;
                menor = Math.min(menor, total);
                maior = Math.max(maior, total);
            }

            saida.add(new Eficiencia(fator, soma / (ULTIMAS / 5) / CAPACIDADE, menor, maior,
                    Enlace.rodadaEmQueFicaJusto(execucao, JUSTO)));
        }

        return eficiencia = List.copyOf(saida);
    }

    /* ===================== 4. a formula ===================== */

    public record Formula(int janelaMaxima, double probabilidade, double perdaMedida,
            double vazao, double formula) {

        public double razao() {
            return vazao / formula;
        }
    }

    /** As janelas maximas medidas no serrote de perda certa. */
    public static final int[] JANELAS = {4, 8, 16, 32, 64, 128, 256};

    /** As probabilidades medidas no serrote de perda sorteada. */
    public static final double[] PROBABILIDADES = {0.1, 0.05, 0.01, 0.005, 0.001};

    /** Quantas idas e voltas o serrote sorteado roda. */
    public static final int IDAS_E_VOLTAS = 200_000;

    private static List<Formula> comPerdaCerta;
    private static List<Formula> comPerdaSorteada;

    public static List<Formula> comPerdaCerta() {
        if (comPerdaCerta != null) return comPerdaCerta;

        List<Formula> saida = new ArrayList<>();

        for (int janela : JANELAS) {
            Serrote.Resultado resultado = Serrote.comPerdaCerta(janela, 1.0, 2000);

            saida.add(new Formula(janela, 0, resultado.taxaDePerda(), resultado.vazao(),
                    Serrote.formula(1.0, resultado.taxaDePerda())));
        }

        return comPerdaCerta = List.copyOf(saida);
    }

    public static List<Formula> comPerdaSorteada() {
        if (comPerdaSorteada != null) return comPerdaSorteada;

        List<Formula> saida = new ArrayList<>();

        for (double probabilidade : PROBABILIDADES) {
            Serrote.Resultado resultado = Serrote.comPerdaSorteada(probabilidade, 1.0,
                    IDAS_E_VOLTAS, new Sorteio(SEMENTE));

            saida.add(new Formula(0, probabilidade, resultado.taxaDePerda(),
                    resultado.vazao(), Serrote.formula(1.0, resultado.taxaDePerda())));
        }

        return comPerdaSorteada = List.copyOf(saida);
    }

    /* ===================== 5. ida e volta ===================== */

    public record IdaEVolta(int razao, double taxaDoRapido, double taxaDoLento) {

        public double razaoDasTaxas() {
            return taxaDoRapido / taxaDoLento;
        }
    }

    /** As razoes de tempo de ida e volta medidas. */
    public static final int[] RAZOES = {1, 2, 3, 4, 8};

    private static List<IdaEVolta> idaEVolta;

    /**
     * DOIS FLUXOS IGUAIS, COM TEMPOS DE IDA E VOLTA DIFERENTES.
     *
     * <p>A mesma lei, o mesmo enlace, a mesma taxa inicial. A unica diferenca e quanto tempo
     * a confirmacao demora, e nada na lei de controle fala disso.
     */
    public static List<IdaEVolta> idaEVolta() {
        if (idaEVolta != null) return idaEVolta;

        List<IdaEVolta> saida = new ArrayList<>();

        for (int razao : RAZOES) {
            Enlace.Execucao execucao = ENLACE.rodarComIdaEVolta(Lei.aimd(),
                    new double[] {CAPACIDADE / 2, CAPACIDADE / 2}, new int[] {1, razao}, 4000);

            double[] media = Enlace.mediaDasUltimas(execucao, 1000);

            saida.add(new IdaEVolta(razao, media[0], media[1]));
        }

        return idaEVolta = List.copyOf(saida);
    }
}
