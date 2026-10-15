package br.com.conde.congestionamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TODO NUMERO DO README ESTA AQUI.
 *
 * <p>Texto nao quebra quando deixa de ser verdade. Teste quebra. Se alguma medida mudar,
 * este arquivo e a tabela do README caem juntos, e nenhum numero fica no texto sozinho.
 */
class MedidasTest {

    private static final List<Medidas.Correcao> CORRECAO = Medidas.correcao();
    private static final List<Medidas.Convergencia> CONVERGENCIA = Medidas.convergencia();
    private static final List<Medidas.Eficiencia> EFICIENCIA = Medidas.eficiencia();
    private static final List<Medidas.Formula> PERDA_CERTA = Medidas.comPerdaCerta();
    private static final List<Medidas.Formula> PERDA_SORTEADA = Medidas.comPerdaSorteada();
    private static final List<Medidas.IdaEVolta> IDA_E_VOLTA = Medidas.idaEVolta();

    private static Medidas.Correcao correcao(String lei) {
        return CORRECAO.stream().filter(linha -> linha.lei().equals(lei))
                .findFirst().orElseThrow();
    }

    private static Medidas.Convergencia convergencia(int fluxos) {
        return CONVERGENCIA.stream().filter(linha -> linha.fluxos() == fluxos)
                .findFirst().orElseThrow();
    }

    private static Medidas.Eficiencia eficiencia(double fator) {
        return EFICIENCIA.stream().filter(linha -> Math.abs(linha.fatorDeQueda() - fator) < 1e-9)
                .findFirst().orElseThrow();
    }

    private static Medidas.Formula perdaCerta(int janela) {
        return PERDA_CERTA.stream().filter(linha -> linha.janelaMaxima() == janela)
                .findFirst().orElseThrow();
    }

    private static Medidas.Formula perdaSorteada(double probabilidade) {
        return PERDA_SORTEADA.stream()
                .filter(linha -> Math.abs(linha.probabilidade() - probabilidade) < 1e-9)
                .findFirst().orElseThrow();
    }

    private static Medidas.IdaEVolta idaEVolta(int razao) {
        return IDA_E_VOLTA.stream().filter(linha -> linha.razao() == razao)
                .findFirst().orElseThrow();
    }

    // ───────────────────── 1. correcao ─────────────────────

    @Nested
    @DisplayName("tabela 1: as quatro leis contra o teorema de Chiu e Jain")
    class ACorrecao {

        @Test
        @DisplayName("sao as quatro leis, com 500 divisoes iniciais cada")
        void saoAsQuatro() {
            assertThat(CORRECAO).hasSize(4);

            for (Medidas.Correcao linha : CORRECAO)
                assertThat(linha.casos()).isEqualTo(500);
        }

        /**
         * A UNICA QUE CONVERGE.
         *
         * <p>Quinhentas divisoes iniciais sorteadas, de dois a seis fluxos: o aumento somado
         * com a queda multiplicada chega a equidade 1 em todas, e a equidade nunca piora no
         * caminho. Nenhuma das outras tres faz as duas coisas.
         */
        @Test
        @DisplayName("soma e multiplica converge nas 500 e nunca piora no caminho")
        void somaEMultiplicaConverge() {
            Medidas.Correcao linha = correcao("soma e multiplica");

            assertThat(linha.convergiram()).isEqualTo(500);
            assertThat(linha.pioraram()).isZero();
            assertThat(linha.indiceMedioFinal()).isCloseTo(1.0, within(1e-6));

            assertThat(linha.menorIndiceFinal())
                    .as("o pior dos 500 ainda esta a menos de um milionesimo de 1")
                    .isCloseTo(1.0, within(1e-6));
        }

        @Test
        @DisplayName("multiplica e multiplica congela: variacao exatamente zero")
        void multiplicaCongela() {
            Medidas.Correcao linha = correcao("multiplica e multiplica");

            assertThat(linha.maiorVariacao())
                    .as("a unica diferenca em 500 execucoes e o arredondamento do ponto"
                            + " flutuante, na decima sexta casa")
                    .isLessThan(1e-14);

            assertThat(linha.pioraram()).isZero();
            assertThat(linha.convergiram())
                    .as("os 29 que terminaram justos ja tinham comecado justos")
                    .isEqualTo(29);
        }

        @Test
        @DisplayName("soma e soma oscila: piora em todas as 500 e converge em 23")
        void somaESomaOscila() {
            Medidas.Correcao linha = correcao("soma e soma");

            assertThat(linha.pioraram()).isEqualTo(500);
            assertThat(linha.convergiram()).isEqualTo(23);
            assertThat(linha.indiceMedioFinal()).isCloseTo(0.6640, within(0.0001));
        }

