// Dados fictícios usados no lugar do backend real quando VITE_USE_MOCKS=true
// (ver apiClient.ts). Objetivo: quem está no frontend consegue rodar
// `npm run dev` e testar as telas sem precisar subir o academico-backend
// nem o banco. Veja docs/CONECTANDO_FRONT_BACKEND.md, seção "Testando sem
// o backend".
//
// Rotas com segmento fixo (sem :id) moram num `case` do switch abaixo; rotas
// com id dinâmico (ex.: /projetos/:id) são casadas por regex logo depois do
// switch. Cada uma devolve os dados já no formato do DTO do backend (mesma
// forma que o service espera). Pra simular uma tela nova sem backend, edite
// aqui — não precisa mexer no service nem na página.
import { ApiError, type ErroCampo } from './apiError';
import type { CadastroRequest, CadastroResponse, LoginRequest, LoginResponse, UsuarioResumo } from './authService';
import type { Curso } from './cursoService';
import type { Habilidade } from './habilidadeService';
import type { AtualizarProjetoRequest, CriarProjetoRequest, Projeto, ProjetoDetalhe, StatusProjeto } from './projetoService';
import type { ProjetoMembro } from './projetoMembroService';
import type { ProjetoRecomendado } from './recomendacaoService';
import type { UsuarioPerfil } from './usuarioService';
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

const HABILIDADES_MOCK: Habilidade[] = [
  { id: 'aaaaaaaa-0001-0001-0001-000000000001', nome: 'React', categoria: 'Frontend', descricao: null, usuariosCount: 4, projetosCount: 2, criadoEm: '2024-01-01T00:00:00Z' },
  { id: 'aaaaaaaa-0001-0001-0001-000000000002', nome: 'Node.js', categoria: 'Backend', descricao: null, usuariosCount: 3, projetosCount: 1, criadoEm: '2024-01-01T00:00:00Z' },
  { id: 'aaaaaaaa-0001-0001-0001-000000000003', nome: 'Figma', categoria: 'Design', descricao: null, usuariosCount: 2, projetosCount: 1, criadoEm: '2024-01-01T00:00:00Z' },
  { id: 'aaaaaaaa-0001-0001-0001-000000000004', nome: 'Python', categoria: 'Backend', descricao: null, usuariosCount: 5, projetosCount: 3, criadoEm: '2024-01-01T00:00:00Z' },
];

// Credenciais fixas pra testar o login sem backend.
export const CREDENCIAIS_MOCK = {
  email: 'ana.silva@academico.edu.br',
  senha: 'senha123',
};

// "Usuário logado" nos mocks — mesma pessoa que o login mock devolve.
const USUARIO_LOGADO_MOCK: UsuarioResumo = {
  id: 'usuario-mock-1',
  nome: 'Ana Silva',
  curso: 'Ciência da Computação',
  fotoUrl: null,
  permission: 'ALUNO',
  periodo: 4,
  notaMedia: 4.5,
  totalAvaliacoes: 3,
};

const LUCAS_MENDES_MOCK: UsuarioResumo = {
  id: 'usuario-mock-2',
  nome: 'Lucas Mendes',
  curso: 'Ciência da Computação',
  fotoUrl: null,
  permission: 'ALUNO',
  periodo: 6,
  notaMedia: 4.8,
  totalAvaliacoes: 5,
};

const MARIA_COSTA_MOCK: UsuarioResumo = {
  id: 'usuario-mock-3',
  nome: 'Maria Costa',
  curso: 'Administração',
  fotoUrl: null,
  permission: 'ALUNO',
  periodo: 5,
  notaMedia: 4.2,
  totalAvaliacoes: 2,
};

// Usar esse e-mail no cadastro simula "e-mail já cadastrado" (erro 400 com
// erro de campo, igual ao que o backend devolve).
const EMAIL_JA_CADASTRADO_MOCK = 'duplicado@academico.edu.br';

function habilidadeNecessariaMock(habilidadeId: string, obrigatoria: boolean) {
  const habilidade = HABILIDADES_MOCK.find((h) => h.id === habilidadeId)!;
  return {
    id: `vinculo-mock-${habilidade.id}`,
    habilidade: { id: habilidade.id, nome: habilidade.nome, categoria: habilidade.categoria },
    obrigatoria,
  };
}

