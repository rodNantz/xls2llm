# Codebook Operacional

Este arquivo traduz o livro de códigos em definições operacionais curtas, com regras de inclusão e exclusão.

## V0. Dados gerais

### ID_COM
Identificador sequencial ou identificador original do corpus.

### ID_COD
Identificador do codificador. Se houver apenas um agente codificando, usar um valor fixo e documentá-lo.

---

## V1. POS_NOR

### 00 Contra, crítica ou ataque ao Nordeste
Marque quando houver discurso de ódio, xenofobia, insulto, desqualificação, inferiorização ou exclusão do Nordeste ou de nordestinos.

Inclui.
- insultos diretos ao povo ou à região
- desqualificação moral, intelectual, econômica ou política
- propostas de separação territorial ou boicote
- associação negativa do Nordeste a atraso ou parasitismo

Exclui.
- críticas a Lula ou Bolsonaro sem ataque ao Nordeste
- mera descrição do resultado eleitoral
- uso do radical Nordest sem posição identificável

### 01 Favorável ou defesa do Nordeste
Marque quando o comentário defender, exaltar ou responder aos ataques xenofóbicos dirigidos ao Nordeste.

Inclui.
- orgulho regional
- exaltação do povo nordestino
- denúncia de xenofobia como crime
- defesa do direito de votar
- resposta ofensiva a outras regiões ou atores como reação aos ataques ao Nordeste

Exclui.
- elogio vazio sem referência clara ao Nordeste
- menção ao Nordeste sem posição valorativa identificável
- elogio ao Nordeste acompanhado de inferiorização predominante

### 99 Não se aplica
Marque quando o comentário menciona Nordeste ou o radical Nordest, mas não permite classificar como ataque nem defesa.

---

## V1.1. AC_DOM
Usar somente quando `POS_NOR = 00`.

### 01 Comportamentos estereotipados ou reducionistas
Atribui ao nordestino preguiça, desonestidade, esperteza, ignorância manipulável, venda de voto, recusa ao trabalho ou outros traços comportamentais inferiores.

Gatilhos comuns.
- preguiçoso
- vagabundo
- safado
- burro porque vende voto
- só quer coisa dada

### 02 Econômico ou recursos materiais
Apresenta o Nordeste como peso econômico, dependente de benefícios, pobre por escolha ou sustentado por outras regiões.

Gatilhos comuns.
- vive de Bolsa Família
- Sul e Sudeste sustentam
- não gasto mais meu dinheiro lá
- região pobre e atrasada

### 03 Educação
Ataca a escolaridade, inteligência ou capacidade cognitiva de nordestinos.

Gatilhos comuns.
- analfabeto
- burro
- jumento
- ignorante
- incapaz intelectualmente

### 04 Recursos naturais
Associa o ataque à seca, fome, sede, água, sertão, caatinga ou transposição do São Francisco.

Gatilhos comuns.
- morrer na seca
- deixar passar fome
- sertão sem água

### 05 Relações com comunismo ou socialismo
Relaciona o Nordeste a comunismo, socialismo, Cuba, Venezuela, Maduro e equivalentes.

Gatilhos comuns.
- Cuba do Sul
- Venezuela 3.0
- comunistas do Nordeste

### 99 Não se aplica
Há xenofobia, mas o argumento dominante não cabe nas categorias 01 a 05.

### 999 Não se aplica
Uso estrutural quando `POS_NOR` não for 00.

---

## V1.2. FD_DOM
Usar somente quando `POS_NOR = 01`.

### 01 Orgulho pelo Nordeste
Defesa baseada em orgulho regional, resgate histórico, exaltação cultural ou valorização do povo nordestino.

Gatilhos comuns.
- meu Nordeste
- povo guerreiro
- aqui é Nordeste
- amo o Nordeste

### 02 Exaltação a Lula ou PT
Defesa do Nordeste centrada em Lula, PT, Dilma ou políticas associadas ao partido e à região.

Gatilhos comuns.
- orgulho do Nordeste com Lula
- Faz o L
- Lula nordestino
- PT fez pelo Nordeste

### 03 Jurídico ou denúncia de crime
Defesa centrada na denúncia da xenofobia como crime ou na cobrança de ação de Justiça, polícia, MP, TSE, Justiça Eleitoral ou PRF.

Gatilhos comuns.
- xenofobia é crime
- deixem o Nordeste votar
- denúncia ao TSE
- atuação da PRF

### 04 Resposta ofensiva
Resposta defensiva com revanchismo, insulto ou ataque a Sul, Sudeste, Bolsonaro, bolsonaristas ou autores da xenofobia.

Gatilhos comuns.
- paulistas, cariocas ou sulistas insultados em resposta
- ofensa a bolsonaristas como reação
- ironia agressiva em defesa do Nordeste

### 99 Não se aplica
Há defesa do Nordeste, mas o argumento dominante não cabe nas categorias 01 a 04.

### 999 Não se aplica
Uso estrutural quando `POS_NOR` não for 01.

---

## V2. ADVE

### 0 Ausente
Não há adversário ou inimigo comum discernível.

### 1 Presente
Existe ator individual, coletivo, regional, partidário ou institucional construído como inimigo, obstáculo ou alvo principal do comentário.

---

## V2.1. ADVE_TIPO
Usar somente quando `ADVE = 1`.

### 01 Esquerda
PT, esquerda, comunismo, socialismo, Cuba, Venezuela, mensalão, petrolão.

### 02 Direita
Direita, liberais, capitalismo ou blocos geopolíticos acionados como símbolo da direita.

### 03 Nordeste ou nordestinos
O alvo principal é a região ou seu povo.

### 04 Sul ou Sudeste ou seus gentílicos
Sulista, paulista, carioca, Sudeste, Sul, São Paulo, Rio de Janeiro e equivalentes.

### 05 Lula
Lula ou seus apoiadores como alvo principal. Se Lula e PT aparecerem juntos com peso semelhante, priorize Lula quando ele estiver nomeado diretamente.

### 06 Bolsonaro
Bolsonaro, bolsonaristas, bozo e equivalentes.

### 07 Políticos ou instituições
Outros políticos, TSE, Câmara, Senado, PRF, Justiça e demais instituições.

### 0 Outros
Adversário presente, mas fora das categorias anteriores. Especificar na justificativa.

### 999 Não se aplica
Usar quando `ADVE = 0`.

---

## V3. POSIC_COMENT

### 00 Neutro ou indeterminado
Não há apoio explícito a Lula ou Bolsonaro.

### 01 Favorável a Bolsonaro
O texto declara apoio a Bolsonaro, ao número 22, ao PL ou à sua reeleição.

### 02 Favorável a Lula
O texto declara apoio a Lula, ao número 13, ao PT em chave de vitória eleitoral ou usa expressões inequívocas como Faz o L.

## Regra crítica
Ser contra um candidato não torna o comentário automaticamente favorável ao outro.
