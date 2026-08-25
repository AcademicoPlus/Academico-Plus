-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V5)
-- usuarios.ativo, cursos.ativo e projetos.ativo foram criados sem NOT NULL no V1,
-- mas as entidades JPA (Usuario, Curso, Projeto) declaram @Column(nullable = false)
-- com default true. Um NULL nessas colunas passa batido pelo ddl-auto=validate
-- (nullability não é verificada) e, em usuarios, escaparia silenciosamente do
-- @SQLRestriction("ativo = true") — um registro com ativo NULL não é nem ativo
-- nem inativo, some de toda consulta JPA padrão sem passar pelo fluxo de soft-delete.
-- ========================================
UPDATE usuarios SET ativo = TRUE WHERE ativo IS NULL;
ALTER TABLE usuarios ALTER COLUMN ativo SET NOT NULL;

UPDATE cursos SET ativo = TRUE WHERE ativo IS NULL;
ALTER TABLE cursos ALTER COLUMN ativo SET NOT NULL;

UPDATE projetos SET ativo = TRUE WHERE ativo IS NULL;
ALTER TABLE projetos ALTER COLUMN ativo SET NOT NULL;
