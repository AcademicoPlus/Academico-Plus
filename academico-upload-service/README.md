# Academico+ Upload Service

Microserviço responsável por validar e enviar fotos de perfil pro Cloudflare R2. Chamado via REST síncrono pelo monolito (`academico-api`) — o upload de arquivo binário não é um bom fit pra fila de mensagens (payload grande, e o fluxo é request/response por natureza: o front precisa da URL da foto na resposta).

Extraído do monolito porque já era uma peça bem isolada (interface `StorageService` + implementação R2), e o próprio ADR do backend já registrava a migração pra storage externo como próximo passo.

## Stack

- Java 21 + Spring Boot 3.5
- AWS SDK v2 (`s3`) — Cloudflare R2 é compatível com a API do S3

## Endpoints

Não é público — só o monolito deve chamar, autenticado por uma chave compartilhada no header `X-Internal-Api-Key`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/fotos` (multipart, campo `arquivo`) | Valida (tipo MIME + assinatura real dos bytes) e envia a imagem pro R2. Retorna `{ "url": "..." }` |
| `DELETE` | `/fotos?url=...` | Remove a imagem do R2 |

## Configuração

```bash
cp .env.example .env
```

| Variável | Descrição |
|---|---|
| `INTERNAL_API_KEY` | Chave compartilhada com o monolito (`UPLOAD_SERVICE_API_KEY` no `.env` dele) |
| `R2_ACCOUNT_ID` / `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` / `R2_BUCKET_NAME` / `R2_PUBLIC_URL` | Credenciais do bucket Cloudflare R2 — ver `academico-api/DEPLOY.md` para o passo a passo de criação |

## Rodando localmente

```bash
./mvnw spring-boot:run
```

Sobe em `http://localhost:8082`. Aponte o `.env` do monolito (`UPLOAD_SERVICE_URL=http://localhost:8082`) pra testar o fluxo de ponta a ponta.
