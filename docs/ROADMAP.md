# 📅 Roadmap & Development Timeline

## 🎯 Visão Geral do Projeto

```
v0.1 (Setup)        v0.2 (Backend)      v0.3 (Frontend)     v0.4 (Features)     v1.0 (MVP)
├─ Database         ├─ APIs             ├─ Pages            ├─ Recommendation   ├─ Polish
├─ Infra            ├─ Auth             ├─ Components       ├─ Endorsements     ├─ Tests
├─ Docker           ├─ CRUD             ├─ Styling          ├─ Messages         ├─ Deploy
└─ Docs             └─ Validation       └─ Responsivo       └─ Integration      └─ Release
```

---

## 📊 Versão 0.1 - Setup & Database ✅ (CONCLUÍDO)

**Duração:** 1 semana (18/06 - 25/06)  
**Status:** ✅ Completo

### Atividades

| Task | Status | Responsável |
|------|--------|-------------|
| ✅ Estrutura de pastas | Concluído | Equipe |
| ✅ Documentação (README, ARCHITECTURE, DATABASE) | Concluído | Equipe |
| ✅ Docker Compose setup | Concluído | Equipe |
| ✅ PostgreSQL schema | Concluído | Equipe |
| ✅ Seed data | Concluído | Equipe |
| ✅ .env & configurações | Concluído | Equipe |
| ✅ Git & GitHub setup | Concluído | Equipe |

### Entregáveis

- [x] README.md completo
- [x] ARCHITECTURE.md
- [x] DATABASE.md
- [x] SETUP.md
- [x] docker-compose.yml
- [x] schema.sql
- [x] seed-data.sql
- [x] .gitignore
- [x] .env.example

---

## 🔧 Versão 0.2 - Backend API Implementation

**Duração:** 3-4 semanas  
**Período:** 25/06 - 23/07  
**Responsáveis:** Carlos, Jefferson, Vitor

### Spring Boot Project Setup

```
Semana 1:
├─ Criar projeto Maven com Spring Boot
├─ Configurar application.properties (dev/prod)
├─ Configurar Spring Data JPA + PostgreSQL
├─ Configurar Spring Security + JWT
└─ Setup estrutura de pacotes
```

### Implementação de Entities

```java
Week 1-2:
├─ User.java
├─ Competency.java
├─ UserCompetency.java
├─ Project.java
├─ ProjectRequirement.java
├─ ProjectMember.java
├─ Endorsement.java
├─ Message.java
└─ Recommendation.java
```

### Implementação de Repositories

```
Week 2:
├─ UserRepository (JpaRepository)
├─ CompetencyRepository
├─ ProjectRepository
├─ RecommendationRepository
└─ MessageRepository
```

### Implementação de Services

```
Week 2-3:
├─ AuthService
│   ├─ register()
│   ├─ login()
│   └─ refreshToken()
├─ UserService
│   ├─ getUserById()
│   ├─ updateUser()
│   ├─ addCompetency()
│   └─ getCompetencies()
├─ ProjectService
│   ├─ createProject()
│   ├─ getProjectById()
│   ├─ updateProject()
│   ├─ deleteProject()
│   ├─ addMember()
│   └─ getRecommendations()
├─ CompetencyService
│   ├─ getAllCompetencies()
│   └─ createCompetency()
├─ RecommendationService
│   ├─ generateRecommendations()
│   └─ calculateJaccard()
└─ EndorsementService
    ├─ endorseCompetency()
    └─ getEndorsements()
```

### Implementação de Controllers (REST APIs)

```
Week 3:
├─ AuthController
│   ├─ POST /auth/register
│   ├─ POST /auth/login
│   └─ POST /auth/refresh
├─ UserController
│   ├─ GET /users/:id
│   ├─ PUT /users/:id
│   ├─ GET /users/search
│   └─ POST /users/:id/competencies
├─ ProjectController
│   ├─ GET /projects
│   ├─ POST /projects
│   ├─ GET /projects/:id
│   ├─ PUT /projects/:id
│   ├─ DELETE /projects/:id
│   └─ GET /projects/:id/recommendations
├─ CompetencyController
│   ├─ GET /competencies
│   └─ POST /competencies
└─ EndorsementController
    ├─ POST /users/:userId/endorse
    └─ GET /endorsements
```

