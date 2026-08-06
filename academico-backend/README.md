# Academico+ API

API REST em Spring Boot para a plataforma Academico+ — cadastro de usuários, projetos acadêmicos, candidaturas e habilidades/competências.

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
   | `RESEND_API_KEY` | Opcional — sem ela, e-mails de recuperação de senha só são logados no console (modo simulação) |

   O `application.properties` carrega o `.env` automaticamente via `spring.config.import`.

3. Suba o banco local:
   ```bash
   docker-compose up -d
   ```
   Isso levanta o Postgres (porta `5432`) e o pgAdmin (porta `5050`, login `admin@academico.edu.br` / `admin`).

## Rodando

```bash
./mvnw spring-boot:run
```

As migrações do Flyway (`src/main/resources/db/migration`) rodam automaticamente na subida, criando o schema e populando os níveis de acesso padrão (`ALUNO`, `PROFESSOR`, `ADMIN`).

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

Login (`POST /auth/login`) retorna um JWT (`Bearer`). Ele carrega o ID do usuário e o nível de acesso (`ALUNO`, `PROFESSOR` ou `ADMIN`) no payload; endpoints administrativos usam `@PreAuthorize("hasRole('ADMIN')")`. Não há refresh token — o token de acesso dura `JWT_EXPIRATION` (ms) e precisa de novo login ao expirar. Logout (`POST /usuarios/logout`) invalida o token corrente numa blacklist, limpa automaticamente por um job diário.

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

A lista completa de rotas, parâmetros e schemas está no Swagger UI.

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

## Limitações conhecidas / fora do escopo atual

- **Sem refresh token**: avaliado como desnecessário para a duração atual do access token (24h por padrão); reconsiderar se o tempo de expiração for reduzido.
- **Sem algoritmo de recomendação** (similaridade entre perfis/projetos por habilidade) e **sem sistema de endosso de competências por professores** — ainda não implementados.
- **Upload de foto** salva em disco local (`LocalStorageService`); não há integração com storage externo (S3, etc.).
