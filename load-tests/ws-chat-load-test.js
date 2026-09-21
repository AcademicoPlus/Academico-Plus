// Teste de carga do chat via WebSocket/STOMP.
//
// Limitação importante: só o criador do projeto ou membros com candidatura
// aceita podem entrar na sala (ver ChatProjetoService.obterProjetoComAcesso).
// Como criar N contas + aprovar candidatura de cada uma exigiria reproduzir
// todo o fluxo de candidatura no setup, este script simula carga de conexão
// abrindo várias conexões concorrentes autenticadas como o MESMO usuário
// (dono do projeto informado). Isso já estressa autenticação do handshake
// STOMP, o broker em memória (/topic) e o broadcast — mas não testa limites
// de autorização por usuário distinto. Para isso, rode primeiro o cadastro
// de vários usuários + fluxo de candidatura/aceite via API ou UI.
//
// Uso:
//   1. Faça login manualmente (ou via curl) com uma conta que seja dona de
//      um projeto e pegue o token JWT e o id do projeto.
//   2. k6 run -e TOKEN=<jwt> -e PROJETO_ID=<uuid> load-tests/ws-chat-load-test.js
//
// Variáveis de ambiente:
//   BASE_URL   (default: http://localhost:8080/api)
//   TOKEN      JWT de um usuário com acesso ao projeto (obrigatório)
//   PROJETO_ID UUID do projeto cujo chat será usado (obrigatório)
//   MAX_VUS    conexões concorrentes simuladas (default: 20)

import ws from 'k6/ws';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const WS_URL = BASE_URL.replace(/^http/, 'ws') + '/ws/websocket';
const TOKEN = __ENV.TOKEN;
const PROJETO_ID = __ENV.PROJETO_ID;
const MAX_VUS = Number(__ENV.MAX_VUS || 20);

if (!TOKEN || !PROJETO_ID) {
  throw new Error('Defina TOKEN e PROJETO_ID via -e TOKEN=... -e PROJETO_ID=...');
}

export const options = {
  scenarios: {
    conexoes_chat: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: MAX_VUS },
        { duration: '40s', target: MAX_VUS },
        { duration: '10s', target: 0 },
      ],
    },
  },
};

const NULO = '\x00';

function frameConnect() {
  return (
    `CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${TOKEN}\n\n${NULO}`
  );
}

function frameSubscribe(id, destino) {
  return `SUBSCRIBE\nid:${id}\ndestination:${destino}\n\n${NULO}`;
}

function frameSend(destino, corpoJson) {
  const corpo = JSON.stringify(corpoJson);
  return (
    `SEND\ndestination:${destino}\ncontent-type:application/json\ncontent-length:${corpo.length}\n\n${corpo}${NULO}`
  );
}

export default function () {
  const destinoTopico = `/topic/projetos/${PROJETO_ID}`;
  const destinoEnvio = `/app/projetos/${PROJETO_ID}/mensagens`;

  const res = ws.connect(WS_URL, {}, (socket) => {
    socket.on('open', () => {
      socket.send(frameConnect());
    });

    let conectado = false;

    socket.on('message', (data) => {
      if (!conectado && data.startsWith('CONNECTED')) {
        conectado = true;
        socket.send(frameSubscribe(`sub-${__VU}`, destinoTopico));
        socket.send(
          frameSend(destinoEnvio, { conteudo: `Mensagem de carga do VU ${__VU} — iteração ${__ITER}` }),
        );
      }
    });

    socket.on('error', (e) => {
      console.error(`Erro no WS (VU ${__VU}): ${e.error ? e.error() : e}`);
    });

    socket.setTimeout(() => {
      socket.close();
    }, 5000);
  });

  check(res, { 'handshake WS 101': (r) => r && r.status === 101 });

  sleep(1);
}