### Security & JWT

```
Week 3-4:
├─ JwtTokenProvider
├─ JwtAuthenticationFilter
├─ AuthenticationController
├─ SecurityConfig
└─ Exception handlers
```

### Testes Unitários

```
Week 4:
├─ Unit tests para Services
├─ Unit tests para Controllers
├─ Unit tests para Repositories
└─ Integration tests
```

### Entregáveis

- [x] Backend compilando
- [x] Todas as APIs implementadas
- [x] Testes com 80%+ cobertura
- [x] Documentação Swagger/OpenAPI
- [x] Docker image do backend

### Cronograma Detalhado

| Data | Atividade | Owner |
|------|-----------|-------|
| 25/06 | Setup Maven, Dependencies | Carlos |
| 26/06 | Entities & Repositories | Jefferson |
| 27/06 | Repositories & Basic Services | Vitor |
| 28/06 | Auth Service & JWT | Carlos |
| 29/06 | User & Project Services | Jefferson |
| 30/06 | Competency & Endorsement Services | Vitor |
| 01/07 | Controllers - Part 1 | Carlos |
| 02/07 | Controllers - Part 2 | Jefferson |
| 03/07 | Integration & Testing | Vitor |
| 04/07 | Testing & Bug fixes | Equipe |
| 05/07 | Documentation | Equipe |
| 06/07 | Buffer / Code Review | Equipe |

---

## 🎨 Versão 0.3 - Frontend Implementation

**Duração:** 3 semanas  
**Período:** 23/07 - 13/08  
**Responsáveis:** Luana, Eduarda

### Setup Next.js Project

```
Week 1:
├─ Criar projeto Next.js com TypeScript
├─ Configurar Tailwind CSS / Styled Components
├─ Configurar .env.local
├─ Setup API client (axios/fetch)
└─ Setup folder structure
```

### Implementação de Páginas

```
Week 1-2:
├─ pages/login.tsx
├─ pages/dashboard.tsx
├─ pages/projects/index.tsx
├─ pages/projects/[id].tsx
├─ pages/projects/create.tsx
├─ pages/profile/[id].tsx
├─ pages/profile/edit.tsx
└─ pages/messages.tsx
```

### Implementação de Componentes

```
Week 2:
├─ components/Header
├─ components/Sidebar
├─ components/ProjectCard
├─ components/UserProfile
├─ components/CompetencyBadge
├─ components/SearchBar
├─ components/FilterPanel
└─ components/Forms
```

### Context & State Management

```
Week 2:
├─ AuthContext
├─ UserContext
├─ ProjectContext
└─ Hooks customizados
```

### Integração com Backend

```
Week 2-3:
├─ Conectar Login
├─ Conectar CRUD de Projetos
├─ Conectar Busca & Filtros
├─ Conectar Competências
└─ Conectar Recomendações
```

### Styling & Responsividade

```
Week 3:
├─ Mobile (< 768px)
├─ Tablet (768px - 1024px)
├─ Desktop (> 1024px)
├─ Testes em diferentes browsers
└─ Acessibilidade (WCAG 2.1)
```

### Testes Frontend

```
Week 3:
├─ Unit tests (Jest)
├─ Component tests (React Testing Library)
└─ E2E tests (Playwright/Cypress)
```

### Entregáveis

- [x] Frontend compila
- [x] Todas as páginas implementadas
- [x] Responsivo em todos os devices
- [x] Integração com backend
- [x] Testes com 70%+ cobertura

### Cronograma Detalhado

| Data | Atividade | Owner |
|------|-----------|-------|
| 23/07 | Setup Next.js, Tailwind | Luana |
| 24/07 | Pages setup | Eduarda |
| 25/07 | Auth pages | Luana |
| 26/07 | Dashboard & Project pages | Eduarda |
| 27/07 | Profile pages | Luana |
| 28/07 | Components - Part 1 | Eduarda |
| 29/07 | Components - Part 2 | Luana |
| 30/07 | API Integration - Auth | Eduarda |
| 31/07 | API Integration - Projects | Luana |
| 01/08 | API Integration - Search | Eduarda |
| 02/08 | Styling & Responsividade | Luana |
| 03/08 | Testing | Equipe |
| 04/08 | Bug fixes & Polish | Equipe |

