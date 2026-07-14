-- ========================================
-- ACADEMICO+ - SEED DATA (Dados Iniciais)
-- ========================================
-- PostgreSQL 14+
-- Populate database with test data

-- ========================================
-- 1. INSERT USUARIOS (Users)
-- ========================================
INSERT INTO usuarios (email, nome, curso, periodo, tipo, bio, foto_url, github_url, linkedin_url, senha_hash, ativo, criado_em, atualizado_em) 
VALUES 
    ('joao.silva@unifei.edu.br', 'João Silva', 'Engenharia de Software', 6, 'ALUNO', 
     'Apaixonado por desenvolvimento backend e arquitetura de software.', 
     'https://api.placeholder.com/photos/user1.jpg', 'https://github.com/joaosilva', 'https://linkedin.com/in/joaosilva',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('maria.santos@unifei.edu.br', 'Maria Santos', 'Sistemas de Informação', 5, 'ALUNO', 
     'Especialista em frontend e design de interfaces.', 
     'https://api.placeholder.com/photos/user2.jpg', 'https://github.com/mariasantos', 'https://linkedin.com/in/mariasantos',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('pedro.oliveira@unifei.edu.br', 'Pedro Oliveira', 'Engenharia de Software', 7, 'ALUNO', 
     'DevOps e infraestrutura em nuvem é meu foco principal.', 
     'https://api.placeholder.com/photos/user3.jpg', 'https://github.com/pedrooliveira', 'https://linkedin.com/in/pedrooliveira',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('ana.costa@unifei.edu.br', 'Ana Costa', 'Sistemas de Informação', 4, 'ALUNO', 
     'Data scientist em formação, com experiência em Python e Machine Learning.', 
     'https://api.placeholder.com/photos/user4.jpg', 'https://github.com/anacosta', 'https://linkedin.com/in/anacosta',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('prof.carlos@unifei.edu.br', 'Prof. Carlos Mendes', NULL, NULL, 'PROFESSOR', 
     'Professor de Engenharia de Software com 15 anos de experiência.', 
     'https://api.placeholder.com/photos/prof1.jpg', 'https://github.com/carlosmendes', 'https://linkedin.com/in/carlosmendes',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('prof.fernanda@unifei.edu.br', 'Prof. Fernanda Lima', NULL, NULL, 'PROFESSOR', 
     'Professora especialista em Sistemas Distribuídos e Cloud Computing.', 
     'https://api.placeholder.com/photos/prof2.jpg', 'https://github.com/fernanda', 'https://linkedin.com/in/fernanda',
     '$2a$10$cXzxhTXLpZm8f0F7c9F0aO3xXxzxhTXLpZm8f0F7c9F0aO3xXxzxhT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ========================================
-- 2. INSERT HABILIDADES (Skills)
-- ========================================
INSERT INTO habilidades (nome, categoria, descricao, usuarios_count, projetos_count, criado_em) 
VALUES 
    ('Java', 'Linguagem de Programação', 'Linguagem orientada a objetos para desenvolvimento de aplicações robustas', 0, 0, CURRENT_TIMESTAMP),
    ('Python', 'Linguagem de Programação', 'Linguagem versátil para web, data science e automação', 0, 0, CURRENT_TIMESTAMP),
    ('JavaScript', 'Linguagem de Programação', 'Linguagem para desenvolvimento frontend e backend com Node.js', 0, 0, CURRENT_TIMESTAMP),
    ('TypeScript', 'Linguagem de Programação', 'JavaScript com tipagem estática para projetos maiores', 0, 0, CURRENT_TIMESTAMP),
    ('C++', 'Linguagem de Programação', 'Linguagem de alto desempenho para sistemas e jogos', 0, 0, CURRENT_TIMESTAMP),
    ('SQL', 'Linguagem de Programação', 'Linguagem para gestão de bases de dados', 0, 0, CURRENT_TIMESTAMP),
    ('Spring Boot', 'Framework', 'Framework Java para desenvolvimento rápido de APIs REST', 0, 0, CURRENT_TIMESTAMP),
    ('React', 'Framework', 'Biblioteca JavaScript para construção de interfaces interativas', 0, 0, CURRENT_TIMESTAMP),
    ('Vue.js', 'Framework', 'Framework JavaScript progressivo e acessível', 0, 0, CURRENT_TIMESTAMP),
    ('Next.js', 'Framework', 'Framework React com SSR e geração estática', 0, 0, CURRENT_TIMESTAMP),
    ('Django', 'Framework', 'Framework Python para desenvolvimento web rápido', 0, 0, CURRENT_TIMESTAMP),
    ('Express.js', 'Framework', 'Framework minimalista para APIs em Node.js', 0, 0, CURRENT_TIMESTAMP),
    ('PostgreSQL', 'Database', 'Sistema gerenciador de banco de dados relacional avançado', 0, 0, CURRENT_TIMESTAMP),
    ('MongoDB', 'Database', 'Banco de dados NoSQL orientado a documentos', 0, 0, CURRENT_TIMESTAMP),
    ('Redis', 'Database', 'Cache em memória de alta performance', 0, 0, CURRENT_TIMESTAMP),
    ('Docker', 'DevOps', 'Containerização de aplicações para deployment consistente', 0, 0, CURRENT_TIMESTAMP),
    ('Kubernetes', 'DevOps', 'Orquestração de containers em produção', 0, 0, CURRENT_TIMESTAMP),
    ('AWS', 'Cloud', 'Plataforma de computação em nuvem da Amazon', 0, 0, CURRENT_TIMESTAMP),
    ('Azure', 'Cloud', 'Plataforma de cloud da Microsoft', 0, 0, CURRENT_TIMESTAMP),
    ('Google Cloud', 'Cloud', 'Plataforma de cloud do Google', 0, 0, CURRENT_TIMESTAMP),
    ('Liderança', 'Soft Skill', 'Capacidade de liderar e motivar equipes', 0, 0, CURRENT_TIMESTAMP),
    ('Comunicação', 'Soft Skill', 'Habilidade de comunicar ideias de forma clara e efetiva', 0, 0, CURRENT_TIMESTAMP),
    ('Gestão de Projetos', 'Soft Skill', 'Experiência em planejamento e execução de projetos', 0, 0, CURRENT_TIMESTAMP);



-- ========================================
-- 3. INSERT USUARIO_HABILIDADES (User Skills)
-- ========================================
INSERT INTO usuario_habilidades (usuario_id, habilidade_id, nivel, endossado, endossos_count, criado_em) 
VALUES 
    -- João Silva (usuario_id 13)
    (13, 2, 'AVANCADO', true, 2, CURRENT_TIMESTAMP),  -- Java (hab_id 2)
    (13, 8, 'AVANCADO', true, 2, CURRENT_TIMESTAMP),  -- Spring Boot (hab_id 8)
    (13, 14, 'INTERMEDIARIO', true, 1, CURRENT_TIMESTAMP),  -- PostgreSQL (hab_id 14)
    (13, 17, 'INTERMEDIARIO', false, 0, CURRENT_TIMESTAMP),  -- Docker (hab_id 17)
    (13, 22, 'AVANCADO', false, 0, CURRENT_TIMESTAMP),  -- Liderança (hab_id 22)
    
    -- Maria Santos (usuario_id 14)
    (14, 4, 'AVANCADO', true, 3, CURRENT_TIMESTAMP),  -- JavaScript (hab_id 4)
    (14, 9, 'AVANCADO', true, 3, CURRENT_TIMESTAMP),  -- React (hab_id 9)
    (14, 11, 'INTERMEDIARIO', true, 1, CURRENT_TIMESTAMP),  -- Next.js (hab_id 11)
    (14, 23, 'AVANCADO', true, 2, CURRENT_TIMESTAMP),  -- Comunicação (hab_id 23)
    
    -- Pedro Oliveira (usuario_id 15)
    (15, 2, 'INTERMEDIARIO', false, 0, CURRENT_TIMESTAMP),  -- Java (hab_id 2)
    (15, 18, 'AVANCADO', true, 2, CURRENT_TIMESTAMP),  -- Kubernetes (hab_id 18)
    (15, 19, 'AVANCADO', true, 3, CURRENT_TIMESTAMP),  -- AWS (hab_id 19)
    (15, 17, 'AVANCADO', true, 2, CURRENT_TIMESTAMP),  -- Docker (hab_id 17)
    
    -- Ana Costa (usuario_id 16)
    (16, 3, 'AVANCADO', true, 3, CURRENT_TIMESTAMP),  -- Python (hab_id 3)
    (16, 7, 'INTERMEDIARIO', true, 1, CURRENT_TIMESTAMP),  -- SQL (hab_id 7)
    (16, 15, 'INTERMEDIARIO', false, 0, CURRENT_TIMESTAMP),  -- MongoDB (hab_id 15)
    (16, 14, 'INTERMEDIARIO', false, 0, CURRENT_TIMESTAMP),  -- PostgreSQL (hab_id 14)
    
    -- Prof. Carlos Mendes (usuario_id 17)
    (17, 2, 'EXPERT', true, 5, CURRENT_TIMESTAMP),  -- Java (hab_id 2)
    (17, 8, 'EXPERT', true, 5, CURRENT_TIMESTAMP),  -- Spring Boot (hab_id 8)
    (17, 22, 'EXPERT', true, 4, CURRENT_TIMESTAMP),  -- Liderança (hab_id 22)
    
    -- Prof. Fernanda Lima (usuario_id 18)
    (18, 18, 'EXPERT', true, 4, CURRENT_TIMESTAMP),  -- Kubernetes (hab_id 18)
    (18, 19, 'EXPERT', true, 4, CURRENT_TIMESTAMP),  -- AWS (hab_id 19)
    (18, 24, 'AVANCADO', true, 3, CURRENT_TIMESTAMP);  -- Gestão de Projetos (hab_id 24)



-- ========================================
-- 4. INSERT PROJETOS (Projects)
-- ========================================
INSERT INTO projetos (criador_id, titulo, descricao, status, vagas, vagas_preenchidas, data_fim, ativo, criado_em, atualizado_em) 
VALUES 
    (13, 'Plataforma de E-commerce', 
     'Desenvolvemos uma plataforma completa de e-commerce com Java Spring Boot, React e PostgreSQL. Buscamos desenvolvedores para frontend, backend e DevOps.',
     'ABERTO', 5, 1, NOW() + INTERVAL '3 months', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    (15, 'Sistema de Monitoramento em Tempo Real', 
     'Projeto que envolve desenvolvimento de um sistema de monitoramento de infraestrutura utilizando Kubernetes, Docker e ferramentas de observabilidade.',
     'ABERTO', 3, 1, NOW() + INTERVAL '4 months', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    (13, 'App Mobile com React Native', 
     'Desenvolvimento de aplicativo mobile multiplataforma utilizando React Native. Ideal para quem tem experiência com JavaScript/TypeScript e UI/UX.',
     'EM_ANDAMENTO', 4, 2, NOW() + INTERVAL '2 months', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    (14, 'Data Pipeline e Machine Learning', 
     'Projeto de processamento de dados em grande escala usando Python, PySpark e modelos de ML. Experiência com dados é essencial.',
     'ABERTO', 3, 0, NOW() + INTERVAL '5 months', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    (17, 'Arquitetura de Microsserviços', 
     'Projeto acadêmico que explora arquitetura de microsserviços com Spring Boot, Docker e Kubernetes. Ótimo para aprender DevOps.',
     'ABERTO', 6, 2, NOW() + INTERVAL '6 months', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);




-- ========================================
-- 5. INSERT PROJETO_HABILIDADES (Project Requirements)
-- ========================================
INSERT INTO projeto_habilidades (projeto_id, habilidade_id, obrigatoria) 
VALUES 
    -- Plataforma de E-commerce (projeto 1)
    (1, 1, true),   -- Java
    (1, 7, true),   -- Spring Boot
    (1, 3, true),   -- JavaScript
    (1, 8, true),   -- React
    (1, 13, true),  -- PostgreSQL
    (1, 16, false), -- Docker
    (1, 18, false), -- AWS
    
    -- Sistema de Monitoramento (projeto 2)
    (2, 1, false),  -- Java
    (2, 17, true),  -- Kubernetes
    (2, 16, true),  -- Docker
    (2, 18, true),  -- AWS
    (2, 23, false), -- Gestão de Projetos
    
    -- App Mobile (projeto 3)
    (3, 3, true),   -- JavaScript
    (3, 4, true),   -- TypeScript
    (3, 8, true),   -- React
    (3, 22, false), -- Comunicação
    
    -- Data Pipeline (projeto 4)
    (4, 2, true),   -- Python
    (4, 6, true),   -- SQL
    (4, 14, false), -- MongoDB
    (4, 18, false), -- AWS
    
    -- Microsserviços (projeto 5)
    (5, 1, true),   -- Java
    (5, 7, true),   -- Spring Boot
    (5, 16, true),  -- Docker
    (5, 17, true),  -- Kubernetes
    (5, 21, false); -- Liderança



-- ========================================
-- 6. INSERT PROJETO_MEMBROS (Project Members)
-- ========================================
INSERT INTO projeto_membros (projeto_id, usuario_id, funcao, data_adesao) 
VALUES 
    (1, 1, 'Arquiteto de Backend', CURRENT_TIMESTAMP),
    (1, 2, 'Lead Frontend', CURRENT_TIMESTAMP),
    (3, 2, 'Product Owner', CURRENT_TIMESTAMP),
    (3, 4, 'Data Analyst', CURRENT_TIMESTAMP),
    (2, 3, 'Líder de DevOps', CURRENT_TIMESTAMP),
    (5, 1, 'Mentor', CURRENT_TIMESTAMP),
    (5, 3, 'Instrutor DevOps', CURRENT_TIMESTAMP);



-- ========================================
-- 7. INSERT CANDIDATURAS (Applications)
-- ========================================
INSERT INTO candidaturas (projeto_id, usuario_id, status, mensagem, data_candidatura) 
VALUES 
    (1, 4, 'PENDENTE', 'Tenho experiência em análise de dados e gostaria de contribuir com insights.', 
     NOW() - INTERVAL '3 days'),
    
    (2, 1, 'PENDENTE', 'Possuo experiência avançada em Java e Spring Boot, muito interessado neste projeto.', 
     NOW() - INTERVAL '2 days'),
    
    (4, 2, 'PENDENTE', 'Apesar de minha especialidade ser frontend, gostaria de aprender mais sobre data science.',
     NOW() - INTERVAL '5 days'),
    
    (3, 4, 'ACEITO', 'Quero fazer parte deste projeto para expandir minhas habilidades em mobile.', 
     NOW() - INTERVAL '7 days'),
    
    (5, 2, 'PENDENTE', 'Interessada em aprender arquitetura de microsserviços com profissionais experientes.',
     NOW() - INTERVAL '1 day');



-- ========================================
-- 8. INSERT ENDOSSOS_PROFESSOR (Professor Endorsements)
-- ========================================
INSERT INTO endossos_professor (professor_id, aluno_id, habilidade_id, nivel, data_endosso) 
VALUES 
    (5, 1, 1, 'AVANCADO', CURRENT_TIMESTAMP),   -- Prof. Carlos endossa Java de João
    (5, 1, 7, 'AVANCADO', CURRENT_TIMESTAMP),   -- Prof. Carlos endossa Spring Boot de João
    (6, 4, 2, 'AVANCADO', CURRENT_TIMESTAMP),   -- Prof. Fernanda endossa Python de Ana
    (5, 2, 8, 'AVANCADO', CURRENT_TIMESTAMP),   -- Prof. Carlos endossa React de Maria
    (6, 3, 17, 'AVANCADO', CURRENT_TIMESTAMP),  -- Prof. Fernanda endossa Kubernetes de Pedro
    (6, 3, 18, 'AVANCADO', CURRENT_TIMESTAMP);  -- Prof. Fernanda endossa AWS de Pedro

-- ========================================
-- SEED DATA INSERTION COMPLETE
-- ========================================