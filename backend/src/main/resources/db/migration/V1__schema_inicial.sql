-- ========================================
-- ACADEMICO+ - DATABASE SCHEMA (FLYWAY V1)
-- ========================================

-- Extension para suporte a UUID no PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ========================================
-- 1. USUARIOS TABLE
-- ========================================
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_tipo ON usuarios(tipo);
CREATE INDEX idx_usuarios_ativo ON usuarios(ativo);

-- ========================================
-- 2. REFRESH_TOKENS TABLE
-- ========================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_id UUID NOT NULL,
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens(usuario_id);

-- ========================================
-- 3. HABILIDADES TABLE
-- ========================================
CREATE TABLE habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(100),
    descricao TEXT,
    usuarios_count INTEGER DEFAULT 0,
    projetos_count INTEGER DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_habilidades_nome ON habilidades(nome);
CREATE INDEX idx_habilidades_categoria ON habilidades(categoria);

-- ========================================
-- 4. USUARIO_HABILIDADES TABLE
-- ========================================
CREATE TABLE usuario_habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    habilidade_id UUID NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    nivel VARCHAR(50) NOT NULL DEFAULT 'INICIANTE',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_habilidade UNIQUE(usuario_id, habilidade_id)
);

CREATE INDEX idx_usuario_habilidades_usuario ON usuario_habilidades(usuario_id);
CREATE INDEX idx_usuario_habilidades_habilidade ON usuario_habilidades(habilidade_id);

-- ========================================
-- 5. PROJETOS TABLE
-- ========================================
CREATE TABLE projetos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    criador_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
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

CREATE INDEX idx_projetos_criador ON projetos(criador_id);
CREATE INDEX idx_projetos_status ON projetos(status);
CREATE INDEX idx_projetos_ativo ON projetos(ativo);
CREATE INDEX idx_projetos_criado_em ON projetos(criado_em);

-- ========================================
-- 6. PROJETO_HABILIDADES TABLE
-- ========================================
CREATE TABLE projeto_habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    habilidade_id UUID NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_projeto_habilidade UNIQUE(projeto_id, habilidade_id)
);

CREATE INDEX idx_projeto_habilidades_projeto ON projeto_habilidades(projeto_id);
CREATE INDEX idx_projeto_habilidades_habilidade ON projeto_habilidades(habilidade_id);

-- ========================================
-- 7. PROJETO_MEMBROS TABLE
-- ========================================
CREATE TABLE projeto_membros (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    funcao VARCHAR(100),
    data_adesao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_projeto_membro UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX idx_projeto_membros_projeto ON projeto_membros(projeto_id);
CREATE INDEX idx_projeto_membros_usuario ON projeto_membros(usuario_id);

-- ========================================
-- 8. CANDIDATURAS TABLE
-- ========================================
CREATE TABLE candidaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    mensagem TEXT,
    motivo_rejeicao TEXT,
    data_candidatura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_resposta TIMESTAMP,
    CONSTRAINT uk_candidatura_projeto_usuario UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX idx_candidaturas_projeto ON candidaturas(projeto_id);
CREATE INDEX idx_candidaturas_usuario ON candidaturas(usuario_id);
CREATE INDEX idx_candidaturas_status ON candidaturas(status);