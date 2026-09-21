# Testes de carga (k6)

Scripts para testar o comportamento do backend sob carga antes de ir para produção.

## Instalação do k6

- Windows: `winget install k6` ou `choco install k6`
- Ou baixe em https://k6.io/docs/get-started/installation/

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

## 2. Carga no chat via WebSocket (`ws-chat-load-test.js`)

Testa o handshake STOMP, autenticação do WebSocket e broadcast de mensagens.
Exige um token de um usuário que já tenha acesso a um projeto (dono ou membro
aceito) — veja o comentário no topo do arquivo para as limitações.

```bash
k6 run -e TOKEN=<jwt-do-login> -e PROJETO_ID=<uuid-do-projeto> load-tests/ws-chat-load-test.js
```

## Lendo o resultado

O k6 imprime um resumo com `http_req_duration` (p95, p99), taxa de erro e
throughput. Os `thresholds` definidos no `rest-load-test.js` (menos de 1% de
falha, p95 abaixo de 800ms) fazem o comando terminar com código de saída
diferente de zero se o sistema não aguentar a carga — útil para rodar isso
num pipeline de CI de forma automatizada no futuro, se quiser.
