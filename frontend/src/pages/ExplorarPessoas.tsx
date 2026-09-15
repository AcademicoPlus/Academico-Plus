// Página "Explorar Pessoas" — lista paginada de usuários com busca por nome
// e filtros por curso e habilidade. Usa explorarPerfis() do usuarioService
// (GET /usuarios/explorar) e os padrões do projeto (apiFetch, construirQuery,
// PaginaResposta).
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  explorarPerfis,
  type FiltroExplorarPerfis,
} from '../services/usuarioService';
import { apiFetch, type PaginaResposta } from '../services/apiClient';
import { listarCursos, type Curso } from '../services/cursoService';
import type { UsuarioResumo } from '../services/authService';
import Card from '../components/Card';
import Badge from '../components/Badge';
import ErroCard from '../components/ErroCard';
import EstadoVazio from '../components/EstadoVazio';
import Skeleton from '../components/Skeleton';

// ─── Tipos locais ─────────────────────────────────────────────────────────────

// O endpoint /usuarios/explorar pode retornar habilidades além do UsuarioResumo
// base. Tipamos localmente para não alterar o service.
type HabilidadeResumo = { id: string; nome: string };

type UsuarioExplorar = UsuarioResumo & {
  habilidades?: HabilidadeResumo[];
};

type HabilidadeCatalogo = { id: string; nome: string };

// ─── Utilitários ──────────────────────────────────────────────────────────────

function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

const avatarPalette = [
  'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/40 dark:text-indigo-300',
  'bg-violet-100 text-violet-700 dark:bg-violet-900/40 dark:text-violet-300',
  'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
  'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
  'bg-rose-100 text-rose-700 dark:bg-rose-900/40 dark:text-rose-300',
];

