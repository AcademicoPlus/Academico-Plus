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
import type { CadastroRequest, CadastroResponse, LoginRequest, LoginResponse, UsuarioResumo } from './authService';
import type { Curso } from './cursoService';
import type { PaginaResposta } from './apiClient';

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

type HabilidadeMock = { id: string; nome: string };

// Catálogo fictício de habilidades, no mesmo formato do endpoint real
// GET /habilidades (ver habilidadeService.ts). Usado tanto para preencher
// o select de filtro em ExplorarPessoas quanto para dar às pessoas
// fictícias abaixo (USUARIOS_MOCK) habilidades reais para exibir nos cards.
const HABILIDADES_MOCK: HabilidadeMock[] = [
  { id: 'hab-java', nome: 'Java' },
  { id: 'hab-spring', nome: 'Spring Boot' },
  { id: 'hab-react', nome: 'React' },
  { id: 'hab-docker', nome: 'Docker' },
  { id: 'hab-marketing', nome: 'Marketing Digital' },
  { id: 'hab-branding', nome: 'Branding' },
  { id: 'hab-seo', nome: 'SEO' },
  { id: 'hab-ads', nome: 'Google Ads' },
  { id: 'hab-reactnative', nome: 'React Native' },
  { id: 'hab-ts', nome: 'TypeScript' },
  { id: 'hab-node', nome: 'Node.js' },
  { id: 'hab-aws', nome: 'AWS' },
  { id: 'hab-psico', nome: 'Psicologia' },
  { id: 'hab-pesquisa', nome: 'Pesquisa' },
  { id: 'hab-uxr', nome: 'UX Research' },
  { id: 'hab-uiux', nome: 'UI/UX' },
  { id: 'hab-figma', nome: 'Figma' },
];

// Atalho pra puxar uma habilidade do catálogo pelo id, sem repetir o
// objeto inteiro toda vez que um usuário fictício precisa dela.
const acha = (id: string) => HABILIDADES_MOCK.find((h) => h.id === id)!;

// Credenciais fixas pra testar o login sem backend.
export const CREDENCIAIS_MOCK = {
  email: 'ana.silva@academico.edu.br',
  senha: 'senha123',
};

type UsuarioMock = UsuarioResumo & { habilidades: HabilidadeMock[] };



// Pessoas fictícias para a tela Explorar Pessoas — nomes, cursos e
// habilidades variados de propósito, para testar busca, filtro por curso
// e filtro por habilidade com resultados diferentes.
const USUARIOS_MOCK: UsuarioMock[] = [
  { id: 'usuario-mock-2', nome: 'Bruno Alves', curso: 'Ciência da Computação', fotoUrl: null, permission: 'ALUNO', periodo: 6, notaMedia: 4.8, totalAvaliacoes: 5, habilidades: [acha('hab-java'), acha('hab-spring'), acha('hab-react'), acha('hab-docker')] },
  { id: 'usuario-mock-3', nome: 'Carla Mendes', curso: 'Design Gráfico', fotoUrl: null, permission: 'ALUNO', periodo: 3, notaMedia: 4.2, totalAvaliacoes: 2, habilidades: [acha('hab-marketing'), acha('hab-branding'), acha('hab-seo'), acha('hab-ads')] },
  { id: 'usuario-mock-4', nome: 'Diego Santos', curso: 'Engenharia de Software', fotoUrl: null, permission: 'ALUNO', periodo: 5, notaMedia: 4.6, totalAvaliacoes: 8, habilidades: [acha('hab-reactnative'), acha('hab-ts'), acha('hab-node'), acha('hab-aws')] },
  { id: 'usuario-mock-5', nome: 'Eduarda Lima', curso: 'Psicologia', fotoUrl: null, permission: 'ALUNO', periodo: 2, notaMedia: 4.9, totalAvaliacoes: 1, habilidades: [acha('hab-psico'), acha('hab-pesquisa'), acha('hab-uxr')] },
  { id: 'usuario-mock-6', nome: 'Felipe Rocha', curso: 'Administração', fotoUrl: null, permission: 'PROFESSOR', periodo: null, notaMedia: null, totalAvaliacoes: null, habilidades: [acha('hab-uiux'), acha('hab-pesquisa')] },
  { id: 'usuario-mock-7', nome: 'Giovana Ribeiro', curso: 'Ciência da Computação', fotoUrl: null, permission: 'ALUNO', periodo: 8, notaMedia: 4.7, totalAvaliacoes: 6, habilidades: [acha('hab-java'), acha('hab-react')] },
];

