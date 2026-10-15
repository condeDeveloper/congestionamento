package br.com.conde.congestionamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class CongestionamentoTest {

    // ───────────────────── o sorteio ─────────────────────

    @Nested
    @DisplayName("o sorteio da casa")
    class OSorteio {

        private long estado;

        private int proximoSemDeslocamento() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            return (int) estado;
        }

        private int ateSemDeslocamento(int quantos) {
            return Math.floorMod(proximoSemDeslocamento(), quantos);
        }

        @Test
        @DisplayName("o sorteio da os mesmos numeros em qualquer maquina")
        void oSorteioEhFixo() {
            Sorteio sorteio = new Sorteio(1);

            assertThat(List.of(sorteio.proximo(), sorteio.proximo(), sorteio.proximo()))
                    .containsExactly(1817669548, -2107078989, -1510284903);
        }

        /**
         * O SORTEIO ESCOLHE QUEM PERDE O PACOTE, E O NUMERO DE FLUXOS E UMA POTENCIA DE DOIS.
         *
         * <p>Com os bits de baixo, {@code ate(2)} e o bit zero, que alterna a cada passo.
         * O descarte deixa de ser sorteado e vira um rodizio perfeito.
         */
        @Test
        @DisplayName("sem o deslocamento, o descarte vira rodizio perfeito")
        void semODeslocamentoODescarteViraRodizio() {
            estado = Medidas.SEMENTE;

            StringBuilder comDois = new StringBuilder();

            for (int i = 0; i < 16; i++) comDois.append(ateSemDeslocamento(2));

            assertThat(comDois.toString()).isEqualTo("0101010101010101");

            estado = Medidas.SEMENTE;

            StringBuilder comQuatro = new StringBuilder();

            for (int i = 0; i < 16; i++) comQuatro.append(ateSemDeslocamento(4));

            assertThat(comQuatro.toString()).isEqualTo("0321032103210321");

            Sorteio sorteio = new Sorteio(Medidas.SEMENTE);

            StringBuilder bom = new StringBuilder();

            for (int i = 0; i < 16; i++) bom.append(sorteio.ate(2));

            assertThat(bom.toString()).isNotEqualTo("0101010101010101");
        }

        /**
         * E O QUE ISSO FARIA COM A MEDIDA.
         *
         * <p>Um rodizio perfeito reparte a queda igualmente entre os fluxos, e a divisao sai
         * quase tao justa quanto na realimentacao sincrona. A tabela da convergencia diria
         * que descartar um pacote de quem quer que seja nao atrapalha a equidade, que e o
         * contrario do que acontece.
         */
        @Test
        @DisplayName("e o rodizio faria a perda em um fluxo parecer justa")
        void oRodizioFariaAPerdaParecerJusta() {
            double comRodizio = indiceFinalComDescarte(true);
            double comSorteio = indiceFinalComDescarte(false);

            assertThat(comRodizio)
                    .as("o rodizio perfeito chega perto da realimentacao sincrona")
                    .isGreaterThan(0.95);

            assertThat(comSorteio)
                    .as("e o sorteio de verdade fica bem abaixo disso")
                    .isLessThan(0.93);
        }

        private double indiceFinalComDescarte(boolean semDeslocamento) {
            estado = Medidas.SEMENTE;

            Sorteio sorteio = new Sorteio(Medidas.SEMENTE);

            double[] taxas = {90, 10};

            double soma = 0;
            int contadas = 0;

            for (int rodada = 0; rodada < 2000; rodada++) {
                if (Equidade.soma(taxas) <= Medidas.CAPACIDADE)
                    for (int i = 0; i < taxas.length; i++) taxas[i] = Lei.aimd().subir(taxas[i]);
                else {
                    int azarado = semDeslocamento ? ateSemDeslocamento(2) : sorteio.ate(2);

                    taxas[azarado] = Lei.aimd().cair(taxas[azarado]);
                }

                if (rodada < 1500) continue;

                soma += Equidade.indice(taxas);
                contadas++;
            }

            return soma / contadas;
        }
    }

    // ───────────────────── a equidade ─────────────────────

    @Nested
    @DisplayName("o indice de Jain")
    class OIndice {

        @Test
        @DisplayName("vale 1 quando todos recebem o mesmo, em qualquer escala")
        void valeUmQuandoTodosRecebemOMesmo() {
            assertThat(Equidade.indice(new double[] {5, 5, 5})).isCloseTo(1, within(1e-12));
            assertThat(Equidade.indice(new double[] {500, 500, 500}))
                    .isCloseTo(1, within(1e-12));
        }

        @Test
        @DisplayName("vale k sobre n quando k fluxos dividem tudo e o resto fica a zero")
        void valeKSobreN() {
            assertThat(Equidade.indice(new double[] {10, 10, 0, 0}))
                    .isCloseTo(0.5, within(1e-12));

            assertThat(Equidade.indice(new double[] {10, 0, 0, 0}))
                    .isCloseTo(0.25, within(1e-12));
        }

        @Test
        @DisplayName("multiplicar todo mundo pela mesma constante nao muda nada")
        void multiplicarNaoMuda() {
            double[] taxas = {3, 7, 11, 19};

            double antes = Equidade.indice(taxas);

            double[] dobrado = new double[taxas.length];

            for (int i = 0; i < taxas.length; i++) dobrado[i] = taxas[i] * 2;

            assertThat(Equidade.indice(dobrado)).isCloseTo(antes, within(1e-12));
        }

        @Test
        @DisplayName("somar a mesma constante a todo mundo aproxima da justica")
        void somarAproxima() {
            double[] taxas = {3, 7, 11, 19};

            double[] somado = new double[taxas.length];

            for (int i = 0; i < taxas.length; i++) somado[i] = taxas[i] + 10;

            assertThat(Equidade.indice(somado)).isGreaterThan(Equidade.indice(taxas));
        }
    }

    // ───────────────────── as leis ─────────────────────

    @Nested
    @DisplayName("as leis de controle")
    class AsLeis {

        @Test
        @DisplayName("cada lei e um par de retas, e nada mais")
        void cadaLeiEhUmParDeRetas() {
            assertThat(Lei.aimd().subir(10)).isCloseTo(11, within(1e-12));
            assertThat(Lei.aimd().cair(10)).isCloseTo(5, within(1e-12));

            assertThat(Lei.mimd().subir(10)).isCloseTo(12, within(1e-12));
            assertThat(Lei.aiad().cair(10)).isCloseTo(9, within(1e-12));
        }

        @Test
        @DisplayName("nenhuma taxa cai abaixo do minimo, senao a multiplicacao mata o fluxo")
        void aTaxaTemPiso() {
            double taxa = 10;

            for (int i = 0; i < 100; i++) taxa = Lei.aimd().cair(taxa);

            assertThat(taxa).isEqualTo(Lei.MINIMO);
            assertThat(Lei.aimd().subir(taxa)).isGreaterThan(taxa);
        }

        /**
         * A LEI QUE SO MULTIPLICA NAO PODE MUDAR A EQUIDADE, E ISSO E ARITMETICA.
         *
         * <p>O indice de Jain nao muda quando todas as taxas sao multiplicadas pela mesma
         * constante, e e exatamente isso que o aumento multiplicado e a queda multiplicada
         * fazem. A divisao inicial fica congelada para sempre.
         */
        @Test
        @DisplayName("multiplica e multiplica congela a divisao inicial, exatamente")
        void multiplicaCongelaADivisao() {
            Enlace enlace = new Enlace(Medidas.CAPACIDADE);

            Enlace.Execucao execucao =
                    enlace.rodar(Lei.mimd(), new double[] {80, 15, 5}, 300);

            double inicial = execucao.indiceInicial();

            for (int rodada = 0; rodada < execucao.historia().size(); rodada++)
                assertThat(Equidade.indice(execucao.em(rodada)))
                        .as("rodada " + rodada)
                        .isCloseTo(inicial, within(1e-9));
        }
    }

    // ───────────────────── o enlace ─────────────────────

    @Nested
    @DisplayName("o enlace")
    class OEnlace {

        private final Enlace enlace = new Enlace(Medidas.CAPACIDADE);

        @Test
        @DisplayName("o sinal e um bit so, e e o mesmo para todos")
        void oSinalEhUmBit() {
            assertThat(enlace.cabe(new double[] {50, 49})).isTrue();
            assertThat(enlace.cabe(new double[] {50, 50})).isTrue();
            assertThat(enlace.cabe(new double[] {50, 51})).isFalse();
        }

        @Test
        @DisplayName("a historia guarda o inicio e uma linha por rodada")
        void aHistoriaGuardaTudo() {
            Enlace.Execucao execucao = enlace.rodar(Lei.aimd(), new double[] {10, 10}, 50);

            assertThat(execucao.historia()).hasSize(51);
            assertThat(execucao.em(0)).containsExactly(10, 10);
        }

        @Test
        @DisplayName("com aumento somado e queda pela metade, a soma fica entre metade e um")
        void aSomaFicaNaFaixa() {
            Enlace.Execucao execucao = enlace.rodar(Lei.aimd(), new double[] {10, 10}, 300);

            for (int rodada = 100; rodada < execucao.historia().size(); rodada++) {
                double soma = Equidade.soma(execucao.em(rodada));

                assertThat(soma)
                        .as("rodada " + rodada)
                        .isBetween(Medidas.CAPACIDADE / 2 - 1, Medidas.CAPACIDADE + 2);
            }
        }

        @Test
        @DisplayName("quem comeca justo continua justo")
        void quemComecaJustoContinua() {
            Enlace.Execucao execucao = enlace.rodar(Lei.aimd(), new double[] {20, 20, 20}, 300);

            assertThat(execucao.indiceFinal()).isCloseTo(1, within(1e-12));
        }
    }

    // ───────────────────── o serrote ─────────────────────

    @Nested
    @DisplayName("o serrote")
    class OSerrote {

        @Test
        @DisplayName("a janela media e tres quartos do teto, que e o triangulo do serrote")
        void aJanelaMediaEhTresQuartos() {
            for (int teto : new int[] {16, 32, 64, 128}) {
                Serrote.Resultado resultado = Serrote.comPerdaCerta(teto, 1.0, 2000);

                assertThat(resultado.janelaMedia())
                        .as("teto " + teto)
                        .isCloseTo(0.75 * teto, within(0.01));
            }
        }

        /**
         * E UM A MAIS, QUE NAO E DETALHE.
         *
         * <p>O ciclo vai da metade do teto ATE o teto, inclusive os dois, entao ele dura
         * teto/2 + 1 idas e voltas, e nao teto/2. Esse um a mais e parte do motivo de a
         * formula, que e assintotica, errar tanto com janela pequena: com teto 4, ele e um
         * terco do ciclo inteiro.
         */
        @Test
        @DisplayName("um ciclo tem uma perda so, e dura metade do teto mais uma ida e volta")
        void umCicloTemUmaPerda() {
            Serrote.Resultado resultado = Serrote.comPerdaCerta(64, 1.0, 10);

            assertThat(resultado.perdidos()).isEqualTo(10);
            assertThat(resultado.tempo()).isEqualTo(10 * (32 + 1));
        }

        @Test
        @DisplayName("a formula e a vazao concordam quando a janela e grande")
        void aFormulaBateComJanelaGrande() {
            Serrote.Resultado resultado = Serrote.comPerdaCerta(512, 1.0, 2000);

            assertThat(resultado.vazao() / Serrote.formula(1.0, resultado.taxaDePerda()))
                    .isCloseTo(1, within(0.005));
        }

        @Test
        @DisplayName("dobrar a ida e volta corta a vazao pela metade, com a mesma perda")
        void dobrarAIdaEVoltaCortaAVazao() {
            Serrote.Resultado rapido = Serrote.comPerdaCerta(64, 1.0, 500);
            Serrote.Resultado lento = Serrote.comPerdaCerta(64, 2.0, 500);

            assertThat(rapido.taxaDePerda()).isCloseTo(lento.taxaDePerda(), within(1e-12));
            assertThat(lento.vazao()).isCloseTo(rapido.vazao() / 2, within(1e-9));
        }
    }
}
