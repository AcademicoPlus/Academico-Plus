-- ========================================
-- ACADEMICO+ - DATABASE SCHEMA
-- ========================================
-- PostgreSQL 14+
-- Database: academico_db

-- ========================================
-- 1. USUARIOS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    curso VARCHAR(255),
    periodo INTEGER,
    tipo VARCHAR(50) NOT NULL DEFAULT 'ALUNO',
    bio TEXT,
    foto_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    github_url VARCHAR(500),
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_tipo ON usuarios(tipo);
CREATE INDEX IF NOT EXISTS idx_usuarios_ativo ON usuarios(ativo);

-- ========================================
-- 2. HABILIDADES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS habilidades (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(100),
    descricao TEXT,
    usuarios_count INTEGER DEFAULT 0,
    projetos_count INTEGER DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_habilidades_nome ON habilidades(nome);
CREATE INDEX IF NOT EXISTS idx_habilidades_categoria ON habilidades(categoria);

-- ========================================
-- 3. USUARIO_HABILIDADES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS usuario_habilidades (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    habilidade_id BIGINT NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    nivel VARCHAR(50) NOT NULL DEFAULT 'INICIANTE',
    endossado BOOLEAN DEFAULT FALSE,
    endossos_count INTEGER DEFAULT 0,
    endossado_por VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(usuario_id, habilidade_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_habilidades_usuario ON usuario_habilidades(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_habilidades_habilidade ON usuario_habilidades(habilidade_id);
CREATE INDEX IF NOT EXISTS idx_usuario_habilidades_endossado ON usuario_habilidades(endossado);

-- ========================================
-- 4. PROJETOS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS projetos (
    id BIGSERIAL PRIMARY KEY,
    criador_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ABERTO',
    vagas INTEGER NOT NULL,
    vagas_preenchidas INTEGER DEFAULT 0,
    data_fim TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_projetos_criador ON projetos(criador_id);
CREATE INDEX IF NOT EXISTS idx_projetos_status ON projetos(status);
CREATE INDEX IF NOT EXISTS idx_projetos_ativo ON projetos(ativo);
CREATE INDEX IF NOT EXISTS idx_projetos_criado_em ON projetos(criado_em);

-- ========================================
-- 5. PROJETO_HABILIDADES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS projeto_habilidades (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    habilidade_id BIGINT NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(projeto_id, habilidade_id)
);

CREATE INDEX IF NOT EXISTS idx_projeto_habilidades_projeto ON projeto_habilidades(projeto_id);
CREATE INDEX IF NOT EXISTS idx_projeto_habilidades_habilidade ON projeto_habilidades(habilidade_id);

-- ========================================
-- 6. PROJETO_MEMBROS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS projeto_membros (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    funcao VARCHAR(100),
    data_adesao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_projeto_membros_projeto ON projeto_membros(projeto_id);
CREATE INDEX IF NOT EXISTS idx_projeto_membros_usuario ON projeto_membros(usuario_id);

-- ========================================
-- 7. CANDIDATURAS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS candidaturas (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    mensagem TEXT,
    motivo_rejeicao TEXT,
    data_candidatura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_resposta TIMESTAMP,
    UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_candidaturas_projeto ON candidaturas(projeto_id);
CREATE INDEX IF NOT EXISTS idx_candidaturas_usuario ON candidaturas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_candidaturas_status ON candidaturas(status);

-- ========================================
-- 8. ENDOSSOS_PROFESSOR TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS endossos_professor (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    aluno_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    habilidade_id BIGINT NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    nivel VARCHAR(50) NOT NULL DEFAULT 'INTERMEDIARIO',
    data_endosso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(professor_id, aluno_id, habilidade_id)
);

CREATE INDEX IF NOT EXISTS idx_endossos_professor ON endossos_professor(professor_id);
CREATE INDEX IF NOT EXISTS idx_endossos_aluno ON endossos_professor(aluno_id);
CREATE INDEX IF NOT EXISTS idx_endossos_habilidade ON endossos_professor(habilidade_id);

-- ========================================
-- SCHEMA CREATION COMPLETE
-- ========================================