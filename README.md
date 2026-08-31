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
| **Frontend** | React 19 + TypeScript + Vite, React Router, Tailwind CSS | Em desenvolvimento ativo — telas de autenticação, dashboard, projetos, candidaturas e um painel de administração já implementadas |
| **Database** | PostgreSQL 14+ | Schema versionado via Flyway (`ddl-auto=validate`) |
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
├── frontend/                   # React + Vite + TypeScript (SPA com rotas e telas reais)
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

```bash
cd frontend
cp .env.example .env   # ajuste VITE_API_URL se o backend não estiver em localhost:8080
npm install
npm run dev
```

Dá pra rodar as telas sem o backend de pé, com dados fictícios: defina `VITE_USE_MOCKS=true`
no `.env` (reinicie o `npm run dev` depois de mudar). Ver
**[docs/CONECTANDO_FRONT_BACKEND.md](./docs/CONECTANDO_FRONT_BACKEND.md)** para o guia completo
de como o frontend fala com a API (camadas, padrão de tela nova, mocks).

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
- [x] Autenticação (login, cadastro, termos de uso, política de privacidade)
- [x] Dashboard com projetos recentes, recomendações e indicadores
- [x] Explorar/buscar projetos, ver detalhes, criar e editar projeto
- [x] Candidatar-se a um projeto, acompanhar e cancelar candidaturas (`/candidaturas`), e o
      criador aceitar/rejeitar candidatos direto na tela de detalhes
- [x] Painel de administração (`/admin`): usuários e nível de acesso, catálogo de habilidades,
      fila de denúncias — restrito a quem tem `nivelAcesso` ADMIN
- [ ] Tela de perfil próprio (o menu já linka pra `/perfil`, mas a rota ainda não existe)
- [ ] "Encontrar Pessoas" (`/pessoas`) — hoje é só um placeholder, sem busca implementada
- [ ] Avaliação entre colegas de projeto e recomendação de candidatos (services prontos, sem tela)
- [ ] Testes automatizados de frontend

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
