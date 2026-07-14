# 📊 Database Documentation

## Visão Geral

O banco de dados do **Academico+** é uma estrutura relacional implementada em **PostgreSQL 14+**, projetada para suportar um ecossistema completo de networking e colaboração acadêmica.

## 🏗️ Arquitetura do Banco de Dados

### Camadas de Dados

```
┌─────────────────────────────────────────┐
│      Application Layer (Spring Boot)    │
├─────────────────────────────────────────┤
│      Repository Layer (JPA/Hibernate)   │
├─────────────────────────────────────────┤
│      Persistence Layer (JDBC)           │
├─────────────────────────────────────────┤
│      Database Layer (PostgreSQL)        │
└─────────────────────────────────────────┘
```

---

## 📋 Tabelas Principais

### 1. **users** - Usuários da Plataforma

Armazena dados de alunos e professores.

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type ENUM('STUDENT', 'PROFESSOR'),
    academic_info VARCHAR(255),
    bio TEXT,
    profile_photo_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Campos:**
- `id`: Identificador único
- `email`: Email institucional (único)
- `full_name`: Nome completo do usuário
- `password_hash`: Hash bcrypt da senha
- `user_type`: STUDENT ou PROFESSOR
- `academic_info`: Curso e período (ex: "Computação - 8º período")
- `bio`: Biografia do usuário
- `profile_photo_url`: URL da foto de perfil
- `is_active`: Indica se conta está ativa

**Índices:**
```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_user_type ON users(user_type);
```

---

### 2. **competencies** - Catálogo de Competências

Armazena todas as competências/tags disponíveis no sistema.

```sql
CREATE TABLE competencies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50),
    created_at TIMESTAMP
);
```

**Categorias:**
- Backend (Java, Spring Boot, Python, Node.js)
- Frontend (React, Next.js, TypeScript, HTML/CSS)
- Database (PostgreSQL, MongoDB, SQL)
- DevOps (Docker, Kubernetes, AWS, CI/CD)
- Design (UI Design, UX Design, Figma)
- Soft Skills (Liderança, Comunicação)
- Specialization (IA, Segurança, Mobile)

---

### 3. **user_competencies** - Perfil de Habilidades

Mapeia competências dos usuários com níveis de proficiência.

```sql
CREATE TABLE user_competencies (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    competency_id INTEGER NOT NULL REFERENCES competencies(id),
    proficiency_level VARCHAR(20) DEFAULT 'INTERMEDIATE',
    is_endorsed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    UNIQUE(user_id, competency_id)
);
```

**Níveis de Proficiência:**
- `BEGINNER` - Iniciante
- `INTERMEDIATE` - Intermediário
- `ADVANCED` - Avançado
- `EXPERT` - Especialista

**Nota:** A flag `is_endorsed` indica se a competência foi validada por um professor.

**Índices:**
```sql
CREATE INDEX idx_user_competencies_user_id ON user_competencies(user_id);
CREATE INDEX idx_user_competencies_competency_id ON user_competencies(competency_id);
```

---

### 4. **projects** - Projetos e Oportunidades

Armazena todos os projetos publicados na plataforma.

