# Diagramas técnicos — Academico+

Diagramas de apoio ao TCC2, em Mermaid (renderiza nativamente no GitHub). Para colar no
documento do TCC, abra este arquivo no GitHub/VS Code e exporte a imagem renderizada de
cada bloco (ou use uma extensão de export Mermaid → PNG/SVG).

## Integrantes do grupo

- **Carlos Eduardo Viana Alves**
- **Jefferson Mateus Lima Silva**
- **Luana Maria Alves Caldas**
- **Maria Eduarda Viana Nascimento**
- **Vitor Manoel da Silva Costa**

**Orientador:** Professor(a) Pedro Alex

**Instituição:** Centro Universitário do Piauí (UNIFAPI)
**Curso:** Bacharelado em Ciência da Computação
**Data:** Junho de 2026

## Tema e escopo do projeto

**Tema:** Academico+ — Plataforma Digital para Fomento à Colaboração Interdisciplinar e
Networking Acadêmico.

O projeto nasce de um problema comum em instituições de ensino: alunos com competências
técnicas complementares (programação, design, gestão, redação, etc.) frequentemente não têm
como se descobrir mutuamente para formar equipes em projetos acadêmicos, iniciativas de
extensão ou trabalhos multidisciplinares. As formas atuais de busca — grupos de WhatsApp,
murais físicos, indicação boca a boca — não são pesquisáveis por habilidade nem guardam
histórico de colaboração. O Academico+ propõe resolver isso com uma plataforma web (MVP) que
mapeia competências dos alunos em forma de tags e centraliza projetos e oportunidades dentro
de uma mesma instituição, permitindo buscar pessoas e projetos por afinidade de habilidades.

O escopo do MVP está organizado em quatro fases funcionais:

1. **Autenticação & Perfis** — cadastro e login com JWT, perfis de usuário com tags de
   competências (`/usuarios/me/habilidades`) e upload de foto de perfil.
2. **Mural & Busca** — CRUD de projetos, candidaturas e equipe, com busca de projetos por
   texto e status.
3. **Recomendação** — ranking de projetos e candidatos por compatibilidade de habilidades
   (adaptação do Índice de Jaccard, com cobertura ponderada dos requisitos do projeto — ver
   README do backend para o detalhamento do algoritmo), incluindo sugestão de candidatos para
   o criador de um projeto.
4. **Reputação & Networking** — sistema de avaliação entre colegas que colaboraram no mesmo
   projeto (nota de 1 a 5 com comentário, liberado após o encerramento do projeto), denúncia
   de avaliação abusiva com resolução por administrador, e exclusão da própria conta em
   conformidade com a LGPD.

