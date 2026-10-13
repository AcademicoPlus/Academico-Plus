-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V10)
-- Generaliza a denúncia (antes só de avaliação) para também alcançar
-- projetos e perfis, e cria a escada de sanções: ao julgar procedente, o
-- admin escolhe a medida (arquivar / remover conteúdo / advertir / suspender).
--
-- A tabela denuncias_avaliacao vira "denuncias" com três FKs opcionais —
-- uma por tipo de alvo — em vez de um alvo polimórfico solto (uuid + string),
-- para não abrir mão da integridade referencial. Um CHECK garante que
-- exatamente a FK correspondente ao tipo_alvo está preenchida.
-- ========================================

ALTER TABLE denuncias_avaliacao RENAME TO denuncias;

-- As linhas que já existem são todas de avaliação; o DEFAULT cobre o backfill
-- e é descartado logo em seguida para obrigar o tipo explícito daqui pra frente.
ALTER TABLE denuncias ADD COLUMN tipo_alvo VARCHAR(20) NOT NULL DEFAULT 'AVALIACAO';
ALTER TABLE denuncias ALTER COLUMN tipo_alvo DROP DEFAULT;
ALTER TABLE denuncias ADD CONSTRAINT ck_denuncias_tipo_alvo
    CHECK (tipo_alvo IN ('AVALIACAO', 'PROJETO', 'PERFIL'));

ALTER TABLE denuncias ALTER COLUMN avaliacao_id DROP NOT NULL;
ALTER TABLE denuncias ADD COLUMN projeto_id UUID REFERENCES projetos(id);
ALTER TABLE denuncias ADD COLUMN usuario_denunciado_id UUID REFERENCES usuarios(id);

-- Categoria dá triagem ao admin; as denúncias antigas (motivo em texto livre)
-- entram como OUTRO, que é exatamente o que elas eram.
ALTER TABLE denuncias ADD COLUMN categoria VARCHAR(30) NOT NULL DEFAULT 'OUTRO';
ALTER TABLE denuncias ALTER COLUMN categoria DROP DEFAULT;
ALTER TABLE denuncias ADD CONSTRAINT ck_denuncias_categoria
    CHECK (categoria IN ('SPAM', 'CONTEUDO_OFENSIVO', 'INFORMACAO_FALSA', 'GOLPE', 'OUTRO'));

-- Medida aplicada na resolução: fica na própria denúncia (inclusive as que não
-- geram sanção, como ARQUIVAR e REMOVER_CONTEUDO). Nulo enquanto PENDENTE.
ALTER TABLE denuncias ADD COLUMN medida VARCHAR(20);
ALTER TABLE denuncias ADD CONSTRAINT ck_denuncias_medida
    CHECK (medida IS NULL OR medida IN ('ARQUIVAR', 'REMOVER_CONTEUDO', 'ADVERTENCIA', 'SUSPENSAO'));

ALTER TABLE denuncias ADD CONSTRAINT ck_denuncias_alvo_unico CHECK (
       (tipo_alvo = 'AVALIACAO' AND avaliacao_id IS NOT NULL AND projeto_id IS NULL AND usuario_denunciado_id IS NULL)
    OR (tipo_alvo = 'PROJETO'   AND projeto_id   IS NOT NULL AND avaliacao_id IS NULL AND usuario_denunciado_id IS NULL)
    OR (tipo_alvo = 'PERFIL'    AND usuario_denunciado_id IS NOT NULL AND avaliacao_id IS NULL AND projeto_id IS NULL)
);

-- Rede de segurança para a regra "ninguém denuncia o próprio perfil"
-- (também validada no DenunciaService, com mensagem de negócio).
ALTER TABLE denuncias ADD CONSTRAINT ck_denuncias_nao_autodenuncia
    CHECK (usuario_denunciado_id IS NULL OR usuario_denunciado_id <> denunciante_id);

-- A unique de coluna única não serve mais (as outras FKs são nulas na maior
-- parte das linhas): vira um índice parcial por tipo de alvo, que é o que
-- de fato impede a mesma pessoa denunciar o mesmo alvo duas vezes.
ALTER TABLE denuncias DROP CONSTRAINT IF EXISTS uk_denuncia_avaliacao_denunciante;

CREATE UNIQUE INDEX IF NOT EXISTS uk_denuncia_avaliacao_denunciante
    ON denuncias (avaliacao_id, denunciante_id) WHERE avaliacao_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_denuncia_projeto_denunciante
    ON denuncias (projeto_id, denunciante_id) WHERE projeto_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_denuncia_perfil_denunciante
    ON denuncias (usuario_denunciado_id, denunciante_id) WHERE usuario_denunciado_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_denuncias_tipo_alvo ON denuncias (tipo_alvo);

-- ========================================
-- SANCOES
-- Histórico de advertências e suspensões aplicadas a um usuário. É ao mesmo
-- tempo o registro auditável da moderação (RNF08) e o canal de entrega: a
-- sanção ainda não vista é devolvida no login (ver AuthService), do mesmo
-- jeito que o aviso de projeto cancelado.
-- ========================================
CREATE TABLE sancoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    denuncia_id UUID REFERENCES denuncias(id),
    medida VARCHAR(20) NOT NULL CHECK (medida IN ('ADVERTENCIA', 'SUSPENSAO')),
    justificativa TEXT NOT NULL,
    aplicado_por_id UUID NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMP,
    visualizado_em TIMESTAMP,
    CONSTRAINT ck_sancoes_expiracao CHECK (medida <> 'SUSPENSAO' OR expira_em IS NOT NULL)
);

CREATE INDEX idx_sancoes_usuario ON sancoes (usuario_id);
CREATE INDEX idx_sancoes_medida ON sancoes (medida);

-- Espelha em usuarios a suspensão vigente, para o filtro JWT checar o acesso
-- sem join a cada requisição. NÃO reaproveita "ativo": lá significa conta
-- excluída pelo titular (LGPD, @SQLDelete/@SQLRestriction em Usuario) — somar
-- punição a direito do titular corromperia os dois significados.
ALTER TABLE usuarios ADD COLUMN suspenso_ate TIMESTAMP;
