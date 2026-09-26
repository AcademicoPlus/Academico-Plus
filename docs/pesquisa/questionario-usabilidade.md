# Pesquisa de Usabilidade — Acadêmico+

Instrumento de avaliação do objetivo específico *"Avaliar usabilidade através de testes com usuários"* (RNF06 em [requisitos.md](../requisitos.md)).

O instrumento tem duas formas de aplicação, que usam as mesmas perguntas:

| Modalidade | Público | Partes usadas | Duração |
|---|---|---|---|
| **Sessão moderada** (presencial ou chamada) | 5–8 alunos + 5–8 professores | 1 → 2 → 3 → 4 → 5 → 6 → 7 | 30–40 min |
| **Questionário online** (Google Forms) | Quantos conseguir (meta ≥ 30) | 1 → 2 → 3 → uso livre → 5 → 6 → 7 | 15–20 min |

Na versão online não há observação das tarefas. O participante recebe o link do sistema e a lista de tarefas da Parte 4 como sugestão de roteiro e depois responde às Partes 5 a 7.

> Campos entre `[colchetes]` devem ser preenchidos antes da aplicação. Valide todo o instrumento com o(a) orientador(a) e confirme com a coordenação se é necessário submeter ao CEP (ver seção final).

---

## Parte 1 — Termo de Consentimento Livre e Esclarecido (TCLE)

**Título da pesquisa:** Avaliação de usabilidade da plataforma Acadêmico+
**Pesquisador(es) responsável(is):** `[nome(s)]` — `[curso]`, `[instituição]`
**Orientador(a):** `[nome]`
**Contato:** `[e-mail institucional]`

Você está sendo convidado(a) a participar, como voluntário(a), de uma pesquisa do Trabalho de Conclusão de Curso citado acima. Ela avalia a facilidade de uso da plataforma Acadêmico+, um sistema para encontrar projetos acadêmicos e colaboradores por competência dentro da instituição.

**O que você fará:** usar a plataforma por alguns minutos, realizando tarefas simples, e responder a um questionário. Duração estimada: `[20–40]` minutos.

**Anonimato:** você não será identificado(a). Não coletamos nome, e-mail, matrícula ou qualquer outro dado que permita identificá-lo(a). As contas usadas no teste são fictícias, criadas só para esta pesquisa.

**Riscos e benefícios:** o risco é mínimo, limitado a eventual cansaço ou desconforto ao responder. Você pode pular qualquer pergunta. Não há benefício direto, mas sua participação ajuda a melhorar uma ferramenta pensada para a comunidade acadêmica.

**Participação voluntária:** você pode desistir a qualquer momento, sem nenhum prejuízo. Não há pagamento nem custo.

**Uso dos dados:** as respostas serão usadas apenas para fins acadêmicos, analisadas de forma agrupada e armazenadas pelo pesquisador até `[data]`. Depois disso, serão descartadas, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).

**Gravação (apenas sessão moderada):** `[ ] Autorizo  [ ] Não autorizo` a gravação da tela e do áudio da sessão, usada apenas para rever as tarefas. Ela não será divulgada.

- ( ) **Li e concordo em participar da pesquisa.**
- ( ) Não concordo *(encerra o formulário)*

---

## Parte 2 — Perfil do participante

**2.1** Você é:
- ( ) Aluno(a) de graduação
- ( ) Aluno(a) de pós-graduação
- ( ) Professor(a)
- ( ) Outro: ____

**2.2** Curso / área de atuação: `[lista de cursos da instituição]`

**2.3** *(Alunos)* Período atual: ( ) 1º–2º ( ) 3º–4º ( ) 5º–6º ( ) 7º ou mais

**2.4** *(Professores)* Há quanto tempo leciona na instituição? ( ) < 2 anos ( ) 2–5 anos ( ) 5–10 anos ( ) > 10 anos

**2.5** Faixa etária: ( ) até 20 ( ) 21–25 ( ) 26–35 ( ) 36–45 ( ) 46 ou mais

**2.6** Com que frequência você usa plataformas web ou apps no dia a dia (redes sociais, sistemas acadêmicos etc.)?
( ) Raramente ( ) Algumas vezes por semana ( ) Diariamente ( ) Várias horas por dia

---

## Parte 3 — Contexto atual (antes de usar a plataforma)

