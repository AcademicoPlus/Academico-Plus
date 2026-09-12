// Página de visualização de perfil (/usuarios/:id).
// Exibe banner decorativo, avatar sobreposto, dados do usuário,
// botão de editar perfil em violeta/roxo de destaque com ícones de redes sociais (LinkedIn/GitHub)
// ao lado (visíveis apenas para o dono), seção de habilidades com paleta do Figma,
// e listagem de projetos em cards.
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import {
  buscarUsuarioPorId,
  type UsuarioPerfil,
} from '../services/usuarioService';
import { obterMeuPerfilCache } from '../hooks/useMeuPerfil';
import {
  listarProjetos,
  listarMeusProjetos,
  listarProjetosParticipando,
  type Projeto,
} from '../services/projetoService';
import type { NivelHabilidade } from '../services/usuarioHabilidadeService';
import { listarAvaliacoesRecebidas, type Avaliacao } from '../services/avaliacaoService';
import { denunciarAvaliacao } from '../services/denunciaService';
import { ApiError } from '../services/apiClient';
import { formatarData, iniciaisDoNome } from '../utils/projeto';

import Card from '../components/Card';
import Badge from '../components/Badge';

// ─── Tipos Locais ─────────────────────────────────────────────────────────────

type HabilidadeDoUsuario = {
  id: string;
  habilidade: { id: string; nome: string };
  nivel?: NivelHabilidade | string;
};

// ─── Utilitários ──────────────────────────────────────────────────────────────

