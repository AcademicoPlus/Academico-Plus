# 🎓 Academico+
**Plataforma Digital para Fomento à Colaboração Interdisciplinar e Networking Acadêmico**

[![Status](https://img.shields.io/badge/Status-Development-yellow)]()
[![License](https://img.shields.io/badge/License-MIT-blue)]()
[![Java](https://img.shields.io/badge/Java-17+-green)]()
[![Node](https://img.shields.io/badge/Node-18+-blue)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)]()

---

## 📋 Visão Geral

O **Academico+** é uma plataforma web (MVP) desenvolvida para mapear competências e centralizar oportunidades acadêmicas dentro de uma mesma instituição. O sistema funciona por meio de:

- **Perfis profissionais** baseados em tags de habilidades
- **Mural de projetos** com filtros inteligentes
- **Sistema de endosso** onde professores validam competências
- **Algoritmo de recomendação** para sugerir colaboradores compatíveis

### 🎯 Objetivos

#### Objetivo Geral
Desenvolver uma plataforma web (MVP) que ofereça projetos e permita busca por competências técnicas dentro de uma instituição acadêmica, facilitando a formação de equipes multidisciplinares.

#### Objetivos Específicos
- Implementar sistema de perfis baseado em habilidades (tags)
- Criar mural de oportunidades com filtros inteligentes
- Desenvolver mecanismo de validação de competências por professores
- Projetar interface intuitiva e responsiva
- Implementar algoritmo de recomendação usando Índice de Similaridade de Jaccard
- Avaliar usabilidade através de testes com usuários

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (Next.js)                      │
│              React Components + TypeScript                  │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot)                      │
│         Java + Spring Framework + JWT Auth                  │
│  - Controllers (REST APIs)                                  │
│  - Services (Business Logic)                                │
│  - Repositories (Data Access)                               │
│  - Recommendation Engine (Jaccard Similarity)               │
└────────────────────────┬────────────────────────────────────┘
                         │ JDBC
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              Database (PostgreSQL)                          │
│         Relational Database + Persistence Layer             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Stack Técnico

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| **Frontend** | Next.js + TypeScript | 14.x |
| **Backend** | Java + Spring Boot | 3.x |
| **Database** | PostgreSQL | 14+ |
| **Auth** | JWT (JSON Web Tokens) | - |
| **Deployment** | Railway (PaaS) | - |
| **Version Control** | Git + GitHub | - |

---

## 📁 Estrutura do Projeto

```
academico-plus/
├── backend/                    # API Java/Spring Boot
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                   # Next.js App
│   ├── app/
│   ├── components/
│   ├── package.json
│   └── Dockerfile
├── database/                   # Schemas e Scripts SQL
│   ├── migrations/
│   ├── schema.sql
│   └── seed-data.sql
├── docs/                       # Documentação
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DATABASE.md
│   └── SETUP.md
├── infra/                      # Configurações de Deploy
│   ├── docker-compose.yml
│   └── railway.yml
├── .github/                    # GitHub Actions
│   └── workflows/
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## 🚀 Quick Start

### Pré-requisitos
- Docker & Docker Compose
- Git
- (Opcional) Java 17+, Node.js 18+, PostgreSQL 14+

### Instalação Rápida (Docker)

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/academico-plus.git
cd academico-plus

# 2. Crie arquivo .env (copiar de .env.example)
cp .env.example .env

# 3. Inicie os containers
docker-compose up -d

# 4. Acesse a aplicação
# Frontend: http://localhost:3000
# API: http://localhost:8080
# pgAdmin: http://localhost:5050
```

### Instalação Manual

Veja [SETUP.md](./docs/SETUP.md) para instruções detalhadas.

---

## 📚 Documentação

- **[ARCHITECTURE.md](./docs/ARCHITECTURE.md)** - Design da arquitetura
- **[DATABASE.md](./docs/DATABASE.md)** - Schema e relacionamentos
- **[API.md](./docs/API.md)** - Documentação da API REST
- **[SETUP.md](./docs/SETUP.md)** - Guia de instalação completo
- **[DEVELOPMENT.md](./docs/DEVELOPMENT.md)** - Guia para desenvolvedores

---

## 🔑 Funcionalidades (MVP)

### ✅ Fase 1: Autenticação & Perfis
- [x] Sistema de cadastro (alunos/professores)
- [x] Login com JWT
- [x] Perfis com tags de competências
- [ ] Upload de foto de perfil

### ✅ Fase 2: Mural & Busca
- [ ] Mural de oportunidades/projetos
- [ ] Filtros por área e competência
- [ ] Busca avançada

### ✅ Fase 3: Recomendação
- [ ] Algoritmo de Jaccard Similarity
- [ ] Sugestão automática de colaboradores
- [ ] Ranking de compatibilidade

### ✅ Fase 4: Endosso & Networking
- [ ] Sistema de endosso por professores
- [ ] Validação de competências
- [ ] Sistema de mensagens

---

## 🧪 Testes

```bash
# Backend - Testes unitários
cd backend
mvn test

# Frontend - Testes
cd frontend
npm test
```

---

## 📊 Banco de Dados

### Tabelas Principais
- `users` - Usuários (alunos/professores)
- `competencies` - Competências/Tags
- `user_competencies` - Mapeamento de habilidades
- `projects` - Projetos/Oportunidades
- `project_requirements` - Requisitos dos projetos
- `endorsements` - Validações por professores
- `messages` - Sistema de mensagens

Veja [DATABASE.md](./docs/DATABASE.md) para schema completo.

---

## 🤝 Contribuindo

1. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
2. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
3. Push para a branch (`git push origin feature/AmazingFeature`)
4. Abra um Pull Request

---

## 📋 Roadmap

- **v0.1** (Atual) - Setup & Database Design
- **v0.2** - Backend API Básica
- **v0.3** - Frontend Inicial
- **v0.4** - Algoritmo de Recomendação
- **v0.5** - Testes & Polimento
- **v1.0** - MVP Completo

---

## 📝 Licença

Este projeto está licenciado sob a Licença MIT - veja [LICENSE](./LICENSE) para detalhes.

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

## 📞 Suporte

Para dúvidas ou problemas, abra uma [Issue](https://github.com/seu-usuario/academico-plus/issues).

---

**Desenvolvido com ❤️ para a comunidade acadêmica**
