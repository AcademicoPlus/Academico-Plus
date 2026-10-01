// Teste de carga dos fluxos REST principais: cadastro, login, listagem e
// detalhes de projeto. Cada VU (usuário virtual) cria sua própria conta no
// setup, para evitar contenção de linha única de usuário/token.
//
// Uso:
//   k6 run load-tests/rest-load-test.js
//   BASE_URL=https://sua-api.exemplo.com/api k6 run load-tests/rest-load-test.js
//
// Ajuste os "stages" abaixo para o perfil de carga que quiser simular.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const MAX_VUS = Number(__ENV.MAX_VUS || 50);

export const options = {
  scenarios: {
    fluxo_principal: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: Math.round(MAX_VUS * 0.4) }, // ramp-up gradual
        { duration: '1m', target: MAX_VUS },                    // carga alvo
        { duration: '1m', target: MAX_VUS },                    // sustentação
        { duration: '30s', target: 0 },                         // ramp-down
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],     // menos de 1% de falhas
    http_req_duration: ['p(95)<800'],   // 95% das respostas abaixo de 800ms
  },
};

// setup() roda uma única vez antes do teste: cria um usuário por VU planejado
// e reaproveita esses usuários em todas as iterações (evita recriar conta a
// cada iteração, o que sobrecarregaria o cadastro/hash de senha em vez do
// resto da API).
export function setup() {
  const cursosRes = http.get(`${BASE_URL}/cursos`);
  check(cursosRes, { 'GET /cursos 200': (r) => r.status === 200 });
  const cursos = cursosRes.json();
  if (!cursos || cursos.length === 0) {
    throw new Error('Nenhum curso ativo encontrado — rode as migrations antes do teste.');
  }
  const idCurso = cursos[0].id;

  const usuarios = [];
  for (let i = 0; i < MAX_VUS; i++) {
    const email = `k6-loadtest-${Date.now()}-${i}@teste.local`;
    const senha = 'senhaDeTeste123';

    const cadastroRes = http.post(
      `${BASE_URL}/auth/cadastro`,
      JSON.stringify({
        email,
        nome: `Usuario Carga ${i}`,
        periodo: 1,
        idCurso,
        senha,
        aceitouTermos: true,
      }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(cadastroRes, { 'cadastro 200/201': (r) => r.status === 200 || r.status === 201 });

    const loginRes = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email, senha }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const ok = check(loginRes, { 'login 200': (r) => r.status === 200 });
    if (!ok) continue;

    const token = loginRes.json('token');
    usuarios.push({ token });
  }

  if (usuarios.length === 0) {
    throw new Error('Nenhum usuário de teste conseguiu logar — verifique BASE_URL e o cadastro.');
  }

  return { usuarios };
}

// Tempo de "leitura" entre ações, simulando um usuário real (não uma rajada
// constante) — sorteado dentro de uma faixa a cada chamada.
function pausaHumana(minSegundos, maxSegundos) {
  sleep(minSegundos + Math.random() * (maxSegundos - minSegundos));
}

export default function (data) {
  const usuario = data.usuarios[__VU % data.usuarios.length];
  const headers = { Authorization: `Bearer ${usuario.token}`, 'Content-Type': 'application/json' };

  const listaRes = http.get(`${BASE_URL}/projetos?page=0&size=10`, { headers });
  check(listaRes, { 'GET /projetos 200': (r) => r.status === 200 });

  pausaHumana(1, 3); // usuário "lendo" a lista antes de continuar

  const meusRes = http.get(`${BASE_URL}/projetos/meus-projetos`, { headers });
  check(meusRes, { 'GET /projetos/meus-projetos 200': (r) => r.status === 200 });

  const lista = listaRes.json('content');
  if (lista && lista.length > 0) {
    pausaHumana(0.5, 2); // usuário escolhendo um projeto para abrir
    const projetoId = lista[0].id;
    const detalhesRes = http.get(`${BASE_URL}/projetos/${projetoId}`, { headers });
    check(detalhesRes, { 'GET /projetos/{id} 200': (r) => r.status === 200 });
  }

  pausaHumana(2, 5); // intervalo até a próxima "sessão" de navegação
}
