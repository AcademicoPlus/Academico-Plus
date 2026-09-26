-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V9)
-- Até quando cada participante leu o chat de um projeto. Mensagens de
-- outros autores com criado_em posterior a lido_ate contam como não lidas
-- (caixa "Mensagens" e badge do menu lateral).
-- ========================================
CREATE TABLE leituras_chat_projeto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    lido_ate TIMESTAMP NOT NULL,
    CONSTRAINT uk_leitura_chat_projeto_usuario UNIQUE (projeto_id, usuario_id)
);

CREATE INDEX idx_leituras_chat_projeto_usuario ON leituras_chat_projeto (usuario_id);
