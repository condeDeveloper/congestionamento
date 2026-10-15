# congestionamento

Controle de congestionamento do zero em Java 21: **só o aumento somado com a queda
multiplicada converge para a divisão justa, e isso depende de todo mundo cair junto.**

Cada fluxo recebe **um bit por rodada**: cabe ou não cabe. Nenhum sabe quantos outros
existem, nenhum sabe a capacidade do enlace, nenhum fala com os outros. A pergunta inteira do
repositório é se a divisão justa aparece disso.

## As quatro leis, e o teorema de 1989

Uma lei de controle linear tem a forma `x <- a + b * x`, com um par `(a, b)` para quando cabe
e outro para quando não cabe. Aumento **somado** é `b = 1` com `a` positivo; aumento
**multiplicado** é `a = 0` com `b` maior que 1. São quatro combinações.

Chiu e Jain mostraram em 1989 que só uma delas converge para a divisão justa. Este
repositório não cita isso: ele sorteia 500 divisões iniciais para cada lei e olha o que
acontece.

```
$ mvn exec:java -Dexec.args="correcao"

  lei                         convergiram   pioraram   indice medio    pior indice maior variacao
  soma e multiplica                   500          0         1.0000         1.0000       0.078292
  soma e soma                          23        500         0.6640         0.3100       0.078292
  multiplica e multiplica              29          0         0.7942         0.3798       0.000000
  multiplica e soma                     0        500         0.3092         0.1668       0.028566
```

A coluna que importa não é a do índice final: é a de quantas vezes a equidade **piorou no
caminho**. Uma lei pode terminar justa por ter começado justa, e o teorema fala sobre a
direção do movimento.

- **Soma e multiplica**: converge nas 500, e a equidade nunca anda para trás.
- **Soma e soma**: oscila. Somar a mesma constante a todo mundo aproxima da justiça e subtrair
  afasta, então ela sobe e desce para sempre sem chegar.
- **Multiplica e multiplica**: variação **exatamente zero**, a menos do arredondamento do
  ponto flutuante na décima sexta casa. O índice de Jain não muda quando todas as taxas são
  multiplicadas pela mesma constante, então ela congela a divisão que encontrou, seja ela qual
  for. Os 29 que terminaram justos já tinham começado justos.
- **Multiplica e soma**: piora de propósito. Nenhuma das 500 converge, e a média cai para
  0,3092, que para quatro fluxos é perto de um fluxo ficar com tudo.

A medida da equidade é o **índice de Jain**, de 1984: vale 1 quando todos recebem o mesmo,
vale `k/n` quando `k` fluxos dividem tudo e o resto fica a zero, e não muda de escala.

## O que sustentava a convergência

```
$ mvn exec:java -Dexec.args="convergencia"

  fluxos      rodadas ate justo    nunca ficou    indice sincrono indice com perda em um
  2                        39.7              0             1.0000             0.9052
  3                        36.6              0             1.0000             0.8605
  4                        33.4              0             1.0000             0.8380
  6                        25.7              0             1.0000             0.8102
  8                        21.1              0             1.0000             0.7898
```

A coluna do meio é o modelo do teorema: **realimentação síncrona**, todo mundo recebe o sinal
na mesma rodada. A última troca essa hipótese por uma fila que descarta por excesso: um pacote
se perde, e só o dono dele cai.

A equidade cai de 1 para 0,905 com dois fluxos, e **piora conforme entram mais fluxos**. A
convergência não é uma propriedade só da lei: é da lei mais a hipótese de todo mundo cair
junto.

## A troca que o fator de queda esconde

```
$ mvn exec:java -Dexec.args="eficiencia"

  fator             uso medio     menor soma     maior soma  rodadas ate justo
  0.300                 64.4%           30.0          100.0                 59
  0.500                 75.5%           50.0          100.0                 79
  0.700                 84.8%           70.0          100.0                 94
  0.875                 94.7%           87.5          102.0                115
  0.950                 98.4%           95.0          102.0                144
```

Cair pela metade devolve o enlace depressa e deixa o cano um quarto vazio. Cair pouco mantém o
cano cheio e demora quase o dobro para dividir igual. Não existe fator certo: as duas colunas
sobem juntas em todas as linhas, e escolher é escolher de qual lado ficar.

## O serrote contra a fórmula

O outro modelo do repositório é de um fluxo só: a janela sobe de um em um por tempo de ida e
volta e cai pela metade quando um pacote se perde. Em 1997, Mathis e outros mostraram que a
vazão média desse serrote é

```
vazao = raiz(3/2) / (ida e volta * raiz da taxa de perda)
```

e essa fórmula não sai de implementação nenhuma: sai da geometria do triângulo. Ela é o
segundo juiz.

```
$ mvn exec:java -Dexec.args="formula"

  janela maxima           perda        vazao      formula      razao
  4                    0.111111        3.000        3.674     0.8165
  8                    0.033333        6.000        6.708     0.8944
  16                   0.009259       12.000       12.728     0.9428
  32                   0.002451       24.000       24.739     0.9701
  64                   0.000631       48.000       48.744     0.9847
  128                  0.000160       96.000       96.747     0.9923
  256                  0.000040      192.000      192.749     0.9961

  probabilidade    perda medida        vazao      formula      razao
  0.1000               0.099884        3.526        3.875     0.9098
  0.0500               0.050191        5.196        5.467     0.9505
  0.0100               0.009997       12.444       12.249     1.0159
  0.0050               0.004983       17.892       17.351     1.0312
  0.0010               0.000996       40.915       38.806     1.0544
```

