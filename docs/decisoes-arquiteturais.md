# Decisões arquiteturais — Academico+

Registro curto (formato ADR: Contexto / Decisão / Consequências) das decisões técnicas reais
já tomadas no backend, para servir de base à seção de metodologia/resultados do TCC2. Cada
entrada documenta um trade-off consciente, não uma limitação acidental.

## 1. Autenticação stateless via JWT, sem refresh token

**Contexto**: a API precisa de autenticação sem estado no servidor (facilita escalar
horizontalmente e simplifica o deploy de um MVP acadêmico).

**Decisão**: um único access token JWT, emitido no login, com expiração configurável
(`JWT_EXPIRATION`, padrão 24h). Não existe fluxo de refresh token.

**Consequências**: menos complexidade de implementação e de superfície de ataque (não há
refresh token para roubar). O custo é que o usuário precisa logar novamente a cada expiração —
aceitável para uma expiração de 24h. Logout é resolvido com uma blacklist de tokens revogados
(`tokens_revogados`), limpa por um job diário (`TokenCleanupScheduler`). Se a expiração for
reduzida no futuro (ex.: para 15 min, padrão mais seguro), essa decisão deve ser revisitada.

## 2. Recomendação por cobertura ponderada dos requisitos, não Jaccard puro

**Contexto**: ranquear projetos/candidatos por compatibilidade de habilidades. O índice de
Jaccard clássico (`|interseção| / |união|`) é a escolha "óbvia" de manual de recomendação.

**Decisão**: usar cobertura ponderada dos requisitos do projeto — soma dos pesos das
habilidades exigidas que a outra parte possui, dividida pela soma de todos os pesos exigidos;
habilidades obrigatórias pesam o dobro das desejáveis.

**Consequências**: o Jaccard simétrico penaliza quem tem habilidades extras não relacionadas ao
projeto, o que não responde à pergunta real ("esse candidato atende o que eu preciso?"). A
cobertura ponderada resolve isso, mas deixa de ser uma métrica de similaridade simétrica — não
é intercambiável com Jaccard em outros contextos, e essa escolha deve ser justificada
explicitamente na banca como uma adaptação deliberada, não um erro de implementação do índice
clássico.

## 3. Moderação de avaliação via denúncia + soft-delete, não hard-delete

**Contexto**: uma avaliação entre colaboradores pode ser abusiva, falsa ou fora do propósito.
Precisa existir um mecanismo de remoção, mas remover a linha do banco quebraria a referência da
própria denúncia que motivou a remoção (FK de `denuncias_avaliacao.avaliacao_id`).

**Decisão**: denúncia julgada procedente marca `Avaliacao.removida = true` (soft-delete) — a
linha continua no banco para histórico/auditoria, mas some da listagem pública e do cálculo de
`notaMedia`/`totalAvaliacoes`.

**Consequências**: preserva histórico para auditoria e evita o problema de integridade
referencial. Em compensação, toda leitura de avaliações precisa lembrar de filtrar
`removida = false` manualmente (não há `@SQLRestriction` nessa entidade, diferente de
`Usuario`) — um ponto de atenção para qualquer nova query futura sobre `avaliacoes`.

## 4. Exclusão lógica de conta (LGPD), não exclusão física

**Contexto**: a LGPD garante ao titular o direito de eliminação dos seus dados, mas apagar a
linha do usuário quebraria a integridade referencial de projetos, candidaturas e avaliações
ligadas a ele.

**Decisão**: `DELETE /usuarios/me` marca `ativo = false` e `deletado_em = now()`
(soft-delete), mediante confirmação de senha. A entidade `Usuario` tem
`@SQLRestriction("ativo = true")`, então uma conta excluída desaparece automaticamente de toda
consulta JPA padrão — inclusive do próprio `JwtAuthenticationFilter`, o que invalida qualquer
token ainda válido dessa conta sem precisar de uma blacklist explícita para esse caso.

**Consequências**: atende ao espírito da LGPD (o dado para de ser processado/exibido) sem
comprometer a integridade do histórico de projetos/avaliações de terceiros. É uma decisão de
"anonimização por invisibilidade", não de eliminação física — se a interpretação jurídica exigir
apagamento físico dos dados pessoais, essa decisão precisaria ser revisitada (ex.: expurgo
posterior por um job separado).

## 5. Rate limiting em dois níveis, estado em memória