        @Test
        @DisplayName("multiplica e soma piora de proposito: nenhuma das 500 converge")
        void multiplicaESomaPiora() {
            Medidas.Correcao linha = correcao("multiplica e soma");

            assertThat(linha.convergiram()).isZero();
            assertThat(linha.pioraram()).isEqualTo(500);
            assertThat(linha.indiceMedioFinal()).isCloseTo(0.3092, within(0.0001));
            assertThat(linha.menorIndiceFinal()).isCloseTo(0.1668, within(0.0001));
        }
    }

    // ───────────────────── 2. convergencia ─────────────────────

    @Nested
    @DisplayName("tabela 2: quantas rodadas ate dividir igual")
    class AConvergencia {

        @Test
        @DisplayName("com dois fluxos sao 39,7 rodadas, e nenhum caso fica para tras")
        void comDoisFluxos() {
            Medidas.Convergencia linha = convergencia(2);

            assertThat(linha.rodadasAteJusto()).isCloseTo(39.7, within(0.05));
            assertThat(linha.nuncaFicaramJustos()).isZero();
            assertThat(linha.indiceSincrono()).isCloseTo(1.0, within(1e-6));
        }

        @Test
        @DisplayName("com mais fluxos a conta fica mais rapida, nao mais lenta")
        void maisFluxosConvergeMaisRapido() {
            assertThat(convergencia(8).rodadasAteJusto())
                    .isLessThan(convergencia(2).rodadasAteJusto());

            assertThat(convergencia(8).rodadasAteJusto()).isCloseTo(21.1, within(0.05));
        }

        @Test
        @DisplayName("a realimentacao sincrona chega a 1 com qualquer numero de fluxos")
        void aSincronaChegaAUm() {
            for (Medidas.Convergencia linha : CONVERGENCIA) {
                assertThat(linha.indiceSincrono())
                        .as(linha.fluxos() + " fluxos")
                        .isCloseTo(1.0, within(1e-6));

                assertThat(linha.nuncaFicaramJustos()).isZero();
            }
        }

        /**
         * A HIPOTESE QUE SUSTENTAVA TUDO.
         *
         * <p>Trocar "todos recebem o sinal" por "um pacote se perde e so o dono dele cai"
         * derruba a equidade de 1 para 0,905 com dois fluxos, e ela piora conforme entram
         * mais fluxos. A convergencia do teorema nao sobrevive inteira a essa troca.
         */
        @Test
        @DisplayName("com perda em um fluxo so, a equidade cai e piora com mais fluxos")
        void aPerdaEmUmDerrubaAEquidade() {
            assertThat(convergencia(2).indiceComPerdaEmUm()).isCloseTo(0.9052, within(0.0005));
            assertThat(convergencia(8).indiceComPerdaEmUm()).isCloseTo(0.7898, within(0.0005));

            double anterior = 1;

            for (Medidas.Convergencia linha : CONVERGENCIA) {
                assertThat(linha.indiceComPerdaEmUm())
                        .as(linha.fluxos() + " fluxos")
                        .isLessThan(anterior);

                anterior = linha.indiceComPerdaEmUm();
            }
        }
    }

    // ───────────────────── 3. eficiencia ─────────────────────

    @Nested
    @DisplayName("tabela 3: o que o fator de queda custa")
    class AEficiencia {

        @Test
        @DisplayName("cair pela metade usa 75,5% do enlace e fica justo em 79 rodadas")
        void cairPelaMetade() {
            Medidas.Eficiencia linha = eficiencia(0.5);

            assertThat(linha.usoMedio()).isCloseTo(0.755, within(0.001));
            assertThat(linha.rodadasAteJusto()).isEqualTo(79);
            assertThat(linha.menorSoma()).isCloseTo(50, within(0.5));
        }

        @Test
        @DisplayName("cair pouco usa 94,7% e demora 115 rodadas para dividir igual")
        void cairPouco() {
            Medidas.Eficiencia linha = eficiencia(0.875);

            assertThat(linha.usoMedio()).isCloseTo(0.947, within(0.001));
            assertThat(linha.rodadasAteJusto()).isEqualTo(115);
        }

        /**
         * A TROCA, NAS DUAS COLUNAS AO MESMO TEMPO.
         *
         * <p>Quanto mais perto de 1 o fator de queda, mais cheio fica o enlace e mais devagar
         * a divisao fica justa. Nao ha fator certo: ha os dois lados da mesma escolha, e as
         * duas colunas sobem juntas em todas as linhas.
         */
        @Test
        @DisplayName("quanto maior o fator, maior o uso e maior a demora, sempre")
        void asDuasColunasSobemJuntas() {
            for (int i = 1; i < EFICIENCIA.size(); i++) {
                assertThat(EFICIENCIA.get(i).usoMedio())
                        .as("uso, linha " + i)
                        .isGreaterThan(EFICIENCIA.get(i - 1).usoMedio());

                assertThat(EFICIENCIA.get(i).rodadasAteJusto())
                        .as("demora, linha " + i)
                        .isGreaterThan(EFICIENCIA.get(i - 1).rodadasAteJusto());
            }
        }