A fórmula é **assintótica**, e a primeira tabela mostra de quanto: com janela 4 ela erra 18%
para mais, e só abaixo de 1% de erro a partir de janela 128. Parte disso é um ciclo que dura
`teto/2 + 1` idas e voltas, e não `teto/2`: com teto 4, esse um a mais é um terço do ciclo.

A segunda tabela troca a perda periódica por perda sorteada, e aí o erro **troca de sinal**: a
fórmula passa a subestimar a vazão, e chega a 5,4% abaixo com perda de um por mil.

## Dois fluxos iguais com demoras diferentes

```
$ mvn exec:java -Dexec.args="ida-e-volta"

  razao das demoras        rapido        lento  razao das taxas          1/r         1/r2
  1                        37.534       37.534            1.000        1.000        1.000
  2                        49.531       25.464            1.945        0.500        0.250
  3                        56.881       19.294            2.948        0.333        0.111
  4                        59.927       14.976            4.002        0.250        0.063
  8                        66.439        8.492            7.823        0.125        0.016
```

A mesma lei, o mesmo enlace, a mesma taxa inicial. A única diferença é quanto tempo a
confirmação demora a voltar, e **nada na lei de controle fala de tempo**.

A subida depende da confirmação chegar, então o fluxo lento sobe menos vezes. A queda não: o
pacote se perde na fila e se perde para todo mundo na mesma rodada. Dessa assimetria sai uma
divisão que acompanha a razão das demoras, **e não o quadrado dela**.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `convergencia`, `eficiencia`, `formula` e
`ida-e-volta`.

## O que as medidas me corrigiram

**Eu ia escrever que o aumento somado com a queda multiplicada converge, ponto.** Ele
converge sob a hipótese de que todo mundo recebe o sinal na mesma rodada, que é como o
teorema é enunciado e como quase ninguém o repete. Trocando por descarte de um pacote só, a
equidade cai para 0,905 com dois fluxos e 0,790 com oito. O resultado continua valendo; o que
não vale é a frase curta.

**A lei que só multiplica não é ruim por acaso: ela é aritmeticamente incapaz.** A variação
máxima do índice em 500 execuções é zero, a menos do arredondamento. Multiplicar todas as
taxas pela mesma constante não muda o índice de Jain, e é exatamente isso que ela faz, para
cima e para baixo. Nenhuma quantidade de rodadas vai mudar isso.

**A fórmula de Mathis não é uma aproximação com erro pequeno.** Com janela 4 ela erra 18%, e
com perda sorteada ela erra para o outro lado. Eu ia usá-la como juiz direto do serrote, e o
que ela é de verdade é um juiz assintótico: só vale como juiz onde a janela é grande, e a
tabela mostra onde isso começa.

**E o ciclo do serrote tem uma ida e volta a mais do que eu tinha contado.** Ele vai de
`teto/2` até `teto`, incluindo os dois, o que dá `teto/2 + 1`. É o tipo de um a mais que some
com janela grande e domina com janela pequena, e é parte do erro de 18%.

**A desigualdade por tempo de ida e volta é linear, não quadrática.** A primeira versão do
modelo fazia o fluxo lento também cair menos vezes, e isso compensava exatamente a subida mais
lenta: a tabela saía dizendo que a demora não importa, com o fluxo lento até levando
vantagem. A queda acontece quando o pacote se perde, não quando a confirmação volta, e corrigir
isso é o que faz a razão das taxas ser 7,8 com demora 8.

## O que não tem aqui

**Partida lenta.** O crescimento daqui é sempre de um em um. O começo exponencial de uma
conexão de verdade é outro regime, e ele é o que decide o tempo de uma transferência curta,
que é a maioria delas.

**Fila, atraso e perda por estouro.** O enlace daqui compara a soma com a capacidade e devolve
um bit. Uma fila de verdade acumula, adiciona atraso antes de descartar, e é isso que
transforma o controle por perda em controle por atraso, que é outra família inteira, a do
Vegas e a do BBR.

**Retransmissão.** Pacote perdido aqui é contado e esquecido. Quem precisa dele de volta, e o
custo disso, é o assunto do transporte confiável, não do controle de congestionamento.

**Filas ativas no roteador.** Todo o descarte daqui é por excesso. Descartar cedo e de
propósito, que é o que RED e CoDel fazem, muda qual fluxo recebe o sinal, e a tabela da perda
em um fluxo só mostra que isso importa muito. O lado do roteador está em
[`justica`](https://github.com/condeDeveloper/justica).

**Mais de um gargalo.** Aqui todos os fluxos passam pelo mesmo enlace. Com vários gargalos, a
divisão justa deixa de ser "todos iguais" e vira max-min, e aí a pergunta muda de figura.

## Licença

MIT.
