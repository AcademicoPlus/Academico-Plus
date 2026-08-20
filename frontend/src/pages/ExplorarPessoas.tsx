// Página "Explorar Pessoas" — lista paginada de usuários com busca por nome,
// filtro por curso e por habilidade. Consome GET /usuarios (autenticado) via
// apiFetch, o mesmo client central usado no restante do frontend.
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { apiFetch } from '../services/apiClient';
import { listarCursos, type Curso } from '../services/cursoService';
import Card from '../components/Card';
import Badge from '../components/Badge';

// ─── Tipos ────────────────────────────────────────────────────────────────────

type Habilidade = {
  id: string;
  nome: string;
};

type Usuario = {
  id: string;
  nome: string;
  email: string;
  curso: string;
  habilidades: Habilidade[];
  projetos: number;
  endossos: number;
  foto?: string;
  bio?: string;
};

type PaginaUsuarios = {
  pagina: number;
  limite: number;
  total: number;
  totalPaginas: number;
  dados: Usuario[];
};

type Params = {
  busca: string;
  habilidade: string;
  curso: string;
};

// ─── Utilitários ──────────────────────────────────────────────────────────────

/** Retorna as iniciais do nome (até 2 letras). */
function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

/** Cores de avatar — cicla pelo nome para manter consistência. */
const avatarColors = [
  'bg-indigo-100 text-indigo-700',
  'bg-violet-100 text-violet-700',
  'bg-emerald-100 text-emerald-700',
  'bg-amber-100  text-amber-700',
  'bg-rose-100   text-rose-700',
];