**Contexto**: proteger a API contra força bruta de login/OTP e contra flood genérico, sem
depender de infraestrutura externa (Redis) num MVP acadêmico com uma única instância.

**Decisão**: dois filtros independentes — `RateLimiterService` (por conta, 5 tentativas falhas
em 15 min → bloqueio de 15 min, compartilhado entre login e validação de OTP) e
`GlobalRateLimitFilter` (por IP, 120 requisições/min, roda antes de tudo, inclusive antes do
JWT). Ambos guardam estado em `ConcurrentHashMap` na própria instância da aplicação.

**Consequências**: simples de implementar e zero dependência externa. O custo explícito é que
não escala horizontalmente — se a aplicação um dia rodar em múltiplas instâncias, cada instância
teria seu próprio contador, efetivamente multiplicando o limite real pelo número de instâncias.
Migrar para um contador compartilhado (Redis) é o caminho natural nesse cenário.

## 6. Flyway como fonte real do schema, `ddl-auto=validate` (resolvido em 2026-08-21)

**Contexto**: o projeto usa Flyway para versionar o schema, mas o arquivo de migração inicial
usava uma convenção de nome (`V1_schema_inicial.sql`, underscore simples) que o Flyway rejeita —
por isso `spring.jpa.hibernate.ddl-auto=update` era, na prática, quem mantinha o schema
atualizado, incluindo tabelas inteiras (`avaliacoes`, `denuncias_avaliacao`) que nunca tinham
ido para uma migração Flyway.

**Decisão**: o nome do arquivo foi corrigido (`V1__schema_inicial.sql`) e as migrações que
faltavam foram adicionadas (V2: `avaliacoes`/`denuncias_avaliacao`/`projeto_cancelamento_aviso`
e colunas de reputação/LGPD em `usuarios`; V3/V4: seeds de cursos/habilidades/níveis de acesso).
Uma varredura entidade-por-entidade contra as 4 migrações não encontrou nenhuma divergência que
bloquearia `ddl-auto=validate` — a única lacuna real encontrada foi `usuarios.ativo`,
`cursos.ativo` e `projetos.ativo` sem `NOT NULL` no banco, embora as entidades JPA exijam
`nullable = false` (um NULL aí escaparia do `@SQLRestriction("ativo = true")` da entidade
`Usuario`, ver item 4). V5 corrige isso (backfill + `SET NOT NULL`), e `ddl-auto` virou
`validate`. Validado tanto contra um Postgres local só-migração (V1→V5 do zero) quanto contra o
schema real de produção (via `AcademicoBackendApplicationTests`, antes de esse teste ser isolado
com H2 — ver nota abaixo).

**Consequências**: o schema agora tem uma fonte de verdade única e versionada; `validate` só
verifica existência de tabela/coluna e compatibilidade de tipo (não nullability, defaults,
tamanho de VARCHAR ou índices — então drift nesses aspectos ainda é possível e não quebra o
boot). Achado colateral corrigido nesse trabalho: `AcademicoBackendApplicationTests` era um
`@SpringBootTest` sem override de propriedades, o que o fazia herdar a datasource de produção
importada de `.env` — ou seja, um simples `mvn test` local rodava Flyway e `ddl-auto` (antes
`update`, agora `validate`) contra o Postgres de produção. Isolado com H2 dedicado; os únicos
testes que hoje tocam Postgres real são os `@Tag("e2e")`, contra o container local do
`docker-compose.yml`, fora do `mvn test` padrão.

## 7. Upload de foto de perfil via serviço externo (Cloudflare R2), não disco local

**Contexto**: usuários podem enviar uma foto de perfil (`POST /usuarios/me/foto`). A primeira
implementação (`LocalStorageService`) gravava o arquivo no disco da própria instância do
monolito (`app.upload.dir=uploads/perfis`), servido como arquivo estático — registrada aqui
desde o início como pendência, por não sobreviver a um redeploy sem volume persistente nem
escalar para múltiplas instâncias.

**Decisão**: o upload foi extraído para o `academico-upload-service`, um microserviço próprio
que valida o arquivo (tipo MIME + assinatura real dos bytes) e envia pro Cloudflare R2 (storage
compatível com a API do S3, via AWS SDK v2). O monolito chama esse serviço por REST síncrono
(`FotoStorageClient`), autenticado por uma chave compartilhada no header `X-Internal-Api-Key` —
não via RabbitMQ, porque upload de arquivo binário grande não é um bom fit pra fila de
mensagens, e o fluxo é request/response por natureza (o frontend precisa da URL da foto de volta
na mesma resposta).

