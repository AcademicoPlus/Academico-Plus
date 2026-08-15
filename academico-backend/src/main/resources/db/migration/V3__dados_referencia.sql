-- ========================================
-- ATELIER - DATABASE SCHEMA (FLYWAY V3)
-- Dados de referência necessários pra a aplicação funcionar: catálogo de
-- cursos (usado no cadastro e no filtro de "explorar perfis") e catálogo
-- de habilidades (usado no perfil do usuário e nos requisitos de projeto).
-- Níveis de acesso já são inseridos pelo V1. ON CONFLICT DO NOTHING porque
-- "nome" é UNIQUE nas duas tabelas — reexecutar o script não duplica nem
-- falha em bancos que já tenham algum desses registros.
--
-- "id" é gerado aqui via gen_random_uuid() explicitamente, e "ativo"/
-- "criado_em"/"usuarios_count"/"projetos_count" são preenchidos à mão:
-- as entidades JPA geram id em Java (@GeneratedValue(strategy =
-- GenerationType.UUID)) e preenchem esses campos em @PrePersist/
-- @Builder.Default, então essas colunas não têm DEFAULT no Postgres —
-- omiti-las faria o INSERT falhar com NOT NULL violation.
-- ========================================

-- ========================================
-- CURSOS
-- ========================================
INSERT INTO cursos (id, nome, sigla, ativo, criado_em, atualizado_em) VALUES
    (gen_random_uuid(), 'Ciência da Computação', 'CC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Engenharia de Software', 'ESW', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Sistemas de Informação', 'SI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Engenharia Civil', 'ECIV', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Engenharia de Produção', 'EPROD', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Administração', 'ADM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Design', 'DESIGN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Direito', 'DIR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Arquitetura e Urbanismo', 'ARQ', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Psicologia', 'PSI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Enfermagem', 'ENF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Fisioterapia', 'FISIO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (nome) DO NOTHING;

-- ========================================
-- HABILIDADES
-- ========================================
INSERT INTO habilidades (id, nome, categoria, usuarios_count, projetos_count, criado_em) VALUES
    -- Linguagens de programação
    (gen_random_uuid(), 'JavaScript', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TypeScript', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Java', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Python', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'C#', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'PHP', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Kotlin', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Swift', 'Linguagens de Programação', 0, 0, CURRENT_TIMESTAMP),

    -- Frameworks e bibliotecas
    (gen_random_uuid(), 'React', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Spring Boot', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Node.js', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Angular', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Vue.js', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Django', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Flutter', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '.NET', 'Frameworks e Bibliotecas', 0, 0, CURRENT_TIMESTAMP),

    -- Banco de dados
    (gen_random_uuid(), 'PostgreSQL', 'Banco de Dados', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MySQL', 'Banco de Dados', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'MongoDB', 'Banco de Dados', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Redis', 'Banco de Dados', 0, 0, CURRENT_TIMESTAMP),

    -- Dados e IA
    (gen_random_uuid(), 'Machine Learning', 'Dados e IA', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Análise de Dados', 'Dados e IA', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Power BI', 'Dados e IA', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Excel Avançado', 'Dados e IA', 0, 0, CURRENT_TIMESTAMP),

    -- DevOps e infraestrutura
    (gen_random_uuid(), 'Docker', 'DevOps e Infraestrutura', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Git e GitHub', 'DevOps e Infraestrutura', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CI/CD', 'DevOps e Infraestrutura', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'AWS', 'DevOps e Infraestrutura', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Linux', 'DevOps e Infraestrutura', 0, 0, CURRENT_TIMESTAMP),

    -- Design
    (gen_random_uuid(), 'UI/UX Design', 'Design', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Figma', 'Design', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Photoshop', 'Design', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Ilustração Digital', 'Design', 0, 0, CURRENT_TIMESTAMP),

    -- Gestão
    (gen_random_uuid(), 'Gestão de Projetos', 'Gestão', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Metodologias Ágeis (Scrum/Kanban)', 'Gestão', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Liderança de Equipes', 'Gestão', 0, 0, CURRENT_TIMESTAMP),

    -- Comunicação e negócios
    (gen_random_uuid(), 'Redação Técnica', 'Comunicação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Marketing Digital', 'Comunicação', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Apresentação e Oratória', 'Comunicação', 0, 0, CURRENT_TIMESTAMP),

    -- Habilidades gerais de engenharia/áreas correlatas
    (gen_random_uuid(), 'AutoCAD', 'Engenharia', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Modelagem 3D', 'Engenharia', 0, 0, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Elaboração de Projetos Técnicos', 'Engenharia', 0, 0, CURRENT_TIMESTAMP)
ON CONFLICT (nome) DO NOTHING;
