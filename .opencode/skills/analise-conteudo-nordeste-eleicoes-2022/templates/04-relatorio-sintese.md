# Relatório de Síntese

**Sessão:** [nome-da-sessao]
**Data:** [AAAA-MM-DD]

## 1. Descrição do corpus

- Fonte
- Período
- Número de comentários
- Observações sobre qualidade dos dados

## 2. Distribuição geral

### 2.1 POS_NOR

| Código | Rótulo | N | Percentual |
|---|---|---|---|
| 00 | Contra ou ataque ao Nordeste | [N] | [X] |
| 01 | Favorável ou defesa do Nordeste | [N] | [X] |
| 99 | Não se aplica | [N] | [X] |

### 2.2 AC_DOM entre comentários com POS_NOR = 00

| Código | Rótulo | N | Percentual |
|---|---|---|---|
| 01 | Estereótipos comportamentais | [N] | [X] |
| 02 | Econômico | [N] | [X] |
| 03 | Educação | [N] | [X] |
| 04 | Recursos naturais | [N] | [X] |
| 05 | Comunismo ou socialismo | [N] | [X] |
| 99 | Outro ataque | [N] | [X] |

### 2.3 FD_DOM entre comentários com POS_NOR = 01

| Código | Rótulo | N | Percentual |
|---|---|---|---|
| 01 | Orgulho pelo Nordeste | [N] | [X] |
| 02 | Exaltação a Lula ou PT | [N] | [X] |
| 03 | Jurídico ou denúncia de crime | [N] | [X] |
| 04 | Resposta ofensiva | [N] | [X] |
| 99 | Outra defesa | [N] | [X] |

### 2.4 ADVE_TIPO

| Código | Rótulo | N | Percentual |
|---|---|---|---|
| 01 | Esquerda | [N] | [X] |
| 02 | Direita | [N] | [X] |
| 03 | Nordeste ou nordestinos | [N] | [X] |
| 04 | Sul ou Sudeste | [N] | [X] |
| 05 | Lula | [N] | [X] |
| 06 | Bolsonaro | [N] | [X] |
| 07 | Políticos ou instituições | [N] | [X] |
| 0 | Outros | [N] | [X] |
| 999 | Não se aplica | [N] | [X] |

### 2.5 POSIC_COMENT

| Código | Rótulo | N | Percentual |
|---|---|---|---|
| 00 | Neutro ou indeterminado | [N] | [X] |
| 01 | Favorável a Bolsonaro | [N] | [X] |
| 02 | Favorável a Lula | [N] | [X] |

## 3. Ambiguidades e limites

- principais casos ambíguos
- tipo de comentário que exigiu revisão humana
- limites do esquema classificatório diante do corpus

## 4. Observações metodológicas

- critério de predominância
- uso de códigos 99 e 999
- controle de inferência política
- auditoria por amostragem
