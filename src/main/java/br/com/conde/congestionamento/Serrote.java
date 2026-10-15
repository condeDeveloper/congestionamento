package br.com.conde.congestionamento;

/**
 * O SERROTE: uma janela que sobe de um em um e cai pela metade.
 *
 * <p>Este e o outro modelo do repositorio, e ele e de um fluxo so. A janela cresce um pacote
 * por tempo de ida e volta enquanto nada se perde, e cai pela metade quando um pacote se
 * perde. O desenho que isso faz no tempo e o serrote que todo mundo ja viu.
 *
 * <p>Ele existe por causa da FORMULA. Em 1997, Mathis e outros mostraram que a vazao media
 * desse serrote e
 *
 * <pre>vazao = raiz(3/2) / (ida e volta * raiz da taxa de perda)</pre>
 *
 * <p>e essa formula nao sai de nenhuma implementacao: ela sai da geometria do triangulo. Ela
 * e o segundo juiz deste repositorio, e a tabela da formula mede onde ela acerta e onde nao.
 *
 * <h2>O que este modelo nao tem</h2>
 *
 * <p>Partida lenta, retransmissao por tempo esgotado, confirmacao seletiva e o limite da
 * janela anunciada pelo outro lado. Cada um deles muda a vazao de verdade, e a formula
 * tambem nao os tem: comparar o serrote com a formula compara duas descricoes do MESMO
 * modelo, que e o que torna a discordancia entre elas informativa.
 */
public final class Serrote {

    /** Menor janela possivel: com zero pacotes em voo nada volta e nada anda. */
    public static final int MENOR_JANELA = 1;

    public record Resultado(int janelaMaxima, double idaEVolta, int ciclos, long pacotes,
            long perdidos, double tempo) {

        /** Pacotes por unidade de tempo. */
        public double vazao() {
            return pacotes / tempo;
        }

        public double taxaDePerda() {
            return perdidos / (double) pacotes;
        }

        public double janelaMedia() {
            return vazao() * idaEVolta;
        }
    }

    private Serrote() {}

    /**
     * O serrote com perda certa: a janela cresce ate o teto e cai.
     *
     * <p>E o modelo exato por tras da formula. Cada ciclo comeca com metade da janela
     * maxima, sobe de um em um ate o teto, perde um pacote e cai pela metade.
     */
    public static Resultado comPerdaCerta(int janelaMaxima, double idaEVolta, int ciclos) {
        int janela = Math.max(MENOR_JANELA, janelaMaxima / 2);

        long pacotes = 0;
        long perdidos = 0;

        double tempo = 0;

        for (int ciclo = 0; ciclo < ciclos; ciclo++)
            while (true) {
                pacotes += janela;
                tempo += idaEVolta;

                if (janela >= janelaMaxima) {
                    perdidos++;
                    janela = Math.max(MENOR_JANELA, janela / 2);
                    break;
                }

                janela++;
            }

        return new Resultado(janelaMaxima, idaEVolta, ciclos, pacotes, perdidos, tempo);
    }

    /**
     * O mesmo serrote com perda sorteada: cada pacote se perde com esta probabilidade.
     *
     * <p>Aqui a janela maxima nao existe. O que limita e a propria perda, e a pergunta e se
     * a formula, deduzida do caso periodico, ainda vale quando a perda cai onde quer.
     */
    public static Resultado comPerdaSorteada(double probabilidade, double idaEVolta,
            int idasEVoltas, Sorteio sorteio) {

        int janela = MENOR_JANELA;

        long pacotes = 0;
        long perdidos = 0;

        for (int passo = 0; passo < idasEVoltas; passo++) {
            boolean perdeu = false;

            for (int pacote = 0; pacote < janela; pacote++) {
                pacotes++;

                if (!sorteio.com(probabilidade)) continue;

                perdidos++;
                perdeu = true;
            }

            janela = perdeu ? Math.max(MENOR_JANELA, janela / 2) : janela + 1;
        }

        return new Resultado(0, idaEVolta, idasEVoltas, pacotes, perdidos,
                idasEVoltas * idaEVolta);
    }

    /** A formula de Mathis: raiz de tres meios, sobre ida e volta vezes raiz da perda. */
    public static double formula(double idaEVolta, double taxaDePerda) {
        return StrictMath.sqrt(1.5) / (idaEVolta * StrictMath.sqrt(taxaDePerda));
    }
}
