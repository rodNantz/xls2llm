# Configuração do perfil JSON do projeto

O perfil JSON reúne a configuração metodológica usada na classificação: identificação e versão do projeto, instruções permanentes para o LLM, variáveis, domínios de códigos, representação de “não se aplica” e dependências entre variáveis.

O perfil padrão está em:

```text
src/main/resources/profiles/nordeste-eleicoes-2022/1.0.0.json
```

O tipo Java correspondente é `ProjectProfile` (`src/main/java/es/rodrigonant/p2ai/xls2llm/validation/ProjectProfile.java`). O JSON é lido na inicialização da aplicação.

## Selecionar um perfil

Por padrão, a aplicação carrega:

```text
classpath:profiles/nordeste-eleicoes-2022/1.0.0.json
```

Para selecionar outro arquivo, configure `xls2llm.profile` como um recurso Spring (`classpath:` ou `file:`). Por exemplo, no PowerShell:

```powershell
$env:XLS2LLM_PROFILE = 'file:C:/configuracao/meu-perfil/1.1.0.json'
```

Ou passe a propriedade diretamente ao iniciar a aplicação:

```powershell
java -jar target/xls2llm-0.0.3.jar `
  --xls2llm.profile=file:C:/configuracao/meu-perfil/1.1.0.json
