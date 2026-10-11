---
name: analise-conteudo-nordeste-eleicoes-2022
description: Use esta skill sempre que o usuário pedir codificação automatizada ou semiautomatizada de comentários sobre o Nordeste e nordestinos no contexto das eleições presidenciais brasileiras de 2022, com base no livro de códigos anexo. Acione quando houver corpus de comentários, posts, replies ou planilhas textuais e a tarefa for classificar cada item segundo V1, V1.1 ou V1.2, V2, V2.1 e V3, produzindo trilha de auditoria, tabela final e checagem de consistência.
---

# Análise de Conteúdo Automatizada — Nordeste nas Eleições 2022

## Finalidade

Esta skill operacionaliza o livro de códigos do projeto sobre comentários no Twitter/X a respeito do Nordeste durante as eleições presidenciais brasileiras de 2022. O objetivo é classificar cada comentário individualmente, com regra de predominância e trilha de auditoria explícita.

Use esta skill para:
- codificar corpus de comentários em lote
- revisar codificação já existente
- gerar planilha final para análise quantitativa ou qualitativa
- produzir relatório de distribuição das categorias
- identificar casos ambíguos para revisão humana

Não use esta skill para análise temática reflexiva, grounded theory ou síntese interpretativa ampla. Aqui a unidade analítica é o comentário individual e a tarefa é classificatória.

## Princípios não negociáveis

1. Leia o comentário inteiro antes de classificar.
2. Cada comentário recebe apenas uma categoria dominante por variável categorial.
3. Quando a variável anterior bloquear a seguinte, use o código de não aplicação.
4. Não infira apoio político apenas por contexto implícito. O apoio a Lula ou Bolsonaro deve estar expresso.
5. Dados contraditórios ou ambíguos não devem ser apagados. Devem ser marcados e justificados.
6. Quando houver dúvida real entre duas categorias, registre a dúvida na coluna de justificativa e inclua o item na revisão humana.
7. Preserve a literalidade do comentário original no output final.

## Arquivos de apoio

- `referencias/codebook-operacional.md` contém a operacionalização completa do livro de códigos.
- `referencias/arvore-de-decisao.md` resume a ordem correta de classificação.
- `referencias/regras-de-consistencia.md` contém validações obrigatórias.
- `templates/01-preparacao-corpus.md` documenta a origem e a limpeza do corpus.
- `templates/02-codificacao-lote.csv` é o modelo principal de saída.
- `templates/03-revisao-qualidade.md` registra amostras, dúvidas e correções.
- `templates/04-relatorio-sintese.md` organiza a síntese final.
- `exemplos/exemplo-codificacao.md` mostra comentários já classificados.

## Ordem obrigatória de trabalho

Copie esta checklist no início da execução e atualize o status.

- [ ] Etapa 1. Preparar e validar o corpus
- [ ] Etapa 2. Codificar comentário por comentário
- [ ] Etapa 3. Rodar checagens de consistência
- [ ] Etapa 4. Separar casos ambíguos para revisão humana
- [ ] Etapa 5. Exportar tabela final e síntese

Não pule etapas.

---

## Etapa 1. Preparar e validar o corpus

### Objetivo
Garantir que a unidade de análise está correta e que cada linha representa um comentário único.

### Operações obrigatórias

1. Identificar o formato do corpus.
   - CSV
   - XLSX
   - JSONL
   - TXT
   - PDF convertido
   - planilha copiada no chat

2. Definir a unidade de análise.
   - Um comentário por linha
   - Se houver threads longas, a unidade continua sendo o comentário individual, não a thread inteira

3. Preservar os campos originais relevantes.
   - id do comentário
   - texto do comentário
   - autor, data, link, se existirem

4. Limpar ruídos mínimos sem alterar o sentido.
   - manter hashtags, menções e emojis se forem semanticamente úteis
   - não reescrever o texto
   - não resumir o comentário antes de codificar

5. Documentar a preparação do corpus em `01-preparacao-corpus.md`.

### Checkpoint

Só avance se:
- [ ] cada linha representar um comentário único
- [ ] o texto original estiver preservado
- [ ] houver identificador único por comentário, ainda que criado manualmente
- [ ] o corpus preparado estiver descrito em `01-preparacao-corpus.md`

Se algum item falhar, não avance para a codificação.

---

## Etapa 2. Codificar comentário por comentário

### Variáveis obrigatórias