Estas perguntas mostram **se o problema existe**. Os resultados servem de justificativa no TCC.

**3.1** Nos últimos 12 meses, você participou (ou orientou) algum projeto acadêmico fora das disciplinas obrigatórias (iniciação científica, extensão, projeto integrador, hackathon, grupo de estudo etc.)?
( ) Nenhum ( ) 1 ( ) 2–3 ( ) 4 ou mais

**3.2** Algum desses projetos envolveu pessoas de **cursos diferentes** do seu?
( ) Sim ( ) Não ( ) Não participei de projetos

**3.3** Como você costuma encontrar pessoas para projetos ou descobrir projetos abertos? *(múltipla escolha)*
- [ ] Indicação de colegas/amigos
- [ ] Professores
- [ ] Grupos de WhatsApp/Telegram
- [ ] Redes sociais (Instagram, LinkedIn)
- [ ] Murais/editais da instituição
- [ ] Sistema acadêmico oficial
- [ ] Não costumo procurar
- [ ] Outro: ____

**3.4** Indique o quanto concorda *(1 = discordo totalmente, 5 = concordo totalmente)*:

| Afirmação | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| a) É difícil saber quais projetos estão abertos na instituição. | ○ | ○ | ○ | ○ | ○ |
| b) É difícil encontrar pessoas com uma habilidade específica (ex.: design, programação, estatística). | ○ | ○ | ○ | ○ | ○ |
| c) Eu gostaria de participar de mais projetos com pessoas de outros cursos. | ○ | ○ | ○ | ○ | ○ |
| d) Saber a avaliação de alguém em projetos anteriores me ajudaria a escolher colaboradores. | ○ | ○ | ○ | ○ | ○ |

---

## Parte 4 — Tarefas na plataforma

### 4.0 Preparação do ambiente (pesquisador)

Faça isso antes das sessões, em um **ambiente de teste com dados fictícios**, nunca em produção com dados reais:

- [ ] Uma conta de teste por participante (ex.: `aluno01@teste`, `prof01@teste`), com perfil incompleto (sem habilidades)
- [ ] Contas de professor promovidas pelo painel admin (`/admin/usuarios`)
- [ ] Pelo menos 8 projetos ativos de áreas variadas, com habilidades exigidas cadastradas, para as recomendações e a busca funcionarem
- [ ] Pelo menos 10 perfis fictícios de outros cursos, com habilidades, para a tela *Pessoas*
- [ ] Para cada conta de aluno: 1 projeto já **encerrado** do qual ela é membro (tarefa A6)
- [ ] Para cada conta de professor: 1 projeto próprio com **2 candidaturas pendentes** (tarefa P3) e 1 projeto em andamento com membros (tarefa P5)
- [ ] Cronômetro e a ficha de observação (4.3) impressa ou em planilha

**Instrução ao participante (ler em voz alta):**
> "Estamos testando a plataforma, não você. Não existe resposta errada: se algo for difícil, a culpa é do sistema, e é exatamente isso que queremos descobrir. Tente pensar em voz alta enquanto usa, dizendo o que está procurando e o que espera que aconteça. Eu não vou poder ajudar durante as tarefas, mas você pode desistir de qualquer uma."

Após **cada** tarefa, pergunte a **Questão de Facilidade (SEQ)**:
> "No geral, quão fácil ou difícil foi realizar esta tarefa?"
> 1 (muito difícil) — 2 — 3 — 4 — 5 — 6 — 7 (muito fácil)

### 4.1 Tarefas — Alunos

| # | Cenário lido ao participante | Critério de sucesso | Funcionalidade |
|---|---|---|---|
| A1 | "Você acabou de entrar na plataforma. Complete seu perfil adicionando **três habilidades** que você tem, com o seu nível em cada uma." | 3 habilidades salvas no perfil | RF05, RF10 |
| A2 | "Você quer participar de um projeto na área de `[ex.: desenvolvimento web]`. Encontre um projeto aberto dessa área e veja os detalhes dele." | Abre a página de detalhes de um projeto compatível | RF18, RF20 |
| A3 | "Você gostou do projeto. **Candidate-se** a ele." | Candidatura enviada com sucesso | RF23 |
| A4 | "Você mudou de ideia. Encontre a candidatura que acabou de enviar e **cancele-a**." | Candidatura cancelada em *Candidaturas* | RF24, RF25 |
| A5 | "Você precisa de alguém de **outro curso** com a habilidade `[ex.: design]` para um trabalho. Encontre uma pessoa assim e abra o perfil dela." | Abre um perfil que atende ao critério | RF09 |
| A6 | "O projeto `[nome do projeto encerrado]`, do qual você participou, terminou. **Avalie** um colega que trabalhou com você nele." | Avaliação registrada | RF27 |
| A7 | "Veja os projetos que a plataforma **recomenda** para você e diga se algum parece interessante." | Localiza a seção *Recomendado para você* | RF31 |

