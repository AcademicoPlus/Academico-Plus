# Testes de carga (k6)

Scripts para testar o comportamento do backend sob carga antes de ir para produção.

## Instalação do k6

- Windows: `winget install k6` ou `choco install k6`
- Ou baixe em https://k6.io/docs/get-started/installation/

## ⚠️ Antes de rodar: suba os limites de requisição do ambiente alvo

O backend tem rate limit por IP (`GlobalRateLimitFilter`): 120 requisições/minuto no geral
e 10 cadastros/hora. O k6 roda **de um único IP**, então com os valores padrão o que você
mede é o rate limiter, não a aplicação — o `setup()` para de criar usuários no 11º e o resto
do teste vira uma coleção de `429`.

Suba os dois limites no ambiente alvo antes do teste e devolva aos valores normais depois:

```bash
RATE_LIMIT_REQUISICOES_POR_MINUTO=1000000
RATE_LIMIT_CADASTROS_POR_HORA=1000000
```

Localmente dá para passar sem mexer no `.env`:

```bash
cd academico-api
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.rate-limit.requisicoes-por-minuto=1000000 --app.rate-limit.cadastros-por-hora=1000000"
```

## 1. Carga nos endpoints REST (`rest-load-test.js`)

Cobre cadastro, login, listagem de projetos e detalhes de projeto. Cria seus
próprios usuários de teste no setup, então roda direto contra qualquer
ambiente que tenha ao menos um curso ativo cadastrado.

```bash
# Local (backend em localhost:8080)
k6 run load-tests/rest-load-test.js

# Contra staging, com 100 VUs no pico
BASE_URL=https://staging.exemplo.com/api MAX_VUS=100 k6 run load-tests/rest-load-test.js
```

**Nunca rode isso contra produção sem avisar quem administra o ambiente** —
o script cria dezenas de contas reais no banco. Prefira sempre um ambiente de
staging/homologação com banco descartável.

As contas criadas ficam no banco (o e-mail segue o padrão `k6-loadtest-<timestamp>-<n>@teste.local`).
Num banco que não seja descartável, limpe depois:

```sql
DELETE FROM usuarios WHERE email LIKE 'k6-loadtest-%@teste.local';
```

## 2. Carga no chat via WebSocket (`ws-chat-load-test.js`)

Testa o handshake STOMP, autenticação do WebSocket e broadcast de mensagens.
Exige um token de um usuário que já tenha acesso a um projeto (dono ou membro
aceito) — veja o comentário no topo do arquivo para as limitações.

```bash
k6 run -e TOKEN=<jwt-do-login> -e PROJETO_ID=<uuid-do-projeto> load-tests/ws-chat-load-test.js
```

## Baseline registrado

Primeira execução registrada deste script — **em máquina de desenvolvimento**, com Postgres
local em Docker, backend via `spring-boot:run` e os limites de requisição suspensos como
descrito acima:

```
13/10/2026 · MAX_VUS=20 · 3 min (30s ramp-up / 1min alvo / 1min sustentação / 30s ramp-down)

✓ http_req_duration  p(95)=27.79ms   (threshold: p(95)<800ms)
✓ http_req_failed    rate=0.00%      (threshold: rate<1%)

http_req_duration: avg=10.16ms  med=6.92ms  p(90)=12.4ms  p(95)=27.79ms  max=94.73ms
http_reqs:         1145 (6.18/s)
iterations:        368
checks:            1145/1145 (100%)
```

Como ler esses números:

- **Não são números de produção.** Rodou tudo na mesma máquina: sem latência de rede, sem CPU
  compartilhada, sem cold start. No plano Starter do Render (CPU compartilhada, banco em outro
  host) espere latências bem maiores. O valor deste baseline é servir de comparação: se o mesmo
  teste em staging der p(95) de 2s, a diferença é do ambiente, não do código.
- **6,18 req/s não é o teto de throughput.** O script simula leitura humana (`pausaHumana`), então
  o que ele mede é latência sob concorrência realista, não capacidade máxima. Para achar o teto,
  tire as pausas e aumente `MAX_VUS` até os thresholds quebrarem.
- **Ainda falta rodar contra staging** antes de abrir para usuários reais — é lá que aparecem
  pool de conexões do banco, limites de memória do contêiner e cold start.

## Lendo o resultado

O k6 imprime um resumo com `http_req_duration` (p95, p99), taxa de erro e
throughput. Os `thresholds` definidos no `rest-load-test.js` (menos de 1% de
falha, p95 abaixo de 800ms) fazem o comando terminar com código de saída
diferente de zero se o sistema não aguentar a carga — útil para rodar isso
num pipeline de CI de forma automatizada no futuro, se quiser.