// Perfil completo (mutável) do usuário logado nos mocks. GET e PUT
// /usuarios/me leem e escrevem neste mesmo objeto, para que as alterações
// feitas em EditarPerfil realmente "persistam" enquanto o app está aberto
// (sem isso, toda edição seria perdida ao recarregar a tela de perfil).
let meuPerfilMock = {
  id: 'usuario-mock-1',
  email: CREDENCIAIS_MOCK.email,
  nome: 'Ana Silva',
  nivelAcesso: null,
  curso: CURSOS_MOCK[0], // Ciência da Computação
  periodo: 4,
  bio: 'Designer apaixonada por UI/UX e design systems. Busco projetos que unam criatividade e impacto social. Tenho experiência com Figma, pesquisa com usuários e prototipagem de alta fidelidade.',
  fotoUrl: null as string | null,
  linkedinUrl: 'https://linkedin.com/in',
  githubUrl: 'https://github.com',
  habilidades: [
    { id: 'perfil-hab-1', habilidade: acha('hab-uiux'), nivel: 'AVANCADO', endossado: true, endossadoPor: 'Prof. Carvalho' },
    { id: 'perfil-hab-2', habilidade: acha('hab-figma'), nivel: 'EXPERT', endossado: true, endossadoPor: 'Prof. Carvalho' },
    { id: 'perfil-hab-3', habilidade: acha('hab-react'), nivel: 'INTERMEDIARIO', endossado: false },
    { id: 'perfil-hab-4', habilidade: acha('hab-marketing'), nivel: 'INICIANTE', endossado: false },
    { id: 'perfil-hab-5', habilidade: acha('hab-uxr'), nivel: 'INTERMEDIARIO', endossado: false },
  ],
  notaMedia: 4.8,
  totalAvaliacoes: 6,
  termosAceitosEm: '2024-01-01T00:00:00Z',
  ativo: true,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
};

// Versão "resumida" da Ana, no formato UsuarioResumo — usada como `criador`
// nos projetos fictícios abaixo (o mesmo formato que o backend usa quando
// um usuário aparece aninhado em outro recurso).
const ANA_RESUMO: UsuarioResumo = {
  id: meuPerfilMock.id,
  nome: meuPerfilMock.nome,
  curso: meuPerfilMock.curso?.nome ?? null,
  fotoUrl: meuPerfilMock.fotoUrl,
  permission: 'ALUNO',
  periodo: meuPerfilMock.periodo,
  notaMedia: meuPerfilMock.notaMedia,
  totalAvaliacoes: meuPerfilMock.totalAvaliacoes,
};

// Projetos fictícios para a seção "Projetos" de Perfil.tsx — dois criados
// pela Ana (a usuária logada) e dois de outras pessoas, para testar tanto
// o próprio perfil quanto o perfil de terceiros.
const PROJETOS_MOCK = [
  {
    id: 'projeto-mock-1',
    criador: ANA_RESUMO,
    titulo: 'Redesign da Identidade Visual Universitária',
    descricao: 'Reformulação da identidade visual do centro acadêmico, unificando materiais impressos e digitais.',
    status: 'EM_ANDAMENTO',
    habilidadesNecessarias: [],
    vagas: 4,
    vagasPreenchidas: 2,
    dataFim: '2026-11-30',
  },
  {
    id: 'projeto-mock-2',
    criador: ANA_RESUMO,
    titulo: 'Festival de Arte Digital 2024',
    descricao: 'Organização e curadoria visual do festival anual de arte digital da universidade.',
    status: 'CONCLUIDO',
    habilidadesNecessarias: [],
    vagas: 3,
    vagasPreenchidas: 3,
    dataFim: '2024-12-01',
  },
  {
    id: 'projeto-mock-3',
    criador: USUARIOS_MOCK.find((u) => u.id === 'usuario-mock-2')!, // Bruno Alves
    titulo: 'Sistema Inteligente de Proteção Web',
    descricao: 'Ferramenta de análise de comportamento visual para detectar tentativas de phishing em tempo real.',
    status: 'EM_ANDAMENTO',
    habilidadesNecessarias: [],
    vagas: 5,
    vagasPreenchidas: 3,
    dataFim: '2026-12-15',
  },
  {
    id: 'projeto-mock-4',
    criador: USUARIOS_MOCK.find((u) => u.id === 'usuario-mock-3')!, // Carla Mendes
    titulo: 'Plataforma de E-commerce Sustentável',
    descricao: 'Marketplace conectando produtores locais a consumidores conscientes.',
    status: 'ABERTO',
    habilidadesNecessarias: [],
    vagas: 6,
    vagasPreenchidas: 1,
    dataFim: null,
  },
];