---

## 🚀 Versão 0.4 - Advanced Features

**Duração:** 2 semanas  
**Período:** 13/08 - 27/08  
**Responsáveis:** Carlos, Jefferson, Luana, Eduarda

### Algoritmo de Recomendação

```
Week 1:
├─ Implementar JaccardSimilarity
├─ Integrar no RecommendationService
├─ Testar com dados reais
├─ Otimizar performance
└─ Adicionar caching
```

### Sistema de Endossos

```
Week 1:
├─ UI para endossar competências (Professor)
├─ Notificações de endosso
├─ Visualizar endossos no perfil
└─ Testes de validação
```

### Sistema de Mensagens

```
Week 2:
├─ Página de mensagens
├─ Chat em tempo real (WebSocket)
├─ Notificações
├─ Histórico de mensagens
└─ Testes
```

### Search & Filtros Avançados

```
Week 2:
├─ Full-text search
├─ Filtros por competência
├─ Filtros por tipo de projeto
├─ Filtros por experiência
└─ Sorting e paginação
```

### Entregáveis

- [x] Recomendações funcionando
- [x] Endossos implementados
- [x] Mensagens funcionando
- [x] Busca avançada
- [x] Testes E2E

---

## ✅ Versão 1.0 - MVP Release

**Duração:** 1-2 semanas  
**Período:** 27/08 - 10/09

### Testes Finais

```
├─ Teste de carga (Load testing)
├─ Teste de segurança (Security audit)
├─ Teste de usabilidade (User testing)
└─ Teste de compatibilidade (Browsers)
```

### Otimizações

```
├─ Performance (Frontend & Backend)
├─ SEO (Next.js)
├─ Database (Indexes, Query optimization)
└─ Caching (Redis)
```

### Documentação

```
├─ API Documentation (Swagger)
├─ User Guide
├─ Developer Guide
└─ Deployment Guide
```

### Deploy

```
├─ Configure Railway
├─ Setup CI/CD (GitHub Actions)
├─ Database backups
├─ Monitoring (Sentry, NewRelic)
└─ Go live!
```

### Entregáveis

- [x] MVP completo e funcional
- [x] Documentação completa
- [x] Deploy em produção
- [x] Monitoramento ativo
- [x] Suporte pós-lançamento

---

## 📈 Métricas de Qualidade

### Code Quality

```
├─ Code Coverage: 80%+
├─ Code Review: 100% das PRs
├─ Linting: 0 warnings
└─ Static Analysis: 0 issues críticos
```

### Performance

```
├─ Frontend: Lighthouse 90+
├─ Backend: Response time < 500ms
├─ Database: Query time < 100ms
└─ Load: 1000+ concurrent users
```

### Uptime

```
└─ Target: 99.9% uptime
```

---

## 🎯 Milestones

```
┌─────────────────────────────────────────┐
│ Jun 18 - Setup & Database ✅           │
│ Jul 06 - Backend API v1                │
│ Aug 04 - Frontend v1                   │
│ Aug 27 - Advanced Features              │
│ Sep 10 - MVP Release 🚀                │
└─────────────────────────────────────────┘
```

---

## 📝 Notas Importantes

1. **Flexibilidade:** Datas podem mudar baseado em progresso real
2. **Comunicação:** Status updates semanais (segundas)
3. **Code Review:** Todas as PRs devem ser revisadas
4. **Testing:** TDD (Test-Driven Development) onde possível
5. **Documentation:** Documentação deve ser atualizada junto com código

---

## 🔗 Links Úteis

- [GitHub Repository](https://github.com/seu-usuario/academico-plus)
- [Trello Board](https://trello.com/) (criar)
- [Slack Channel](https://slack.com/) (criar)
- [Documentation Wiki](./docs)

---

**Próxima Reunião:** 25/06/2026 às 10:00 (Kick-off do Backend)

---

**Última atualização:** 18/06/2026