// "Banco" em memória — começa com um seed e cresce a cada POST /projetos
// simulado, pra dar pra criar um projeto e já ver ele aparecer no feed.
const PROJETOS_MOCK: ProjetoDetalhe[] = [
  {
    id: 'projeto-seed-1',
    criador: LUCAS_MENDES_MOCK,
    titulo: 'App de Saúde Mental para Universitários',
    descricao: 'Desenvolvimento de uma aplicação móvel focada no bem-estar e saúde mental de estudantes universitários, com recursos de mindfulness, tracking de humor e conexão com suporte psicológico institucional.',
    status: 'ABERTO',
    vagas: 3,
    vagasPreenchidas: 1,
    vagasDisponiveis: 2,
    aceitandoCandidaturas: true,
    dataFim: '2026-12-15T23:59:59',
    ativo: true,
    criadoEm: '2024-02-01T10:00:00Z',
    atualizadoEm: '2024-02-01T10:00:00Z',
    habilidadesNecessarias: [
      habilidadeNecessariaMock('aaaaaaaa-0001-0001-0001-000000000001', true),
      habilidadeNecessariaMock('aaaaaaaa-0001-0001-0001-000000000003', false),
    ],
    totalMembros: 1,
    totalCandidaturasPendentes: 0,
  },
  {
    id: 'projeto-seed-2',
    criador: USUARIO_LOGADO_MOCK,
    titulo: 'Sistema Inteligente de Proteção Web',
    descricao: 'Desenvolvimento de um sistema voltado para segurança online utilizando análise de comportamento visual e aprendizado de máquina para detectar acessos suspeitos.',
    status: 'EM_ANDAMENTO',
    vagas: 5,
    vagasPreenchidas: 2,
    vagasDisponiveis: 3,
    aceitandoCandidaturas: true,
    dataFim: null,
    ativo: true,
    criadoEm: '2024-01-15T10:00:00Z',
    atualizadoEm: '2024-01-15T10:00:00Z',
    habilidadesNecessarias: [
      habilidadeNecessariaMock('aaaaaaaa-0001-0001-0001-000000000002', true),
      habilidadeNecessariaMock('aaaaaaaa-0001-0001-0001-000000000004', true),
    ],
    totalMembros: 2,
    totalCandidaturasPendentes: 1,
  },
  {
    id: 'projeto-seed-3',
    criador: MARIA_COSTA_MOCK,
    titulo: 'Plataforma de E-commerce Sustentável',
    descricao: 'Criação de uma plataforma focada em produtos sustentáveis locais, conectando produtores e consumidores da região de forma direta e sem intermediários.',
    status: 'ABERTO',
    vagas: 4,
    vagasPreenchidas: 3,
    vagasDisponiveis: 1,
    aceitandoCandidaturas: true,
    dataFim: '2026-11-30T23:59:59',
    ativo: true,
    criadoEm: '2024-03-10T10:00:00Z',
    atualizadoEm: '2024-03-10T10:00:00Z',
    habilidadesNecessarias: [habilidadeNecessariaMock('aaaaaaaa-0001-0001-0001-000000000001', true)],
    totalMembros: 3,
    totalCandidaturasPendentes: 0,
  },
];

// Membros por projeto — só pros seeds (projetos criados no mock viram membro
// único: o próprio criador, resolvido em membrosDoProjetoMock).
const MEMBROS_MOCK: Record<string, ProjetoMembro[]> = {
  'projeto-seed-1': [
    { id: 'membro-mock-1', projeto: null, usuario: LUCAS_MENDES_MOCK, funcao: 'Criador(a)', dataAdesao: '2024-02-01T10:00:00Z' },
  ],
  'projeto-seed-2': [
    { id: 'membro-mock-2', projeto: null, usuario: USUARIO_LOGADO_MOCK, funcao: 'Criador(a)', dataAdesao: '2024-01-15T10:00:00Z' },
    { id: 'membro-mock-3', projeto: null, usuario: LUCAS_MENDES_MOCK, funcao: 'Desenvolvedor(a)', dataAdesao: '2024-01-20T10:00:00Z' },
  ],
  'projeto-seed-3': [
    { id: 'membro-mock-4', projeto: null, usuario: MARIA_COSTA_MOCK, funcao: 'Criador(a)', dataAdesao: '2024-03-10T10:00:00Z' },
    { id: 'membro-mock-5', projeto: null, usuario: LUCAS_MENDES_MOCK, funcao: 'Desenvolvedor(a)', dataAdesao: '2024-03-12T10:00:00Z' },
    { id: 'membro-mock-6', projeto: null, usuario: USUARIO_LOGADO_MOCK, funcao: 'Designer', dataAdesao: '2024-03-14T10:00:00Z' },
  ],
};