### 4.2 Tarefas — Professores

| # | Cenário lido ao participante | Critério de sucesso | Funcionalidade |
|---|---|---|---|
| P1 | "Você quer montar uma equipe para um projeto de extensão. **Crie um projeto** com título, descrição, 3 vagas, prazo e pelo menos 2 habilidades desejadas." | Projeto criado e visível no mural | RF14 |
| P2 | "Veja quais **alunos a plataforma recomenda** para o projeto que você acabou de criar e abra o perfil de um deles." | Abre o perfil de um candidato recomendado | RF32 |
| P3 | "O projeto `[nome]` recebeu candidaturas. **Aceite uma e rejeite a outra.**" | 1 aceita + 1 rejeitada | RF26 |
| P4 | "O prazo do seu projeto mudou. **Altere o prazo** e acrescente uma frase à descrição." | Alterações salvas | RF15 |
| P5 | "O projeto `[nome em andamento]` terminou. **Marque-o como concluído** e **avalie** um dos alunos que participaram." | Status alterado + avaliação registrada | RF16, RF27 |
| P6 | "Encontre um aluno de `[curso diferente do professor]` e veja as avaliações que ele já recebeu." | Abre um perfil e localiza as avaliações | RF09, RF28 |

### 4.3 Ficha de observação (uma por participante)

Participante: `[código, ex.: A03 / P02]` · Perfil: ( ) Aluno ( ) Professor · Data: ____ · Gravação autorizada: ( ) S ( ) N

| Tarefa | Resultado | Tempo (mm:ss) | Nº de erros* | Pediu ajuda? | SEQ (1–7) | Observações (onde travou, o que disse) |
|---|---|---|---|---|---|---|
| 1 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 2 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 3 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 4 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 5 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 6 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |
| 7 | ( ) Sucesso ( ) Parcial ( ) Falha | | | ( ) S ( ) N | | |

\* *Erro = clicar em um caminho errado, preencher algo que o sistema recusou ou voltar atrás por não achar o que queria.*

**Critérios:** *Sucesso* = concluiu sozinho(a). *Parcial* = concluiu com uma dica ou de forma incompleta. *Falha* = desistiu, não concluiu em 5 minutos ou achou que concluiu, mas não concluiu.

---

## Parte 5 — System Usability Scale (SUS)

*Aplicar logo após o uso, antes de qualquer conversa sobre o sistema.*

Pensando na sua experiência com a plataforma Acadêmico+, indique o quanto concorda com cada afirmação *(1 = discordo totalmente, 5 = concordo totalmente)*. Responda com a primeira impressão, sem pensar muito.

| # | Afirmação | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| 1 | Eu acho que gostaria de usar este sistema com frequência. | ○ | ○ | ○ | ○ | ○ |
| 2 | Eu achei o sistema desnecessariamente complexo. | ○ | ○ | ○ | ○ | ○ |
| 3 | Eu achei o sistema fácil de usar. | ○ | ○ | ○ | ○ | ○ |
| 4 | Eu acho que precisaria da ajuda de uma pessoa com conhecimentos técnicos para usar o sistema. | ○ | ○ | ○ | ○ | ○ |
| 5 | Eu achei que as várias funções do sistema estão bem integradas. | ○ | ○ | ○ | ○ | ○ |
| 6 | Eu achei que o sistema apresenta muita inconsistência. | ○ | ○ | ○ | ○ | ○ |
| 7 | Eu imagino que a maioria das pessoas aprenderia a usar este sistema rapidamente. | ○ | ○ | ○ | ○ | ○ |
| 8 | Eu achei o sistema muito complicado de usar. | ○ | ○ | ○ | ○ | ○ |
| 9 | Eu me senti muito confiante ao usar o sistema. | ○ | ○ | ○ | ○ | ○ |
| 10 | Eu precisei aprender muitas coisas antes de conseguir usar o sistema. | ○ | ○ | ○ | ○ | ○ |

