# 🏗️ Architecture Documentation

## Visão Geral da Arquitetura

O **Academico+** segue uma arquitetura **Cliente-Servidor** com separação clara de responsabilidades entre Frontend, Backend e Database. O design foi pensado para ser escalável, mantível e testável.

---

## 🎯 Padrões Arquiteturais

### 1. **MVC (Model-View-Controller)**

O backend utiliza o padrão MVC através do Spring Boot:

```
Request HTTP
    ↓
[Controller] → Recebe requisição
    ↓
[Service] → Lógica de negócio
    ↓
[Repository] → Acesso a dados
    ↓
[Database] → Persistência
```

### 2. **Component-Based Architecture (Frontend)**

O frontend utiliza componentes reutilizáveis em React/Next.js:

```
App
├── Layout
│   ├── Header
│   ├── Sidebar
│   └── Footer
├── Pages
│   ├── LoginPage
│   ├── DashboardPage
│   └── ProjectPage
└── Components
    ├── ProjectCard
    ├── UserProfile
    └── SearchBar
```

### 3. **Layered Architecture**

```
┌──────────────────────────────────────┐
│     Presentation Layer               │
│  (Controllers, DTOs, API)            │
├──────────────────────────────────────┤
│     Business Logic Layer             │
│  (Services, Algorithms, Rules)       │
├──────────────────────────────────────┤
│     Persistence Layer                │
│  (Repositories, DAOs)                │
├──────────────────────────────────────┤
│     Data Layer                       │
│  (Entities, Models)                  │
├──────────────────────────────────────┤
│     Database Layer                   │
│  (PostgreSQL)                        │
└──────────────────────────────────────┘
```

---

## 📦 Componentes Principais

### Backend (Java + Spring Boot)

```
academico-plus-api/
├── src/main/java/com/unifapi/academico/
│   ├── api/
│   │   ├── controllers/           # REST Endpoints
│   │   │   ├── AuthController
│   │   │   ├── UserController
│   │   │   ├── ProjectController
│   │   │   ├── CompetencyController
│   │   │   └── RecommendationController
│   │   └── dto/                   # Data Transfer Objects
│   │       ├── AuthDTO
│   │       ├── UserDTO
│   │       ├── ProjectDTO
│   │       └── CompetencyDTO
│   │
│   ├── domain/
│   │   ├── entities/              # JPA Entities
│   │   │   ├── User
│   │   │   ├── Project
│   │   │   ├── Competency
│   │   │   ├── UserCompetency
│   │   │   └── Endorsement
│   │   └── enums/
│   │       ├── UserType
│   │       ├── ProjectType
│   │       └── ProficiencyLevel
│   │
│   ├── service/
│   │   ├── AuthService            # Autenticação
│   │   ├── UserService            # Gestão de usuários
│   │   ├── ProjectService         # Gestão de projetos
│   │   ├── CompetencyService      # Gestão de competências
│   │   ├── RecommendationService  # Algoritmo de Jaccard
│   │   └── EmailService           # Envio de emails
│   │
│   ├── repository/
│   │   ├── UserRepository
│   │   ├── ProjectRepository
│   │   ├── CompetencyRepository
│   │   └── RecommendationRepository
│   │
│   ├── security/
│   │   ├── JwtTokenProvider       # Geração/validação JWT
│   │   ├── SecurityConfig         # Configuração Spring Security
│   │   └── AuthenticationFilter   # Filtro de autenticação
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler # Tratamento de erros
│   │   ├── ResourceNotFoundException
│   │   └── ValidationException
│   │
│   ├── util/
│   │   ├── JaccardSimilarity      # Cálculo de similaridade
│   │   └── ValidationUtils
│   │
│   └── AcademicoApplication.java  # Classe principal
│
└── src/main/resources/
    ├── application.properties       # Configurações
    ├── application-dev.properties   # Dev
    ├── application-prod.properties  # Produção
    └── db/migration/                # Scripts SQL (Flyway)
```

### Frontend (Next.js + TypeScript)