function formatarIniciais(nome: string): string {
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

function obterCorAvatar(nome: string): string {
  let h = 0;
  for (let i = 0; i < nome.length; i++) h = (h * 31 + nome.charCodeAt(i)) >>> 0;
  return avatarPalette[h % avatarPalette.length];
}

function formatarNivel(nivel?: string): string {
  if (!nivel) return '';
  return nivel.charAt(0).toUpperCase() + nivel.slice(1).toLowerCase();
}

function Estrelas({ nota }: { nota: number }) {
  return (
    <div className="flex items-center gap-0.5" aria-label={`Nota ${nota} de 5`}>
      {[1, 2, 3, 4, 5].map((posicao) => (
        <svg
          key={posicao}
          xmlns="http://www.w3.org/2000/svg"
          className={`h-3.5 w-3.5 ${posicao <= Math.round(nota) ? 'text-amber-400' : 'text-gray-200 dark:text-slate-700'}`}
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.958a1 1 0 00.95.69h4.162c.969 0 1.371 1.24.588 1.81l-3.368 2.448a1 1 0 00-.364 1.118l1.287 3.957c.299.922-.756 1.688-1.54 1.118l-3.367-2.447a1 1 0 00-1.176 0l-3.368 2.447c-.783.57-1.838-.196-1.539-1.118l1.286-3.957a1 1 0 00-.363-1.118L2.98 9.385c-.783-.57-.38-1.81.588-1.81h4.162a1 1 0 00.951-.69l1.286-3.958z" />
        </svg>
      ))}
    </div>
  );
}

/**
 * Cores específicas para as habilidades conforme o protótipo Figma:
 * UI/UX (violeta), Figma/Marketing (verde), React (laranja/âmbar), Pesquisa UX (roxo).
 */
function obterEstiloHabilidade(nomeHabilidade: string): string {
  const norm = nomeHabilidade.toLowerCase().trim();

  if (norm.includes('ui/ux') || norm.includes('design')) {
    return 'bg-violet-50 text-violet-700 border-violet-200 dark:bg-violet-900/40 dark:text-violet-300 dark:border-violet-800';
  }
  if (norm.includes('figma') || norm.includes('marketing')) {
    return 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/40 dark:text-emerald-300 dark:border-emerald-800';
  }
  if (norm.includes('react') || norm.includes('java') || norm.includes('front')) {
    return 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/40 dark:text-amber-300 dark:border-amber-800';
  }
  if (norm.includes('pesquisa') || norm.includes('ux')) {
    return 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/40 dark:text-purple-300 dark:border-purple-800';
  }

  // Paleta fallback ciclada
  let h = 0;
  for (let i = 0; i < nomeHabilidade.length; i++) h = (h * 31 + nomeHabilidade.charCodeAt(i)) >>> 0;
  const paletaFallback = [
    'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-900/40 dark:text-sky-300 dark:border-sky-800',
    'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-900/40 dark:text-rose-300 dark:border-rose-800',
    'bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-900/40 dark:text-teal-300 dark:border-teal-800',
    'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-900/40 dark:text-indigo-300 dark:border-indigo-800',
  ];
  return paletaFallback[h % paletaFallback.length];
}

// ─── Componente Principal ─────────────────────────────────────────────────────

export default function Perfil() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [usuario, setUsuario] = useState<UsuarioPerfil | null>(null);
  const [projetos, setProjetos] = useState<Projeto[]>([]);
  const [isDono, setIsDono] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // ── Avaliações recebidas ───────────────────────────────────────────────────
  const [avaliacoes, setAvaliacoes] = useState<Avaliacao[]>([]);
  const [denunciandoId, setDenunciandoId] = useState<string | null>(null);
  const [motivoDenuncia, setMotivoDenuncia] = useState('');
  const [enviandoDenuncia, setEnviandoDenuncia] = useState(false);
  const [erroDenuncia, setErroDenuncia] = useState<string | null>(null);
  const [avaliacoesDenunciadas, setAvaliacoesDenunciadas] = useState<Set<string>>(new Set());

  useEffect(() => {
    let ativo = true;

    async function carregarDados() {
      if (!id) return;
      setCarregando(true);
      setErro(null);

      try {
        // 1. Identifica o usuário logado para checar propriedade — cacheado,
        // então reaproveitamos o mesmo resultado no passo 2 sem nova chamada.
        let meuPerfil: UsuarioPerfil | null = null;
        try {
          meuPerfil = await obterMeuPerfilCache();
        } catch {
          // Usuário não autenticado ou falha ao checar token
        }

        const ehDono = meuPerfil?.id === id;
        if (ativo) setIsDono(ehDono);

        // 2. Busca os dados do perfil (via /me se for dono, ou /usuarios/:id se for outro)
        const perfilDados = ehDono && meuPerfil
          ? meuPerfil
          : await buscarUsuarioPorId(id);

        if (!ativo) return;
        setUsuario(perfilDados);

        // 3. Busca os projetos associados
        try {
          if (ehDono) {
            // Se for o dono, combinamos criados e participando
            const [meusRes, partRes] = await Promise.all([
              listarMeusProjetos().catch(() => null),
              listarProjetosParticipando().catch(() => null),
            ]);

            const listaCombinada = [
              ...(meusRes?.content ?? []),
              ...(partRes?.content ?? []),
            ];

            // Remove possíveis duplicados por ID
            const mapaUnico = new Map<string, Projeto>();
            for (const p of listaCombinada) {
              mapaUnico.set(p.id, p);
            }
            if (ativo) setProjetos(Array.from(mapaUnico.values()));
          } else {
            // Para outros usuários, listamos os projetos gerais e filtramos
            const todosRes = await listarProjetos({ tamanho: 50 }).catch(() => null);
            const filtrados = (todosRes?.content ?? []).filter(
              (p) => p.criador?.id === id
            );
            if (ativo) setProjetos(filtrados);
          }
        } catch {
          if (ativo) setProjetos([]);
        }

        // 4. Busca as avaliações recebidas (feitas por colegas de projeto)
        try {
          const paginaAvaliacoes = await listarAvaliacoesRecebidas(id);
          if (ativo) setAvaliacoes(paginaAvaliacoes.content ?? []);
        } catch {
          if (ativo) setAvaliacoes([]);
        }
      } catch (err) {
        if (ativo) {
          setErro(
            err instanceof Error
              ? err.message
              : 'Não foi possível carregar as informações do perfil.'
          );
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    carregarDados();

    return () => {
      ativo = false;
    };
  }, [id]);

  // ── Handlers de denúncia de avaliação ─────────────────────────────────────

  function handleAbrirDenuncia(avaliacaoId: string) {
    setDenunciandoId(avaliacaoId);
    setMotivoDenuncia('');
    setErroDenuncia(null);
  }

  async function handleEnviarDenuncia() {
    if (!denunciandoId) return;
    if (!motivoDenuncia.trim()) {
      setErroDenuncia('Explique o motivo da denúncia.');
      return;
    }

    setEnviandoDenuncia(true);
    setErroDenuncia(null);
    try {
      await denunciarAvaliacao(denunciandoId, motivoDenuncia.trim());
      setAvaliacoesDenunciadas((prev) => new Set(prev).add(denunciandoId));
      setDenunciandoId(null);
    } catch (e) {
      setErroDenuncia(e instanceof ApiError ? e.message : 'Não foi possível enviar a denúncia.');
    } finally {
      setEnviandoDenuncia(false);
    }
  }

  // ── Render: Carregando ───────────────────────────────────────────────────────
  if (carregando) {
    return (
      <div className="mx-auto max-w-4xl flex flex-col gap-6 py-4 animate-pulse">
        {/* Banner Skeleton */}
        <div className="h-40 w-full rounded-2xl bg-gray-200 dark:bg-slate-700" />

        {/* Card Informações Skeleton */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-700 p-6 flex flex-col gap-4">
          <div className="flex items-center gap-4">
            <div className="h-20 w-20 rounded-full bg-gray-300 dark:bg-slate-700 shrink-0" />
            <div className="flex-1 space-y-2">
              <div className="h-5 w-48 bg-gray-300 dark:bg-slate-700 rounded" />
              <div className="h-4 w-32 bg-gray-200 dark:bg-slate-800 rounded" />
            </div>
          </div>
          <div className="h-16 w-full bg-gray-100 dark:bg-slate-800 rounded-xl" />
        </div>
      </div>
    );
  }

  // ── Render: Erro ou Não Encontrado ──────────────────────────────────────────
  if (erro || !usuario) {
    return (
      <div className="mx-auto max-w-2xl py-16 flex flex-col items-center justify-center text-center gap-4">
        <div className="h-16 w-16 rounded-full bg-red-50 dark:bg-red-950/40 text-red-500 dark:text-red-400 flex items-center justify-center text-2xl">
          ⚠️
        </div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">Perfil não encontrado</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 max-w-md">
          {erro ?? 'O usuário solicitado não existe ou você não possui permissão para visualizá-lo.'}
        </p>
        <button
          onClick={() => navigate('/pessoas')}
          className="mt-2 text-sm font-semibold text-violet-600 dark:text-violet-400 hover:text-violet-800 dark:hover:text-violet-300 transition-colors"
        >
          ← Voltar para Explorar Pessoas
        </button>
      </div>
    );
  }

  const habilidades = (usuario.habilidades ?? []) as HabilidadeDoUsuario[];
  const nomeCurso = usuario.curso?.nome ?? (typeof usuario.curso === 'string' ? usuario.curso : 'Estudante');
  const cursoEPeriodo = usuario.periodo != null ? `${nomeCurso} · ${usuario.periodo}º período` : nomeCurso;

  // ── Render Principal ────────────────────────────────────────────────────────
  return (
    <div className="mx-auto max-w-4xl flex flex-col gap-6 pb-12">

      {/* Botão de Voltar */}
      <div>
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-100 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none"
            viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
          Voltar
        </button>
      </div>

      {/* Card Principal de Perfil com Banner */}
      <Card className="overflow-hidden p-0 border border-gray-100 dark:border-slate-700 shadow-sm">
        {/* Banner Decorativo no Topo */}
        <div className="h-36 sm:h-44 w-full bg-linear-to-r from-gray-100 via-gray-200 to-gray-300 dark:from-slate-800 dark:via-slate-700 dark:to-slate-600 relative" />

        {/* Informações do Usuário com Avatar Sobreposto */}
        <div className="px-6 sm:px-8 pb-6 pt-0">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 -mt-12 sm:-mt-14 mb-4">

            {/* Avatar */}
            <div className="relative">
              {usuario.fotoUrl ? (
                <img
                  src={usuario.fotoUrl}
                  alt={usuario.nome}
                  className="h-24 w-24 sm:h-28 sm:w-28 rounded-2xl object-cover ring-4 ring-white dark:ring-slate-900 shadow-md bg-white dark:bg-slate-900"
                />
              ) : (
                <div
                  className={`h-24 w-24 sm:h-28 sm:w-28 rounded-2xl ring-4 ring-white dark:ring-slate-900 shadow-md flex items-center justify-center text-2xl font-bold ${obterCorAvatar(
                    usuario.nome
                  )}`}
                >
                  {formatarIniciais(usuario.nome)}
                </div>
              )}
            </div>

            {/* Ações: Editar Perfil e Redes Sociais (visíveis apenas para o dono do perfil) */}
            {isDono && (
              <div className="flex items-center gap-2">
                {/* Botão Editar Perfil */}
                <button
                  onClick={() => navigate(`/usuarios/${id}/editar`)}
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold text-violet-600 dark:text-violet-300 bg-violet-50 dark:bg-violet-900/40 hover:bg-violet-100 dark:hover:bg-violet-900/60 border border-violet-100 dark:border-violet-800 transition-colors shadow-2xs"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-violet-600" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                  </svg>
                  Editar perfil
                </button>

                {/* Ícone LinkedIn */}
                {usuario.linkedinUrl && (
                  <a
                    href={usuario.linkedinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="LinkedIn"
                    title="LinkedIn"
                    className="p-1.5 rounded-xl text-violet-600 dark:text-violet-300 hover:text-violet-800 dark:hover:text-violet-200 hover:bg-violet-50 dark:hover:bg-violet-900/40 border border-violet-100 dark:border-violet-800 transition-colors flex items-center justify-center"
                  >
                    <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24">
                      <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.46 10.9v8.37H9.2V10.9H6.46M7.83 6.2a1.66 1.66 0 0 0-1.66 1.66c0 .92.74 1.66 1.66 1.66.92 0 1.66-.74 1.66-1.66 0-.92-.74-1.66-1.66-1.66Z" />
                    </svg>
                  </a>
                )}

                {/* Ícone GitHub */}
                {usuario.githubUrl && (
                  <a
                    href={usuario.githubUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="GitHub"
                    title="GitHub"
                    className="p-1.5 rounded-xl text-violet-600 dark:text-violet-300 hover:text-violet-800 dark:hover:text-violet-200 hover:bg-violet-50 dark:hover:bg-violet-900/40 border border-violet-100 dark:border-violet-800 transition-colors flex items-center justify-center"
                  >
                    <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24">
                      <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2Z" />
                    </svg>
                  </a>
                )}
              </div>
            )}
          </div>

          {/* Nome, Curso, Avaliação e Bio */}
          <div className="space-y-2">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight">
                {usuario.nome}
              </h1>
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1 mt-0.5">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  {cursoEPeriodo}
                </p>

                {/* Nota média das avaliações recebidas de colegas de projeto */}
                {usuario.notaMedia != null && usuario.totalAvaliacoes != null && (
                  <span className="inline-flex items-center gap-1 text-sm">
                    <span className="text-gray-300 dark:text-gray-600">·</span>
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-amber-400" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.958a1 1 0 00.95.69h4.162c.969 0 1.371 1.24.588 1.81l-3.368 2.448a1 1 0 00-.364 1.118l1.287 3.957c.299.922-.756 1.688-1.54 1.118l-3.367-2.447a1 1 0 00-1.176 0l-3.368 2.447c-.783.57-1.838-.196-1.539-1.118l1.286-3.957a1 1 0 00-.363-1.118L2.98 9.385c-.783-.57-.38-1.81.588-1.81h4.162a1 1 0 00.951-.69l1.286-3.958z" />
                    </svg>
                    <span className="font-semibold text-gray-700 dark:text-gray-200">{usuario.notaMedia.toFixed(1)}</span>
                    <span className="text-xs text-gray-400 dark:text-gray-500">
                      ({usuario.totalAvaliacoes} {usuario.totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'})
                    </span>
                  </span>
                )}
              </div>
            </div>

            {usuario.bio ? (
              <p className="text-sm text-gray-600 dark:text-gray-300 leading-relaxed pt-1 max-w-2xl">
                {usuario.bio}
              </p>
            ) : (
              <p className="text-sm text-gray-400 dark:text-gray-500 italic pt-1">
                Nenhuma biografia informada.
              </p>
            )}
          </div>
        </div>
      </Card>

      {/* Seção: Habilidades */}
      <Card className="p-6">
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-4">Habilidades</h2>

        {habilidades.length > 0 ? (
          <div className="flex flex-col gap-3">
            {habilidades.map((h) => {
              const estiloPill = obterEstiloHabilidade(h.habilidade.nome);

              return (
                <div
                  key={h.id}
                  className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-gray-50 dark:border-slate-700 last:border-0"
                >
                  <div className="flex flex-wrap items-center gap-2.5">
                    {/* Badge da Habilidade com cor temática do Figma */}
                    <span
                      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold border ${estiloPill}`}
                    >
                      {h.habilidade.nome}
                    </span>
                  </div>

                  {/* Nível da Habilidade */}
                  {h.nivel && (
                    <span className="text-[11px] text-gray-500 dark:text-gray-400 font-medium px-2 py-0.5 bg-gray-100 dark:bg-slate-800 rounded-md">
                      {formatarNivel(h.nivel)}
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-sm text-gray-400 dark:text-gray-500">Nenhuma habilidade cadastrada.</p>
        )}
      </Card>

      {/* Seção: Avaliações recebidas (feitas por colegas de projeto) */}
      <Card className="p-6">
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-4">
          Avaliações {avaliacoes.length > 0 ? `(${avaliacoes.length})` : ''}
        </h2>

        {avaliacoes.length > 0 ? (
          <div className="flex flex-col gap-4">
            {avaliacoes.map((av) => {
              const jaDenunciada = avaliacoesDenunciadas.has(av.id);
              const denunciandoEsta = denunciandoId === av.id;

              return (
                <div key={av.id} className="flex flex-col gap-2 py-3 border-b border-gray-50 dark:border-slate-700 last:border-0">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${obterCorAvatar(av.avaliador.nome)}`}
                      >
                        {iniciaisDoNome(av.avaliador.nome)}
                      </span>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-gray-800 dark:text-gray-100 truncate">{av.avaliador.nome}</p>
                        <p className="text-xs text-gray-400 dark:text-gray-500 truncate">
                          {av.projeto.titulo} · {formatarData(av.criadoEm)}
                        </p>
                      </div>
                    </div>
                    <Estrelas nota={av.nota} />
                  </div>

                  {av.comentario && (
                    <p className="text-sm text-gray-600 dark:text-gray-300 leading-relaxed pl-10">{av.comentario}</p>
                  )}

                  {/* Denunciar — só o próprio avaliado pode denunciar uma avaliação sobre si */}
                  {isDono && (
                    jaDenunciada ? (
                      <p className="text-xs text-gray-400 dark:text-gray-500 pl-10">Denúncia enviada — em análise.</p>
                    ) : denunciandoEsta ? (
                      <div className="pl-10 flex flex-col gap-2">
                        {erroDenuncia && (
                          <p role="alert" className="text-xs text-red-500 dark:text-red-400">{erroDenuncia}</p>
                        )}
                        <textarea
                          value={motivoDenuncia}
                          onChange={(e) => setMotivoDenuncia(e.target.value)}
                          placeholder="Explique por que essa avaliação é abusiva ou indevida..."
                          rows={2}
                          className="w-full rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 px-3 py-2 text-xs text-gray-800 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 outline-none focus:bg-white dark:focus:bg-slate-800 focus:ring-2 focus:ring-red-400 focus:border-red-300 transition-colors resize-none"
                        />
                        <div className="flex gap-2 justify-end">
                          <button
                            type="button"
                            onClick={() => setDenunciandoId(null)}
                            disabled={enviandoDenuncia}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
                          >
                            Cancelar
                          </button>
                          <button
                            type="button"
                            onClick={handleEnviarDenuncia}
                            disabled={enviandoDenuncia}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold text-white bg-red-600 hover:bg-red-700 transition-colors disabled:opacity-50"
                          >
                            {enviandoDenuncia ? 'Enviando…' : 'Enviar denúncia'}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleAbrirDenuncia(av.id)}
                        className="self-start pl-10 text-xs font-medium text-gray-400 dark:text-gray-500 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                      >
                        Denunciar
                      </button>
                    )
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-sm text-gray-400 dark:text-gray-500">Nenhuma avaliação recebida ainda.</p>
        )}
      </Card>

      {/* Seção: Projetos */}
      <div className="space-y-3">
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">
          Projetos {projetos.length > 0 ? `(${projetos.length})` : ''}
        </h2>

        {projetos.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {projetos.map((proj) => (
              <Card
                key={proj.id}
                onClick={() => navigate(`/detalhes/${proj.id}`)}
                className="hover:border-violet-200 dark:hover:border-violet-800 transition-colors"
              >
                <div className="flex flex-col h-full justify-between gap-3">
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-1.5">
                      <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 line-clamp-1">
                        {proj.titulo}
                      </h3>
                      <Badge variant="status" status={proj.status}>{proj.status}</Badge>
                    </div>
                    <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2">
                      {proj.descricao}
                    </p>
                  </div>

                  <div className="text-xs font-semibold text-violet-600 dark:text-violet-400 hover:text-violet-800 dark:hover:text-violet-300 transition-colors mt-auto pt-2 flex items-center gap-1">
                    Ver detalhes
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" clipRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" />
                    </svg>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <Card className="p-8 text-center text-sm text-gray-400 dark:text-gray-500">
            Nenhum projeto encontrado para este usuário.
          </Card>
        )}
      </div>

    </div>
  );
}
