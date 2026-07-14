# 🤝 Contributing Guide

Este documento descreve como contribuir para o projeto **Academico+**.

---

## 📋 Índice

- [Código de Conduta](#código-de-conduta)
- [Como Começar](#como-começar)
- [Git Workflow](#git-workflow)
- [Padrões de Código](#padrões-de-código)
- [Enviando Pull Request](#enviando-pull-request)
- [Code Review](#code-review)
- [Testes](#testes)

---

## 📜 Código de Conduta

### Nossa Comunidade

Nos comprometemos a fornecer um ambiente acolhedor e inclusivo para todos.

### Nossos Padrões

Exemplos de comportamento que contribuem para um ambiente positivo:

- Usar linguagem acolhedora e inclusiva
- Ser respeitoso com pontos de vista e experiências diferentes
- Aceitar crítica construtiva com graciosidade
- Focar no que é melhor para a comunidade
- Mostrar empatia com outros membros da comunidade

### Aplicação

Instâncias de comportamento abusivo, de assédio ou inaceitável podem ser reportadas aos mantenedores do projeto.

---

## 🚀 Como Começar

### 1. Fork e Clone

```bash
# Fork o repositório no GitHub
# (Clique no botão "Fork")

# Clone seu fork
git clone https://github.com/SEU_USERNAME/academico-plus.git
cd academico-plus

# Adicione o repositório original como upstream
git remote add upstream https://github.com/original-owner/academico-plus.git
```

### 2. Configure seu Ambiente

```bash
# Copie as variáveis de ambiente
cp .env.example .env

# Inicie o Docker
docker-compose up -d

# Para Backend: vá para a seção Backend
# Para Frontend: vá para a seção Frontend
```

### 3. Crie uma Branch

```bash
# Sincronize com a branch main
git fetch upstream
git checkout main
git merge upstream/main

# Crie uma nova branch
git checkout -b feature/sua-feature
# ou
git checkout -b bugfix/seu-bugfix
# ou
git checkout -b docs/sua-documentacao
```

---

## 🌳 Git Workflow

### Nomes de Branches

Use prefixos para categorizar suas branches:

```
feature/     - Nova funcionalidade
bugfix/      - Correção de bug
hotfix/      - Correção urgente
docs/        - Documentação
refactor/    - Refatoração de código
test/        - Testes
```

**Exemplos:**

```bash
git checkout -b feature/user-authentication
git checkout -b bugfix/fix-jaccard-calculation
git checkout -b docs/api-documentation
```

### Commits

Use mensagens de commit claras e descritivas:

**Formato:**

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Tipos:**

- `feat`: Nova feature
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação (sem mudança de lógica)
- `refactor`: Refatoração de código
- `test`: Testes
- `chore`: Build, dependências, etc

**Exemplos:**

```bash
git commit -m "feat(auth): add JWT token refresh endpoint"
git commit -m "fix(recommendation): correct Jaccard similarity calculation"
git commit -m "docs(api): add endpoint documentation"
git commit -m "test(user): add unit tests for UserService"
```

### Push e Pull Request

```bash
# Push sua branch
git push origin feature/sua-feature

# Crie Pull Request no GitHub
# (Clique no botão "Compare & pull request")
```

---

## 💻 Padrões de Código

### Backend (Java)

#### Estrutura de Pacotes

```
com.unifapi.academico
├── api/              # Controllers e DTOs
├── domain/           # Entities
├── service/          # Lógica de negócio
├── repository/       # Acesso a dados
├── security/         # Autenticação e autorização
└── exception/        # Exceções customizadas
```

#### Exemplo de Entity

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String fullName;
    
    @Enumerated(EnumType.STRING)
    private UserType userType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

#### Exemplo de Service

```java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDTO(user);
    }
    
    public UserDTO createUser(UserRegistrationDTO dto) {
        validateEmail(dto.getEmail());
        
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPasswordHash(hashPassword(dto.getPassword()));
        
        User savedUser = userRepository.save(user);
        return new UserDTO(savedUser);
    }
}
```

#### Exemplo de Controller

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserRegistrationDTO dto) {
        UserDTO createdUser = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
}
```

### Frontend (TypeScript/React)

#### Estrutura

```
components/
├── common/           # Componentes reutilizáveis
├── layout/           # Layout, Header, Sidebar
├── user/             # Componentes de usuário
└── project/          # Componentes de projeto

pages/
├── auth/             # Login, Cadastro
├── dashboard/        # Dashboard
├── projects/         # Projetos
└── profile/          # Perfil

lib/
├── api.ts            # Cliente HTTP
├── auth.ts           # Lógica de autenticação
└── types.ts          # TypeScript types
```

#### Exemplo de Componente

```typescript
interface UserCardProps {
  user: User;
  onSelect?: (userId: number) => void;
}

export const UserCard: React.FC<UserCardProps> = ({ user, onSelect }) => {
  return (
    <div className="user-card" onClick={() => onSelect?.(user.id)}>
      <img src={user.profilePhotoUrl} alt={user.fullName} />
      <h3>{user.fullName}</h3>
      <p>{user.academicInfo}</p>
      <div className="competencies">
        {user.competencies.map((comp) => (
          <CompetencyBadge key={comp.id} competency={comp} />
        ))}
      </div>
    </div>
  );
};
```

#### Exemplo de Hook Customizado

```typescript
export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      validateToken(token);
    } else {
      setLoading(false);
    }
  }, []);
  
  const login = async (email: string, password: string) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const { token, user } = response.data;
      
      localStorage.setItem('token', token);
      setUser(user);
      
      return user;
    } catch (error) {
      setError('Login failed');
      throw error;
    }
  };
  
  return { user, loading, error, login };
};
```

---

## 📮 Enviando Pull Request

### Checklist Antes de Enviar

- [ ] Código segue os padrões do projeto
- [ ] Testes foram adicionados/atualizados
- [ ] Testes passam localmente
- [ ] Documentação foi atualizada
- [ ] Commits têm mensagens claras
- [ ] Nenhum console.log() ou debugger deixado
- [ ] Sem arquivos desnecessários

### Template de PR

```markdown
## Descrição
Breve descrição do que esta PR faz.