        @Test
        @DisplayName("a oscilacao da soma e exatamente o que a queda tira")
        void aOscilacaoEhAQueda() {
            for (Medidas.Eficiencia linha : EFICIENCIA)
                assertThat(linha.menorSoma())
                        .as("fator " + linha.fatorDeQueda())
                        .isCloseTo(linha.fatorDeQueda() * Medidas.CAPACIDADE, within(0.6));
        }
    }

    // ───────────────────── 4. a formula ─────────────────────

    @Nested
    @DisplayName("tabela 4: o serrote contra a formula da vazao")
    class AFormula {

        @Test
        @DisplayName("com janela 4 a formula erra 18%, com janela 256 erra 0,4%")
        void aFormulaEhAssintotica() {
            assertThat(perdaCerta(4).razao()).isCloseTo(0.8165, within(0.0001));
            assertThat(perdaCerta(256).razao()).isCloseTo(0.9961, within(0.0001));
        }

        @Test
        @DisplayName("e ela erra sempre para o mesmo lado: para mais")
        void elaErraSempreParaMais() {
            for (Medidas.Formula linha : PERDA_CERTA)
                assertThat(linha.razao())
                        .as("janela " + linha.janelaMaxima())
                        .isLessThan(1.0);
        }

        @Test
        @DisplayName("a razao sobe a cada dobro da janela, monotonicamente")
        void aRazaoSobeComAJanela() {
            for (int i = 1; i < PERDA_CERTA.size(); i++)
                assertThat(PERDA_CERTA.get(i).razao())
                        .as("janela " + PERDA_CERTA.get(i).janelaMaxima())
                        .isGreaterThan(PERDA_CERTA.get(i - 1).razao());
        }

        /**
         * COM PERDA SORTEADA, O ERRO TROCA DE SINAL.
         *
         * <p>A formula foi deduzida do serrote periodico. Com a perda caindo onde quer, ela
         * subestima a vazao em perda baixa: a razao passa de 1 e chega a 1,05.
         */
        @Test
        @DisplayName("com perda sorteada ela passa a subestimar: razao 1,05 em perda baixa")
        void comPerdaSorteadaOErroTrocaDeSinal() {
            assertThat(perdaSorteada(0.1).razao()).isCloseTo(0.9098, within(0.0005));
            assertThat(perdaSorteada(0.001).razao()).isCloseTo(1.0544, within(0.0005));

            assertThat(perdaSorteada(0.001).razao()).isGreaterThan(1);
        }

        @Test
        @DisplayName("a perda medida bate com a pedida, que e o que valida o sorteio")
        void aPerdaMedidaBateComAPedida() {
            for (Medidas.Formula linha : PERDA_SORTEADA)
                assertThat(linha.perdaMedida())
                        .as("perda " + linha.probabilidade())
                        .isCloseTo(linha.probabilidade(), within(linha.probabilidade() * 0.05));
        }
    }

    // ───────────────────── 5. ida e volta ─────────────────────

    @Nested
    @DisplayName("tabela 5: dois fluxos com tempos de ida e volta diferentes")
    class AIdaEVolta {

        @Test
        @DisplayName("com a mesma demora, os dois ficam com a mesma taxa")
        void comAMesmaDemoraEmpatam() {
            assertThat(idaEVolta(1).razaoDasTaxas()).isCloseTo(1.0, within(1e-9));
        }

        /**
         * A DESIGUALDADE E A RAZAO DAS DEMORAS, NAO O QUADRADO DELA.
         *
         * <p>Com demora dobrada o fluxo lento recebe metade; com demora oito vezes maior,
         * um oitavo. Nada na lei de controle fala de tempo, e mesmo assim o tempo de ida e
         * volta aparece inteiro na divisao.
         */
        @Test
        @DisplayName("a razao das taxas acompanha a razao das demoras")
        void aRazaoAcompanhaADemora() {
            for (Medidas.IdaEVolta linha : IDA_E_VOLTA)
                assertThat(linha.razaoDasTaxas())
                        .as("razao " + linha.razao())
                        .isCloseTo(linha.razao(), within(linha.razao() * 0.06));
        }

        @Test
        @DisplayName("e nao o quadrado dela, que seria 64 vezes com demora 8")
        void naoEhOQuadrado() {
            Medidas.IdaEVolta linha = idaEVolta(8);

            assertThat(linha.razaoDasTaxas()).isCloseTo(7.823, within(0.005));
            assertThat(linha.razaoDasTaxas()).isLessThan(10);
        }

        @Test
        @DisplayName("o fluxo lento nunca e zerado, so espremido")
        void oLentoNaoEhZerado() {
            for (Medidas.IdaEVolta linha : IDA_E_VOLTA)
                assertThat(linha.taxaDoLento())
                        .as("razao " + linha.razao())
                        .isGreaterThan(1);
        }
    }
}
