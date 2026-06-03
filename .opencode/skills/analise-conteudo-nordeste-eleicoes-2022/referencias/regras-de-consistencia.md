# Regras de Consistência

## Regras lógicas obrigatórias

1. Se `POS_NOR = 00`, então `AC_DOM` deve ser `01`, `02`, `03`, `04`, `05` ou `99`, e `FD_DOM = 999`.
2. Se `POS_NOR = 01`, então `FD_DOM` deve ser `01`, `02`, `03`, `04` ou `99`, e `AC_DOM = 999`.
3. Se `POS_NOR = 99`, então `AC_DOM = 999` e `FD_DOM = 999`.
4. Se `ADVE = 0`, então `ADVE_TIPO = 999`.
5. Se `ADVE = 1`, então `ADVE_TIPO` não pode ser `999`.
6. Se `POSIC_COMENT = 01`, reavalie casos em que `ADVE_TIPO = 06`.
7. Se `POSIC_COMENT = 02`, reavalie casos em que `ADVE_TIPO = 05`.
8. Ausência de apoio explícito implica `POSIC_COMENT = 00`.
9. Toda linha deve ter `JUSTIFICATIVA_CURTA`.
10. Toda linha deve ter `TRECHOS_GATILHO`.

## Sinais de alerta para revisão humana

- ironia ou sarcasmo sem pistas suficientes
- comentário fragmentado
- excesso de emojis ou links sem texto substantivo
- coexistência forte entre duas subcategorias
- ambiguidade entre orgulho regional e exaltação a Lula
- ambiguidade entre resposta ofensiva e simples ataque a Sudeste sem referência defensiva ao Nordeste

## Auditoria por amostragem

Revisar manualmente:
- ao menos 10 por cento do lote
- todos os casos marcados com `AMBIGUO_REVISAO = SIM`
- todos os casos com `ADVE_TIPO = 0`
- todos os casos com `POS_NOR = 99`

## Boas práticas

- conservar um log das mudanças de regra
- revisar lotes anteriores se a interpretação dominante mudar
- nunca apagar a classificação original sem registrar a correção
