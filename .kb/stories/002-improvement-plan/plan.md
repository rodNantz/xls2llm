# Plano de Melhorias do xls2llm

## 1. Objetivo

Evoluir o xls2llm para que a stakeholder processe planilhas pela interface web, sem usar CLI, com qualidade próxima à da skill metodológica existente.

O sistema deve:

- preservar texto, IDs e rastreabilidade;
- usar contexto metodológico de forma eficiente;
- reduzir tokens, custo e tempo;
- validar resultados automaticamente;
- encaminhar ambiguidades e erros para revisão humana;
- registrar integralmente cada chamada de API;
- medir qualidade, custo e assertividade;
- adaptar-se a diferentes projetos sem alterar o código principal.

## 2. Situação atual

A branch `develop` já possui leitura e escrita de XLSX, integração com LLM, processamento em lotes, upload web, progresso e testes.

As principais limitações são:

- prompt dependente da planilha e sem perfil metodológico versionado;
- lotes fixos e sem orçamento explícito de tokens;
- IDs e correspondência entre entrada e saída ainda insuficientemente controlados;
- validação estrutural e lógica incompleta;
- processamento síncrono durante o upload;
- reabertura da planilha a cada lote;
- logs sem evento estruturado completo por chamada;
- ausência de métricas de qualidade e custo.

## 3. Arquitetura desejada

```text
Upload
  -> validar arquivo e configuração
  -> ler corpus e criar IDs estáveis
  -> carregar perfil do projeto
  -> montar prompt
  -> classificar em lotes
  -> validar respostas
  -> repetir apenas falhas
  -> aplicar pós-validação
  -> salvar XLSX e relatórios
  -> revisão humana, se necessária
  -> download
```

Cada execução deve possuir:

- `runId`: execução completa;
- `batchId`: lote;
- `requestId`: chamada à API;
- `commentId`: comentário individual.

## 4. Configuração por projeto

As regras devem ser configuração, não lógica fixa em Java. A planilha ou um perfil associado deve definir:

- variáveis, colunas e códigos;
- descrições do codebook;
- regras de dependência e bloqueio;
- critérios de ambiguidade;
- exemplos metodológicos;
- modelo e orçamento de tokens;
- obrigatoriedade de justificativa e trecho gatilho;
- ação para erros;
- percentual de auditoria;
- política de retenção dos logs.

### 4.1. Abas recomendadas

#### `Codebook`

| Variável | Código | Descrição |
|---|---|---|
| V1 | 00 | Contra |
| V1 | 01 | Favorável |
| V1 | 99 | Não se aplica |

#### `ValidationRules`

| Regra ID | Tipo | Variável | Condição ou expressão | Ação |
|---|---|---|---|---|
| R001 | domínio | V1 | `value in [00,01,99]` | vermelho/manual |
| R002 | domínio | V2 | `value in [0,1]` | vermelho/manual |
| R003 | bloqueio | V1.1 | `V1=99 -> V1.1=999` | vermelho/manual |

#### `ProjectConfig`

Deve definir nomes de colunas, linha inicial, coluna do texto, modelo, limites, lote, cor de erro e ações de falha.

Registrar em cada execução a versão e o hash da configuração usada. Exemplo:

```text
nordeste-eleicoes-2022@1.0.0
```

## 5. Prompt e uso do contexto

Separar o prompt em quatro camadas:

1. regras permanentes do sistema;
2. perfil metodológico e codebook;
3. instruções específicas da planilha;
4. dados do lote.

O perfil deve incluir unidade de análise, predominância, códigos válidos, bloqueios, não inferência política, justificativa curta, trechos gatilho, ambiguidade e exemplos.

Não solicitar cadeia de pensamento extensa. A auditoria deve usar justificativa curta, trechos gatilho, regra aplicada e resultado das validações.

### 5.1. Contexto remoto e OpenAI

As chamadas da API devem ser tratadas como independentes. O modelo não deve depender de uma conversa longa para manter memória entre lotes.

Usar três camadas de contexto:

1. **Prompt-base estável:** regras críticas, formato JSON, não inferência e bloqueios;
2. **Contexto recuperável:** codebook completo, documentação e exemplos;
3. **Dados variáveis:** os 10 comentários do lote.

O prompt-base deve permanecer idêntico e vir antes dos comentários. Isso permite aproveitar prompt caching quando disponível, reduzindo o custo dos tokens de entrada. O cache não substitui o armazenamento do contexto nem deve ser tratado como memória permanente.