function paginaDeProjetos(lista: typeof PROJETOS_MOCK, page: number, size: number) {
  const inicio = page * size;
  const pagina = lista.slice(inicio, inicio + size);
  return {
    content: pagina,
    totalElements: lista.length,
    totalPages: Math.ceil(lista.length / size) || 1,
    number: page,
    size,
    last: inicio + size >= lista.length,
  };
}

// Perfil completo de outro usuário (GET /usuarios/:id) — monta um
// UsuarioPerfil a partir da entrada correspondente em USUARIOS_MOCK,
// já que UsuarioResumo (usado no Explorar Pessoas) não tem bio/links/etc.
function mockBuscarUsuarioPorId(id: string) {
  const usuario = USUARIOS_MOCK.find((u) => u.id === id);
  if (!usuario) {
    return rejeitarComAtraso(new ApiError(404, 'Usuário não encontrado.'));
  }
  const curso = CURSOS_MOCK.find((c) => c.nome === usuario.curso) ?? null;
  return resolverComAtraso({
    id: usuario.id,
    email: `${usuario.nome.toLowerCase().replace(/\s+/g, '.')}@academico.edu.br`,
    nivelAcesso: null,
    nome: usuario.nome,
    curso,
    periodo: usuario.periodo,
    bio: null,
    fotoUrl: usuario.fotoUrl,
    linkedinUrl: null,
    githubUrl: null,
    habilidades: usuario.habilidades.map((h, i) => ({
      id: `${usuario.id}-hab-${i}`,
      habilidade: h,
      nivel: 'INTERMEDIARIO',
      endossado: false,
    })),
    notaMedia: usuario.notaMedia,
    totalAvaliacoes: usuario.totalAvaliacoes,
    termosAceitosEm: '2024-01-01T00:00:00Z',
    ativo: true,
    criadoEm: '2024-01-01T00:00:00Z',
    atualizadoEm: '2024-01-01T00:00:00Z',
  });
}





// Usar esse e-mail no cadastro simula "e-mail já cadastrado" (erro 400 com
// erro de campo, igual ao que o backend devolve).
const EMAIL_JA_CADASTRADO_MOCK = 'duplicado@academico.edu.br';

type MockOptions = {
  method: string;
  body?: unknown;
};

// Simula o comportamento de explorarPerfis (busca, filtro por curso e
// paginação), já que essa rota tem query string variável e não cabe em
// um `case` fixo do switch abaixo.
function mockExplorarPerfis(query: URLSearchParams): Promise<PaginaResposta<UsuarioResumo>> {
  const busca = query.get('busca')?.toLowerCase() ?? '';
  const idCurso = query.get('idCurso') ?? '';
  const page = Number(query.get('page') ?? '0');
  const size = Number(query.get('size') ?? '10');

  // idCurso vem como id (ver CadastroRequest), mas UsuarioResumo.curso é
  // só o nome — então convertemos o id pro nome antes de filtrar.
  const nomeCursoFiltro = idCurso
    ? CURSOS_MOCK.find((curso) => curso.id === idCurso)?.nome
    : undefined;

  const filtrados = USUARIOS_MOCK.filter((usuario) => {
    const bateBusca =
      !busca ||
      usuario.nome.toLowerCase().includes(busca) ||
      (usuario.curso?.toLowerCase().includes(busca) ?? false);
    const bateCurso = !nomeCursoFiltro || usuario.curso === nomeCursoFiltro;
    return bateBusca && bateCurso;
  });

  const inicio = page * size;
  const pagina = filtrados.slice(inicio, inicio + size);

  const resposta: PaginaResposta<UsuarioResumo> = {
    content: pagina,
    totalElements: filtrados.length,
    totalPages: Math.ceil(filtrados.length / size) || 1,
    number: page,
    size,
    last: inicio + size >= filtrados.length,
  };

  return resolverComAtraso(resposta);
}

