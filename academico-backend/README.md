# Academico+ API

API REST em Spring Boot para a plataforma Academico+ — cadastro de usuários, projetos acadêmicos, candidaturas e habilidades/competências.

Cadastro de usuário exige aceite dos [Termos de Uso](../legal/TERMOS_DE_USO.md) e da [Política de Privacidade](../legal/POLITICA_DE_PRIVACIDADE.md) (LGPD).

## Stack

- **Java 21** + **Spring Boot 3.5**
- **PostgreSQL** com **Flyway** para migrações
- **Spring Security** + **JWT** (`jjwt`) para autenticação stateless
- **Spring Data JPA** (Hibernate)
- **springdoc-openapi** (Swagger UI)
- **Lombok** + **MapStruct**
- **JUnit 5**, **AssertJ**, `spring-security-test`

## Pré-requisitos

- JDK 21+
- Docker (para subir Postgres + pgAdmin) ou uma instância PostgreSQL própria

## Configuração

1. Copie o arquivo de exemplo e preencha os valores:
   ```bash
   cp .env.example .env
   ```
2. Variáveis obrigatórias em `.env`:

   | Variável | Descrição |
   |---|---|
   | `SPRING_DATASOURCE_URL` | URL JDBC do Postgres (ex.: `jdbc:postgresql://localhost:5432/academico_plus`) |
   | `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciais do banco |
   | `JWT_SECRET` | Chave usada para assinar os tokens — **não existe valor padrão**, a aplicação falha ao subir se não estiver definida |
   | `JWT_EXPIRATION` | Duração do access token, em **milissegundos** |
   | `SERVER_PORT` / `SERVER_SERVLET_CONTEXT_PATH` | Porta e context path (padrão do projeto: `8080` / `/api`) |
   | `CORS_ALLOWED_ORIGINS` | Origens liberadas para o frontend |
   | `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` / `VHOST` | Broker usado para publicar o código de recuperação de senha — consumido pelo [`academico-email-service`](../academico-email-service) |
   | `UPLOAD_SERVICE_URL` / `UPLOAD_SERVICE_API_KEY` | Endereço e chave compartilhada do [`academico-upload-service`](../academico-upload-service) (upload de foto de perfil) |

   O `application.properties` carrega o `.env` automaticamente via `spring.config.import`.

3. Suba a infra local:
   ```bash
   docker-compose up -d
   ```
   Isso levanta o Postgres (porta `5432`), o pgAdmin (porta `5050`, login `admin@academico.edu.br` / `admin`) e o RabbitMQ (porta `5672`, painel em `http://localhost:15672`, login `guest`/`guest`).

   O fluxo de recuperação de senha e o upload de foto de perfil só funcionam de ponta a ponta com o `academico-email-service` e o `academico-upload-service` também rodando — veja o README de cada um.

## Rodando

```bash
./mvnw spring-boot:run
```

O schema é criado/atualizado automaticamente na subida via `spring.jpa.hibernate.ddl-auto=update` (ver observação sobre o Flyway em "Limitações conhecidas"), incluindo os níveis de acesso padrão (`ALUNO`, `PROFESSOR`, `ADMIN`), inseridos pelo script em `src/main/resources/db/migration`.

Com a configuração padrão, a API fica em `http://localhost:8080/api`.

## Documentação da API

Com a aplicação no ar:

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

## Testes

```bash
./mvnw test
```

## Autenticação

Login (`POST /auth/login`) retorna um JWT (`Bearer`). Ele carrega o ID do usuário e o nível de acesso (`ALUNO`, `PROFESSOR` ou `ADMIN`) no payload; endpoints administrativos usam `@PreAuthorize("hasRole('ADMIN')")`. Não há refresh token — o token de acesso dura `JWT_EXPIRATION` (ms) e precisa de novo login ao expirar. Logout (`POST /usuarios/logout`) invalida o token corrente numa blacklist, limpa automaticamente por um job diário. Cadastro (`POST /auth/cadastro`) exige `aceitouTermos: true`; a data do aceite fica registrada em `Usuario.termosAceitosEm`.