```

Para manter o perfil dentro do artefato da aplicação, use um caminho `classpath:`, por exemplo:

```text
classpath:profiles/meu-projeto/1.1.0.json
```

Recomenda-se criar um novo arquivo para cada versão metodológica, em vez de substituir um perfil já usado em execuções anteriores. O perfil e sua versão são registrados na aba `XLS2LLM` do arquivo de saída.

## Estrutura do JSON

```json
{
  "projectId": "meu-projeto",
  "version": "1.1.0",
  "model": "gpt-4.1",
  "source": "caminho/para/o-codebook.md",
  "instructions": "Instruções metodológicas permanentes para o modelo.",
  "variables": [
    {
      "name": "V1",
      "evaluationType": "V1ALT",
      "codes": ["00", "01"],
      "notApplicable": true
    }
  ],
  "notApplicableAliases": ["99", "999"],
  "notApplicableOutput": {
    "V1": "99"
  },
  "dependencies": [
    {
      "whenVariable": "V1",
      "whenCode": "00",
      "requireVariable": "V1.1",
      "requiredSemantic": "CLASSIFIED"
    }
  ]
}
```

Os nomes dos campos e os valores de `evaluationType` e `requiredSemantic` diferenciam maiúsculas de minúsculas.

### Campos do perfil

| Campo | Tipo | Obrigatório | Uso |
|---|---|---:|---|
| `projectId` | texto | Sim | Identificador estável do projeto. É registrado nos metadados da execução. |
| `version` | texto | Sim | Versão do perfil metodológico, por exemplo `1.0.0`. É registrada nos metadados da execução. |
| `model` | texto | Não | Metadado descritivo no JSON. **Não seleciona o modelo usado na chamada.** O modelo em execução vem de `llm.model` ou `--llm.model`. |
| `source` | texto | Não | Referência documental do codebook. É informativa; o aplicativo não carrega nem incorpora automaticamente o arquivo indicado. |
| `instructions` | texto | Sim | Instruções estáveis do projeto, concatenadas às instruções da planilha e enviadas no contexto de sistema do LLM. |
| `variables` | lista de variáveis | Sim | Define os nomes, os tipos internos, os domínios aceitos e se cada variável admite o estado “não se aplica”. |
| `notApplicableAliases` | lista de textos | Sim | Representações numéricas que significam o mesmo estado semântico `NOT_APPLICABLE`, como `99` e `999`. |
| `notApplicableOutput` | objeto texto → texto | Sim | Representação que será escrita na planilha de saída para cada variável. A chave é o `name` da variável. |
| `dependencies` | lista de dependências | Sim | Regras condicionais entre variáveis, verificadas pelo validador local. Use `[]` quando não houver dependências. |

### Variáveis

Cada objeto em `variables` tem esta forma:

```json
{
  "name": "V1.1",
  "evaluationType": "V1_1ALT",
  "codes": ["01", "02", "03", "04", "05"],
  "notApplicable": true
}
```

- `name`: nome lógico usado nas dependências e em `notApplicableOutput`. Deve corresponder à variável e à coluna pretendidas.
- `evaluationType`: tipo interno usado para identificar a categoria retornada pelo LLM e mapeá-la à coluna. Deve ser um dos valores definidos por `CategoryType`: `V1ALT`, `V1_1ALT`, `V1_2ALT`, `V2ALT`, `V2_1ALT` ou `V3ALT`.
- `codes`: lista permitida de códigos ordinários. Configure todos como **strings**, por exemplo `"00"`; assim, zeros à esquerda não são removidos. O validador compara os códigos exatamente como strings.
- `notApplicable`: quando `true`, a variável também aceita qualquer código configurado em `notApplicableAliases`. Quando `false`, os aliases não são aceitos para essa variável.

O perfil padrão associa as variáveis desta forma:

| `name` | `evaluationType` | Domínio ordinário padrão | Aceita não aplicável |
|---|---|---|---:|
| `V1` | `V1ALT` | `00`, `01` | Sim |
| `V1.1` | `V1_1ALT` | `01`–`05` | Sim |
| `V1.2` | `V1_2ALT` | `01`–`04` | Sim |
| `V2` | `V2ALT` | `00`, `01` | Não |
| `V2.1` | `V2_1ALT` | `00`–`07` | Sim |
| `V3` | `V3ALT` | `00`–`02` | Não |

O domínio indicado por `codes` prevalece. Se uma categoria devolver valor fora do domínio, a classificação é preservada na planilha, marcada para revisão e contabilizada como falha de validação.

### Estado “não se aplica”

`notApplicableAliases` declara os códigos que têm o mesmo significado de negócio:

```json
"notApplicableAliases": ["99", "999"]
```

Na validação e nas dependências, `99` e `999` são tratados como o mesmo estado semântico `NOT_APPLICABLE`. Ao escrever o arquivo de saída, `notApplicableOutput` escolhe a representação persistida por variável:

```json
"notApplicableOutput": {
  "V1": "99",
  "V1.1": "999",
  "V1.2": "999",
  "V2.1": "999"
}
```

Se uma variável não estiver no objeto `notApplicableOutput`, o serializador mantém o alias recebido. Aliases são aceitos pelo domínio somente se a variável tiver `notApplicable: true`.

### Dependências

Cada dependência declara uma condição de origem e o estado semântico exigido na variável de destino:

```json
{
  "whenVariable": "V1",
  "whenCode": "00",
  "requireVariable": "V1.1",
  "requiredSemantic": "CLASSIFIED"
}
```

- `whenVariable`: `name` da variável cuja resposta aciona a regra.
- `whenCode`: código que aciona a regra. Se o código estiver em `notApplicableAliases`, o acionamento também compara o estado semântico `NOT_APPLICABLE`.
- `requireVariable`: `name` da variável cujo valor será verificado.
- `requiredSemantic`: deve ser `CLASSIFIED` ou `NOT_APPLICABLE`.
  - `CLASSIFIED`: o valor de destino deve pertencer a `codes` dessa variável e não pode ser um alias de não aplicável.
  - `NOT_APPLICABLE`: o valor de destino deve pertencer a `notApplicableAliases`.

Exemplo: `V1=00` exige que `V1.1` seja classificada e que `V1.2` seja não aplicável. Uma dependência não define códigos novos; os códigos aceitos continuam sendo os definidos na variável de destino.

## Instruções metodológicas e saída estruturada

`instructions` contém as regras permanentes do projeto. O texto é concatenado às instruções metodológicas presentes na planilha antes de enviar a chamada ao LLM. Use esse campo para regras que devem permanecer iguais entre planilhas e lotes; mantenha nele orientações concisas e estáveis.

O formato da resposta estruturada também exige `commentId`, `categories`, `justification`, `triggerSpans`, `ambiguous` e `reviewReason`. Esses campos pertencem ao contrato de resposta da aplicação; eles não são configurados pela lista `variables`.

O modelo efetivamente usado é definido separadamente:

```text
llm.model=gpt-4.1
```

Pode ser sobrescrito por `LLM_MODEL` ou por `--llm.model=gpt-5-mini` na linha de comando. O campo JSON `model` não substitui essa configuração.

## O que a aplicação valida

Por comentário, o validador local verifica:

1. presença de cada variável configurada;
2. ausência de variáveis duplicadas;
3. associação de cada categoria a um `evaluationType` conhecido no perfil;
4. domínio de códigos configurado e permissão do estado não aplicável;
5. dependências declaradas;
6. justificativa não vazia e lista `triggerSpans` presente;
7. sinalização de ambiguidade, que requer revisão humana.

Resultado fora do domínio ou com regra inconsistente não é corrigido silenciosamente: é preservado e destacado para revisão. Se faltar um `commentId` no retorno do LLM, as células de classificação da linha ficam vazias e vermelhas, com `MISSING_LLM_RESULT` na coluna de status. Essa reconciliação é baseada nos IDs da chamada, não em ordem de resposta.

## Recomendações ao editar

- Crie um arquivo novo para cada mudança metodológica e incremente `version`.
- Preserve códigos como strings: use `"00"`, não `0`.
- Garanta que cada `evaluationType` seja único e exista no enum `CategoryType`.
- Garanta que os nomes referenciados por dependências existam em `variables`.
- Configure todos os códigos de não aplicável em `notApplicableAliases` e escolha explicitamente o formato de saída de cada variável aplicável.
- Use JSON válido: aspas duplas em chaves e textos; sem comentários ou vírgula depois do último item.
- A aplicação desserializa o JSON no início, mas não executa uma validação de esquema completa do perfil. Revise tipos, nomes, enums, domínios e dependências antes de uma execução real.
