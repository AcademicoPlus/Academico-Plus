// Dados fictícios usados no lugar do backend real quando VITE_USE_MOCKS=true
// (ver apiClient.ts). Objetivo: quem está no frontend consegue rodar
// `npm run dev` e testar as telas sem precisar subir o academico-backend
// nem o banco. Veja docs/CONECTANDO_FRONT_BACKEND.md, seção "Testando sem
// o backend".
//
// Cada rota simulada mora num `case` do switch abaixo e devolve os dados já
// no formato do DTO do backend (mesma forma que o service espera). Pra
// simular uma tela nova sem backend, adicione um `case "MÉTODO /rota"` aqui
// — não precisa mexer no service nem na página, só no apiClient (que já
// está pronto) e aqui.
import { ApiError, type ErroCampo } from './apiError';
import type { CadastroRequest, CadastroResponse, LoginRequest, LoginResponse } from './authService';
import type { Curso } from './cursoService';

// Latência artificial pra tela de fato passar pelo estado de "carregando".
const ATRASO_MS = 500;

function resolverComAtraso<T>(valor: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(valor), ATRASO_MS));
}

function rejeitarComAtraso<T>(erro: ApiError): Promise<T> {
  return new Promise((_, reject) => setTimeout(() => reject(erro), ATRASO_MS));
}

const CURSOS_MOCK: Curso[] = [
  { id: '11111111-1111-1111-1111-111111111111', nome: 'Ciência da Computação', criadoEm: '2024-01-01T00:00:00Z' },
  { id: '22222222-2222-2222-2222-222222222222', nome: 'Engenharia de Software', criadoEm: '2024-01-01T00:00:00Z' },
  { id: '33333333-3333-3333-3333-333333333333', nome: 'Design Gráfico', criadoEm: '2024-01-01T00:00:00Z' },
  { id: '44444444-4444-4444-4444-444444444444', nome: 'Administração', criadoEm: '2024-01-01T00:00:00Z' },
  { id: '55555555-5555-5555-5555-555555555555', nome: 'Psicologia', criadoEm: '2024-01-01T00:00:00Z' },
];

// Credenciais fixas pra testar o login sem backend.
export const CREDENCIAIS_MOCK = {
  email: 'ana.silva@academico.edu.br',
  senha: 'senha123',
};

// Usar esse e-mail no cadastro simula "e-mail já cadastrado" (erro 400 com
// erro de campo, igual ao que o backend devolve).
const EMAIL_JA_CADASTRADO_MOCK = 'duplicado@academico.edu.br';

type MockOptions = {
  method: string;
  body?: unknown;
};

export function mockFetch<TResposta>(caminho: string, { method, body }: MockOptions): Promise<TResposta> {
  const rota = `${method} ${caminho}`;

  switch (rota) {
    case 'GET /cursos':
      return resolverComAtraso(CURSOS_MOCK as unknown as TResposta);

    case 'POST /auth/login': {
      const { email, senha } = body as LoginRequest;
      if (email === CREDENCIAIS_MOCK.email && senha === CREDENCIAIS_MOCK.senha) {
        const resposta: LoginResponse = {
          sucesso: true,
          mensagem: 'Login realizado com sucesso.',
          token: 'token-mock-dev',
          usuario: {
            id: 'usuario-mock-1',
            nome: 'Ana Silva',
            curso: 'Ciência da Computação',
            fotoUrl: null,
            permission: 'ALUNO',
            periodo: 4,
            notaMedia: 4.5,
            totalAvaliacoes: 3,
          },
          avisosCancelamento: [],
        };
        return resolverComAtraso(resposta as unknown as TResposta);
      }
      // Mesma mensagem que o AuthService real devolve pra credenciais erradas.
      return rejeitarComAtraso(new ApiError(401, 'Email ou senha incorretos.'));
    }

    case 'POST /auth/cadastro': {
      const dados = body as CadastroRequest;
      if (dados.email === EMAIL_JA_CADASTRADO_MOCK) {
        const erroCampo: ErroCampo = { campo: 'email', mensagem: 'E-mail já cadastrado.' };
        return rejeitarComAtraso(
          new ApiError(400, `E-mail '${dados.email}' já está cadastrado no sistema.`, [erroCampo]),
        );
      }
      const resposta: CadastroResponse = {
        sucesso: true,
        mensagem: 'Cadastro realizado com sucesso.',
      };
      return resolverComAtraso(resposta as unknown as TResposta);
    }

    default:
      return rejeitarComAtraso(
        new ApiError(
          501,
          `Mock não implementado para "${rota}". Adicione um caso em services/mocks.ts ou rode com VITE_USE_MOCKS=false para usar o backend real.`,
        ),
      );
  }
}