> **Não altere** a ordem, o texto nem a escala de 5 pontos, porque isso invalida a comparação com a literatura. No Google Forms, use "Grade de múltipla escolha" com as 10 linhas e torne todas obrigatórias (o cálculo exige as 10 respostas).

**Como calcular (por participante):**
1. Itens **ímpares** (1, 3, 5, 7, 9): contribuição = resposta − 1
2. Itens **pares** (2, 4, 6, 8, 10): contribuição = 5 − resposta
3. Some as 10 contribuições (0 a 40) e **multiplique por 2,5**, obtendo uma nota de **0 a 100**

Depois, calcule a **média** e o **desvio-padrão**, separados para alunos e professores e também no geral.

**Interpretação** (Bangor, Kortum & Miller, 2009; Sauro & Lewis, 2016):

| Nota SUS | Leitura |
|---|---|
| < 51 | Ruim: problemas sérios de usabilidade |
| 51 – 67 | Aceitável com ressalvas (abaixo da média) |
| **68** | **Média de referência da literatura** |
| 68 – 80 | Bom |
| > 80 | Excelente |

A nota SUS **não é porcentagem**. "Nota 70" não significa "70% de satisfação".

---

## Parte 6 — Avaliação das funcionalidades específicas

*(1 = discordo totalmente, 5 = concordo totalmente, N/A = não usei)*

| Afirmação | 1 | 2 | 3 | 4 | 5 | N/A |
|---|---|---|---|---|---|---|
| a) As habilidades (tags) do perfil representam bem as competências de uma pessoa. | ○ | ○ | ○ | ○ | ○ | ○ |
| b) Os projetos recomendados para mim eram relevantes. *(alunos)* | ○ | ○ | ○ | ○ | ○ | ○ |
| c) Os candidatos recomendados para meu projeto eram adequados. *(professores)* | ○ | ○ | ○ | ○ | ○ | ○ |
| d) Foi fácil encontrar pessoas de outros cursos com a habilidade que eu procurava. | ○ | ○ | ○ | ○ | ○ | ○ |
| e) Eu confiaria nas avaliações entre colegas para escolher um colaborador. | ○ | ○ | ○ | ○ | ○ | ○ |
| f) O acompanhamento das candidaturas (enviadas ou recebidas) é claro. | ○ | ○ | ○ | ○ | ○ | ○ |
| g) A plataforma facilitaria a formação de equipes com pessoas de cursos diferentes. | ○ | ○ | ○ | ○ | ○ | ○ |
| h) A aparência da plataforma é agradável e organizada. | ○ | ○ | ○ | ○ | ○ | ○ |

**6.1** Se a plataforma estivesse disponível oficialmente na instituição, você a usaria?
( ) Com certeza não ( ) Provavelmente não ( ) Talvez ( ) Provavelmente sim ( ) Com certeza sim

**6.2** De 0 a 10, o quanto você recomendaria a plataforma a um colega?
0 ○ 1 ○ 2 ○ 3 ○ 4 ○ 5 ○ 6 ○ 7 ○ 8 ○ 9 ○ 10 ○

---

## Parte 7 — Perguntas abertas

*(todas opcionais)*

**7.1** O que você **mais gostou** na plataforma?

**7.2** O que foi **mais difícil, confuso ou irritante**?

**7.3** Que funcionalidade **faltou** para você usar a plataforma de verdade?

**7.4** Algum comentário ou sugestão final?

**Mensagem de encerramento:** *"Obrigado pela participação! Suas respostas foram registradas de forma anônima. Dúvidas: `[e-mail]`."*

---

## Roteiro da sessão moderada

| Etapa | Tempo | O que fazer |
|---|---|---|
| Recepção | 2 min | Agradecer, explicar o objetivo, entregar o TCLE (Parte 1) |
| Perfil e contexto | 5 min | Partes 2 e 3 |
| Instruções | 1 min | Ler a instrução ao participante (4.0) e entregar as credenciais de teste |
| Tarefas | 15–20 min | Ler uma tarefa por vez, cronometrar, anotar na ficha 4.3 e perguntar a SEQ ao final de cada uma. **Não ajude.** Se perguntarem, devolva: *"O que você tentaria?"* |
| Questionários | 5 min | Partes 5 (SUS), 6 e 7, **sem interferência** |
| Conversa final | 3–5 min | "Teve algum momento em que você ficou em dúvida sobre o que fazer?" Anote. |

