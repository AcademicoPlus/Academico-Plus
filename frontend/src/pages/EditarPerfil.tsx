// Página de edição do próprio perfil (/usuarios/:id/editar).
// Só acessível pelo dono da conta: compara o :id da URL com o id retornado por GET /usuarios/me.
// Permite alterar foto de perfil (JPG/PNG até 2MB), nome, curso (via listarCursos), bio,
// links externos (LinkedIn, GitHub) e gerenciar habilidades.
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { apiFetch, ApiError, type PaginaResposta } from '../services/apiClient';
import {
  atualizarMeuPerfil,
  alterarMinhaSenha,
  excluirMinhaConta,
  enviarFotoDePerfil,
  type UsuarioPerfil,
} from '../services/usuarioService';
import { removerToken } from '../utils/auth';
import {
  obterMeuPerfilCache,
  atualizarCacheMeuPerfil,
  atualizarHabilidadesNoCacheMeuPerfil,
} from '../hooks/useMeuPerfil';
import { listarCursos, type Curso } from '../services/cursoService';
import {
  adicionarHabilidadeAoPerfil,
  atualizarNivelHabilidade,
  removerHabilidadeDoPerfil,
  type NivelHabilidade,
} from '../services/usuarioHabilidadeService';

import Input from '../components/Input';

// ─── Tipos ────────────────────────────────────────────────────────────────────

type HabilidadeNoPerfil = {
  id: string;
  habilidade: { id: string; nome: string };
  nivel: NivelHabilidade;
};

type HabilidadeResumo = {
  id: string;
  nome: string;
};

const NIVEIS_HABILIDADE: { valor: NivelHabilidade; label: string }[] = [
  { valor: 'INICIANTE', label: 'Iniciante' },
  { valor: 'INTERMEDIARIO', label: 'Intermediário' },
  { valor: 'AVANCADO', label: 'Avançado' },
  { valor: 'EXPERT', label: 'Expert' },
];

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

// ─── Componente Principal ─────────────────────────────────────────────────────

