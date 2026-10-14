# Deploy em produção

Guia para colocar no ar, via **Render**: o monolito (`academico-api`), o **academico-email-service** (consome RabbitMQ, envia e-mails via Resend) e o **academico-upload-service** (fotos de perfil, via Cloudflare R2). O broker RabbitMQ roda no **CloudAMQP** (Render não tem um serviço de fila gerenciado). O frontend (SPA estática) é hospedado separadamente (Vercel, Netlify ou Cloudflare Pages — qualquer uma delas serve).

## 1. Cloudflare R2 (storage de fotos de perfil)

1. Crie uma conta na Cloudflare (se ainda não tiver) e acesse **R2 Object Storage** no painel.
2. Crie um bucket (ex.: `academico-plus-uploads`).
3. Em **Settings** do bucket, habilite acesso público:
   - Mais simples: ative o **Public Development URL** (gera algo como `https://pub-xxxxxxxx.r2.dev`) — use esse valor como `R2_PUBLIC_URL`.
   - Alternativa (mais "profissional", exige domínio próprio): conecte um domínio/subdomínio custom ao bucket (ex.: `cdn.seudominio.com.br`) e use-o como `R2_PUBLIC_URL`.
4. Em **Manage R2 API Tokens**, crie um token com permissão de leitura/escrita (Object Read & Write) só para esse bucket. Guarde o **Access Key ID** e o **Secret Access Key** — a Cloudflare só mostra o secret uma vez.
5. Anote também o **Account ID** (aparece no painel da Cloudflare, na URL ou na página de R2).
6. Você vai precisar de 5 valores para configurar o backend:
   - `R2_ACCOUNT_ID`
   - `R2_ACCESS_KEY_ID`
   - `R2_SECRET_ACCESS_KEY`
   - `R2_BUCKET_NAME` (nome do bucket criado)
   - `R2_PUBLIC_URL` (a URL pública do passo 3, **sem** barra no final)

## 2. CloudAMQP (fila RabbitMQ, usada pelo fluxo de e-mail)

