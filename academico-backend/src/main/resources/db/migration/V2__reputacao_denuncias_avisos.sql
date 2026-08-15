-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V2)
-- Reputação (avaliações), denúncias de avaliação e avisos de projeto
-- cancelado: tabelas/colunas adicionadas depois do V1 e, até aqui,
-- mantidas em sincronia só via spring.jpa.hibernate.ddl-auto=update.
-- IF NOT EXISTS/ADD COLUMN IF NOT EXISTS porque bancos já provisionados
-- (ex.: o de produção) já têm esses objetos criados pelo próprio
-- ddl-auto=update — este script só formaliza o schema para quem provisionar
-- um banco novo do zero.
-- ========================================

-- ========================================
-- Correção de TOKENS_REVOGADOS: o V1 criou "token" como chave primária,
-- mas a entidade TokenRevogado usa um "id" UUID gerado à parte (token é
-- só único). Bancos provisionados direto pelo ddl-auto=update (ex.:
-- produção) já nasceram certos — isto só corrige quem rodou puro o V1.
-- ========================================
ALTER TABLE tokens_revogados ADD COLUMN IF NOT EXISTS id UUID;
UPDATE tokens_revogados SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE tokens_revogados ALTER COLUMN id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'tokens_revogados'::regclass
          AND con.contype = 'p'
          AND att.attname = 'id'
    ) THEN
        ALTER TABLE tokens_revogados DROP CONSTRAINT tokens_revogados_pkey;
        ALTER TABLE tokens_revogados ADD PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conrelid = 'tokens_revogados'::regclass AND contype = 'u'
    ) THEN
        ALTER TABLE tokens_revogados ADD CONSTRAINT uk_tokens_revogados_token UNIQUE (token);
    END IF;
END $$;

-- ========================================
-- Reputação e LGPD em USUARIOS
-- ========================================
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nota_media DOUBLE PRECISION;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS total_avaliacoes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS termos_aceitos_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========================================
-- AVALIACOES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS avaliacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id),
    avaliador_id UUID NOT NULL REFERENCES usuarios(id),
    avaliado_id UUID NOT NULL REFERENCES usuarios(id),
    nota INTEGER NOT NULL CHECK (nota >= 1 AND nota <= 5),
    comentario TEXT,
    removida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_avaliacao_projeto_avaliador_avaliado UNIQUE (projeto_id, avaliador_id, avaliado_id)
);

CREATE INDEX IF NOT EXISTS idx_avaliacoes_projeto ON avaliacoes(projeto_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_avaliado ON avaliacoes(avaliado_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_avaliador ON avaliacoes(avaliador_id);

-- ========================================
-- DENUNCIAS_AVALIACAO TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS denuncias_avaliacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    avaliacao_id UUID NOT NULL REFERENCES avaliacoes(id),
    denunciante_id UUID NOT NULL REFERENCES usuarios(id),
    motivo TEXT NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'PROCEDENTE', 'IMPROCEDENTE')),
    analisado_por_id UUID REFERENCES usuarios(id),
    analisado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_denuncia_avaliacao_denunciante UNIQUE (avaliacao_id, denunciante_id)
);

CREATE INDEX IF NOT EXISTS idx_denuncias_avaliacao ON denuncias_avaliacao(avaliacao_id);
CREATE INDEX IF NOT EXISTS idx_denuncias_status ON denuncias_avaliacao(status);

-- ========================================
-- PROJETO_CANCELAMENTO_AVISO TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS projeto_cancelamento_aviso (
    id_aviso UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuarios(id),
    id_projeto UUID NOT NULL REFERENCES projetos(id),
    projeto_nome VARCHAR(200) NOT NULL,
    projeto_data DATE NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'cancelamento',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visualizado_em TIMESTAMP
);
