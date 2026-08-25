# Deploy em produção

Guia para colocar o backend no ar via **Render** (backend + Postgres gerenciado) e o storage de fotos de perfil no **Cloudflare R2**. O frontend (SPA estática) é hospedado separadamente (Vercel, Netlify ou Cloudflare Pages — qualquer uma delas serve).

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

## 2. Render — Postgres

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

## 3. Render — Web Service (backend)

1. **New > Web Service**, conecte o repositório Git.
2. Runtime: **Docker**. Root directory: `academico-backend` (o `Dockerfile` está lá).
3. Plano: Starter (US$7/mês) ou superior.
4. Health check path: `/api/actuator/health`.
5. Configure as variáveis de ambiente (aba **Environment**) — mesmas chaves do `.env.example`:

   | Variável | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | do passo 2 |
   | `SPRING_DATASOURCE_USERNAME` | do passo 2 |
   | `SPRING_DATASOURCE_PASSWORD` | do passo 2 |
   | `JWT_SECRET` | gere uma string aleatória longa (ex.: `openssl rand -base64 48`) |
   | `JWT_EXPIRATION` | ex.: `86400000` (24h, em ms) |
   | `SERVER_PORT` | `8080` |
   | `SERVER_SERVLET_CONTEXT_PATH` | `/api` |
   | `SPRING_APPLICATION_NAME` | `academico-plus-api` |
   | `CORS_ALLOWED_ORIGINS` | URL do frontend em produção (ex.: `https://academicoplus.vercel.app`) |
   | `RESEND_API_KEY` | sua chave `re_...` |
   | `STORAGE_PROVIDER` | `r2` |
   | `R2_ACCOUNT_ID` | do passo 1 |
   | `R2_ACCESS_KEY_ID` | do passo 1 |
   | `R2_SECRET_ACCESS_KEY` | do passo 1 |
   | `R2_BUCKET_NAME` | do passo 1 |
   | `R2_PUBLIC_URL` | do passo 1 |
   | `LOGGING_LEVEL_ROOT` | `INFO` |
   | `LOGGING_LEVEL_COM_UNIFAPI_ACADEMICO` | `INFO` |

6. Deploy. O Render provisiona HTTPS automaticamente (não precisa configurar nada na aplicação).

## 4. Frontend

Build estático (`npm run build` gera `frontend/dist/`), hospedável em qualquer CDN estática (Vercel, Netlify, Cloudflare Pages). Configure a única variável de ambiente do frontend:

```
VITE_API_URL=https://<seu-backend>.onrender.com/api
```

## Pendências que continuam fora do código (decisão de infra, não de código)

Ver seção "Antes de lançar em produção" no [README.md](README.md) do backend — CAPTCHA, observabilidade (Sentry) e verificação de e-mail institucional continuam pendentes e exigem configuração em serviços externos à parte.

**Backup do banco**: no plano pago do Render Postgres, backups automáticos diários já vêm inclusos — não tem configuração adicional a fazer, mas confirme o plano escolhido cobre isso.