Para cada comentário, preencher:
- `ID_COM`
- `ID_COD`
- `TEXTO_COMENTARIO`
- `POS_NOR`
- `AC_DOM`
- `FD_DOM`
- `ADVE`
- `ADVE_TIPO`
- `POSIC_COMENT`
- `JUSTIFICATIVA_CURTA`
- `TRECHOS_GATILHO`
- `AMBIGUO_REVISAO`

### Regras estruturais

1. Primeiro classifique `POS_NOR`.
2. Se `POS_NOR = 00`, classifique `AC_DOM` e defina `FD_DOM = 999`.
3. Se `POS_NOR = 01`, classifique `FD_DOM` e defina `AC_DOM = 999`.
4. Se `POS_NOR = 99`, defina `AC_DOM = 999` e `FD_DOM = 999`.
5. Depois classifique `ADVE`.
6. Se `ADVE = 0`, defina `ADVE_TIPO = 999`.
7. Se `ADVE = 1`, classifique `ADVE_TIPO` pelo adversário predominante.
8. Por fim classifique `POSIC_COMENT`.

### Regra de predominância

Quando um comentário parecer caber em mais de uma categoria, escolha a que organiza o sentido dominante do comentário. Não distribua um mesmo comentário por duas categorias da mesma variável.

### Regra de evidência

Toda classificação deve apontar ao menos um trecho gatilho literal. O trecho gatilho pode ser uma expressão, palavra, hashtag ou construção sintática.

### Regra de contenção inferencial

Não use conhecimento externo para decidir a categoria. Use apenas o texto do comentário e, quando disponível, metadados diretamente presentes no corpus.

### Formato de justificativa curta

Use uma frase simples.

Exemplos.
- `Ataque ao Nordeste com ênfase econômica ao dizer que Sul e Sudeste sustentam vagabundos de benefícios sociais.`
- `Defesa do Nordeste por orgulho regional e exaltação do povo nordestino.`
- `Neutro quanto a Lula e Bolsonaro porque só descreve resultado eleitoral.`

### Checkpoint

Só avance se:
- [ ] todas as linhas tiverem variáveis obrigatórias preenchidas
- [ ] toda decisão tiver justificativa curta
- [ ] toda decisão tiver trecho gatilho
- [ ] regras de não aplicação tiverem sido obedecidas

---

## Etapa 3. Rodar checagens de consistência

Aplique obrigatoriamente as regras de `referencias/regras-de-consistencia.md`.

### Verificações mínimas

1. `POS_NOR = 00` não pode coexistir com `FD_DOM` diferente de 999.
2. `POS_NOR = 01` não pode coexistir com `AC_DOM` diferente de 999.
3. `POS_NOR = 99` exige `AC_DOM = 999` e `FD_DOM = 999`.
4. `ADVE = 0` exige `ADVE_TIPO = 999`.
5. `POSIC_COMENT = 01` não pode coexistir com `ADVE_TIPO = 06` quando Bolsonaro é o adversário predominante.
6. `POSIC_COMENT = 02` não pode coexistir com `ADVE_TIPO = 05` quando Lula é o adversário predominante.
7. Se o comentário só mencionar Lula ou Bolsonaro negativamente, isso não basta para marcar apoio ao outro.
8. Se não houver evidência textual clara, marque `POSIC_COMENT = 00`.

### Saída obrigatória desta etapa

Preencher `03-revisao-qualidade.md` com:
- total de itens verificados
- inconsistências encontradas
- correções realizadas
- lista de casos ambíguos

Se inconsistências persistirem, não exporte a tabela final como concluída.

---

## Etapa 4. Separar casos ambíguos para revisão humana

Marque `AMBIGUO_REVISAO = SIM` quando ocorrer pelo menos uma das situações abaixo.

- ironia difícil de resolver
- comentário truncado ou sem contexto mínimo
- coexistência forte entre duas categorias da mesma variável
- menção política implícita sem apoio expresso
- adversário presente, mas sem predominância nítida
- elogio ao Nordeste acompanhado de inferiorização xenofóbica

Nesses casos:
1. mantenha a melhor classificação provisória
2. explique a ambiguidade na justificativa
3. destaque o item na seção de revisão humana

---

## Etapa 5. Exportar tabela final e síntese

### Output principal

Gerar `02-codificacao-lote.csv` com uma linha por comentário e as seguintes colunas na ordem:

`ID_COM,ID_COD,TEXTO_COMENTARIO,POS_NOR,AC_DOM,FD_DOM,ADVE,ADVE_TIPO,POSIC_COMENT,JUSTIFICATIVA_CURTA,TRECHOS_GATILHO,AMBIGUO_REVISAO`