```sql
CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    creator_id INTEGER NOT NULL REFERENCES users(id),
    project_type ENUM('TCC', 'STARTUP', 'RESEARCH', 'EXTENSION', 'OTHER'),
    status ENUM('OPEN', 'IN_PROGRESS', 'CLOSED') DEFAULT 'OPEN',
    max_members INTEGER,
    start_date DATE,
    deadline DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Tipos de Projeto:**
- `TCC` - Trabalho de Conclusão de Curso
- `STARTUP` - Projeto de startup
- `RESEARCH` - Pesquisa acadêmica
- `EXTENSION` - Projeto de extensão
- `OTHER` - Outros

**Status:**
- `OPEN` - Aberto para receber membros
- `IN_PROGRESS` - Em desenvolvimento
- `CLOSED` - Finalizado

**Índices:**
```sql
CREATE INDEX idx_projects_creator_id ON projects(creator_id);
CREATE INDEX idx_projects_status ON projects(status);
```

---

### 5. **project_requirements** - Requisitos dos Projetos

Define quais competências são necessárias para cada projeto.

```sql
CREATE TABLE project_requirements (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    competency_id INTEGER NOT NULL REFERENCES competencies(id),
    min_proficiency_level VARCHAR(20),
    created_at TIMESTAMP,
    UNIQUE(project_id, competency_id)
);
```

**Nota:** Cada projeto pode ter múltiplos requisitos de competência.

---

### 6. **project_members** - Membros dos Projetos

Registra quais usuários fazem parte de cada projeto.

```sql
CREATE TABLE project_members (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    role VARCHAR(50), -- LEADER, MEMBER, CONTRIBUTOR
    joined_at TIMESTAMP,
    UNIQUE(project_id, user_id)
);
```

**Índices:**
```sql
CREATE INDEX idx_project_members_project_id ON projects(id);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
```

---

### 7. **endorsements** - Validações de Competências

Professores endossam (validam) as competências dos alunos.

```sql
CREATE TABLE endorsements (
    id SERIAL PRIMARY KEY,
    user_competency_id INTEGER NOT NULL REFERENCES user_competencies(id),
    professor_id INTEGER NOT NULL REFERENCES users(id),
    endorsed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    comment TEXT,
    UNIQUE(user_competency_id, professor_id)
);
```

**Nota:** Um professor pode endossar a mesma competência de um aluno apenas uma vez.

---

### 8. **messages** - Sistema de Mensagens

Facilita comunicação direta entre usuários e dentro de projetos.

```sql
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL REFERENCES users(id),
    receiver_id INTEGER NOT NULL REFERENCES users(id),
    project_id INTEGER REFERENCES projects(id),
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    read_at TIMESTAMP
);
```

**Índices:**
```sql
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX idx_messages_is_read ON messages(is_read);
```

---

### 9. **recommendations** - Histórico de Recomendações

Armazena resultados do algoritmo de Jaccard Similarity.

```sql
CREATE TABLE recommendations (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    jaccard_similarity DECIMAL(3, 2),
    recommended_at TIMESTAMP,
    UNIQUE(project_id, user_id)
);
```

**Nota:** `jaccard_similarity` varia de 0 a 1, onde 1 indica compatibilidade perfeita.

**Índices:**
```sql
CREATE INDEX idx_recommendations_project_id ON recommendations(project_id);
```

---

## 🔗 Relacionamentos

### Diagrama ER (Entity-Relationship)

```
                    ┌─────────────────┐
                    │     USERS       │
                    ├─────────────────┤
                    │ id (PK)         │
                    │ email (UQ)      │
                    │ full_name       │
                    │ user_type (FK)  │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                    ▼                 ▼
        ┌─────────────────────┐  ┌──────────────────────┐
        │ USER_COMPETENCIES   │  │    ENDORSEMENTS      │
        ├─────────────────────┤  ├──────────────────────┤
        │ user_id (FK)        │  │ user_competency_id   │
        │ competency_id (FK)  │──│ professor_id (FK)    │
        │ proficiency_level   │  │ endorsed_at          │
        │ is_endorsed         │  └──────────────────────┘
        └─────────────────────┘
                    │
                    │ competency_id (FK)
                    ▼
        ┌─────────────────────┐
        │  COMPETENCIES       │
        ├─────────────────────┤
        │ id (PK)             │
        │ name (UQ)           │
        │ category            │
        └─────────────────────┘
                    ▲
                    │ competency_id (FK)
                    │
        ┌───────────┴──────────┐
        │                      │
        ▼                      ▼
