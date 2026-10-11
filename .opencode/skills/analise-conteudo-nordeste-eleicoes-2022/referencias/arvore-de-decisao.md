# Árvore de Decisão

Siga nesta ordem para cada comentário.

1. O comentário expressa ataque ao Nordeste ou a nordestinos.
   - Se sim, `POS_NOR = 00`.
   - Depois classifique `AC_DOM`.
   - Defina `FD_DOM = 999`.

2. Se não for ataque, o comentário defende ou exalta o Nordeste ou reage aos ataques xenofóbicos.
   - Se sim, `POS_NOR = 01`.
   - Depois classifique `FD_DOM`.
   - Defina `AC_DOM = 999`.

3. Se não for possível identificar ataque nem defesa, `POS_NOR = 99`.
   - Defina `AC_DOM = 999`.
   - Defina `FD_DOM = 999`.

4. Há adversário ou inimigo comum.
   - Se não, `ADVE = 0` e `ADVE_TIPO = 999`.
   - Se sim, `ADVE = 1` e classifique o tipo predominante.

5. Há apoio explícito a Bolsonaro ou Lula.
   - Se apoio explícito a Bolsonaro, `POSIC_COMENT = 01`.
   - Se apoio explícito a Lula, `POSIC_COMENT = 02`.
   - Caso contrário, `POSIC_COMENT = 00`.

## Perguntas auxiliares

### Para `POS_NOR`
- O Nordeste aparece como alvo inferiorizado.
- O comentário responde a xenofobia defendendo a região.
- O texto só cita a região sem valoração.

### Para `AC_DOM`
- O foco é comportamento, moralidade ou venda de voto.
- O foco é economia, pobreza ou benefícios sociais.
- O foco é burrice, analfabetismo ou inteligência.
- O foco é seca, fome, sede, água, sertão.
- O foco é comunismo, socialismo, Cuba ou Venezuela.

### Para `FD_DOM`
- O foco é orgulho regional.
- O foco é Lula ou PT como defensores do Nordeste.
- O foco é denúncia jurídica ou institucional.
- O foco é resposta ofensiva revanchista.

### Para `ADVE_TIPO`
- Quem é o alvo predominante do comentário.
- Se houver mais de um alvo, escolha o mais central.

### Para `POSIC_COMENT`
- Existe declaração positiva explícita a Lula ou Bolsonaro.
- Existe apenas rejeição ao adversário.
- Há número 13 ou 22 com claro sentido eleitoral.
