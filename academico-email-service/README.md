# Academico+ Email Service

Microserviço responsável por enviar o e-mail com o código OTP de recuperação de senha. Consome a fila `email.codigo-verificacao` (RabbitMQ), publicada pelo monolito (`academico-backend`) quando um usuário solicita reset de senha, e envia o e-mail via [Resend](https://resend.com/).

Extraído do monolito porque essa parte não tinha nenhum acoplamento com o restante do domínio (nenhuma dependência de repositório/entidade) e o fluxo já era conceitualmente assíncrono — bom candidato didático de microserviço pro TCC.

## Stack

- Java 21 + Spring Boot 3.5
- Spring AMQP (RabbitMQ)
- Thymeleaf (template do e-mail)

## Contrato da fila

- **Exchange**: `email.exchange` (topic, durável) — declarada também pelo monolito (publisher), idempotente.
- **Routing key**: `codigo-verificacao`
- **Fila**: `email.codigo-verificacao` (durável), com dead-letter para `email.codigo-verificacao.dlq` — declaradas só aqui, o consumidor é o dono da topologia.
- **Payload** (JSON): `{ "destinatario": "...", "codigo": "123456", "minutosExpiracao": 15 }`

Falha real de envio (Resend fora do ar, resposta não-2xx) joga a mensagem pra DLQ — não há retry automático (`spring.rabbitmq.listener.simple.default-requeue-rejected=false`).

## Configuração

```bash
cp .env.example .env
```

| Variável | Descrição |
|---|---|
| `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` / `VHOST` | Conexão com o broker (local: docker-compose do `academico-backend`; produção: CloudAMQP) |
| `RESEND_API_KEY` | Opcional — sem ela (ou sem prefixo `re_`), cai em modo simulação e só loga o código no console |
| `EMAIL_FROM` | Remetente exibido no e-mail |

## Rodando localmente

Precisa do RabbitMQ de pé (`docker-compose up -d` no `academico-backend`):

```bash
./mvnw spring-boot:run
```

Sobe em `http://localhost:8081` (só health check em `/actuator/health` — não expõe API pública).