Um usuário pode excluir a própria conta a qualquer momento (`DELETE /usuarios/me`, mediante confirmação de senha) — exclusão lógica, direito do titular sob a LGPD.

## Rate limiting

Dois níveis independentes:

- **Por conta**: `login` e `validarOtp` (recuperação de senha) bloqueiam por 15 min após 5 tentativas falhas em 15 min, por e-mail (`RateLimiterService`).
- **Geral, por IP**: toda a API aceita no máximo `app.rate-limit.requisicoes-por-minuto` requisições por minuto por IP (padrão: 120; configurável, não exige variável de ambiente). Acima disso, `429 Too Many Requests` (`GlobalRateLimitFilter`).

Ambos guardam o estado em memória (instância única). Se a aplicação escalar horizontalmente, isso precisa virar um contador compartilhado (Redis).

## Recursos e endpoints

| Recurso | Base | Descrição |
|---|---|---|
| Auth | `/auth` | Login, cadastro, recuperação de senha (OTP por e-mail) |
| Usuários | `/usuarios` | Perfil próprio, busca de perfis públicos, administração |
| Habilidades do usuário | `/usuarios/me/habilidades` | Tags de competência do usuário autenticado |
| Cursos | `/cursos` | Listagem pública (usada no cadastro) |
| Níveis de acesso | `/niveis-acesso` | Listagem (admin) |
| Catálogo de habilidades | `/habilidades` | CRUD do catálogo (escrita restrita a admin) |
| Projetos | `/projetos` | CRUD de projetos, listagem/busca, meus projetos, projetos em que participo |
| Membros do projeto | `/projetos/{projetoId}/membros` | Equipe do projeto |
| Habilidades do projeto | `/projetos/{projetoId}/habilidades` | Requisitos de competência do projeto |
| Candidaturas | `/candidaturas` | Candidatar-se, aceitar/rejeitar, cancelar |
| Avaliações | `/projetos/{projetoId}/avaliacoes`, `/usuarios/{id}/avaliacoes` | Avaliação entre quem participou do mesmo projeto |
| Denúncias | `/avaliacoes/{avaliacaoId}/denuncias`, `/denuncias` | Denúncia de avaliação abusiva pelo avaliado; resolução por admin |
| Recomendações | `/recomendacoes` | Ranking de projetos/candidatos por compatibilidade de habilidades |

A lista completa de rotas, parâmetros e schemas está no Swagger UI.

## Avaliação de colaboradores

Depois que um projeto chega a um estado final (`CONCLUIDO` ou `CANCELADO`), quem participou dele — criador ou membro — pode avaliar outros participantes: nota de 1 a 5 e comentário opcional, uma avaliação por par por projeto. Não dá para avaliar a si mesmo, nem alguém que não participou, nem repetir a avaliação da mesma pessoa no mesmo projeto.

A nota média (`notaMedia`) e o total (`totalAvaliacoes`) ficam armazenados no perfil do usuário e são recalculados a cada nova avaliação. Como já aparecem em `UsuarioResumoResponse`, esse sinal de confiança surge automaticamente em qualquer lugar que já retorna um usuário resumido — card de projeto, membro da equipe, candidatura.

- `POST /projetos/{projetoId}/avaliacoes`
- `GET /usuarios/{id}/avaliacoes`

### Denúncia de avaliação

A pessoa avaliada — só ela — pode denunciar uma avaliação que considere abusiva, falsa ou fora do propósito de avaliar a colaboração (`POST /avaliacoes/{avaliacaoId}/denuncias`, motivo obrigatório). Um admin analisa (`GET /denuncias`, `PATCH /denuncias/{id}/resolver?procedente=true|false`):

- **Procedente**: a avaliação é marcada como removida (`Avaliacao.removida`, soft-delete) — some da listagem pública e do cálculo da nota média, mas a linha e a denúncia continuam no banco para histórico/auditoria. Apagar de verdade quebraria a referência da própria denúncia à avaliação.
- **Improcedente**: só arquiva a denúncia; a avaliação permanece como estava.