function avatarColor(nome: string): string {
  let h = 0;
  for (let i = 0; i < nome.length; i++) h = (h * 31 + nome.charCodeAt(i)) >>> 0;
  return avatarPalette[h % avatarPalette.length];
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function SkeletonCard() {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-700 shadow-sm p-5 flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <Skeleton className="h-11 w-11 rounded-full shrink-0" />
        <div className="flex-1 flex flex-col gap-2">
          <Skeleton className="h-3.5 w-3/5 rounded" />
          <Skeleton className="h-3 w-2/5 rounded" />
        </div>
      </div>
      <div className="flex gap-2">
        <Skeleton className="h-5 w-16 rounded-full" />
        <Skeleton className="h-5 w-20 rounded-full" />
        <Skeleton className="h-5 w-14 rounded-full" />
      </div>
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function ExplorarPessoas() {
  const navigate = useNavigate();

  // ── Filtros ────────────────────────────────────────────────────────────────
  const [busca, setBusca] = useState('');
  const [idCursoSelecionado, setIdCursoSelecionado] = useState('');
  const [idHabilidadeSelecionada, setIdHabilidadeSelecionada] = useState('');

  // ── Dados ──────────────────────────────────────────────────────────────────
  const [usuarios, setUsuarios] = useState<UsuarioExplorar[]>([]);
  const [total, setTotal] = useState<number | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // ── Listas de filtro ───────────────────────────────────────────────────────
  const [cursos, setCursos] = useState<Curso[]>([]);
  const [habilidadesCatalogo, setHabilidadesCatalogo] = useState<HabilidadeCatalogo[]>([]);

  // Debounce da busca por texto
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Filtros "commitados" (após debounce)
  const [filtroAtivo, setFiltroAtivo] = useState<FiltroExplorarPerfis>({});

  // ── Carrega cursos e catálogo de habilidades uma vez ──────────────────────
  useEffect(() => {
    listarCursos()
      .then(setCursos)
      .catch(() => {/* select de cursos ficará vazio */});

    apiFetch<PaginaResposta<HabilidadeCatalogo>>('/habilidades?size=100')
      .then((res) => setHabilidadesCatalogo(res.content ?? []))
      .catch(() => {/* select de habilidades ficará vazio */});
  }, []);

  // ── Busca usuários sempre que o filtro ativo mudar ────────────────────────
  useEffect(() => {
    explorarPerfis({ ...filtroAtivo, tamanho: 20 })
      .then((res) => {
        setUsuarios((res.content ?? []) as UsuarioExplorar[]);
        setTotal(res.totalElements ?? null);
      })
      .catch((e: Error) => setErro(e.message ?? 'Erro ao carregar pessoas.'))
      .finally(() => setCarregando(false));
  }, [filtroAtivo]);

  // ── Handlers ───────────────────────────────────────────────────────────────

  // Atualiza o filtro ativo já sinalizando o início do carregamento —
  // evita disparar setState de forma síncrona dentro do effect acima.
  function atualizarFiltroAtivo(
    atualizar: (f: FiltroExplorarPerfis) => FiltroExplorarPerfis,
  ) {
    setCarregando(true);
    setErro(null);
    setFiltroAtivo(atualizar);
  }

  function handleBusca(valor: string) {
    setBusca(valor);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      atualizarFiltroAtivo((f) => ({ ...f, busca: valor || undefined, pagina: undefined }));
    }, 400);
  }

  function handleCurso(valor: string) {
    setIdCursoSelecionado(valor);
    atualizarFiltroAtivo((f) => ({ ...f, idCurso: valor || undefined, pagina: undefined }));
  }

  function handleHabilidade(valor: string) {
    setIdHabilidadeSelecionada(valor);
    atualizarFiltroAtivo((f) => ({ ...f, idHabilidade: valor || undefined, pagina: undefined }));
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  const subtitulo = (() => {
    if (carregando) return 'Buscando…';

    if (total !== null) {
      return `${total} ${total === 1 ? 'pessoa encontrada' : 'pessoas encontradas'}`;
    }

    return 'Encontre colaboradores por nome, curso ou habilidade';
  })();

  return (
    <div className="flex flex-col gap-6">

      {/* Cabeçalho */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Explorar Pessoas</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{subtitulo}</p>
      </div>

      {/* Barra de busca + filtros */}
      <div className="flex flex-col sm:flex-row gap-3">

        {/* Campo de busca */}
        <div className="relative flex-1">
          <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-gray-400 dark:text-gray-500">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none"
              viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
          </span>
          <input
            type="search"
            value={busca}
            onChange={(e) => handleBusca(e.target.value)}
            placeholder="Buscar por nome ou curso..."
            className="w-full rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 py-2.5 pl-9 pr-4
                       text-sm text-gray-800 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 outline-none
                       focus:bg-white dark:focus:bg-slate-800 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500
                       transition-colors duration-150"
          />
        </div>

        {/* Filtro por curso */}
        <select
          value={idCursoSelecionado}
          onChange={(e) => handleCurso(e.target.value)}
          className="rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 py-2.5 px-3 text-sm
                     text-gray-700 dark:text-gray-200 outline-none focus:border-indigo-400 focus:ring-2
                     focus:ring-indigo-500 focus:bg-white dark:focus:bg-slate-800 transition-colors duration-150
                     min-w-42.5"
        >
          <option value="">Todos os cursos</option>
          {cursos.map((c) => (
            <option key={c.id} value={c.id}>{c.nome}</option>
          ))}
        </select>

        {/* Filtro por habilidade */}
        <select
          value={idHabilidadeSelecionada}
          onChange={(e) => handleHabilidade(e.target.value)}
          className="rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 py-2.5 px-3 text-sm
                     text-gray-700 dark:text-gray-200 outline-none focus:border-indigo-400 focus:ring-2
                     focus:ring-indigo-500 focus:bg-white dark:focus:bg-slate-800 transition-colors duration-150
                     min-w-42.5"
        >
          <option value="">Todas as habilidades</option>
          {habilidadesCatalogo.map((h) => (
            <option key={h.id} value={h.id}>{h.nome}</option>
          ))}
        </select>
      </div>

      {/* Erro */}
      {erro && <ErroCard>{erro}</ErroCard>}

      {/* Grid */}
      {carregando ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      ) : usuarios.length === 0 ? (
        <EstadoVazio
          icone="🔍"
          titulo="Nenhuma pessoa encontrada."
          descricao="Tente outros termos ou remova os filtros."
          className="py-20"
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {usuarios.map((u) => (
            <Card key={u.id} onClick={() => navigate(`/usuarios/${u.id}`)}>
              <div className="flex flex-col gap-3">

                {/* Avatar + nome + curso */}
                <div className="flex items-center gap-3">
                  {u.fotoUrl ? (
                    <img
                      src={u.fotoUrl}
                      alt={u.nome}
                      className="h-11 w-11 rounded-full object-cover shrink-0"
                    />
                  ) : (
                    <span
                      className={`inline-flex h-11 w-11 items-center justify-center
                                  rounded-full text-sm font-semibold shrink-0
                                  ${avatarColor(u.nome)}`}
                    >
                      {iniciais(u.nome)}
                    </span>
                  )}

                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{u.nome}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{u.curso ?? '—'}</p>
                  </div>
                </div>

                {/* Badge de professor */}
                {u.permission === 'PROFESSOR' && (
                  <div>
                    <span className="inline-flex items-center rounded-full px-2.5 py-0.5
                                     text-xs font-medium bg-indigo-100 text-indigo-700 dark:bg-indigo-900/40 dark:text-indigo-300">
                      Professor
                    </span>
                  </div>
                )}

                {/* Habilidades */}
                {(u.habilidades ?? []).length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {(u.habilidades ?? []).slice(0, 4).map((h) => (
                      <Badge key={h.id} variant="skill">{h.nome}</Badge>
                    ))}
                    {(u.habilidades ?? []).length > 4 && (
                      <span className="text-xs text-gray-400 dark:text-gray-500 self-center">
                        +{(u.habilidades ?? []).length - 4}
                      </span>
                    )}
                  </div>
                )}

              </div>
            </Card>
          ))}
        </div>
      )}

    </div>
  );
}
