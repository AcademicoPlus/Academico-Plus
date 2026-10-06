-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V6)
-- Capa/banner do projeto, armazenada no mesmo storage externo (R2) já usado
-- para a foto de perfil (ver FotoStorageClient) — aqui é o equivalente para
-- projetos, opcional, sem valor padrão.
-- ========================================
ALTER TABLE projetos ADD COLUMN banner_url VARCHAR(500);
