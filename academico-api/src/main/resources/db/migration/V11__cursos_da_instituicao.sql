-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V11)
-- Alinha o catálogo de cursos com os cursos realmente oferecidos pela
-- instituição. O V3 trazia uma lista genérica; aqui:
--   * "Ciência da Computação" é renomeado (em vez de recriado) pra preservar
--     o vínculo dos usuários já cadastrados nele;
--   * os cursos que a instituição não oferece são DESATIVADOS, não apagados
--     (usuarios.curso_id pode referenciá-los; o cadastro lista só ativos);
--   * os cursos que faltavam são inseridos.
-- ========================================

UPDATE cursos
   SET nome = 'Ciências da Computação', atualizado_em = CURRENT_TIMESTAMP
 WHERE nome = 'Ciência da Computação'
   AND NOT EXISTS (SELECT 1 FROM cursos WHERE nome = 'Ciências da Computação');

INSERT INTO cursos (id, nome, sigla, ativo, criado_em, atualizado_em) VALUES
    (gen_random_uuid(), 'Ciências da Computação', 'CC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Farmácia', 'FARM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Enfermagem', 'ENF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Psicologia', 'PSI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Fisioterapia', 'FISIO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Administração', 'ADM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Biomedicina', 'BIOMED', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Direito', 'DIR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Engenharia Civil', 'ECIV', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Pedagogia', 'PED', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Educação Física', 'EDF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Ciências Contábeis', 'CONT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Serviço Social', 'SS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Recursos Humanos', 'RH', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Segurança do Trabalho', 'ST', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Tecnologia da Informação', 'TI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (nome) DO UPDATE SET ativo = TRUE, atualizado_em = CURRENT_TIMESTAMP;

UPDATE cursos
   SET ativo = FALSE, atualizado_em = CURRENT_TIMESTAMP
 WHERE nome IN ('Engenharia de Software', 'Sistemas de Informação', 'Engenharia de Produção',
                'Design', 'Arquitetura e Urbanismo');