O Render não tem um serviço de mensageria gerenciado, então o RabbitMQ roda no [CloudAMQP](https://www.cloudamqp.com/) (tier gratuito "Little Lemur" é suficiente para o TCC).

1. Crie uma conta e uma instância (plano free).
2. Na página da instância, copie a **AMQP URL** (formato `amqps://usuario:senha@host/vhost`) — o painel também mostra host, porta (5671, TLS), usuário, senha e vhost separados.
3. Você vai precisar de 5 valores para configurar o monolito e o `academico-email-service`:
   - `RABBITMQ_HOST`
   - `RABBITMQ_PORT` (`5671`, porta TLS do CloudAMQP)
   - `RABBITMQ_USERNAME`
   - `RABBITMQ_PASSWORD`
   - `RABBITMQ_VHOST`
   - `RABBITMQ_SSL_ENABLED=true`

## 3. Render — Postgres

1. No painel do Render, **New > PostgreSQL**.
2. Escolha o plano (Starter/Basic é suficiente para o MVP institucional).
3. Depois de criado, abra a página do banco e copie a **Internal Database URL** (formato `postgres://usuario:senha@host:porta/nome_do_banco`) — o Render mostra host, porta, usuário, senha e nome do banco separados também.
4. O Spring Boot espera uma URL JDBC, não o formato `postgres://` do Render. Monte manualmente:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<porta>/<nome_do_banco>
   SPRING_DATASOURCE_USERNAME=<usuario>
   SPRING_DATASOURCE_PASSWORD=<senha>
   ```
   (Se usar a Internal Database URL — recomendado quando backend e banco estão na mesma região do Render, é mais rápido e não conta como tráfego externo.)

## 4. Render — Web Service (academico-email-service)

Deploy este serviço antes do monolito — ele é quem declara a fila/DLQ no RabbitMQ na inicialização.

1. **New > Web Service**, conecte o repositório Git.
2. Runtime: **Docker**. Root directory: `academico-email-service`.
3. Plano: Starter é suficiente (serviço leve, sem banco).
4. Health check path: `/actuator/health`.
5. Variáveis de ambiente (mesmas chaves do `.env.example` do serviço):

   | Variável | Valor |
   |---|---|
   | `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` / `VHOST` | do passo 2 (CloudAMQP) |
   | `RABBITMQ_SSL_ENABLED` | `true` |
   | `RESEND_API_KEY` | sua chave `re_...` |
   | `EMAIL_FROM` | remetente exibido no e-mail |
   | `LOGGING_LEVEL_ROOT` | `INFO` |

## 5. Render — Web Service (academico-upload-service)

1. **New > Web Service**, conecte o repositório Git.
2. Runtime: **Docker**. Root directory: `academico-upload-service`.
3. Plano: Starter é suficiente.
4. Health check path: `/actuator/health`.
5. Variáveis de ambiente:

   | Variável | Valor |
   |---|---|
   | `INTERNAL_API_KEY` | gere uma string aleatória — o monolito precisa da mesma chave |
   | `R2_ACCOUNT_ID` | do passo 1 |
   | `R2_ACCESS_KEY_ID` | do passo 1 |
   | `R2_SECRET_ACCESS_KEY` | do passo 1 |
   | `R2_BUCKET_NAME` | do passo 1 |
   | `R2_PUBLIC_URL` | do passo 1 |
   | `LOGGING_LEVEL_ROOT` | `INFO` |

6. Anote a URL pública deste serviço no Render (ex.: `https://academico-upload-service.onrender.com`) — o monolito vai usá-la como `UPLOAD_SERVICE_URL`.

## 6. Render — Web Service (backend)

1. **New > Web Service**, conecte o repositório Git.
2. Runtime: **Docker**. Root directory: `academico-api` (o `Dockerfile` está lá).
3. Plano: Starter (US$7/mês) ou superior.
4. Health check path: `/api/actuator/health`.
5. Configure as variáveis de ambiente (aba **Environment**) — mesmas chaves do `.env.example`:

   | Variável | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | do passo 3 |
   | `SPRING_DATASOURCE_USERNAME` | do passo 3 |
   | `SPRING_DATASOURCE_PASSWORD` | do passo 3 |
   | `JWT_SECRET` | gere uma string aleatória longa (ex.: `openssl rand -base64 48`) |
   | `JWT_EXPIRATION` | ex.: `86400000` (24h, em ms) |
   | `SERVER_PORT` | `8080` |
   | `SERVER_SERVLET_CONTEXT_PATH` | `/api` |
   | `SPRING_APPLICATION_NAME` | `academico-plus-api` |
   | `CORS_ALLOWED_ORIGINS` | URL do frontend em produção (ex.: `https://academicoplus.vercel.app`) |
   | `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` / `VHOST` | do passo 2 (CloudAMQP) |
   | `RABBITMQ_SSL_ENABLED` | `true` |
   | `UPLOAD_SERVICE_URL` | URL do passo 5.6 |
   | `UPLOAD_SERVICE_API_KEY` | mesma chave configurada no passo 5 (`INTERNAL_API_KEY`) |
   | `LOGGING_LEVEL_ROOT` | `INFO` |
   | `LOGGING_LEVEL_COM_UNIFAPI_ACADEMICO` | `INFO` |
   | `ADMIN_BOOTSTRAP_EMAIL` | deixe vazio no primeiro deploy — ver passo 8 |
   | `SWAGGER_ENABLED` | **não defina** (padrão `false`). Ligado, publica o mapa completo da API sem autenticação |

6. Deploy. O Render provisiona HTTPS automaticamente (não precisa configurar nada na aplicação).

## 7. Frontend

Build estático (`npm run build` gera `frontend/dist/`), hospedável em qualquer CDN estática (Vercel, Netlify, Cloudflare Pages). Configure as variáveis de ambiente do frontend:

```
VITE_API_URL=https://<seu-backend>.onrender.com/api
VITE_USE_MOCKS=false
```

`VITE_USE_MOCKS` **precisa** estar `false` (ou ausente — esse é o padrão) em produção — com
`true` o frontend nunca chama o backend real, só devolve dados fictícios de `services/mocks.ts`
(usado só pra desenvolver telas sem o backend de pé, ver `docs/CONECTANDO_FRONT_BACKEND.md`).

## 8. Primeiro admin

Alterar o nível de acesso de alguém exige `hasRole('ADMIN')`, e o cadastro público sempre
atribui ALUNO — então num banco novo **não existe nenhum admin** e o painel `/admin` (incluindo
a fila de denúncias) fica inacessível até alguém ser promovido. Este passo resolve isso uma vez só:

1. Com o frontend no ar, **cadastre-se normalmente** pela tela de cadastro, com o e-mail que vai
   ser o do administrador.
2. No Render, no serviço do backend, defina `ADMIN_BOOTSTRAP_EMAIL` com esse mesmo e-mail.
3. Reinicie o serviço (**Manual Deploy > Restart service**). No log de inicialização deve aparecer
   `Usuário '<email>' promovido a ADMIN via ADMIN_BOOTSTRAP_EMAIL.`
4. A partir daí esse admin promove os demais pela própria interface (`/admin` > Usuários), então a
   variável pode ser **removida** do ambiente. Deixá-la configurada é inofensivo: a promoção é
   idempotente e vira no-op se a conta já for ADMIN.

Se o e-mail não corresponder a nenhuma conta ativa, o serviço sobe normalmente e registra um
`WARN` no log — nenhuma conta é criada por essa variável, só promovida (assim não é preciso passar
senha por variável de ambiente).

## ⚠️ Não aumente o número de instâncias do backend

Os limites de requisição (`GlobalRateLimitFilter` e `RateLimiterService`) contam **em memória**, dentro do processo. Com uma instância, correto. Com duas ou mais atrás do balanceador do Render, cada uma passa a ter seu próprio contador: o limite efetivo vira o dobro, o triplo, e a proteção contra força bruta de login e contra criação de contas em massa enfraquece na mesma proporção.

Então, no Render, mantenha o backend com **1 instância** (o padrão dos planos Starter). Se um dia precisar escalar horizontalmente, troque os contadores por um store compartilhado (Redis) **antes** — não depois. Os dois serviços auxiliares (e-mail e upload) não têm esse acoplamento e podem escalar livremente.

## Pendências que continuam fora do código (decisão de infra, não de código)

Ver seção "Antes de lançar em produção" no [README.md](README.md) do backend — CAPTCHA, observabilidade (Sentry) e verificação de e-mail institucional continuam pendentes e exigem configuração em serviços externos à parte.

**Backup do banco**: no plano pago do Render Postgres, backups automáticos diários já vêm inclusos — não tem configuração adicional a fazer, mas confirme o plano escolhido cobre isso.
