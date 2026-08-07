// Client HTTP central do frontend. Toda comunicação com o backend Spring Boot
// (academico-backend) passa por aqui, para não espalhar `fetch` cru pelas telas
// e para tratar erros sempre do mesmo jeito (formato do GlobalExceptionHandler
// do backend: { status, erro, mensagem, path, timestamp, erros? }).
import { obterToken } from '../utils/auth';

// Vem do .env do frontend (VITE_API_URL). Já inclui o context-path "/api"
// configurado no backend (SERVER_SERVLET_CONTEXT_PATH).
const BASE_URL = import.meta.env.VITE_API_URL as string;

type ErroCampo = {
  campo: string;
  mensagem: string;
};

type ErroResposta = {
  status: number;
  erro: string;
  mensagem: string;
  path: string;
  erros?: ErroCampo[];
};

// Erro lançado quando o backend responde com status >= 400, já com a
// mensagem pronta para mostrar ao usuário (ex.: "Email ou senha inválidos").
export class ApiError extends Error {
  status: number;
  erros?: ErroCampo[];

  constructor(status: number, mensagem: string, erros?: ErroCampo[]) {
    super(mensagem);
    this.status = status;
    this.erros = erros;
  }
}

type ApiFetchOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  // FormData é usada pelo upload de foto de perfil (multipart/form-data);
  // qualquer outro valor vai como JSON.
  body?: unknown;
  // Algumas rotas (login, cadastro, listagem de cursos) são públicas e não
  // devem enviar o header Authorization mesmo se houver um token antigo salvo.
  autenticado?: boolean;
};

export async function apiFetch<TResposta>(
  caminho: string,
  { method = 'GET', body, autenticado = true }: ApiFetchOptions = {},
): Promise<TResposta> {
  const headers: Record<string, string> = {};

  const corpoFormData = body instanceof FormData;
  // Para FormData o navegador define o Content-Type (multipart/form-data;
  // boundary=...) sozinho; se a gente fixar "application/json" aqui, o
  // backend não consegue mais separar os campos do arquivo.
  if (!corpoFormData) {
    headers['Content-Type'] = 'application/json';
  }

  if (autenticado) {
    const token = obterToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const resposta = await fetch(`${BASE_URL}${caminho}`, {
    method,
    headers,
    body: body === undefined ? undefined : corpoFormData ? (body as FormData) : JSON.stringify(body),
  });

  // 204 No Content não tem corpo para converter em JSON.
  const dados = resposta.status === 204 ? null : await resposta.json();

  if (!resposta.ok) {
    const erro = dados as ErroResposta;
    throw new ApiError(
      resposta.status,
      erro?.mensagem ?? 'Não foi possível completar a requisição.',
      erro?.erros,
    );
  }

  return dados as TResposta;
}

// GET /projetos, /candidaturas, /habilidades etc. devolvem uma Page do
// Spring Data (paginação embutida), não um array puro como /cursos. Só os
// campos usados pelas telas estão tipados aqui; o restante do JSON
// (pageable, sort, first, empty...) é ignorado.
export type PaginaResposta<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
};

// Monta a query string a partir de um objeto de filtros/paginação, pulando
// chaves com valor undefined — evita mandar "?status=undefined" pro backend.
// Usado pelas rotas de listagem que aceitam filtro opcional (busca, status,
// categoria, page, size...).
export function construirQuery(parametros: Record<string, string | number | boolean | undefined>): string {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== undefined) {
      busca.set(chave, String(valor));
    }
  }
  const query = busca.toString();
  return query ? `?${query}` : '';
}