Para documentos e exemplos, avaliar o uso da `Responses API` com `file_search` e um `vector store` por projeto. Alternativamente, implementar retrieval próprio com banco, JSONL ou índice vetorial. Em ambos os casos, recuperar somente as regras e exemplos relevantes para o lote.

Regras críticas, como `V1 = 99 -> V1.1 = 999 e V1.2 = 999`, devem estar no prompt-base e ser aplicadas também pelo validador local. Não depender exclusivamente do retrieval para regras estruturais.

O contexto recuperado e os exemplos utilizados devem ser registrados nos logs, junto com a versão e o hash do perfil. Não enviar novamente a planilha inteira, respostas anteriores ou logs completos.

Evolução recomendada:

1. estabilizar o prompt-base para caching;
2. versionar o perfil metodológico;
3. implementar validação local;
4. migrar a classificação para a `Responses API`, se necessário para usar ferramentas;
5. adicionar `file_search` ou retrieval próprio;
6. comparar custo e qualidade com e sem retrieval.

## 6. Corpus e lotes

Representar cada comentário com ID e linha de origem:

```json
{
  "comment_id": "row-000123",
  "source_row": 123,
  "text": "texto original"
}
```

Preservar o texto original. Uma eventual versão normalizada deve ser armazenada separadamente.

Detectar antes da chamada itens vazios, truncados, apenas URL, apenas emoji, duplicados e sem conteúdo suficiente.

### 6.1. Estratégia inicial de lotes

Para simplificar a primeira versão, usar lotes fixos de **10 comentários por chamada**, mantendo o comportamento atual da aplicação.

Essa escolha facilita:

- implementação e depuração;
- comparação com os testes existentes;
- auditoria de cada lote;
- identificação de itens ausentes ou duplicados;
- retry de grupos pequenos;
- controle inicial de custo e latência.

Mesmo com tamanho fixo, cada comentário deve possuir `commentId` e a resposta deve ser validada contra os IDs recebidos.

O tamanho 10 deve ser tratado como parâmetro configurável, não como regra permanente. Após a coleta de métricas, avaliar lotes adaptativos por orçamento de tokens, considerando contexto, comentários, resposta esperada, limite do modelo e margem de segurança.

Estratégia futura:

- comentários curtos: lotes maiores;
- comentários longos ou complexos: lotes menores;
- itens ambíguos ou inválidos: retry seletivo ou processamento individual.

Exemplo de fluxo:

```text
10 comentários
  -> validação
  -> itens válidos: aceitos
  -> itens inconsistentes: retry em lote menor
  -> itens ambíguos: revisão humana ou chamada individual
```

Exemplos few-shot devem ser selecionados e versionados, não incluídos indiscriminadamente.

Uma segunda passagem deve ser usada somente para respostas inválidas, itens ausentes ou duplicados, inconsistências, ambiguidades, baixa confiança ou conflitos entre regras.

## 7. Formato de saída

Retornar uma classificação por comentário, com IDs estáveis:

```json
{
  "comment_id": "row-000123",
  "codes": {
    "V1": "00",
    "V1.1": "02",
    "V1.2": "999",
    "V2": "1",
    "V2.1": "04",
    "V3": "00"
  },
  "justification": "Ataque com foco em dependência econômica.",
  "trigger_spans": ["vive de benefícios"],
  "ambiguous": false,
  "review_reason": null
}
```

Estados possíveis: `SUCCESS`, `INVALID_SCHEMA`, `MISSING_ITEM`, `DUPLICATE_ITEM`, `CONSISTENCY_ERROR`, `RETRY_REQUIRED`, `HUMAN_REVIEW` e `FAILED`.

## 8. Validação e pós-validação

O validador deve ser independente do LLM e verificar campos obrigatórios, IDs, cobertura, duplicidades, domínios, justificativa, trechos gatilho, dependências e regras de inferência.

### 8.1. Domínios atuais

| Coluna | Códigos válidos |
|---|---|
| V1 | `00`, `01`, `99` |
| V1.1 | `01`, `02`, `03`, `04`, `05`, `99` |
| V1.2 | `01`, `02`, `03`, `04`, `99` |
| V2 | `0`, `1` |
| V2.1 | `01`, `02`, `03`, `04`, `05`, `06`, `07`, `999`, `0` |
| V3 | `00`, `01`, `02` |

Qualquer outro valor deve:

- ser preservado;
- receber fonte vermelha no XLSX;
- ser registrado com linha, coluna e regra violada;
- marcar a linha como `REVISAO_MANUAL`;
- impedir conclusão automática sem revisão.

A cor vermelha indica erro; não é uma correção silenciosa.

### 8.2. Bloqueio de `V1`