export function mockFetch<TResposta>(caminho: string, { method, body }: MockOptions): Promise<TResposta> {
  const [caminhoBase, queryString] = caminho.split('?');

  if (method === 'GET' && caminhoBase === '/usuarios/explorar') {
    return mockExplorarPerfis(new URLSearchParams(queryString ?? '')) as unknown as Promise<TResposta>;
  }

  // Perfil de outro usuário: GET /usuarios/usuario-mock-N (mas não /usuarios/me)
  if (method === 'GET' && /^\/usuarios\/usuario-mock-\d+$/.test(caminhoBase)) {
    const usuarioId = caminhoBase.split('/').pop()!;
    return mockBuscarUsuarioPorId(usuarioId) as unknown as Promise<TResposta>;
  }

  // Listagens de projetos — todas com query string variável (filtros/paginação).
  if (
    method === 'GET' &&
    (caminhoBase === '/projetos' || caminhoBase === '/projetos/meus-projetos' || caminhoBase === '/projetos/participando')
  ) {
    const params = new URLSearchParams(queryString ?? '');
    const page = Number(params.get('page') ?? '0');
    const size = Number(params.get('size') ?? '10');

    let lista = PROJETOS_MOCK;
    if (caminhoBase === '/projetos/meus-projetos') {
      lista = PROJETOS_MOCK.filter((p) => p.criador?.id === meuPerfilMock.id);
    } else if (caminhoBase === '/projetos/participando') {
      lista = []; // nos dados fictícios, Ana ainda não participa de projetos de outras pessoas
    }
    return resolverComAtraso(paginaDeProjetos(lista, page, size) as unknown as TResposta);
  }

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
    case 'GET /usuarios/me':
      return resolverComAtraso({ ...meuPerfilMock } as unknown as TResposta);

    case 'PUT /usuarios/me': {
      const dados = body as {
        nome: string; idCurso?: string; bio?: string; linkedinUrl?: string; githubUrl?: string;
      };
      const cursoEscolhido = dados.idCurso
        ? CURSOS_MOCK.find((c) => c.id === dados.idCurso) ?? meuPerfilMock.curso
        : meuPerfilMock.curso;
      meuPerfilMock = {
        ...meuPerfilMock,
        nome: dados.nome,
        curso: cursoEscolhido,
        bio: dados.bio ?? null,
        linkedinUrl: dados.linkedinUrl ?? null,
        githubUrl: dados.githubUrl ?? null,
      };
      return resolverComAtraso({ ...meuPerfilMock } as unknown as TResposta);
    }

    case 'POST /usuarios/me/foto': {
      const formData = body as FormData;
      const arquivo = formData.get('foto') as File | null;
      if (arquivo) {
        meuPerfilMock = { ...meuPerfilMock, fotoUrl: URL.createObjectURL(arquivo) };
      }
      return resolverComAtraso({ ...meuPerfilMock } as unknown as TResposta);
    }

    // Catálogo usado pelo select "Todas as habilidades" em ExplorarPessoas
    // (ver ExplorarPessoas.tsx, useEffect que carrega habilidadesCatalogo).
    case 'GET /habilidades?size=100':
      return resolverComAtraso({
        content: HABILIDADES_MOCK,
        totalElements: HABILIDADES_MOCK.length,
        totalPages: 1,
        number: 0,
        size: 100,
        last: true,
      } as unknown as TResposta);
    default:
      return rejeitarComAtraso(
        new ApiError(
          501,
          `Mock não implementado para "${rota}". Adicione um caso em services/mocks.ts ou rode com VITE_USE_MOCKS=false para usar o backend real.`,
        ),
      );
  }
}