function membrosDoProjetoMock(projeto: ProjetoDetalhe): ProjetoMembro[] {
  return (
    MEMBROS_MOCK[projeto.id] ?? [
      {
        id: `membro-mock-criador-${projeto.id}`,
        projeto: null,
        usuario: projeto.criador,
        funcao: 'Criador(a)',
        dataAdesao: projeto.criadoEm,
      },
    ]
  );
}

function paraResumo(projeto: ProjetoDetalhe): Projeto {
  return clonar({
    id: projeto.id,
    criador: projeto.criador,
    titulo: projeto.titulo,
    descricao: projeto.descricao,
    status: projeto.status,
    habilidadesNecessarias: projeto.habilidadesNecessarias,
    vagas: projeto.vagas,
    vagasPreenchidas: projeto.vagasPreenchidas,
    dataFim: projeto.dataFim,
  });
}

// PROJETOS_MOCK guarda os objetos "reais" do banco em memória — nunca
// devolve eles direto pro front. Sem clonar, o componente que recebe a
// resposta segura a MESMA referência, e um `[...habilidadesNecessarias, x]`
// feito ali em cima de um array que a própria mutação aqui já alterou
// duplica a entrada (foi exatamente o que aconteceu com o vínculo de
// habilidade antes desse helper existir).
function clonar<T>(valor: T): T {
  return structuredClone(valor);
}

function paginar<T>(itens: T[]): PaginaResposta<T> {
  return {
    content: itens,
    totalElements: itens.length,
    totalPages: 1,
    number: 0,
    size: itens.length,
    last: true,
  };
}

type MockOptions = {
  method: string;
  body?: unknown;
};