Ficam fora do escopo atual, por decisão consciente do grupo para manter o MVP viável dentro
do prazo do TCC: endosso de competências por professores, sistema de mensagens entre
usuários e diferenciação de permissões entre os papéis Aluno e Professor (hoje idênticas no
código). Arquiteturalmente, o sistema é um monolito em Spring Boot, com apenas duas partes de
menor acoplamento — envio de e-mail e upload de fotos — extraídas como microserviços por
razões didáticas (ver [decisão 9](./decisoes-arquiteturais.md#9-extração-de-e-mail-e-upload-como-microserviços-monolito-para-o-resto)).

## 1. Diagrama Entidade-Relacionamento

Construído a partir das migrações Flyway em
`academico-backend/src/main/resources/db/migration/` (schema versionado, `ddl-auto=validate` —
ver [decisões arquiteturais](./decisoes-arquiteturais.md), item 6) mais as entidades JPA para os
campos e relacionamentos completos.

```mermaid
erDiagram
    NIVEIS_ACESSO {
        uuid id PK
        varchar nome
        varchar descricao
    }
    CURSOS {
        uuid id PK
        varchar nome
        varchar sigla
        boolean ativo
    }
    USUARIOS {
        uuid id PK
        varchar email
        varchar nome
        varchar senha_hash
        double nota_media
        integer total_avaliacoes
        boolean ativo
        timestamp deletado_em
        uuid nivel_acesso_id FK
        uuid curso_id FK
    }
    HABILIDADES {
        uuid id PK
        varchar nome
        varchar categoria
        integer usuarios_count
        integer projetos_count
    }
    USUARIO_HABILIDADES {
        uuid id PK
        uuid usuario_id FK
        uuid habilidade_id FK
        varchar nivel
    }
    PROJETOS {
        uuid id PK
        uuid criador_id FK
        varchar titulo
        text descricao
        varchar status
        integer vagas
        integer vagas_preenchidas
        boolean ativo
    }
    PROJETO_HABILIDADES {
        uuid id PK
        uuid projeto_id FK
        uuid habilidade_id FK
        boolean obrigatoria
    }
    PROJETO_MEMBROS {
        uuid id PK
        uuid projeto_id FK
        uuid usuario_id FK
        varchar funcao
    }
    CANDIDATURAS {
        uuid id PK
        uuid projeto_id FK
        uuid usuario_id FK
        varchar status
        text mensagem
        text motivo_rejeicao
    }
    AVALIACOES {
        uuid id PK
        uuid projeto_id FK
        uuid avaliador_id FK
        uuid avaliado_id FK
        integer nota
        text comentario
        boolean removida
    }
    DENUNCIAS_AVALIACAO {
        uuid id PK
        uuid avaliacao_id FK
        uuid denunciante_id FK
        uuid analisado_por_id FK
        text motivo
        varchar status
    }
    PROJETO_CANCELAMENTO_AVISO {
        uuid id PK
        uuid projeto_id FK
        uuid usuario_id FK
        varchar projeto_nome
        date projeto_data
        timestamp visualizado_em
    }
    TOKENS_REVOGADOS {
        varchar token PK
        timestamp data_revogacao
        timestamp data_expiracao
    }

    NIVEIS_ACESSO ||--o{ USUARIOS : classifica
    CURSOS ||--o{ USUARIOS : "matricula (opcional)"
    USUARIOS ||--o{ USUARIO_HABILIDADES : possui
    HABILIDADES ||--o{ USUARIO_HABILIDADES : "e listada em"
    USUARIOS ||--o{ PROJETOS : cria
    PROJETOS ||--o{ PROJETO_HABILIDADES : requer
    HABILIDADES ||--o{ PROJETO_HABILIDADES : "e exigida em"
    PROJETOS ||--o{ PROJETO_MEMBROS : tem
    USUARIOS ||--o{ PROJETO_MEMBROS : "participa de"
    PROJETOS ||--o{ CANDIDATURAS : recebe
    USUARIOS ||--o{ CANDIDATURAS : envia
    PROJETOS ||--o{ AVALIACOES : "e avaliado em"
    USUARIOS ||--o{ AVALIACOES : "avalia (avaliador)"
    USUARIOS ||--o{ AVALIACOES : "e avaliado (avaliado)"
    AVALIACOES ||--o{ DENUNCIAS_AVALIACAO : "pode ser denunciada em"
    USUARIOS ||--o{ DENUNCIAS_AVALIACAO : "denuncia (denunciante)"
    USUARIOS ||--o{ DENUNCIAS_AVALIACAO : "resolve (admin)"
    PROJETOS ||--o{ PROJETO_CANCELAMENTO_AVISO : "gera aviso em"
    USUARIOS ||--o{ PROJETO_CANCELAMENTO_AVISO : recebe
```

## 2. Diagrama de casos de uso

Mermaid não tem notação UML de caso de uso nativa — a representação abaixo usa um grafo de
atores conectados às funcionalidades, agrupadas pela mesma área usada na tabela de endpoints
do README do backend. **Observação importante**: hoje `Aluno` e `Professor` têm exatamente as
mesmas permissões no código (nenhum endpoint distingue os dois papéis) — o papel `Professor`
existe no seed do banco (`niveis_acesso`) mas ainda não tem nenhum caso de uso próprio
implementado (ex.: endosso de competências, mencionado como fora do escopo atual no README do
backend). Isso está representado abaixo pelos dois atores levando às mesmas funcionalidades.

```mermaid
graph LR
    Aluno([Aluno])
    Professor([Professor])
    Admin([Administrador])

    subgraph Autenticacao["Autenticação"]
        UC01[Cadastrar-se]
        UC02[Fazer login]
        UC03[Recuperar senha por OTP]
        UC04[Encerrar sessão]
    end

    subgraph Perfil["Perfil e habilidades"]
        UC05[Editar perfil]
        UC06[Trocar senha]
        UC07[Enviar foto de perfil]
        UC08[Excluir a própria conta - LGPD]
        UC09[Explorar perfis públicos]
        UC10[Gerenciar habilidades do próprio perfil]
    end

    subgraph Projetos["Projetos"]
        UC11[Criar projeto]
        UC12[Editar projeto]
        UC13[Alterar status do projeto]
        UC14[Encerrar projeto]
        UC15[Buscar / listar projetos]
        UC16[Gerenciar habilidades exigidas do projeto]
        UC17[Gerenciar membros do projeto]
    end

    subgraph Candidaturas["Candidaturas"]
        UC18[Candidatar-se a um projeto]
        UC19[Cancelar candidatura]
        UC20[Aceitar / rejeitar candidatura recebida]
    end

    subgraph Avaliacoes["Avaliações"]
        UC21[Avaliar colaborador]
        UC22[Consultar avaliações recebidas]
        UC23[Denunciar avaliação abusiva]
    end

    subgraph Recomendacao["Recomendação"]
        UC24[Ver projetos recomendados]
        UC25[Ver candidatos recomendados]
    end

    subgraph Administracao["Administração"]
        UC26[Gerenciar catálogo de habilidades]
        UC27[Listar usuários / alterar nível de acesso]
        UC28[Analisar e resolver denúncias]
    end

    Aluno --> Autenticacao
    Aluno --> Perfil
    Aluno --> Projetos
    Aluno --> Candidaturas
    Aluno --> Avaliacoes
    Aluno --> Recomendacao

    Professor --> Autenticacao
    Professor --> Perfil
    Professor --> Projetos
    Professor --> Candidaturas
    Professor --> Avaliacoes
    Professor --> Recomendacao

    Admin --> Autenticacao
    Admin --> Perfil
    Admin --> Administracao
```

## 3. Diagrama de arquitetura

Expande o diagrama ASCII do README raiz, detalhando a cadeia de filtros de segurança do
monolito (a ordem importa: rate limit por IP roda **antes** da validação do JWT, então até uma
tentativa de flood em `/auth/**` sem token é barrada), os dois microserviços extraídos e as
integrações externas. Ver [decisão 9](./decisoes-arquiteturais.md#9-extração-de-e-mail-e-upload-como-microserviços-monolito-para-o-resto)
para o racional de por que só essas duas partes viraram serviço separado.

```mermaid
graph TB
    FE["Frontend<br/>React + Vite + TypeScript"]

    subgraph Backend["academico-backend (monolito)"]
        direction TB
        RL["GlobalRateLimitFilter<br/>limite por IP (120 req/min)"]
        JWT["JwtAuthenticationFilter<br/>valida Bearer token"]
        SEC["SecurityConfig<br/>authorizeHttpRequests + @PreAuthorize"]
        CTRL["Controllers<br/>13 REST controllers"]
        SRV["Services<br/>regras de negócio"]
        REPO["Repositories<br/>Spring Data JPA"]
        SCHED["TokenCleanupScheduler<br/>job diário"]
        PUB["CodigoVerificacaoPublisher"]
        UPCLIENT["FotoStorageClient"]
    end

    DB[("PostgreSQL")]
    MQ{{"RabbitMQ<br/>email.exchange"}}
    EMAILSVC["academico-email-service"]
    UPLOADSVC["academico-upload-service"]
    RESEND[["Resend API<br/>envio de e-mail (OTP)"]]
    R2[("Cloudflare R2<br/>fotos de perfil")]

    FE -->|"HTTP/REST + Bearer JWT"| RL --> JWT --> SEC --> CTRL --> SRV --> REPO --> DB
    SRV -.-> PUB -.->|"fire-and-forget"| MQ -.-> EMAILSVC
    EMAILSVC -.->|"código de recuperação de senha"| RESEND
    SRV -.-> UPCLIENT -->|"REST síncrono<br/>X-Internal-Api-Key"| UPLOADSVC --> R2
    SCHED -.->|"limpeza de tokens revogados expirados"| DB
```