## Tipo de Mudança
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Como Testar
Descreva os passos para testar a funcionalidade:
1. Abra a página X
2. Clique em Y
3. Observe Z

## Screenshots (se aplicável)
Adicione screenshots das mudanças visuais.

## Checklist
- [ ] Testes passam
- [ ] Documentação atualizada
- [ ] Sem breaking changes
```

---

## 🔍 Code Review

### Expectativas de Review

Todo código deve passar por code review antes de ser mergeado.

**Revisor deve verificar:**

- [ ] Código segue padrões do projeto
- [ ] Lógica está correta
- [ ] Testes adequados
- [ ] Sem duplicação de código
- [ ] Performance aceitável
- [ ] Documentação clara

**Comentários úteis:**

```
❌ Ruim:
"Isso tá errado"

✅ Bom:
"Podemos usar Optional.ofNullable() aqui para evitar NullPointerException.
Exemplo: userRepository.findById(id).orElseThrow(...)"
```

### Como Responder a Críticas

- Seja aberto a feedback
- Faça perguntas se não entender
- Agradeça pelos comentários
- Faça as alterações solicitadas
- Marque como "resolved" quando corrigido

---

## 🧪 Testes

### Backend (JUnit + Mockito)

```java
@SpringBootTest
public class UserServiceTest {
    
    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Test
    void testGetUserById_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setEmail("test@unifapi.edu.br");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Act
        UserDTO result = userService.getUserById(userId);
        
        // Assert
        assertNotNull(result);
        assertEquals("test@unifapi.edu.br", result.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }
    
    @Test
    void testGetUserById_NotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> userService.getUserById(userId));
    }
}
```

### Frontend (Jest + React Testing Library)

```typescript
import { render, screen } from '@testing-library/react';
import { UserCard } from '@/components/user/UserCard';

describe('UserCard', () => {
  const mockUser = {
    id: 1,
    fullName: 'John Doe',
    email: 'john@unifapi.edu.br',
    academicInfo: 'Computação - 8º período',
    competencies: [],
  };

  it('renders user information correctly', () => {
    render(<UserCard user={mockUser} />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Computação - 8º período')).toBeInTheDocument();
  });

  it('calls onSelect when card is clicked', () => {
    const onSelect = jest.fn();
    render(<UserCard user={mockUser} onSelect={onSelect} />);
    
    const card = screen.getByRole('img').closest('.user-card');
    fireEvent.click(card);
    
    expect(onSelect).toHaveBeenCalledWith(1);
  });
});
```

### Cobertura de Testes

- **Backend:** Mínimo 80% de cobertura
- **Frontend:** Mínimo 70% de cobertura

```bash
# Backend
mvn jacoco:report

# Frontend
npm test -- --coverage
```

---

## 📚 Recursos Adicionais

- [Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Google TypeScript Guide](https://google.github.io/styleguide/tsguide.html)
- [Spring Boot Best Practices](https://spring.io/guides/gs/spring-boot/)
- [React Best Practices](https://react.dev/learn)

---

## ✅ Checklist Final

Antes de fazer push:

- [ ] `git status` - Nenhuma mudança não commitada
- [ ] `npm test` - Testes passam
- [ ] `npm run lint` - Sem warnings
- [ ] Mensagens de commit claras
- [ ] Branch atualizada com main
- [ ] PR template preenchido

---

## 🆘 Precisa de Ajuda?

- GitHub Issues - Para bugs e features
- Pull Request Comments - Para discussões de código
- Slack/Discord - Para chat em tempo real
- Reuniões Semanais - Segundas, 10h

---

**Obrigado por contribuir! 🎉**

**Última atualização:** 18/06/2026
