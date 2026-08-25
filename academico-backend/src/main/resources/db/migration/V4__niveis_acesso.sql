-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V4)
-- Níveis de acesso (ALUNO/PROFESSOR/ADMIN) — o cadastro público
-- (AuthService.cadastrar) exige que exista um nível "ALUNO" pra atribuir
-- ao novo usuário; sem isso, POST /auth/cadastro falha.
--
-- Já existia um INSERT igual dentro do V1, mas em bancos cujo schema foi
-- provisionado pelo Hibernate (ddl-auto=update) antes do Flyway rodar
-- pela primeira vez, o Flyway faz um "baseline" da V1 em vez de executá-la
-- — ou seja, o INSERT do V1 nunca chega a rodar nesses bancos. V1 não
-- pode ser editado (checksum já gravado em ambientes que o aplicaram de
-- verdade), então este V4 refaz o seed de forma idempotente.
-- ========================================
INSERT INTO niveis_acesso (id, nome, descricao) VALUES
    (gen_random_uuid(), 'ALUNO', 'Estudante da instituição'),
    (gen_random_uuid(), 'PROFESSOR', 'Docente ou orientador de projetos'),
    (gen_random_uuid(), 'ADMIN', 'Administrador do sistema')
ON CONFLICT (nome) DO NOTHING;