### Output secundário

Gerar `04-relatorio-sintese.md` com:
- descrição do corpus
- número total de comentários
- distribuição de `POS_NOR`
- distribuição de `AC_DOM` entre os ataques
- distribuição de `FD_DOM` entre as defesas
- distribuição de `ADVE_TIPO`
- distribuição de `POSIC_COMENT`
- principais ambiguidades
- observações metodológicas

---

## Procedimento recomendado para lotes grandes

Quando houver muitos comentários:
1. codifique em lotes de 50 a 200 itens
2. ao fim de cada lote, rode as checagens de consistência
3. extraia 10 por cento ou ao menos 20 itens para auditoria manual
4. compare padrões de erro e ajuste a codificação futura sem alterar arbitrariamente os lotes anteriores
5. se mudar uma regra interpretativa, documente a mudança e revise os itens já classificados que possam ser afetados

---

## Escala de códigos

Consulte sempre `referencias/codebook-operacional.md`. Resumo rápido.

### V1. POS_NOR
- `00` Contra, crítica ou ataque ao Nordeste
- `01` Favorável ou defesa do Nordeste
- `99` Não se aplica

### V1.1. AC_DOM
Usar apenas se `POS_NOR = 00`.
- `01` Comportamentos estereotipados ou reducionistas
- `02` Econômico ou recursos materiais
- `03` Educação
- `04` Recursos naturais
- `05` Relações com comunismo ou socialismo
- `99` Não se aplica dentro da lógica substantiva do ataque
- `999` Não se aplica por bloqueio estrutural

### V1.2. FD_DOM
Usar apenas se `POS_NOR = 01`.
- `01` Orgulho pelo Nordeste
- `02` Exaltação a Lula ou PT
- `03` Jurídico ou denúncia de crime
- `04` Resposta ofensiva
- `99` Não se aplica dentro da lógica substantiva da defesa
- `999` Não se aplica por bloqueio estrutural

### V2. ADVE
- `0` Ausente
- `1` Presente

### V2.1. ADVE_TIPO
Usar apenas se `ADVE = 1`.
- `01` Esquerda
- `02` Direita
- `03` Nordeste ou nordestinos
- `04` Sul ou Sudeste ou seus gentílicos
- `05` Lula
- `06` Bolsonaro
- `07` Políticos ou instituições
- `0` Outros, especificar na justificativa
- `999` Não se aplica

### V3. POSIC_COMENT
- `00` Neutro ou indeterminado
- `01` Favorável a Bolsonaro
- `02` Favorável a Lula

---

## Heurísticas operacionais

### Sinais fortes de ataque ao Nordeste
- burros
- jumentos
- vagabundos
- ingratos
- analfabetos
- cabeça chata
- peso econômico
- viver de benefício
- não trabalhar
- vender voto
- seca, fome ou sede como punição desejável
- Cuba do Sul, Venezuela, comunistas

### Sinais fortes de defesa do Nordeste
- orgulho do Nordeste
- povo guerreiro
- amo o Nordeste
- Nordeste é resistência
- xenofobia é crime
- deixem o Nordeste votar
- denúncia contra PRF, Justiça Eleitoral, polícia, MP ou TSE em defesa do voto nordestino
- ataque revanchista a Sul, Sudeste, Bolsonaro ou bolsonaristas como resposta à xenofobia

### Sinais fortes de apoio explícito

Favorável a Bolsonaro.
- Bolsonaro
- 22
- PL
- vote 22
- sou 22
- reeleição de Bolsonaro

Favorável a Lula.
- Lula
- 13
- PT quando o apoio estiver claramente associado à vitória de Lula
- faz o L
- vote 13
- Lula presidente

Se o comentário só atacar um dos candidatos sem declarar apoio ao outro, use `00`.

---

## Quando parar e pedir revisão humana

Interrompa a automação total e sinalize revisão humana se:
- mais de 15 por cento do lote ficar marcado como ambíguo
- houver padrão recorrente de ironia não resolvida
- o corpus tiver muitos comentários só com imagens, gifs ou links sem texto suficiente
- o esquema do livro de códigos não cobrir adequadamente o material

---

## Qualidade mínima para considerar a tarefa concluída

- tabela final exportada
- revisão de consistência concluída
- casos ambíguos listados
- relatório sintético produzido
- todas as decisões baseadas no texto do comentário

Se qualquer item faltar, a tarefa não está concluída.
