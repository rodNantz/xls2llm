# Suite de Testes

Esta suite verifica se a skill aplica o livro de códigos com disciplina classificatória e sem inferências indevidas.

## O que deve ser testado

- bloqueio da codificação quando a unidade de análise está errada
- distinção entre ataque, defesa e não aplicação
- uso correto de 999 por bloqueio estrutural
- recusa em inferir apoio político implícito
- encaminhamento de ambiguidades para revisão humana

## Cenários

1. `cenario-1-inferir-apoio-politico.md`
2. `cenario-2-duas-categorias-em-conflito.md`
3. `cenario-3-ataque-economico-vs-estereotipo.md`
4. `cenario-4-ironia-e-resposta-ofensiva.md`

## Critério de sucesso

Em todos os cenários, a skill deve:
- identificar a variável problemática
- justificar a decisão com base no texto
- aplicar a regra de predominância
- registrar ambiguidade quando necessário