function avatarColor(nome: string): string {
  let hash = 0;
  for (let i = 0; i < nome.length; i++) {
    hash = (hash * 31 + nome.charCodeAt(i)) >>> 0;
  }
  return avatarColors[hash % avatarColors.length];
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function SkeletonCard() {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 flex flex-col gap-3 animate-pulse">
      <div className="flex items-center gap-3">
        <div className="h-12 w-12 rounded-full bg-gray-200 shrink-0" />
        <div className="flex-1 flex flex-col gap-2">
          <div className="h-4 w-2/3 rounded bg-gray-200" />
          <div className="h-3 w-1/2 rounded bg-gray-100" />
        </div>
      </div>
      <div className="flex gap-2 mt-1">
        <div className="h-5 w-16 rounded-full bg-gray-100" />
        <div className="h-5 w-20 rounded-full bg-gray-100" />
        <div className="h-5 w-14 rounded-full bg-gray-100" />
      </div>
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function ExplorarPessoas() {
  const navigate = useNavigate();

  // Filtros controlados
  const [params, setParams] = useState<Params>({
    busca: '',
    habilidade: '',
    curso: '',
  });

  // Dados
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // Cursos para o select de filtro (lista pública)
  const [cursos, setCursos] = useState<Curso[]>([]);

  // Habilidades únicas extraídas dos resultados para popular o select
  const [habilidades, setHabilidades] = useState<string[]>([]);

  // Debounce da busca por texto
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── Carrega cursos uma vez ──────────────────────────────────────────────────
  useEffect(() => {
    listarCursos()
      .then(setCursos)
      .catch(() => {/* silencia — select ficará vazio */});
  }, []);

  // ── Busca usuários sempre que os filtros mudarem ────────────────────────────
  useEffect(() => {
    setCarregando(true);
    setErro(null);

    const query = new URLSearchParams({ pagina: '1', limite: '20' });
    if (params.busca)      query.set('busca', params.busca);
    if (params.habilidade) query.set('habilidade', params.habilidade);
    if (params.curso)      query.set('curso', params.curso);

    apiFetch<PaginaUsuarios>(`/usuarios?${query.toString()}`)
      .then((res) => {
        setUsuarios(res.dados);
        // Extrai habilidades únicas dos resultados para o filtro
        const nomes = Array.from(
          new Set(
            res.dados.flatMap((u) => u.habilidades.map((h) => h.nome)),
          ),
        ).sort();
        setHabilidades(nomes);
      })
      .catch((e: Error) => setErro(e.message ?? 'Erro ao carregar pessoas.'))
      .finally(() => setCarregando(false));
  }, [params]);

  // ── Handlers ────────────────────────────────────────────────────────────────

  function handleBusca(valor: string) {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setParams((p) => ({ ...p, busca: valor }));
    }, 400);
  }

  function handleCurso(valor: string) {
    setParams((p) => ({ ...p, curso: valor }));
  }

  function handleHabilidade(valor: string) {
    setParams((p) => ({ ...p, habilidade: valor }));
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col gap-6">
      {/* Cabeçalho */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Explorar Pessoas</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Encontre colaboradores por nome, curso ou habilidade
        </p>
      </div>

      {/* Barra de busca + filtros */}
      <div className="flex flex-col sm:flex-row gap-3">
        {/* Busca por nome */}
        <div className="relative flex-1">
          <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-gray-400">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none"
              viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
          </span>
          <input
            type="search"
            placeholder="Buscar por nome..."
            defaultValue={params.busca}
            onChange={(e) => handleBusca(e.target.value)}
            className="w-full rounded-xl border border-gray-200 bg-gray-50 py-2.5 pl-9 pr-4
                       text-sm text-gray-800 placeholder:text-gray-400 outline-none
                       focus:bg-white focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500
                       transition-colors duration-150"
          />
        </div>

        {/* Filtro por curso */}
        <select
          value={params.curso}
          onChange={(e) => handleCurso(e.target.value)}
          className="rounded-xl border border-gray-200 bg-gray-50 py-2.5 px-3 text-sm
                     text-gray-700 outline-none focus:border-indigo-400 focus:ring-2
                     focus:ring-indigo-500 focus:bg-white transition-colors duration-150
                     min-w-[160px]"
        >
          <option value="">Todos os cursos</option>
          {cursos.map((c) => (
            <option key={c.id} value={c.nome}>{c.nome}</option>
          ))}
        </select>

        {/* Filtro por habilidade */}
        <select
          value={params.habilidade}
          onChange={(e) => handleHabilidade(e.target.value)}
          className="rounded-xl border border-gray-200 bg-gray-50 py-2.5 px-3 text-sm
                     text-gray-700 outline-none focus:border-indigo-400 focus:ring-2
                     focus:ring-indigo-500 focus:bg-white transition-colors duration-150
                     min-w-[160px]"
        >
          <option value="">Todas as habilidades</option>
          {habilidades.map((h) => (
            <option key={h} value={h}>{h}</option>
          ))}
        </select>
      </div>

      {/* Erro */}
      {erro && (
        <p className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600">
          {erro}
        </p>
      )}

      {/* Grid de cards */}
      {carregando ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : usuarios.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-center">
          <span className="text-4xl">🔍</span>
          <p className="text-gray-500 text-sm">Nenhuma pessoa encontrada.</p>
          <p className="text-gray-400 text-xs">Tente outros termos ou remova os filtros.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {usuarios.map((usuario) => (
            <Card
              key={usuario.id}
              onClick={() => navigate(`/usuarios/${usuario.id}`)}
            >
              <div className="flex flex-col gap-3">
                {/* Avatar + nome + curso */}
                <div className="flex items-center gap-3">
                  {usuario.foto ? (
                    <img
                      src={usuario.foto}
                      alt={usuario.nome}
                      className="h-12 w-12 rounded-full object-cover shrink-0"
                    />
                  ) : (
                    <span
                      className={`inline-flex h-12 w-12 items-center justify-center
                                  rounded-full text-sm font-semibold shrink-0
                                  ${avatarColor(usuario.nome)}`}
                    >
                      {iniciais(usuario.nome)}
                    </span>
                  )}

                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 truncate">
                      {usuario.nome}
                    </p>
                    <p className="text-xs text-gray-500 truncate">{usuario.curso}</p>
                  </div>
                </div>

                {/* Bio (quando disponível) */}
                {usuario.bio && (
                  <p className="text-xs text-gray-500 line-clamp-2 leading-relaxed">
                    {usuario.bio}
                  </p>
                )}

                {/* Habilidades */}
                {usuario.habilidades.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {usuario.habilidades.slice(0, 4).map((h) => (
                      <Badge key={h.id} variant="skill">
                        {h.nome}
                      </Badge>
                    ))}
                    {usuario.habilidades.length > 4 && (
                      <span className="text-xs text-gray-400 self-center">
                        +{usuario.habilidades.length - 4}
                      </span>
                    )}
                  </div>
                )}

                {/* Rodapé com métricas */}
                <div className="flex gap-4 pt-1 border-t border-gray-50 mt-auto">
                  <span className="text-xs text-gray-500">
                    <span className="font-semibold text-gray-700">{usuario.projetos}</span>{' '}
                    {usuario.projetos === 1 ? 'projeto' : 'projetos'}
                  </span>
                  <span className="text-xs text-gray-500">
                    <span className="font-semibold text-gray-700">{usuario.endossos}</span>{' '}
                    {usuario.endossos === 1 ? 'endosso' : 'endossos'}
                  </span>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