export function mockFetch<TResposta>(caminho: string, { method, body }: MockOptions): Promise<TResposta> {
  const [caminhoSemQuery, query] = caminho.split('?');
  const parametros = new URLSearchParams(query ?? '');
  const rota = `${method} ${caminhoSemQuery}`;

  switch (rota) {
    case 'GET /cursos':
      return resolverComAtraso(CURSOS_MOCK as unknown as TResposta);

    case 'GET /habilidades':
      return resolverComAtraso(paginar(HABILIDADES_MOCK) as unknown as TResposta);

    case 'GET /usuarios/me': {
      const perfil: UsuarioPerfil = {
        id: USUARIO_LOGADO_MOCK.id,
        email: CREDENCIAIS_MOCK.email,
        nome: USUARIO_LOGADO_MOCK.nome,
        nivelAcesso: null,
        curso: CURSOS_MOCK[0],
        periodo: USUARIO_LOGADO_MOCK.periodo,
        bio: null,
        fotoUrl: USUARIO_LOGADO_MOCK.fotoUrl,
        linkedinUrl: null,
        githubUrl: null,
        habilidades: [],
        notaMedia: USUARIO_LOGADO_MOCK.notaMedia,
        totalAvaliacoes: USUARIO_LOGADO_MOCK.totalAvaliacoes,
        termosAceitosEm: '2024-01-01T00:00:00Z',
        ativo: true,
        criadoEm: '2024-01-01T00:00:00Z',
        atualizadoEm: '2024-01-01T00:00:00Z',
      };
      return resolverComAtraso(perfil as unknown as TResposta);
    }

    case 'GET /projetos': {
      const busca = parametros.get('busca')?.toLowerCase();
      const status = parametros.get('status');
      const filtrados = PROJETOS_MOCK.filter((projeto) => {
        if (status && projeto.status !== status) return false;
        if (busca && !projeto.titulo.toLowerCase().includes(busca) && !projeto.descricao.toLowerCase().includes(busca)) {
          return false;
        }
        return true;
      });
      return resolverComAtraso(paginar(filtrados.map(paraResumo)) as unknown as TResposta);
    }

    case 'GET /projetos/meus-projetos': {
      const meus = PROJETOS_MOCK.filter((p) => p.criador?.id === USUARIO_LOGADO_MOCK.id);
      return resolverComAtraso(paginar(meus.map(paraResumo)) as unknown as TResposta);
    }

    case 'GET /projetos/participando': {
      const participando = PROJETOS_MOCK.filter((p) =>
        membrosDoProjetoMock(p).some((m) => m.usuario?.id === USUARIO_LOGADO_MOCK.id && p.criador?.id !== USUARIO_LOGADO_MOCK.id),
      );
      return resolverComAtraso(paginar(participando.map(paraResumo)) as unknown as TResposta);
    }

    case 'GET /recomendacoes/projetos': {
      const limite = Number(parametros.get('limite')) || 5;
      // Mesmo filtro do RecomendacaoService real: só ABERTO e de outra pessoa
      // (a pontuação de compatibilidade em si é só ilustrativa aqui).
      const recomendados: ProjetoRecomendado[] = PROJETOS_MOCK
        .filter((p) => p.status === 'ABERTO' && p.criador?.id !== USUARIO_LOGADO_MOCK.id)
        .slice(0, limite)
        .map((p) => ({
          projeto: paraResumo(p),
          compatibilidade: 0.75,
          habilidadesEmComum: clonar(p.habilidadesNecessarias.slice(0, 1).map((h) => h.habilidade)),
        }));
      return resolverComAtraso(recomendados as unknown as TResposta);
    }

    case 'POST /projetos': {
      const dados = body as CriarProjetoRequest;
      const habilidadesNecessarias = (dados.habilidades ?? [])
        .filter((vinculo) => HABILIDADES_MOCK.some((h) => h.id === vinculo.habilidadeId))
        .map((vinculo) => habilidadeNecessariaMock(vinculo.habilidadeId, vinculo.obrigatoria ?? false));

      const agora = new Date().toISOString();
      const novoProjeto: ProjetoDetalhe = {
        id: `projeto-mock-${agora}`,
        criador: USUARIO_LOGADO_MOCK,
        titulo: dados.titulo,
        descricao: dados.descricao,
        status: 'ABERTO',
        vagas: dados.vagas,
        vagasPreenchidas: 0,
        vagasDisponiveis: dados.vagas,
        aceitandoCandidaturas: true,
        dataFim: dados.dataFim ?? null,
        ativo: true,
        criadoEm: agora,
        atualizadoEm: agora,
        habilidadesNecessarias,
        totalMembros: 1,
        totalCandidaturasPendentes: 0,
      };
      PROJETOS_MOCK.unshift(novoProjeto);
      return resolverComAtraso(clonar(novoProjeto) as unknown as TResposta);
    }

    case 'POST /auth/login': {
      const { email, senha } = body as LoginRequest;
      if (email === CREDENCIAIS_MOCK.email && senha === CREDENCIAIS_MOCK.senha) {
        const resposta: LoginResponse = {
          sucesso: true,
          mensagem: 'Login realizado com sucesso.',
          token: 'token-mock-dev',
          usuario: USUARIO_LOGADO_MOCK,
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

    default: {
      // Rotas com :id (ex.: "/projetos/abc123" ou "/projetos/abc123/membros") —
      // casadas por regex porque o id muda a cada projeto criado.
      const matchMembros = method === 'GET' && caminhoSemQuery.match(/^\/projetos\/([^/]+)\/membros$/);
      if (matchMembros) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchMembros[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        return resolverComAtraso(clonar(membrosDoProjetoMock(projeto)) as unknown as TResposta);
      }

      const matchProjeto = method === 'GET' && caminhoSemQuery.match(/^\/projetos\/([^/]+)$/);
      if (matchProjeto) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchProjeto[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        return resolverComAtraso(clonar(projeto) as unknown as TResposta);
      }

      const matchAtualizar = method === 'PUT' && caminhoSemQuery.match(/^\/projetos\/([^/]+)$/);
      if (matchAtualizar) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchAtualizar[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        if (projeto.status === 'CONCLUIDO' || projeto.status === 'CANCELADO') {
          return rejeitarComAtraso(
            new ApiError(400, `Projetos com status ${projeto.status} não podem ser editados.`),
          );
        }
        const dados = body as AtualizarProjetoRequest;
        if (dados.vagas < projeto.totalMembros) {
          return rejeitarComAtraso(
            new ApiError(400, `O número de vagas não pode ser menor do que as vagas já preenchidas (${projeto.totalMembros}).`),
          );
        }
        projeto.titulo = dados.titulo;
        projeto.descricao = dados.descricao;
        projeto.vagas = dados.vagas;
        projeto.vagasDisponiveis = dados.vagas - projeto.vagasPreenchidas;
        projeto.dataFim = dados.dataFim ?? null;
        projeto.atualizadoEm = new Date().toISOString();
        return resolverComAtraso(clonar(projeto) as unknown as TResposta);
      }

      const matchStatus = method === 'PATCH' && caminhoSemQuery.match(/^\/projetos\/([^/]+)\/status$/);
      if (matchStatus) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchStatus[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        if (projeto.status === 'CONCLUIDO' || projeto.status === 'CANCELADO') {
          return rejeitarComAtraso(
            new ApiError(400, `Projetos com status ${projeto.status} não podem ter o status alterado.`),
          );
        }
        const { status } = body as { status: StatusProjeto; motivo?: string };
        if (status === projeto.status) {
          return rejeitarComAtraso(new ApiError(400, `O projeto já está com o status ${status}.`));
        }
        projeto.status = status;
        projeto.aceitandoCandidaturas = status === 'ABERTO';
        projeto.atualizadoEm = new Date().toISOString();
        return resolverComAtraso(clonar(projeto) as unknown as TResposta);
      }

      const matchVincularHabilidade = method === 'POST' && caminhoSemQuery.match(/^\/projetos\/([^/]+)\/habilidades$/);
      if (matchVincularHabilidade) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchVincularHabilidade[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        const { habilidadeId, obrigatoria } = body as { habilidadeId: string; obrigatoria?: boolean };
        if (!HABILIDADES_MOCK.some((h) => h.id === habilidadeId)) {
          return rejeitarComAtraso(new ApiError(404, 'Habilidade não encontrada.'));
        }
        const vinculo = habilidadeNecessariaMock(habilidadeId, obrigatoria ?? false);
        projeto.habilidadesNecessarias = [...projeto.habilidadesNecessarias, vinculo];
        return resolverComAtraso(clonar(vinculo) as unknown as TResposta);
      }

      const matchAlterarObrigatoriedade =
        method === 'PATCH' && caminhoSemQuery.match(/^\/projetos\/([^/]+)\/habilidades\/([^/]+)$/);
      if (matchAlterarObrigatoriedade) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchAlterarObrigatoriedade[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        const habilidadeId = matchAlterarObrigatoriedade[2];
        const obrigatoria = parametros.get('obrigatoria') === 'true';
        const vinculo = projeto.habilidadesNecessarias.find((h) => h.habilidade.id === habilidadeId);
        if (!vinculo) {
          return rejeitarComAtraso(new ApiError(404, 'Vínculo de habilidade não encontrado.'));
        }
        vinculo.obrigatoria = obrigatoria;
        return resolverComAtraso(clonar(vinculo) as unknown as TResposta);
      }

      const matchDesvincularHabilidade =
        method === 'DELETE' && caminhoSemQuery.match(/^\/projetos\/([^/]+)\/habilidades\/([^/]+)$/);
      if (matchDesvincularHabilidade) {
        const projeto = PROJETOS_MOCK.find((p) => p.id === matchDesvincularHabilidade[1]);
        if (!projeto) {
          return rejeitarComAtraso(new ApiError(404, 'Projeto não encontrado.'));
        }
        const habilidadeId = matchDesvincularHabilidade[2];
        projeto.habilidadesNecessarias = projeto.habilidadesNecessarias.filter((h) => h.habilidade.id !== habilidadeId);
        return resolverComAtraso({ sucesso: true, mensagem: 'Habilidade desvinculada.' } as unknown as TResposta);
      }

      return rejeitarComAtraso(
        new ApiError(
          501,
          `Mock não implementado para "${rota}". Adicione um caso em services/mocks.ts ou rode com VITE_USE_MOCKS=false para usar o backend real.`,
        ),
      );
    }
  }
}
