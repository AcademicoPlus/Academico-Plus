-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V1)
-- ========================================

-- Extension para suporte a UUID no PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ========================================
-- 1. NIVEIS_ACESSO TABLE
-- ========================================
CREATE TABLE niveis_acesso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(50) NOT NULL UNIQUE, -- 'ALUNO', 'PROFESSOR', 'ADMIN'
    descricao VARCHAR(255),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE tokens_revogados (
    token VARCHAR(1000) NOT NULL PRIMARY KEY,
    data_revogacao TIMESTAMP NOT NULL,
    data_expiracao TIMESTAMP NOT NULL
);

-- Insere os níveis padrões necessários para o sistema
INSERT INTO niveis_acesso (id, nome, descricao) VALUES 
    (gen_random_uuid(), 'ALUNO', 'Estudante da instituição'),
    (gen_random_uuid(), 'PROFESSOR', 'Docente ou orientador de projetos'),
    (gen_random_uuid(), 'ADMIN', 'Administrador do sistema');

-- ========================================
-- 2. CURSOS TABLE
-- ========================================
CREATE TABLE cursos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL UNIQUE,
    sigla VARCHAR(20),
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 3. USUARIOS TABLE
-- ========================================
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    nivel_acesso_id UUID NOT NULL REFERENCES niveis_acesso(id),
    curso_id UUID REFERENCES cursos(id),
    periodo INTEGER,
    bio TEXT,
    foto_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    github_url VARCHAR(500),
    senha_hash VARCHAR(255) NOT NULL,
    codigo_recuperacao VARCHAR(255),
    codigo_recuperacao_expira_em TIMESTAMP WITH TIME ZONE,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletado_em TIMESTAMP WITH TIME ZONE -- Adicionado para o Soft Delete
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_nivel_acesso ON usuarios(nivel_acesso_id);
CREATE INDEX idx_usuarios_curso ON usuarios(curso_id);
CREATE INDEX idx_usuarios_ativo ON usuarios(ativo);


-- ========================================
-- 5. HABILIDADES TABLE
-- ========================================
CREATE TABLE habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(100),
    descricao TEXT,
    usuarios_count INTEGER DEFAULT 0,
    projetos_count INTEGER DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_habilidades_nome ON habilidades(nome);
CREATE INDEX idx_habilidades_categoria ON habilidades(categoria);

-- ========================================
-- 6. USUARIO_HABILIDADES TABLE
-- ========================================
CREATE TABLE usuario_habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    habilidade_id UUID NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    nivel VARCHAR(50) NOT NULL DEFAULT 'INICIANTE',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_habilidade UNIQUE(usuario_id, habilidade_id)
);

CREATE INDEX idx_usuario_habilidades_usuario ON usuario_habilidades(usuario_id);
CREATE INDEX idx_usuario_habilidades_habilidade ON usuario_habilidades(habilidade_id);

-- ========================================
-- 7. PROJETOS TABLE
-- ========================================
CREATE TABLE projetos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    criador_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ABERTO',
    vagas INTEGER NOT NULL,
    vagas_preenchidas INTEGER DEFAULT 0,
    data_fim TIMESTAMP WITH TIME ZONE,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projetos_criador ON projetos(criador_id);
CREATE INDEX idx_projetos_status ON projetos(status);
CREATE INDEX idx_projetos_ativo ON projetos(ativo);
CREATE INDEX idx_projetos_criado_em ON projetos(criado_em);

-- ========================================
-- 8. PROJETO_HABILIDADES TABLE
-- ========================================
CREATE TABLE projeto_habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    habilidade_id UUID NOT NULL REFERENCES habilidades(id) ON DELETE CASCADE,
    obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_projeto_habilidade UNIQUE(projeto_id, habilidade_id)
);

CREATE INDEX idx_projeto_habilidades_projeto ON projeto_habilidades(projeto_id);
CREATE INDEX idx_projeto_habilidades_habilidade ON projeto_habilidades(habilidade_id);

-- ========================================
-- 9. PROJETO_MEMBROS TABLE
-- ========================================
CREATE TABLE projeto_membros (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    funcao VARCHAR(100),
    data_adesao TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_projeto_membro UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX idx_projeto_membros_projeto ON projeto_membros(projeto_id);
CREATE INDEX idx_projeto_membros_usuario ON projeto_membros(usuario_id);

-- ========================================
-- 10. CANDIDATURAS TABLE
-- ========================================
CREATE TABLE candidaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    mensagem TEXT,
    motivo_rejeicao TEXT,
    data_candidatura TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_resposta TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_candidatura_projeto_usuario UNIQUE(projeto_id, usuario_id)
);

CREATE INDEX idx_candidaturas_projeto ON candidaturas(projeto_id);
CREATE INDEX idx_candidaturas_usuario ON candidaturas(usuario_id);
CREATE INDEX idx_candidaturas_status ON candidaturas(status);