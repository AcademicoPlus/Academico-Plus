-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V7)
-- Chat em tempo real por projeto: uma sala compartilhada pelo criador e
-- pelos membros aceitos (ver ProjetoMembroService), entregue via STOMP/WS.
-- ========================================
CREATE TABLE mensagens_projeto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id UUID NOT NULL REFERENCES projetos(id),
    autor_id UUID NOT NULL REFERENCES usuarios(id),
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_mensagens_projeto_projeto_criado_em ON mensagens_projeto (projeto_id, criado_em);