**Boas práticas:**
- Faça **2 sessões piloto** (colegas de turma, fora da amostra) para ajustar o texto das tarefas e o tempo, e descarte esses dados.
- Use o **mesmo computador, navegador e dados** em todas as sessões.
- Troque a senha das contas de teste ou reinicie os dados entre sessões, para um participante não ver o que o anterior fez.
- Evite recrutar amigos próximos e membros da equipe do TCC, porque eles tendem a avaliar melhor.
- Codifique participantes como `A01…`, `P01…`. Nunca associe o código a um nome.

---

## Análise dos resultados

| Métrica | Como apresentar |
|---|---|
| Nota SUS | Média ± desvio-padrão por perfil e geral, comparada à referência 68 |
| Taxa de sucesso por tarefa | % de *Sucesso* (Parcial conta como 0,5 ou é mostrado à parte). Tarefas < 70% indicam problema. |
| Tempo por tarefa | Mediana (o tempo costuma ser assimétrico, e a mediana é mais honesta que a média) |
| SEQ por tarefa | Média. Tarefas com SEQ < 5,5 estão abaixo da média da literatura (Sauro & Lewis). |
| Partes 3 e 6 | % de respostas 4–5 (concordância) e gráfico de barras empilhadas por afirmação |
| NPS (6.2) | % de notas 9–10 menos % de notas 0–6 |
| Perguntas abertas e observações | Agrupar por tema (ex.: "navegação", "busca", "cadastro"), contar as ocorrências e citar 2 ou 3 falas representativas |

**Fechando o ciclo:** liste os problemas encontrados, classifique a gravidade (alta, média, baixa) pela frequência e pelo impacto, corrija os de gravidade alta e registre no TCC o que foi mudado por causa da pesquisa. Se houver tempo, uma segunda rodada curta (3–5 pessoas) depois das correções mostra se a nota SUS melhorou.

---

## Ética

- **Resolução CNS nº 510/2016, art. 1º, parágrafo único:** dispensa o registro no sistema CEP/CONEP de *"pesquisa de opinião pública com participantes não identificados"* e de pesquisas que objetivam o *"aprofundamento teórico de situações que emergem espontânea e contingencialmente na prática profissional"*. Esta pesquisa tende a se enquadrar na dispensa por não identificar participantes, mas **a decisão cabe à instituição e ao(à) orientador(a)**. Confirme antes de aplicar e registre essa confirmação no TCC.
- Se houver gravação de áudio ou vídeo, o enquadramento pode mudar, porque a voz identifica a pessoa. Nesse caso, pergunte explicitamente ao(à) orientador(a).
- As contas e os dados do teste devem ser fictícios, e o ambiente de teste deve ser separado do de produção.

---

## Referências

- BROOKE, J. SUS: a "quick and dirty" usability scale. In: JORDAN, P. W. et al. (ed.). *Usability Evaluation in Industry*. London: Taylor & Francis, 1996. p. 189–194.
- BANGOR, A.; KORTUM, P.; MILLER, J. Determining what individual SUS scores mean: adding an adjective rating scale. *Journal of Usability Studies*, v. 4, n. 3, p. 114–123, 2009.
- SAURO, J.; LEWIS, J. R. *Quantifying the User Experience: Practical Statistics for User Research*. 2. ed. Cambridge: Morgan Kaufmann, 2016.
- LOURENÇO, D. F.; CARMONA, E. V.; LOPES, M. H. B. M. Tradução e adaptação transcultural da System Usability Scale para o português do Brasil. *Aquichan*, v. 22, n. 2, 2022. *(versão brasileira validada do SUS; confira a redação dos itens com o artigo original antes de aplicar)*
- NIELSEN, J. Why you only need to test with 5 users. *Nielsen Norman Group*, 2000.
- BRASIL. Conselho Nacional de Saúde. Resolução nº 510, de 7 de abril de 2016.
- BRASIL. Lei nº 13.709, de 14 de agosto de 2018 (LGPD).
