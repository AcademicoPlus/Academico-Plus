# 🚀 Setup Guide - Guia de Instalação

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter instalado:

### Essencial (Desenvolvimento com Docker)
- ✅ [Docker](https://www.docker.com/) 20.10+
- ✅ [Docker Compose](https://docs.docker.com/compose/) 1.29+
- ✅ [Git](https://git-scm.com/)

### Recomendado (Desenvolvimento Local)
- ✅ [Java JDK 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- ✅ [Node.js 18+](https://nodejs.org/)
- ✅ [PostgreSQL 14+](https://www.postgresql.org/)
- ✅ [Maven 3.8+](https://maven.apache.org/)

### Ferramentas Úteis
- [Postman](https://www.postman.com/) - Teste de API
- [DBeaver](https://dbeaver.io/) - Gerenciador de Banco de Dados
- [VS Code](https://code.visualstudio.com/) - Editor
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - IDE Java

---

## 🐳 Opção 1: Instalação Rápida com Docker (Recomendado)

A forma mais rápida de começar é usando Docker Compose. Tudo será configurado automaticamente!

### Passo 1: Clone o Repositório

```bash
git clone https://github.com/seu-usuario/academico-plus.git
cd academico-plus
```

### Passo 2: Crie o Arquivo de Configuração

```bash
cp .env.example .env
```

Edite o `.env` se necessário (valores padrão funcionam para desenvolvimento):

```bash
nano .env
# ou
code .env  # No VS Code
```

### Passo 3: Inicie os Containers

```bash
docker-compose up -d
```

Aguarde 30-60 segundos para o PostgreSQL inicializar completamente.

### Passo 4: Verifique o Status

```bash
docker-compose ps
```

Esperado:
```
CONTAINER ID   IMAGE         STATUS
xxx            postgres:15   Up 2 minutes
xxx            dpage/pgadmin4   Up 1 minute
```

### Passo 5: Acesse a Aplicação

| Serviço | URL | Login |
|---------|-----|-------|
| **Frontend (em dev)** | Ainda não está rodando | - |
| **API Backend** | http://localhost:8080 | - |
| **pgAdmin (DB)** | http://localhost:5050 | admin@academico.local / admin |

### Passo 6: Teste a Conexão com o Banco

```bash
# Verificar que PostgreSQL está rodando
curl http://localhost:5050
```

---

## 🛠️ Opção 2: Instalação Local (Desenvolvimento Avançado)

Se preferir desenvolver localmente sem Docker, siga estes passos:

### A. Configurar PostgreSQL

#### Windows/Mac (Usar Installer)

1. Download: https://www.postgresql.org/download/
2. Siga o wizard de instalação
3. Lembre-se da senha do `postgres`

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### Criar Banco de Dados

```bash
# Conectar como superusuário
psql -U postgres

# No prompt do PostgreSQL:
CREATE DATABASE academico_plus;
CREATE USER academico_user WITH PASSWORD 'seu_password_seguro';
ALTER ROLE academico_user SET client_encoding TO 'utf8';
ALTER ROLE academico_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE academico_user SET default_transaction_deferrable TO on;
ALTER ROLE academico_user SET timezone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE academico_plus TO academico_user;
\q  # Sair
```

#### Carregar Schema

```bash
psql -U academico_user -d academico_plus -f database/schema.sql
psql -U academico_user -d academico_plus -f database/seed-data.sql
```

### B. Configurar Backend (Spring Boot)

#### Pré-requisitos

```bash
# Verificar Java
java -version  # Deve ser 17 ou superior

# Verificar Maven
mvn --version
```

#### Clonar e Configurar

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/academico-plus.git
cd academico-plus/backend

# Crie application-local.properties
cat > src/main/resources/application-local.properties << EOF
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/academico_plus
spring.datasource.username=academico_user
spring.datasource.password=seu_password_seguro
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# JWT
jwt.secret=sua_chave_secreta_muito_longa_min_32_caracteres_aqui
jwt.expiration=86400000

# Server
server.port=8080
server.servlet.context-path=/api

# Logging
logging.level.root=INFO
logging.level.com.unifapi.academico=DEBUG
EOF
```

#### Instalar Dependências e Rodar

```bash
# Baixar dependências
mvn clean install

# Rodar aplicação
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Ou compilar e rodar JAR
mvn clean package
java -jar target/academico-plus-0.1.0.jar --spring.profiles.active=local
```

**Esperado:** Aplicação rodando em `http://localhost:8080`

### C. Configurar Frontend (Next.js)

#### Pré-requisitos

```bash
# Verificar Node.js
node --version  # Deve ser 18 ou superior
npm --version   # Deve ser 8 ou superior
```

#### Instalar e Rodar

```bash
cd frontend

# Instalar dependências
npm install

# Criar arquivo .env.local
cat > .env.local << EOF
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=Academico+
EOF

# Rodar em modo desenvolvimento
npm run dev
```

**Esperado:** Frontend rodando em `http://localhost:3000`

---

## ✅ Validação da Instalação

### Teste 1: Banco de Dados

```bash
# Conectar ao banco
psql -U academico_user -d academico_plus

# Verificar tabelas
\dt

# Contar usuários
SELECT COUNT(*) FROM users;

# Sair
\q
```

**Esperado:** Deve mostrar 10 usuários.

### Teste 2: Backend API

```bash
# Health Check
curl http://localhost:8080/api/health

# Listar usuários
curl http://localhost:8080/api/v1/users

# Listar competências
curl http://localhost:8080/api/v1/competencies
```

**Esperado:** Respostas JSON com dados.

### Teste 3: Frontend

```bash
# Abrir no navegador
http://localhost:3000

# Você deve ver:
# - Logo do Academico+
# - Botões de Login/Cadastro
# - Descrição da plataforma
```

### Teste 4: Banco de Dados (pgAdmin)

1. Abra http://localhost:5050
2. Login: `admin@academico.local` / `admin`
3. Adicione servidor PostgreSQL:
   - Host: `postgres`
   - Username: `academico_user`
   - Password: (a que você configurou)
4. Navegue e veja as tabelas

---

## 🔑 Dados de Teste (Login)

Use os dados seedados automaticamente:

### Professores
```
Email: professor.carlos@unifapi.edu.br
Senha: password123

Email: professor.maria@unifapi.edu.br
Senha: password123
```

### Alunos
```
Email: aluno.carlos@unifapi.edu.br
Senha: password123

Email: aluno.jefferson@unifapi.edu.br
Senha: password123
```

**Nota:** Senhas são exemplos. Em produção, gere hashes bcrypt seguros!

---

## 📁 Estrutura de Pastas Esperada

```
academico-plus/
├── .git/                    # Git repository
├── .env                     # Variáveis de ambiente
├── docker-compose.yml       # Containers
├── README.md
├── backend/                 # Spring Boot API
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Next.js App
│   ├── app/
│   ├── package.json
│   └── Dockerfile
├── database/                # Scripts SQL
│   ├── schema.sql
│   └── seed-data.sql
└── docs/                    # Documentação
```

---

## 🐛 Troubleshooting

### Problema: "Connection refused" no banco de dados

**Solução:**
```bash
# Verificar se PostgreSQL está rodando
docker-compose ps

# Se não estiver, iniciar:
docker-compose up -d postgres

# Aguarde 10 segundos e tente novamente
```

### Problema: "Port already in use"

**Solução:**
```bash
# Liberar porta (Linux/Mac)
lsof -i :5432    # PostgreSQL
lsof -i :8080    # Backend
lsof -i :3000    # Frontend

# Matar processo (substitua PID)
kill -9 <PID>

# Ou mudar porta no docker-compose.yml
```

### Problema: "No suitable driver found"

**Solução:**
```bash
# Verificar que PostgreSQL driver está em pom.xml
# Reexecutar Maven
mvn clean install -U
```

### Problema: "Cannot find module" (Frontend)

**Solução:**
```bash
# Limpar cache
npm cache clean --force

# Reinstalar
rm -rf node_modules package-lock.json
npm install
```

---

## 🔄 Comandos Úteis

### Docker Compose

```bash
# Iniciar
docker-compose up -d

# Parar
docker-compose down

# Ver logs
docker-compose logs -f postgres
docker-compose logs -f backend  # (quando implementado)

# Reconstruir
docker-compose up -d --build

# Limpar (remove containers e volumes)
docker-compose down -v
```

### Backend (Maven)

```bash
# Instalar dependências
mvn install

# Compilar
mvn compile

# Testes
mvn test

# Build JAR
mvn package

# Limpar
mvn clean
```

### Frontend (npm)

```bash
# Instalar
npm install

# Desenvolvimento
npm run dev

# Build produção
npm run build

# Produção (serve)
npm run start

# Linter
npm run lint

# Testes
npm test
```

---

## 🚀 Próximos Passos

1. **Backend:** Criar controllers e services (TCC II)
2. **Frontend:** Implementar páginas e componentes (TCC II)
3. **Testes:** Escrever testes unitários e E2E
4. **Deploy:** Configurar Railway para produção
5. **Documentação:** Adicionar mais exemplos

---

## 📞 Suporte

Se encontrar problemas:

1. Verifique este guia novamente
2. Consulte os logs: `docker-compose logs -f`
3. Abra uma [Issue](https://github.com/seu-usuario/academico-plus/issues)
4. Procure ajuda na comunidade

---

## ✨ Checklist de Instalação

- [ ] Repositório clonado
- [ ] Docker & Docker Compose instalados
- [ ] Arquivo `.env` criado
- [ ] Containers iniciados (`docker-compose up -d`)
- [ ] PostgreSQL rodando e acessível
- [ ] Dados seedados no banco
- [ ] Banco acessível via pgAdmin
- [ ] Backend compila e roda (local)
- [ ] Frontend instala dependências
- [ ] Testes de validação passam

---

**Pronto para desenvolver! 🎉**

---

**Última atualização:** 18/06/2026