```
academico-plus-frontend/
├── app/
│   ├── layout.tsx                 # Layout principal
│   ├── page.tsx                   # Home page
│   ├── login/
│   │   ├── page.tsx
│   │   └── LoginForm.tsx
│   ├── dashboard/
│   │   ├── page.tsx
│   │   ├── layout.tsx
│   │   └── DashboardContent.tsx
│   ├── projects/
│   │   ├── page.tsx               # Mural de projetos
│   │   ├── [id]/
│   │   │   └── page.tsx           # Detalhe do projeto
│   │   └── create/
│   │       └── page.tsx           # Criar projeto
│   ├── profile/
│   │   ├── [id]/page.tsx          # Perfil do usuário
│   │   └── edit/page.tsx          # Editar perfil
│   └── messages/
│       └── page.tsx               # Centro de mensagens
│
├── components/
│   ├── common/                    # Componentes reutilizáveis
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   ├── Footer.tsx
│   │   ├── Button.tsx
│   │   └── Card.tsx
│   ├── project/
│   │   ├── ProjectCard.tsx
│   │   ├── ProjectList.tsx
│   │   └── ProjectForm.tsx
│   ├── user/
│   │   ├── UserProfile.tsx
│   │   ├── UserCard.tsx
│   │   └── CompetencyBadge.tsx
│   └── search/
│       ├── SearchBar.tsx
│       ├── FilterPanel.tsx
│       └── SearchResults.tsx
│
├── lib/
│   ├── api.ts                     # Cliente HTTP
│   ├── auth.ts                    # Lógica de autenticação
│   ├── hooks.ts                   # Custom hooks
│   ├── utils.ts                   # Utilidades
│   └── types.ts                   # TypeScript types
│
├── styles/
│   ├── globals.css
│   ├── variables.css
│   └── components.css
│
├── public/
│   ├── images/
│   └── icons/
│
└── package.json
```

---

## 🔌 Integração Frontend-Backend

### Fluxo de Dados

```
User Interaction (Frontend)
    ↓
Component State Update (React)
    ↓
API Call (axios/fetch)
    ↓
HTTP Request → Backend
    ↓
Controller Processing
    ↓
Service Layer Logic
    ↓
Database Query
    ↓
JSON Response → Frontend
    ↓
Component Re-render
```

### Endpoints da API

```
BASE_URL = /api/v1

AUTH:
  POST   /auth/register
  POST   /auth/login
  POST   /auth/refresh
  POST   /auth/logout

USERS:
  GET    /users/:id
  PUT    /users/:id
  GET    /users/search
  GET    /users/:id/competencies

PROJECTS:
  GET    /projects
  GET    /projects/:id
  POST   /projects
  PUT    /projects/:id
  DELETE /projects/:id
  GET    /projects/:id/members
  POST   /projects/:id/members
  GET    /projects/:id/recommendations

COMPETENCIES:
  GET    /competencies
  POST   /competencies
  GET    /competencies/:id

ENDORSEMENTS:
  POST   /users/:userId/competencies/:competencyId/endorse
  GET    /users/:userId/endorsements

MESSAGES:
  GET    /messages
  POST   /messages
  GET    /messages/:id
  PUT    /messages/:id/read

RECOMMENDATIONS:
  GET    /projects/:id/recommendations
  GET    /recommendations/for-user
```

---

## 🔐 Segurança

### Autenticação (JWT)

```
Login Request
    ↓
Validate Credentials
    ↓
Generate JWT Token
    ↓
Return Token to Frontend
    ↓
Frontend stores in localStorage
    ↓
Include in Authorization Header for subsequent requests
    ↓
Backend validates token
    ↓
Grant access if valid
```

### JWT Payload Exemplo

```json
{
  "sub": "4",
  "email": "aluno.carlos@unifapi.edu.br",
  "name": "Carlos Eduardo Viana",
  "userType": "STUDENT",
  "roles": ["USER"],
  "iat": 1687345600,
  "exp": 1687432000
}
```

### Proteção CORS

```
Frontend: http://localhost:3000
Backend: http://localhost:8080

Configuração CORS:
  allowedOrigins: ["http://localhost:3000", "https://academico-plus.railway.app"]
  allowedMethods: ["GET", "POST", "PUT", "DELETE"]
  allowedHeaders: ["Content-Type", "Authorization"]
```

---

## 🎯 Algoritmo de Recomendação (Jaccard Similarity)

### Conceito

```
Dados:
  A = Competências do Projeto {Java, Spring Boot, PostgreSQL}
  B = Competências do Aluno {Java, Spring Boot, Python, PostgreSQL}

Cálculo:
  Interseção (A ∩ B) = {Java, Spring Boot, PostgreSQL}     → 3
  União (A ∪ B) = {Java, Spring Boot, Python, PostgreSQL}  → 4
  
  Jaccard = |A ∩ B| / |A ∪ B| = 3 / 4 = 0.75
```

### Implementação

