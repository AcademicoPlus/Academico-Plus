# 🎓 Academico+
**Plataforma Digital para Fomento à Colaboração Interdisciplinar e Networking Acadêmico**

[![Status](https://img.shields.io/badge/Status-Development-yellow)]()
[![Java](https://img.shields.io/badge/Java-21-green)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)]()

---

## 📋 Visão Geral

O **Academico+** é uma plataforma web (MVP) desenvolvida para mapear competências e centralizar oportunidades acadêmicas dentro de uma mesma instituição. O sistema se propõe a oferecer:

- **Perfis profissionais** baseados em tags de habilidades
- **Mural de projetos** com busca e filtros
- **Sistema de avaliação** entre colegas que colaboraram no mesmo projeto
- **Recomendação** de projetos e colaboradores por compatibilidade de habilidades

### 🎯 Objetivos

#### Objetivo Geral
Desenvolver uma plataforma web (MVP) que ofereça projetos e permita busca por competências técnicas dentro de uma instituição acadêmica, facilitando a formação de equipes multidisciplinares.

#### Objetivos Específicos
- Implementar sistema de perfis baseado em habilidades (tags)
- Criar mural de oportunidades com busca e filtros
- Implementar um mecanismo de reputação entre colaboradores de projetos
- Projetar interface intuitiva e responsiva
- Implementar recomendação de projetos/colaboradores por compatibilidade de habilidades
- Avaliar usabilidade através de testes com usuários

---

## 🏗️ Arquitetura

O sistema é um monolito (Spring Boot) com duas partes extraídas como microserviços — decisão didática para o TCC, escolhidas por serem as de menor acoplamento com o restante do domínio (nenhuma dependência de repositório/entidade, um único ponto de chamada cada). Ver `academico-backend/DEPLOY.md` para o racional completo.

```
┌─────────────────────────────────────────────────────────────┐
│                  Frontend (React + Vite)                    │
│                     TypeScript                               │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              academico-backend (monolito)                   │
│         Java + Spring Security + JWT Auth                   │
│  - Controllers (REST APIs)                                  │
│  - Services (Business Logic)                                │
│  - Repositories (Data Access)                                │
│  - Recomendação por compatibilidade de habilidades           │
└──────┬──────────────────┬──────────────────┬─────────────────┘
       │ JDBC             │ RabbitMQ         │ REST (interno)
       ↓                  ↓ (fire-and-forget) ↓ (síncrono)
┌──────────────┐   ┌──────────────────────┐   ┌───────────────────────┐
│  PostgreSQL  │   │ academico-email-     │   │ academico-upload-     │
│              │   │ service              │   │ service               │
│              │   │ (envia OTP p/ Resend)│   │ (fotos → Cloudflare R2)│
└──────────────┘   └──────────────────────┘   └───────────────────────┘
```

---

## 🛠️ Stack Técnico

| Camada | Tecnologia | Situação |
|--------|-----------|--------|
| **Backend** | Java 21 + Spring Boot 3.5, Spring Security, JWT, Spring Data JPA | Em desenvolvimento ativo — ver [academico-backend/README.md](./academico-backend/README.md) |
| **Frontend** | React 19 + TypeScript + Vite | Projeto inicializado, sem telas implementadas ainda |
| **Database** | PostgreSQL 14+ | Schema mantido por `ddl-auto=update` (ver observação no README do backend) |
| **Auth** | JWT (JSON Web Tokens), sem refresh token | Em uso |

---

## 📁 Estrutura do Projeto

```
Academico-Plus/
├── academico-backend/         # Monolito Java/Spring Boot (ver README próprio)
│   ├── src/
│   ├── docker-compose.yml     # Postgres + pgAdmin + RabbitMQ para dev local
│   └── pom.xml
├── academico-email-service/   # Microserviço: consome RabbitMQ, envia e-mail via Resend
├── academico-upload-service/  # Microserviço: upload de fotos de perfil pro Cloudflare R2
├── frontend/                   # React + Vite (ainda no template inicial)
│   ├── src/
│   └── package.json
├── legal/                      # Termos de Uso e Política de Privacidade
├── .gitignore
└── README.md
```

## 📜 Termos e privacidade

Cadastro na plataforma exige aceite dos [Termos de Uso](./legal/TERMOS_DE_USO.md) e da [Política de Privacidade](./legal/POLITICA_DE_PRIVACIDADE.md) (LGPD). Usuários podem excluir a própria conta a qualquer momento.

---

## 🚀 Quick Start

### Backend

Veja o passo a passo completo em **[academico-backend/README.md](./academico-backend/README.md)** — configuração do `.env`, `docker-compose up -d` para o Postgres local, e `./mvnw spring-boot:run`.

### Frontend

Ainda é o template padrão do Vite (sem telas da aplicação). Para rodar o que existe:

```bash
cd frontend
npm install
npm run dev
```

### Deploy em produção

Ver **[academico-backend/DEPLOY.md](./academico-backend/DEPLOY.md)** — passo a passo para Render (os três serviços + Postgres), CloudAMQP (RabbitMQ) e Cloudflare R2 (storage de fotos de perfil).

---

## 🔑 Funcionalidades

### Fase 1: Autenticação & Perfis
- [x] Cadastro e login com JWT
- [x] Perfis com tags de competências (`/usuarios/me/habilidades`)
- [x] Upload de foto de perfil (`academico-upload-service` → Cloudflare R2)

### Fase 2: Mural & Busca
- [x] CRUD de projetos, candidaturas e equipe
- [x] Busca de projetos por texto e status
- [ ] Filtro de projetos por habilidade específica (hoje existe via recomendação/compatibilidade, não como filtro direto)

### Fase 3: Recomendação
- [x] Ranking de projetos/candidatos por compatibilidade de habilidades — adaptação do Índice de Jaccard (cobertura ponderada dos requisitos, não similaridade simétrica pura; detalhes no README do backend)
- [x] Sugestão de candidatos para o criador de um projeto

### Fase 4: Reputação & Networking
- [x] Avaliação entre colegas que participaram do mesmo projeto (nota 1-5 + comentário, após o projeto encerrar)
- [x] Denúncia de avaliação abusiva pelo avaliado, com resolução por admin
- [x] Exclusão de conta pelo próprio usuário (LGPD)
- [ ] Endosso de competências por professores (diferente do item acima — não implementado)
- [ ] Sistema de mensagens

### Frontend
- [ ] Todas as telas — o projeto React ainda é o template inicial do Vite

---

## 🧪 Testes

```bash
# Backend
cd academico-backend
./mvnw test

# Frontend — ainda sem testes escritos
cd frontend
npm test
```

---

## 📊 Banco de Dados

Tabelas atuais (ver `academico-backend/src/main/resources/db/migration` e as entidades JPA para o schema completo):

`usuarios`, `niveis_acesso`, `cursos`, `habilidades`, `usuario_habilidades`, `projetos`, `projeto_habilidades`, `projeto_membros`, `candidaturas`, `avaliacoes`, `tokens_revogados`, `projeto_cancelamento_aviso`.

---

## 🤝 Contribuindo

1. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
2. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
3. Push para a branch (`git push origin feature/AmazingFeature`)
4. Abra um Pull Request

---

## 📝 Licença

Uso pretendido sob licença MIT — arquivo `LICENSE` ainda não adicionado ao repositório.

---

## 👨‍💼 Autores

- **Carlos Eduardo Viana Alves**
- **Jefferson Mateus Lima Silva**
- **Luana Maria Alves Caldas**
- **Maria Eduarda Viana Nascimento**
- **Vitor Manoel da Silva Costa**

**Orientador:** Professor(a) Pedro Alex

**Instituição:** Centro Universitário do Piauí (UNIFAPI)
**Curso:** Bacharelado em Ciência da Computação
**Data:** Junho de 2026

---

**Desenvolvido com ❤️ para a comunidade acadêmica**