**Consequências**: resolve a pendência original — a foto sobrevive a redeploys e a múltiplas
instâncias do monolito, já que o armazenamento é externo. Em troca, introduz uma dependência de
rede síncrona (se o `academico-upload-service` cair, o upload de foto falha) e mais uma peça de
infraestrutura pra configurar e implantar (ver `academico-api/DEPLOY.md`). Ver decisão 9
para o racional mais amplo de por que essa e a extração do e-mail foram as duas partes
escolhidas para virar microserviço.

## 8. Estratégia de testes em camadas (unitário → slice → poucos e2e)

**Contexto**: cobrir a lógica de negócio do backend sem depender de Postgres real na maioria
dos testes (mais rápido, roda em qualquer máquina/CI sem serviço externo).

**Decisão**: testes de service em unidade pura (Mockito, sem contexto Spring); testes de
controller em slice (`@WebMvcTest`, services mockados, contexto de segurança via anotação
customizada `@WithUsuarioMock`); testes de repositório em `@DataJpaTest` com H2 em modo
`MODE=PostgreSQL`; um pequeno conjunto de testes ponta-a-ponta (`@SpringBootTest`, marcados
`@Tag("e2e")`) contra Postgres real, excluídos do `mvn test` padrão e rodados separadamente
via `mvn test -Pe2e` (local com `docker-compose up -d`, ou no CI com um serviço Postgres
dedicado). A exclusão por padrão é feita via `<properties>` (`surefire.excludedGroups`)
sobrescritas pelo profile Maven `e2e`, não via valor literal direto no `<configuration>` do
plugin nem via `-DexcludedGroups=`/`-Dgroups=` na linha de comando — um valor fixo no XML do
plugin tem precedência sobre a mesma propriedade passada por `-D`, então `-Dgroups=e2e`
sozinho não o sobrescreve (dá conjunto vazio: incluir e excluir "e2e" ao mesmo tempo = zero
testes). Overriding de `<properties>` por profile é a forma confiável de trocar esse
comportamento no Maven.

**Consequências**: a maioria dos testes roda em segundos, sem infraestrutura, o que também
viabiliza um pipeline de CI rápido. O custo é que H2 em modo `PostgreSQL` é uma aproximação, não
o banco real — por isso os testes e2e existem especificamente para os fluxos mais críticos, onde
uma divergência de comportamento entre H2 e Postgres real seria mais cara de não detectar.

## 9. Extração de e-mail e upload como microserviços, monolito para o resto

**Contexto**: o TCC pede uma arquitetura que sirva de material de estudo sobre microserviços,
mas o restante do domínio (usuários, projetos, candidaturas, avaliações, recomendação) tem forte
acoplamento transacional entre si — ex.: aceitar uma candidatura mexe em `Projeto` e
`ProjetoMembro` na mesma transação. Quebrar isso em serviços separados exigiria coordenação
distribuída (saga, transação de duas fases) sem nenhum benefício real para um MVP acadêmico de
instância única.

**Decisão**: extrair só as duas partes sem nenhuma dependência de repositório/entidade do
domínio principal e com um único ponto de chamada cada: `academico-email-service` (consome a
fila `email.codigo-verificacao` via RabbitMQ, publicada pelo monolito, e envia o OTP de
recuperação de senha pelo Resend) e `academico-upload-service` (ver decisão 7). O restante do
domínio continua no monolito (`academico-api`).

**Consequências**: ganho didático de demonstrar tanto comunicação assíncrona (fila + dead-letter
queue, sem retry automático) quanto síncrona (REST interno com chave compartilhada) sem pagar o
custo de distribuir o domínio transacional principal. Custo real: três peças de infraestrutura a
mais para rodar e implantar (RabbitMQ e os dois serviços — ver `academico-api/DEPLOY.md`), e
falha de rede vira um novo modo de falha que não existia quando tudo era uma chamada de método
local — mitigado pela DLQ no lado do e-mail (nada trava se o Resend cair) e pelo upload de foto
ser uma etapa isolada do cadastro/edição de perfil (uma falha ali não impede o resto do
formulário de ser salvo).