export default function EditarPerfil() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // ── Estados de controle ────────────────────────────────────────────────────
  const [verificando, setVerificando] = useState(true);
  const [perfil, setPerfil] = useState<UsuarioPerfil | null>(null);

  // ── Foto de perfil ─────────────────────────────────────────────────────────
  const [fotoUrl, setFotoUrl] = useState<string | null>(null);
  const [enviandoFoto, setEnviandoFoto] = useState(false);
  const [erroFoto, setErroFoto] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── Cursos e Campos do formulário ──────────────────────────────────────────
  const [cursos, setCursos] = useState<Curso[]>([]);
  const [nome, setNome] = useState('');
  const [idCurso, setIdCurso] = useState('');
  const [periodo, setPeriodo] = useState('');
  const [bio, setBio] = useState('');
  const [linkedin, setLinkedin] = useState('');
  const [github, setGithub] = useState('');
  const [erroNome, setErroNome] = useState('');

  // ── Habilidades ────────────────────────────────────────────────────────────
  const [habilidades, setHabilidades] = useState<HabilidadeNoPerfil[]>([]);
  const [buscaHabilidade, setBuscaHabilidade] = useState('');
  const [nivelParaAdicionar, setNivelParaAdicionar] = useState<NivelHabilidade>('INICIANTE');
  const [sugestoes, setSugestoes] = useState<HabilidadeResumo[]>([]);
  const [carregandoSugestoes, setCarregandoSugestoes] = useState(false);
  const [removendo, setRemovendo] = useState<string | null>(null);
  const [adicionando, setAdicionando] = useState(false);
  const [atualizandoNivel, setAtualizandoNivel] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // ── Estados de submissão ───────────────────────────────────────────────────
  const [salvando, setSalvando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);

  // ── Segurança: trocar senha ────────────────────────────────────────────────
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarNovaSenha, setConfirmarNovaSenha] = useState('');
  const [alterandoSenha, setAlterandoSenha] = useState(false);
  const [erroSenha, setErroSenha] = useState<string | null>(null);
  const [sucessoSenha, setSucessoSenha] = useState(false);

  // ── Zona de risco: excluir conta ───────────────────────────────────────────
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false);
  const [senhaExclusao, setSenhaExclusao] = useState('');
  const [excluindoConta, setExcluindoConta] = useState(false);
  const [erroExclusao, setErroExclusao] = useState<string | null>(null);

  // ── 1. Carrega cursos disponíveis ─────────────────────────────────────────
  useEffect(() => {
    listarCursos()
      .then(setCursos)
      .catch(() => {/* silencia — select ficará vazio */ });
  }, []);

  // ── 2. Verificar se o usuário logado é o dono do perfil ───────────────────
  useEffect(() => {
    obterMeuPerfilCache()
      .then((meuPerfil) => {
        if (meuPerfil.id !== id) {
          navigate(`/usuarios/${id}`, { replace: true });
          return;
        }
        setPerfil(meuPerfil);
        setNome(meuPerfil.nome ?? '');
        setIdCurso(meuPerfil.curso?.id ?? '');
        setPeriodo(meuPerfil.periodo != null ? String(meuPerfil.periodo) : '');
        setFotoUrl(meuPerfil.fotoUrl ?? null);
        setBio(meuPerfil.bio ?? '');
        setLinkedin(meuPerfil.linkedinUrl ?? '');
        setGithub(meuPerfil.githubUrl ?? '');
        setHabilidades((meuPerfil.habilidades ?? []) as HabilidadeNoPerfil[]);
      })
      .catch(() => navigate('/login', { replace: true }))
      .finally(() => setVerificando(false));
  }, [id, navigate]);

  // ── 3. Busca sugestões do catálogo (debounced 350 ms) ─────────────────────
  useEffect(() => {
    if (buscaHabilidade.trim().length < 2) {
      return;
    }
    const t = setTimeout(() => {
      setCarregandoSugestoes(true);
      const idsAtuais = new Set(habilidades.map((h) => h.habilidade.id));
      apiFetch<PaginaResposta<HabilidadeResumo>>(
        `/habilidades?busca=${encodeURIComponent(buscaHabilidade)}`,
      )
        .then((res) => setSugestoes((res.content ?? []).filter((h) => !idsAtuais.has(h.id))))
        .catch(() => setSugestoes([]))
        .finally(() => setCarregandoSugestoes(false));
    }, 350);
    return () => clearTimeout(t);
  }, [buscaHabilidade, habilidades]);

  // Fecha o dropdown ao clicar fora
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setSugestoes([]);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // ── Upload de foto de perfil ───────────────────────────────────────────────
  async function handleFotoChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setErroFoto(null);

    // Validação de tipo (JPG/PNG)
    const formatosPermitidos = ['image/jpeg', 'image/png', 'image/jpg'];
    if (!formatosPermitidos.includes(file.type)) {
      setErroFoto('Selecione uma imagem válida (JPG ou PNG).');
      return;
    }

    // Validação de tamanho (máximo 2MB = 2 * 1024 * 1024 bytes)
    const limiteBytes = 2 * 1024 * 1024;
    if (file.size > limiteBytes) {
      setErroFoto('A imagem deve ter no máximo 2MB.');
      return;
    }

    setEnviandoFoto(true);
    try {
      const atualizado = await enviarFotoDePerfil(file);
      setFotoUrl(atualizado.fotoUrl ?? URL.createObjectURL(file));
      setPerfil(atualizado);
      atualizarCacheMeuPerfil(atualizado);
    } catch (err) {
      setErroFoto(
        err instanceof ApiError ? err.message : 'Falha ao enviar foto de perfil.',
      );
    } finally {
      setEnviandoFoto(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }

  // ── Handlers de habilidade ─────────────────────────────────────────────────

  function handleBuscaHabilidadeChange(valor: string) {
    setBuscaHabilidade(valor);
    if (valor.trim().length < 2) {
      setSugestoes([]);
    }
  }

  async function handleAdicionar(h: HabilidadeResumo) {
    setAdicionando(true);
    setErroGeral(null);
    try {
      const nova = await adicionarHabilidadeAoPerfil(h.id, nivelParaAdicionar);
      const novasHabilidades = [...habilidades, nova as HabilidadeNoPerfil];
      setHabilidades(novasHabilidades);
      setBuscaHabilidade('');
      setNivelParaAdicionar('INICIANTE');
      setSugestoes([]);
      atualizarHabilidadesNoCacheMeuPerfil(novasHabilidades as unknown as UsuarioPerfil['habilidades']);
    } catch (e) {
      setErroGeral(e instanceof ApiError ? e.message : 'Erro ao adicionar habilidade.');
    } finally {
      setAdicionando(false);
    }
  }

  async function handleAlterarNivel(item: HabilidadeNoPerfil, nivel: NivelHabilidade) {
    setAtualizandoNivel(item.id);
    setErroGeral(null);
    try {
      // A rota espera o id da habilidade no catálogo (item.habilidade.id), não
      // o id do vínculo (item.id) — ver UsuarioHabilidadeService.obterVinculo,
      // que busca por usuario_id + habilidade_id.
      const atualizada = await atualizarNivelHabilidade(item.habilidade.id, nivel);
      const novasHabilidades = habilidades.map((h) =>
        h.id === item.id ? { ...h, nivel: atualizada.nivel } : h,
      );
      setHabilidades(novasHabilidades);
      atualizarHabilidadesNoCacheMeuPerfil(novasHabilidades as unknown as UsuarioPerfil['habilidades']);
    } catch (e) {
      setErroGeral(e instanceof ApiError ? e.message : 'Erro ao atualizar nível da habilidade.');
    } finally {
      setAtualizandoNivel(null);
    }
  }

  async function handleRemover(item: HabilidadeNoPerfil) {
    setRemovendo(item.id);
    setErroGeral(null);
    try {
      await removerHabilidadeDoPerfil(item.habilidade.id);
      const novasHabilidades = habilidades.filter((h) => h.id !== item.id);
      setHabilidades(novasHabilidades);
      atualizarHabilidadesNoCacheMeuPerfil(novasHabilidades as unknown as UsuarioPerfil['habilidades']);
    } catch (e) {
      setErroGeral(e instanceof ApiError ? e.message : 'Erro ao remover habilidade.');
    } finally {
      setRemovendo(null);
    }
  }

  // ── Handler de salvar ─────────────────────────────────────────────────────

  async function handleSalvar() {
    setErroNome('');
    setErroGeral(null);

    if (!nome.trim()) {
      setErroNome('O nome não pode ficar em branco.');
      return;
    }

    setSalvando(true);
    setSucesso(false);
    try {
      const perfilAtualizado = await atualizarMeuPerfil({
        nome: nome.trim(),
        idCurso: idCurso || undefined,
        periodo: periodo ? Number(periodo) : undefined,
        bio: bio.trim() || undefined,
        linkedinUrl: linkedin.trim() || undefined,
        githubUrl: github.trim() || undefined,
      });
      atualizarCacheMeuPerfil(perfilAtualizado);
      setSucesso(true);
      setTimeout(() => navigate(`/usuarios/${id}`), 1200);
    } catch (e) {
      setErroGeral(
        e instanceof ApiError ? e.message : 'Não foi possível salvar as alterações.',
      );
    } finally {
      setSalvando(false);
    }
  }

  // ── Handler de trocar senha ───────────────────────────────────────────────

  async function handleAlterarSenha() {
    setErroSenha(null);
    setSucessoSenha(false);

    if (!senhaAtual || !novaSenha) {
      setErroSenha('Preencha a senha atual e a nova senha.');
      return;
    }
    if (novaSenha.length < 8) {
      setErroSenha('A nova senha deve ter no mínimo 8 caracteres.');
      return;
    }
    if (novaSenha !== confirmarNovaSenha) {
      setErroSenha('A confirmação não bate com a nova senha.');
      return;
    }

    setAlterandoSenha(true);
    try {
      await alterarMinhaSenha(senhaAtual, novaSenha);
      setSucessoSenha(true);
      setSenhaAtual('');
      setNovaSenha('');
      setConfirmarNovaSenha('');
    } catch (e) {
      setErroSenha(e instanceof ApiError ? e.message : 'Não foi possível alterar a senha.');
    } finally {
      setAlterandoSenha(false);
    }
  }

  // ── Handler de excluir conta ──────────────────────────────────────────────

  async function handleExcluirConta() {
    setErroExclusao(null);

    if (!senhaExclusao) {
      setErroExclusao('Digite sua senha para confirmar.');
      return;
    }

    setExcluindoConta(true);
    try {
      await excluirMinhaConta(senhaExclusao);
      removerToken();
      navigate('/login', { replace: true });
    } catch (e) {
      setErroExclusao(e instanceof ApiError ? e.message : 'Não foi possível excluir a conta.');
    } finally {
      setExcluindoConta(false);
    }
  }

  // ── Render: carregando ────────────────────────────────────────────────────

  if (verificando) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-violet-600 dark:border-violet-400 border-t-transparent" />
      </div>
    );
  }

  if (!perfil) return null;

  // ── Render: formulário ────────────────────────────────────────────────────

  return (
    <div className="mx-auto max-w-2xl flex flex-col gap-6 py-2 pb-12">

      {/* Cabeçalho com link de cancelar/voltar */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(`/usuarios/${id}`)}
          aria-label="Cancelar e voltar ao perfil"
          className="flex items-center gap-1.5 text-sm font-medium text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-100 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none"
            viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
          Cancelar
        </button>
      </div>

      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight">Editar perfil</h1>
      </div>

      {/* Feedback global */}
      {erroGeral && (
        <p role="alert"
          className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:bg-red-950/40 dark:border-red-900/50 dark:text-red-400">
          {erroGeral}
        </p>
      )}
      {sucesso && (
        <p role="status"
          className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 flex items-center gap-2 dark:bg-emerald-950/40 dark:border-emerald-900/50 dark:text-emerald-400">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 shrink-0" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" clipRule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
          </svg>
          Perfil salvo com sucesso! Redirecionando…
        </p>
      )}

      {/* ── Bloco Principal de Edição ──────────────────────────────────────── */}
      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-700 shadow-sm p-6 sm:p-8 flex flex-col gap-6">

        {/* Upload de Foto de Perfil */}
        <div className="flex items-center gap-5 pb-2">
          {fotoUrl ? (
            <img
              src={fotoUrl}
              alt={nome}
              className="h-16 w-16 sm:h-20 sm:w-20 rounded-2xl object-cover ring-2 ring-gray-100 dark:ring-slate-700 shadow-sm shrink-0"
            />
          ) : (
            <div
              className={`h-16 w-16 sm:h-20 sm:w-20 rounded-2xl flex items-center justify-center text-xl font-bold shrink-0 ${obterCorAvatar(
                nome
              )}`}
            >
              {formatarIniciais(nome)}
            </div>
          )}

          <div className="flex flex-col gap-1">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFotoChange}
              accept="image/jpeg,image/png,image/jpg"
              className="hidden"
              id="foto-perfil-input"
            />
            <label
              htmlFor="foto-perfil-input"
              className={`inline-flex items-center justify-center px-3.5 py-1.5 rounded-xl text-xs font-semibold text-violet-700 dark:text-violet-300 bg-violet-50 dark:bg-violet-900/40 hover:bg-violet-100 dark:hover:bg-violet-900/60 cursor-pointer transition-colors w-fit border border-violet-100 dark:border-violet-800 ${enviandoFoto ? 'opacity-50 pointer-events-none' : ''
                }`}
            >
              {enviandoFoto ? 'Enviando...' : 'Alterar foto'}
            </label>
            <span className="text-xs text-gray-400 dark:text-gray-500">JPG ou PNG. Máx 2MB</span>
            {erroFoto && (
              <p className="text-xs text-red-500 dark:text-red-400 mt-0.5">{erroFoto}</p>
            )}
          </div>
        </div>

        {/* Nome Completo */}
        <Input
          label="Nome completo"
          type="text"
          value={nome}
          onChange={(v) => { setNome(v); setErroNome(''); }}
          placeholder="Seu nome completo"
          required
          error={erroNome}
        />

        {/* Select de Curso */}
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-200 leading-none">
            Curso
          </label>
          <select
            value={idCurso}
            onChange={(e) => setIdCurso(e.target.value)}
            className="w-full rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 px-4 py-2.5 text-sm text-gray-800 dark:text-gray-100 outline-none focus:bg-white dark:focus:bg-slate-800 focus:ring-2 focus:ring-violet-500 focus:border-violet-400 transition-colors duration-150"
          >
            <option value="">Selecione seu curso</option>
            {cursos.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </select>
        </div>

        {/* Select de Período */}
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-200 leading-none">
            Período
          </label>
          <select
            value={periodo}
            onChange={(e) => setPeriodo(e.target.value)}
            className="w-full rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 px-4 py-2.5 text-sm text-gray-800 dark:text-gray-100 outline-none focus:bg-white dark:focus:bg-slate-800 focus:ring-2 focus:ring-violet-500 focus:border-violet-400 transition-colors duration-150"
          >
            <option value="">Não informado</option>
            {Array.from({ length: 12 }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}º período
              </option>
            ))}
          </select>
        </div>

        {/* Bio */}
        <Input
          label="Bio"
          type="textarea"
          value={bio}
          onChange={setBio}
          placeholder="Designer apaixonada por UI/UX e design systems. Busco projetos que unam criatividade e impacto social..."
        />

        {/* Links: LinkedIn e GitHub */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="LinkedIn"
            type="url"
            value={linkedin}
            onChange={setLinkedin}
            placeholder="https://linkedin.com"
          />

          <Input
            label="GitHub"
            type="url"
            value={github}
            onChange={setGithub}
            placeholder="https://github.com"
          />
        </div>

        {/* Habilidades */}
        <div className="flex flex-col gap-3 pt-2">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-200 leading-none">
            Habilidades
          </label>

          {/* Campo de busca + seletor de nível para a próxima habilidade a adicionar */}
          <div className="relative" ref={dropdownRef}>
            <div className="flex items-center gap-2">
              <div className="relative flex items-center flex-1">
                <input
                  type="text"
                  value={buscaHabilidade}
                  onChange={(e) => handleBuscaHabilidadeChange(e.target.value)}
                  placeholder="Adicionar habilidade..."
                  className="w-full rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 px-4 py-2.5 pr-10
                             text-sm text-gray-800 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 outline-none
                             focus:bg-white dark:focus:bg-slate-800 focus:border-violet-400 focus:ring-2 focus:ring-violet-500
                             transition-colors duration-150"
                />
                <span className="absolute right-3 text-violet-600 font-bold text-lg pointer-events-none select-none">
                  +
                </span>
              </div>

              <select
                value={nivelParaAdicionar}
                onChange={(e) => setNivelParaAdicionar(e.target.value as NivelHabilidade)}
                title="Nível de experiência da habilidade a adicionar"
                className="rounded-xl border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 px-3 py-2.5 text-sm text-gray-700 dark:text-gray-200
                           outline-none focus:bg-white dark:focus:bg-slate-800 focus:border-violet-400 focus:ring-2 focus:ring-violet-500
                           transition-colors duration-150 shrink-0"
              >
                {NIVEIS_HABILIDADE.map((n) => (
                  <option key={n.valor} value={n.valor}>{n.label}</option>
                ))}
              </select>
            </div>

            {/* Dropdown de sugestões */}
            {(sugestoes.length > 0 || carregandoSugestoes) && (
              <div className="absolute z-20 left-0 right-0 mt-1.5 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700
                              rounded-xl shadow-lg overflow-hidden divide-y divide-gray-50 dark:divide-slate-700">
                {carregandoSugestoes ? (
                  <div className="flex items-center gap-2 px-4 py-3 text-sm text-gray-400 dark:text-gray-500">
                    <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-gray-300 dark:border-slate-600 border-t-violet-500" />
                    Buscando…
                  </div>
                ) : (
                  sugestoes.slice(0, 8).map((h) => (
                    <button
                      key={h.id}
                      type="button"
                      onMouseDown={(e) => { e.preventDefault(); handleAdicionar(h); }}
                      disabled={adicionando}
                      className="w-full text-left px-4 py-2.5 text-sm text-gray-700 dark:text-gray-200
                                 hover:bg-violet-50 dark:hover:bg-violet-900/40 hover:text-violet-700 dark:hover:text-violet-300 flex items-center gap-2
                                 transition-colors disabled:opacity-50"
                    >
                      <span className="text-violet-500 font-bold">+</span>
                      {h.nome}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>

          {/* Badges das habilidades atuais, com seletor de nível inline */}
          {habilidades.length > 0 ? (
            <div className="flex flex-wrap gap-2 pt-1">
              {habilidades.map((h) => {
                const estaRemovendo = removendo === h.id;
                const estaAtualizandoNivel = atualizandoNivel === h.id;

                return (
                  <span
                    key={h.id}
                    className="inline-flex items-center gap-1.5 rounded-full pl-3 pr-1 py-1 text-xs font-medium bg-indigo-50 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 border border-indigo-100 dark:border-indigo-800"
                  >
                    <span>{h.habilidade.nome}</span>

                    <select
                      value={h.nivel}
                      onChange={(e) => handleAlterarNivel(h, e.target.value as NivelHabilidade)}
                      disabled={estaAtualizandoNivel || estaRemovendo}
                      aria-label={`Nível de experiência em ${h.habilidade.nome}`}
                      className="rounded-full bg-white dark:bg-slate-900 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 text-[11px]
                                 font-medium pl-1.5 pr-1 py-0.5 outline-none focus:ring-1 focus:ring-violet-500
                                 disabled:opacity-50"
                    >
                      {NIVEIS_HABILIDADE.map((n) => (
                        <option key={n.valor} value={n.valor}>{n.label}</option>
                      ))}
                    </select>

                    <button
                      type="button"
                      onClick={() => handleRemover(h)}
                      disabled={estaRemovendo || adicionando}
                      aria-label={`Remover ${h.habilidade.nome}`}
                      className="ml-0.5 rounded-full p-0.5 text-indigo-400 dark:text-indigo-300 hover:text-indigo-700 dark:hover:text-indigo-200 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 transition-colors disabled:opacity-40"
                    >
                      {estaRemovendo ? (
                        <svg className="h-3 w-3 animate-spin" viewBox="0 0 24 24" fill="none">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                        </svg>
                      ) : (
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" clipRule="evenodd"
                            d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" />
                        </svg>
                      )}
                    </button>
                  </span>
                );
              })}
            </div>
          ) : (
            <p className="text-xs text-gray-400 dark:text-gray-500">Nenhuma habilidade adicionada ainda.</p>
          )}
        </div>

      </section>

      {/* ── Botões de ação no rodapé ───────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={() => navigate(`/usuarios/${id}`)}
          className="w-full sm:w-auto px-6 py-2.5 rounded-xl text-sm font-semibold text-gray-700 dark:text-gray-200 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 transition-colors"
        >
          Cancelar
        </button>

        <button
          type="button"
          onClick={handleSalvar}
          disabled={salvando}
          className="w-full sm:w-auto px-8 py-2.5 rounded-xl text-sm font-semibold text-white bg-violet-600 hover:bg-violet-700 shadow-sm shadow-violet-200 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
        >
          {salvando ? (
            'Salvando…'
          ) : (
            <>
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" clipRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
              </svg>
              Salvar alterações
            </>
          )}
        </button>
      </div>

      {/* ── Segurança: trocar senha ────────────────────────────────────────── */}
      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-gray-100 dark:border-slate-700 shadow-sm p-6 sm:p-8 flex flex-col gap-4">
        <div>
          <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">Segurança</h2>
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">Altere a senha usada para entrar na sua conta.</p>
        </div>

        {erroSenha && (
          <p role="alert" className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:bg-red-950/40 dark:border-red-900/50 dark:text-red-400">
            {erroSenha}
          </p>
        )}
        {sucessoSenha && (
          <p role="status" className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:bg-emerald-950/40 dark:border-emerald-900/50 dark:text-emerald-400">
            Senha alterada com sucesso.
          </p>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Senha atual"
            type="password"
            value={senhaAtual}
            onChange={setSenhaAtual}
            placeholder="••••••••"
          />
          <div />
          <Input
            label="Nova senha"
            type="password"
            value={novaSenha}
            onChange={setNovaSenha}
            placeholder="Mínimo 8 caracteres"
          />
          <Input
            label="Confirmar nova senha"
            type="password"
            value={confirmarNovaSenha}
            onChange={setConfirmarNovaSenha}
            placeholder="Repita a nova senha"
          />
        </div>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleAlterarSenha}
            disabled={alterandoSenha}
            className="px-6 py-2.5 rounded-xl text-sm font-semibold text-gray-700 dark:text-gray-200 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
          >
            {alterandoSenha ? 'Alterando…' : 'Alterar senha'}
          </button>
        </div>
      </section>

      {/* ── Zona de risco: excluir conta ───────────────────────────────────── */}
      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-red-100 dark:border-red-900/50 shadow-sm p-6 sm:p-8 flex flex-col gap-4">
        <div>
          <h2 className="text-base font-bold text-red-700 dark:text-red-400">Excluir conta</h2>
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
            Essa ação desativa sua conta permanentemente. Não é possível desfazer.
          </p>
        </div>

        {erroExclusao && (
          <p role="alert" className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:bg-red-950/40 dark:border-red-900/50 dark:text-red-400">
            {erroExclusao}
          </p>
        )}

        {!confirmandoExclusao ? (
          <div className="flex justify-end">
            <button
              type="button"
              onClick={() => setConfirmandoExclusao(true)}
              className="px-6 py-2.5 rounded-xl text-sm font-semibold text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 border border-red-100 dark:border-red-900/50 hover:bg-red-100 dark:hover:bg-red-900/60 transition-colors"
            >
              Excluir minha conta
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-4 rounded-xl border border-red-100 dark:border-red-900/50 bg-red-50/40 dark:bg-red-950/30 p-4">
            <p className="text-sm text-red-700 dark:text-red-400 font-medium">
              Tem certeza? Digite sua senha para confirmar a exclusão da sua conta.
            </p>
            <Input
              label="Senha"
              type="password"
              value={senhaExclusao}
              onChange={setSenhaExclusao}
              placeholder="••••••••"
            />
            <div className="flex flex-col sm:flex-row justify-end gap-3">
              <button
                type="button"
                onClick={() => { setConfirmandoExclusao(false); setSenhaExclusao(''); setErroExclusao(null); }}
                disabled={excluindoConta}
                className="px-6 py-2.5 rounded-xl text-sm font-semibold text-gray-700 dark:text-gray-200 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleExcluirConta}
                disabled={excluindoConta}
                className="px-6 py-2.5 rounded-xl text-sm font-semibold text-white bg-red-600 hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {excluindoConta ? 'Excluindo…' : 'Confirmar exclusão'}
              </button>
            </div>
          </div>
        )}
      </section>

    </div>
  );
}