┌─────────────────────┐  ┌──────────────────────┐
│PROJECT_REQUIREMENTS │  │    PROJECTS          │
├─────────────────────┤  ├──────────────────────┤
│ project_id (FK)     │  │ id (PK)              │
│ competency_id (FK)  │  │ creator_id (FK)      │
│ min_level           │  │ title                │
└─────────────────────┘  │ description          │
                         │ status               │
                         └──────────┬───────────┘
                                    │
                         ┌──────────┴──────────┐
                         │                     │
                         ▼                     ▼
            ┌──────────────────────┐  ┌──────────────────────┐
            │  PROJECT_MEMBERS     │  │     MESSAGES         │
            ├──────────────────────┤  ├──────────────────────┤
            │ project_id (FK)      │  │ sender_id (FK)       │
            │ user_id (FK)         │  │ receiver_id (FK)     │
            │ role                 │  │ project_id (FK)      │
            │ joined_at            │  │ content              │
            └──────────────────────┘  │ is_read              │
                                      └──────────────────────┘
                                                │
                                                │
                                      ┌─────────┴──────────┐
                                      │                    │
                                      ▼                    ▼
                            ┌──────────────────────┐  ┌──────────────────────┐
                            │ RECOMMENDATIONS      │  │ MESSAGES (history)   │
                            ├──────────────────────┤  ├──────────────────────┤
                            │ project_id (FK)      │  │ read_at              │
                            │ user_id (FK)         │  │ created_at           │
                            │ jaccard_similarity   │  └──────────────────────┘
                            │ recommended_at       │
                            └──────────────────────┘
```

---

## 🔍 Views Úteis

### View: Projetos com Contagem de Membros

```sql
CREATE VIEW projects_with_member_count AS
SELECT 
    p.id,
    p.title,
    p.status,
    COUNT(pm.id) as member_count,
    p.max_members,
    p.created_at
FROM projects p
LEFT JOIN project_members pm ON p.id = pm.project_id
GROUP BY p.id;
```

### View: Usuários com Contagem de Competências

```sql
CREATE VIEW users_with_competency_count AS
SELECT 
    u.id,
    u.email,
    u.full_name,
    COUNT(uc.id) as competency_count,
    COUNT(CASE WHEN uc.is_endorsed THEN 1 END) as endorsed_count
FROM users u
LEFT JOIN user_competencies uc ON u.id = uc.user_id
GROUP BY u.id;
```

---

## 📊 Queries Frequentes

### Encontrar Projetos Abertos
```sql
SELECT * FROM projects WHERE status = 'OPEN';
```

### Listar Competências de um Usuário
```sql
SELECT c.name, uc.proficiency_level, uc.is_endorsed
FROM user_competencies uc
JOIN competencies c ON uc.competency_id = c.id
WHERE uc.user_id = 4;
```

### Encontrar Professores que Podem Endossar
```sql
SELECT * FROM users WHERE user_type = 'PROFESSOR';
```

### Contar Membros de um Projeto
```sql
SELECT COUNT(*) FROM project_members WHERE project_id = 1;
```

---

## 🔐 Segurança

### Constraints
- Foreign Keys: Garantem integridade referencial
- UNIQUE Constraints: Evitam duplicatas
- NOT NULL: Garantem completude dos dados

### Índices para Performance
- Email (busca de usuários)
- User Type (filtros de professor/aluno)
- Project Status (listagem de projetos abertos)
- Message Read Status (notificações)

---

## 📈 Escalabilidade

### Futuras Otimizações
1. **Particionamento:** Dividir tabelas grandes (messages) por data
2. **Caching:** Redis para queries frequentes
3. **Replicação:** Read replicas para consultas
4. **Archive:** Arquivar dados históricos

---

## 🔄 Backup & Recovery

### Backup Automático
```bash
# Backup completo
pg_dump academico_plus > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore
psql academico_plus < backup_20260618_120000.sql
```

---

## 📝 Migrações

O projeto usa **Liquibase** ou **Flyway** para versionamento do banco de dados:

```
database/migrations/
├── V001__initial_schema.sql
├── V002__add_endorsements_table.sql
└── V003__create_indexes.sql
```

---

## 🧪 Dados de Teste

Os dados iniciais são carregados via `seed-data.sql`:
- 3 Professores
- 7 Alunos
- 30+ Competências
- 5 Projetos de exemplo

---

## ❓ Perguntas Frequentes

**P: Como o algoritmo de Jaccard usa as competências?**  
R: Compara o conjunto de competências do projeto com o conjunto de competências do aluno. Similarity = |A ∩ B| / |A ∪ B|

**P: Posso usar outro banco de dados?**  
R: Sim, o padrão JPA permite MySQL, MariaDB, H2. Apenas ajuste o `application.properties` do Spring Boot.

**P: Como lidar com dados históricos?**  
R: Use soft deletes (add flag `is_deleted`) ou arquive em tabelas separadas.

---

**Última atualização:** 18/06/2026