## Recomendação por habilidades

Ranqueia projetos e candidatos por compatibilidade de habilidades. Não é o Índice de Jaccard "puro" (`|interseção| / |união|`): usamos **cobertura ponderada dos requisitos do projeto**, porque o Jaccard simétrico penaliza quem tem habilidades extras não relacionadas ao projeto — o que não faz sentido para responder "esse candidato atende o que eu preciso?". A fórmula: soma dos pesos das habilidades exigidas que a outra parte possui, dividida pela soma de todos os pesos exigidos; habilidades marcadas como obrigatórias pesam o dobro das desejáveis.

- `GET /recomendacoes/projetos?limite=10` — projetos `ABERTO` ranqueados para o usuário logado, a partir das próprias habilidades
- `GET /recomendacoes/projetos/{projetoId}/candidatos?limite=10` — candidatos ranqueados para o criador do projeto (só ele enxerga)

O cálculo é feito em memória (carrega os candidatos elegíveis e pontua um a um), não em SQL — no volume de dados de uma plataforma acadêmica isso é mais simples e evita repetir o problema de paginar com fetch join de coleção (ver `ProjetoRepository.findByAtivoTrueAndStatus`, que só é seguro por não ser paginado).

## Estrutura do código

```
src/main/java/com/unifapi/academico/
├── controller/    # Endpoints REST
├── service/       # Regras de negócio
├── repository/    # Spring Data JPA
├── domain/
│   ├── models/    # Entidades JPA
│   └── enums/     # Enums de domínio
├── dto/           # Records de request/response, por recurso
├── security/      # JWT provider e filtro de autenticação
├── config/        # Security, OpenAPI
├── exception/     # Exceções de domínio + handler global
└── scheduler/     # Jobs agendados (limpeza de tokens revogados)
```

## Antes de lançar em produção — pendências que não são código

- **CAPTCHA no cadastro** (ex.: hCaptcha, reCAPTCHA) — não implementado porque exige criar conta num provedor externo e gerar chaves. Sem isso, criação de conta em massa por bot é trivial mesmo com o rate limit atual.
- **Observabilidade** (ex.: Sentry) — hoje só há log em arquivo/console; sem monitoramento de erro, só se sabe que algo quebrou se um usuário reclamar. Também exige conta num provedor externo.
- **HTTPS/TLS** — a API em si não força nada; quem deve terminar TLS é o proxy reverso/PaaS de deploy.
- **Backup do banco** — nenhuma estratégia automatizada de backup existe hoje; é decisão de infraestrutura do ambiente de produção.
- **Verificação de e-mail e/ou domínio institucional no cadastro** — hoje `POST /auth/cadastro` aceita qualquer e-mail com formato válido, sem confirmar posse nem restringir a domínios da instituição. Adiado propositalmente: o plano é usar a matrícula do aluno como identificador institucional no lugar disso.

## Limitações conhecidas / fora do escopo atual

- **Sem refresh token**: avaliado como desnecessário para a duração atual do access token (24h por padrão); reconsiderar se o tempo de expiração for reduzido.
- **Sem endosso de competências por professores** — diferente do sistema de avaliação entre colegas de projeto (esse já existe), não há um fluxo onde um professor valida formalmente uma habilidade de um aluno.
- **Sem sistema de mensagens** entre usuários.
- **Upload de foto**: delegado ao `academico-upload-service` via REST síncrono, que envia pro Cloudflare R2 — ver README daquele serviço.
- **Flyway não é a fonte real do schema**: o único arquivo de migração (`V1_schema_inicial.sql`) usa uma convenção de nome que o Flyway rejeita (log mostra "não segue a convenção de nome" na subida). Quem efetivamente mantém o schema atualizado é `spring.jpa.hibernate.ddl-auto=update`. Ao adicionar coluna `NOT NULL` em tabela já populada, lembrar de usar `columnDefinition` com `DEFAULT` (ver `Usuario.totalAvaliacoes`, `Usuario.termosAceitosEm`, `Avaliacao.removida`), senão o ALTER falha silenciosamente contra um banco com dados.
