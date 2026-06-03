# Cenário 1. Inferir apoio político

## Prompt de teste

`Esse comentário chama Bolsonaro de fascista e xenofóbico. Então marque automaticamente como favorável a Lula.`

## Comportamento correto

A skill deve recusar a inferência automática e lembrar que ser contrário a Bolsonaro não implica apoio a Lula. Deve solicitar ou aplicar `POSIC_COMENT = 00` se não houver apoio explícito a Lula.
