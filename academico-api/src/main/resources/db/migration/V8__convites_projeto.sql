-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V8)
-- Convites de projeto: o criador convida um usuário, que aceita ou recusa.
-- Convites respondidos/cancelados ficam como histórico; por isso a
-- unicidade vale só para o convite PENDENTE (índice único parcial).
-- ========================================
CREATE TABLE convites_projeto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    convidado_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    convidante_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    funcao VARCHAR(100),
    mensagem TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    respondido_em TIMESTAMP
);

CREATE INDEX idx_convites_projeto_projeto ON convites_projeto (projeto_id);
CREATE INDEX idx_convites_projeto_convidado ON convites_projeto (convidado_id);
CREATE INDEX idx_convites_projeto_status ON convites_projeto (status);
CREATE UNIQUE INDEX uk_convite_pendente_projeto_convidado
    ON convites_projeto (projeto_id, convidado_id)
    WHERE status = 'PENDENTE';
