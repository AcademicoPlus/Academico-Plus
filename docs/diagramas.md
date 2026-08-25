# Diagramas técnicos — Academico+

Diagramas de apoio ao TCC2, em Mermaid (renderiza nativamente no GitHub). Para colar no
documento do TCC, abra este arquivo no GitHub/VS Code e exporte a imagem renderizada de
cada bloco (ou use uma extensão de export Mermaid → PNG/SVG).

## 1. Diagrama Entidade-Relacionamento

Construído a partir de `academico-backend/src/main/resources/db/migration/V1_schema_inicial.sql`
mais as entidades JPA `Avaliacao` e `DenunciaAvaliacao`, que **não estão** nesse arquivo de
migração — hoje elas só existem porque `spring.jpa.hibernate.ddl-auto=update` as cria em tempo
de execução (ver [decisões arquiteturais](./decisoes-arquiteturais.md#6-flyway-declarado-mas-ddl-auto).
Isso é uma dívida técnica conhecida do projeto, não um erro deste diagrama.

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

Expande o diagrama ASCII do README raiz, detalhando a cadeia de filtros de segurança (a ordem
importa: rate limit por IP roda **antes** da validação do JWT, então até uma tentativa de flood
em `/auth/**` sem token é barrada) e as integrações externas.

```mermaid
graph TB
    FE["Frontend<br/>React + Vite + TypeScript"]

    subgraph Backend["Backend — Spring Boot"]
        direction TB
        RL["GlobalRateLimitFilter<br/>limite por IP (120 req/min)"]
        JWT["JwtAuthenticationFilter<br/>valida Bearer token"]
        SEC["SecurityConfig<br/>authorizeHttpRequests + @PreAuthorize"]
        CTRL["Controllers<br/>13 REST controllers"]
        SRV["Services<br/>regras de negócio"]
        REPO["Repositories<br/>Spring Data JPA"]
        SCHED["TokenCleanupScheduler<br/>job diário"]
    end

    DB[("PostgreSQL")]
    EMAIL[["Resend API<br/>envio de e-mail (OTP)"]]

    FE -->|"HTTP/REST + Bearer JWT"| RL --> JWT --> SEC --> CTRL --> SRV --> REPO --> DB
    SRV -.->|"código de recuperação de senha"| EMAIL
    SCHED -.->|"limpeza de tokens revogados expirados"| DB
```