Classificar `V1` primeiro. Se:

```text
V1 = 99
```

então:

```text
V1.1 = 999
V1.2 = 999
```

V1.1 e V1.2 não são relevantes nesse caso. Qualquer valor diferente de `999` é inconsistência. Preservar o valor bruto e registrar o valor esperado.

### 8.3. Dependências padrão

- `V1 = 00` exige V1.1 válido e `V1.2 = 999`;
- `V1 = 01` exige V1.2 válido e `V1.1 = 999`;
- `V1 = 99` exige `V1.1 = 999` e `V1.2 = 999`;
- `V2 = 0` exige `V2.1 = 999`;
- `V2 = 1` exige V2.1 diferente de `999`.

Prioridade das regras:

```text
1. domínio do código
2. bloqueio estrutural
3. dependência entre variáveis
4. regra semântica
5. revisão humana
```

Status finais: `APROVADO`, `APROVADO_COM_ALERTAS` ou `REVISAO_MANUAL`.

## 9. Amostra manual e avaliação

### 9.1. Planilha-base de testes manuais

A planilha `xls2llm/src/main/resources/xls/test1-simple.xlsx` é a referência principal para testes manuais do fluxo completo. Ela deve ser preservada como fixture estável e usada para verificar:

- leitura da planilha;
- extração do prompt e dos comentários;
- divisão em lotes de 10 itens;
- chamadas ao LLM;
- progresso da execução;
- validação das respostas;
- escrita do XLSX de saída;
- preservação das linhas e colunas;
- download pela interface web.

Alterações nessa planilha devem ser evitadas. Se for necessário mudar seu conteúdo, criar uma nova versão ou fixture, registrar a motivação e atualizar os testes afetados.

### 9.2. Amostra dourada

O arquivo `xls2llm/src/main/resources/xls/amostra-1.xlsx`, preenchido manualmente com dez linhas, deve ser a primeira amostra dourada (`gold sample`).

Usos:

- testar leitura e escrita;
- verificar a preservação de textos, IDs e colunas;
- testar `V1 = 99` e outras regras;
- criar exemplos few-shot;
- comparar prompts e modelos;
- detectar regressões;
- medir tokens, custo e divergências.

As dez linhas não constituem dados suficientes para fine-tuning ou reinforcement learning. Inicialmente, devem ser usadas como referência supervisionada e teste de regressão. A `amostra-1.xlsx` complementa, mas não substitui, a `test1-simple.xlsx` nos testes manuais do fluxo.

Fluxo recomendado:

1. manter as decisões humanas como gabarito;
2. comparar prompt sem exemplos, prompt metodológico e prompt com few-shot;
3. registrar divergências e correções;
4. atualizar regras somente com justificativa;
5. preservar a amostra fora dos ajustes finais quando usada como teste.

Para uma amostra maior, metas práticas:

- 50–100 linhas: ajuste inicial de prompt e regras;
- 100–200 linhas: referência few-shot inicial;
- 300–500 linhas: avaliação mais confiável;
- 500–1.000 linhas: primeira base robusta para o projeto;
- 1.000–3.000 linhas: possível fine-tuning experimental.

O conjunto deve ser diversificado, conter casos ambíguos e ter pelo menos 30–50 exemplos para cada código relevante. Recomenda-se revisão por uma segunda pessoa em 10%–20% dos itens.

## 10. Métricas de qualidade e custo

Criar conjuntos de desenvolvimento, validação e teste, com gabarito humano.

Medir por variável:

- acurácia, precisão, recall e macro-F1;
- matriz de confusão;
- taxa de ambiguidade e revisão humana;
- taxa de `99` e `999`;
- divergências em relação ao gabarito.

Medir também:

- percentual de linhas processadas e válidas;
- linhas ausentes, duplicadas ou corrigidas;
- tokens de entrada, saída e totais;
- número de chamadas e retries;
- custo total e custo da segunda passagem;
- latência e throughput.

Indicadores principais:

```text
tokens por classificação correta
custo por classificação correta
macro-F1 / custo total
```

Comparar diferentes prompts, modelos, lotes, exemplos e estratégias de uma ou duas passagens.

## 11. Observabilidade

Registrar cada chamada integralmente em `api-calls.jsonl`, incluindo:

- timestamp, `runId`, `batchId` e `requestId`;
- provedor, endpoint, modelo e parâmetros;
- system prompt e user prompt completos;
- IDs dos comentários e hashes dos prompts;
- resposta completa e status;
- tokens, custo, duração, tentativa e erro.

Arquivos recomendados:

```text
logs/
  application.log
  api-calls.jsonl
  classifications.jsonl
  quality.jsonl
  errors.log
```

Como os logs conterão comentários e prompts integrais, definir retenção, acesso, proteção, mascaramento opcional e conformidade com a LGPD.

## 12. Execução assíncrona e interface

Substituir o processamento longo dentro de `/upload` por jobs:

```text
POST /runs
GET /runs/{runId}
GET /runs/{runId}/progress
GET /runs/{runId}/quality
GET /runs/{runId}/download
```

Estados: `RECEIVED`, `VALIDATING`, `READING`, `PROCESSING`, `VALIDATING_RESULTS`, `WAITING_HUMAN_REVIEW`, `COMPLETED`, `FAILED` e `CANCELLED`.

A interface deve oferecer:

- upload e seleção do perfil;
- estimativa de custo;
- progresso e status;
- download da planilha;
- relatório de qualidade e custo;
- fila de revisão humana.

A stakeholder deve poder aceitar, corrigir ou marcar um item como inconclusivo. Correções humanas devem gerar novo registro sem apagar o resultado original.

## 13. Eficiência de XLSX

Evitar abrir e salvar o workbook a cada lote. Preferir carregar uma vez, processar, atualizar e salvar ao final. Para arquivos grandes, avaliar streaming.

Manter o mapeamento:

```text
commentId -> sourceRow -> outputRow
```

Antes de escrever, conferir quantidade, identidade, ausência e duplicidade dos itens de entrada e saída.

## 14. Fases de implementação

### Fase A — segurança metodológica

1. Criar perfil versionado baseado na skill.
2. Criar corpus interno com IDs estáveis.
3. Separar contexto permanente e dados.
4. Estabilizar o prompt-base para prompt caching.
5. Implementar saída por comentário.
6. Implementar validador de domínios e dependências.
7. Aplicar pós-validação visual no XLSX.

### Fase B — avaliação e qualidade

1. Formalizar `amostra-1.xlsx` como amostra dourada.
2. Criar gabarito e conjuntos de avaliação.
3. Comparar prompts sem exemplos, metodológicos e few-shot.
4. Medir qualidade, tokens e custo.
5. Implementar segunda passagem seletiva.

### Fase C — observabilidade

1. Criar IDs de execução, lote e chamada.
2. Registrar request e response integrais em JSONL.
3. Registrar tokens, custo, latência e erros.
4. Registrar contexto recuperado, exemplos e hashes usados.
5. Criar relatório por execução.
6. Definir retenção e proteção.

### Fase D — eficiência operacional

1. Calcular lotes por orçamento de tokens.
2. Reutilizar contexto e exemplos.
3. Ler e escrever a planilha uma única vez.
4. Implementar retries com backoff.
5. Reprocessar apenas lotes inválidos.
6. Paralelizar com limite de concorrência.

### Fase E — experiência da stakeholder

1. Tornar o processamento assíncrono.
2. Criar página de status e estimativa de custo.
3. Disponibilizar relatórios.
4. Criar interface de revisão humana.
5. Permitir download do XLSX e dos artefatos de auditoria.

## 15. Critérios de aceite

- toda execução possui `runId` e todo comentário possui ID estável;
- nenhum item desaparece ou é duplicado sem registro;
- respostas inválidas são rejeitadas ou reprocessadas;
- domínios e dependências são validados automaticamente;
- valores inválidos ficam vermelhos no XLSX;
- `V1 = 99` força V1.1 e V1.2 para `999` e gera registro de validação;
- prompts e respostas integrais são armazenados;
- tokens, custo, qualidade e latência são medidos;
- `test1-simple.xlsx` é usada como fixture principal dos testes manuais;
- `amostra-1.xlsx` funciona como teste de regressão;
- casos ambíguos chegam à revisão humana;
- a stakeholder executa o fluxo pela interface web;
- o texto original é preservado;
- regras podem ser alteradas por projeto sem modificar o código principal.

## 16. Prioridade

1. Perfil metodológico e configuração por projeto.
2. IDs estáveis, saída estruturada e validação.
3. Regra `V1 = 99` e pós-validação visual.
4. Amostra dourada e métricas de qualidade/custo.
5. Logs integrais e observabilidade.
6. Retries e lotes por tokens.
7. Leitura/escrita eficiente de XLSX.
8. Processamento assíncrono.
9. Interface de revisão humana.

O ganho de qualidade deve vir da combinação de contexto metodológico, estrutura de dados, exemplos controlados, validação independente, retries seletivos e auditoria mensurável, e não apenas do aumento do tamanho dos prompts.