```java
public class JaccardSimilarity {
    public static double calculateSimilarity(
        Set<String> projectTags,
        Set<String> userTags
    ) {
        Set<String> intersection = new HashSet<>(projectTags);
        intersection.retainAll(userTags);
        
        Set<String> union = new HashSet<>(projectTags);
        union.addAll(userTags);
        
        if (union.isEmpty()) return 0;
        return (double) intersection.size() / union.size();
    }
}
```

### Fluxo de Recomendação

```
Projeto Criado
    ↓
Extrair Tags do Projeto
    ↓
For each Aluno:
    ├─ Extrair Tags do Aluno
    ├─ Calcular Jaccard(Projeto, Aluno)
    └─ Armazenar resultado
    ↓
Ordenar por similaridade (descendente)
    ↓
Retornar Top 5 recomendações
```

---

## 📊 Diagrama de Fluxo: Publicar Projeto

```
User (Frontend)
    │
    ├─ Preenche formulário
    │
    └─ Clica "Publicar"
         │
         └─ POST /api/v1/projects
              │
              └─ ProjectController.createProject()
                   │
                   ├─ Valida dados
                   │
                   └─ ProjectService.createProject()
                        │
                        ├─ Salva projeto no DB
                        │
                        └─ RecommendationService.generateRecommendations()
                             │
                             ├─ For each aluno:
                             │   ├─ Calcula Jaccard
                             │   └─ Salva no DB
                             │
                             └─ Retorna projeto + recomendações
                                  │
                                  └─ JSON Response
                                       │
                                       └─ Frontend atualiza UI
```

---

## 🧪 Testes

### Estratégia de Testes

```
Unit Tests
├─ Service Layer
├─ Utility Functions
└─ Jaccard Algorithm

Integration Tests
├─ Controller Endpoints
├─ Repository Queries
└─ Database Operations

E2E Tests
├─ Login Flow
├─ Project Creation
├─ Search & Filter
└─ Recommendation
```

### Exemplo Test

```java
@SpringBootTest
public class RecommendationServiceTest {
    
    @Autowired
    private RecommendationService service;
    
    @Test
    void testJaccardSimilarityCalculation() {
        double similarity = JaccardSimilarity.calculate(
            Set.of("Java", "Spring"),
            Set.of("Java", "Python")
        );
        assertEquals(0.5, similarity);
    }
}
```

---

## 📈 Performance

### Índices de Banco de Dados

- `users.email` - Busca de usuários por email
- `user_competencies.user_id` - Listar competências de um usuário
- `projects.status` - Filtrar projetos abertos
- `messages.is_read` - Notificações não lidas

### Caching Strategy

```
Cache Layer:
├─ Redis (opcional)
├─ Competencies (TTL: 24h)
├─ User Profiles (TTL: 1h)
└─ Search Results (TTL: 15min)
```

---

## 🚀 Deployment Architecture

### Desenvolvimento

```
Docker Compose:
├─ PostgreSQL (localhost:5432)
├─ Backend (localhost:8080)
├─ Frontend (localhost:3000)
└─ pgAdmin (localhost:5050)
```

### Produção (Railway)

```
Railway PaaS:
├─ Frontend (Next.js)
│   └─ Vercel deployment
├─ Backend (Spring Boot)
│   └─ Docker container
└─ Database (PostgreSQL managed)
    └─ Automatic backups
```

### CI/CD Pipeline

```
Git Push
    ↓
GitHub Actions
    ├─ Run Tests
    ├─ Build Docker Image
    ├─ Push to Railway
    └─ Deploy
    ↓
Production Environment
```

---

## 📝 Padrões de Código

### Nomenclatura

- **Classes:** PascalCase (`UserController`, `JwtTokenProvider`)
- **Métodos:** camelCase (`getUserById`, `calculateSimilarity`)
- **Constantes:** UPPER_SNAKE_CASE (`JWT_SECRET`, `MAX_MEMBERS`)
- **Variáveis:** camelCase (`firstName`, `projectId`)

### Exemplo de Classe Service

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDTO(user);
    }
    
    public void addCompetency(Long userId, Long competencyId) {
        User user = getUserEntity(userId);
        Competency competency = competencyRepository.findById(competencyId)
            .orElseThrow(() -> new ResourceNotFoundException("Competency not found"));
        
        user.addCompetency(competency);
        userRepository.save(user);
    }
}
```

---

## 🔄 Versionamento de API

Usar semantic versioning:

```
/api/v1/        # Versão 1
/api/v2/        # Versão 2 (futuro, breaking changes)
```

---

## 📚 Referências

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Next.js Documentation](https://nextjs.org/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Jaccard Similarity](https://en.wikipedia.org/wiki/Jaccard_index)

---

**Última atualização:** 18/06/2026
