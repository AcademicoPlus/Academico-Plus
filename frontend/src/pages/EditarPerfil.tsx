// Página de edição do próprio perfil (/usuarios/:id/editar).
// Só acessível pelo dono da conta: compara o :id da URL com o id retornado por GET /usuarios/me.
// Permite alterar foto de perfil (JPG/PNG até 2MB), nome, curso (via listarCursos), bio,
// links externos (LinkedIn, GitHub) e gerenciar habilidades (com bloqueio para endossadas).
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { apiFetch, ApiError } from '../services/apiClient';
import {
  obterMeuPerfil,
  atualizarMeuPerfil,
  enviarFotoDePerfil,
  type UsuarioPerfil,
} from '../services/usuarioService';
import { listarCursos, type Curso } from '../services/cursoService';
import {
  adicionarHabilidadeAoPerfil,
  removerHabilidadeDoPerfil,
} from '../services/usuarioHabilidadeService';

import Input from '../components/Input';

// ─── Tipos ────────────────────────────────────────────────────────────────────

type HabilidadeNoPerfil = {
  id: string;
  habilidade: { id: string; nome: string };
  nivel: string;
  endossado?: boolean;
};

type HabilidadeResumo = {
  id: string;
  nome: string;
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
  'bg-indigo-100 text-indigo-700',
  'bg-violet-100 text-violet-700',
  'bg-emerald-100 text-emerald-700',
  'bg-amber-100 text-amber-700',
  'bg-rose-100 text-rose-700',
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
  const [bio, setBio] = useState('');
  const [linkedin, setLinkedin] = useState('');
  const [github, setGithub] = useState('');
  const [erroNome, setErroNome] = useState('');

  // ── Habilidades ────────────────────────────────────────────────────────────
  const [habilidades, setHabilidades] = useState<HabilidadeNoPerfil[]>([]);
  const [buscaHabilidade, setBuscaHabilidade] = useState('');
  const [sugestoes, setSugestoes] = useState<HabilidadeResumo[]>([]);
  const [carregandoSugestoes, setCarregandoSugestoes] = useState(false);
  const [removendo, setRemovendo] = useState<string | null>(null);
  const [adicionando, setAdicionando] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // ── Estados de submissão ───────────────────────────────────────────────────
  const [salvando, setSalvando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);

  // ── 1. Carrega cursos disponíveis ─────────────────────────────────────────
  useEffect(() => {
    listarCursos()
      .then(setCursos)
      .catch(() => {/* silencia — select ficará vazio */ });
  }, []);

  // ── 2. Verificar se o usuário logado é o dono do perfil ───────────────────
  useEffect(() => {
    obterMeuPerfil()
      .then((meuPerfil) => {
        if (meuPerfil.id !== id) {
          navigate(`/usuarios/${id}`, { replace: true });
          return;
        }
        setPerfil(meuPerfil);
        setNome(meuPerfil.nome ?? '');
        setIdCurso(meuPerfil.curso?.id ?? '');
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
      setSugestoes([]);
      return;
    }
    const t = setTimeout(() => {
      setCarregandoSugestoes(true);
      const idsAtuais = new Set(habilidades.map((h) => h.habilidade.id));
      apiFetch<HabilidadeResumo[]>(
        `/habilidades?busca=${encodeURIComponent(buscaHabilidade)}`,
      )
        .then((lista) => setSugestoes(lista.filter((h) => !idsAtuais.has(h.id))))
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

  async function handleAdicionar(h: HabilidadeResumo) {
    setAdicionando(true);
    setErroGeral(null);
    try {
      const nova = await adicionarHabilidadeAoPerfil(h.id);
      setHabilidades((prev) => [...prev, nova as HabilidadeNoPerfil]);
      setBuscaHabilidade('');
      setSugestoes([]);
    } catch (e) {
      setErroGeral(e instanceof ApiError ? e.message : 'Erro ao adicionar habilidade.');
    } finally {
      setAdicionando(false);
    }
  }

  async function handleRemover(habilidadeId: string) {
    setRemovendo(habilidadeId);
    setErroGeral(null);
    try {
      await removerHabilidadeDoPerfil(habilidadeId);
      setHabilidades((prev) => prev.filter((h) => h.id !== habilidadeId));
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
      await atualizarMeuPerfil({
        nome: nome.trim(),
        idCurso: idCurso || undefined,
        bio: bio.trim() || undefined,
        linkedinUrl: linkedin.trim() || undefined,
        githubUrl: github.trim() || undefined,
      });
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

  // ── Render: carregando ────────────────────────────────────────────────────

  if (verificando) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-violet-600 border-t-transparent" />
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
          className="flex items-center gap-1.5 text-sm font-medium text-gray-500 hover:text-gray-800 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none"
            viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
          Cancelar
        </button>
      </div>

      <div>
        <h1 className="text-2xl font-bold text-gray-900 leading-tight">Editar perfil</h1>
      </div>

      {/* Feedback global */}
      {erroGeral && (
        <p role="alert"
          className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600">
          {erroGeral}
        </p>
      )}
      {sucesso && (
        <p role="status"
          className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 shrink-0" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" clipRule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
          </svg>
          Perfil salvo com sucesso! Redirecionando…
        </p>
      )}

      {/* ── Bloco Principal de Edição ──────────────────────────────────────── */}
      <section className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 sm:p-8 flex flex-col gap-6">

        {/* Upload de Foto de Perfil */}
        <div className="flex items-center gap-5 pb-2">
          {fotoUrl ? (
            <img
              src={fotoUrl}
              alt={nome}
              className="h-16 w-16 sm:h-20 sm:w-20 rounded-2xl object-cover ring-2 ring-gray-100 shadow-sm shrink-0"
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
              className={`inline-flex items-center justify-center px-3.5 py-1.5 rounded-xl text-xs font-semibold text-violet-700 bg-violet-50 hover:bg-violet-100 cursor-pointer transition-colors w-fit border border-violet-100 ${enviandoFoto ? 'opacity-50 pointer-events-none' : ''
                }`}
            >
              {enviandoFoto ? 'Enviando...' : 'Alterar foto'}
            </label>
            <span className="text-xs text-gray-400">JPG ou PNG. Máx 2MB</span>
            {erroFoto && (
              <p className="text-xs text-red-500 mt-0.5">{erroFoto}</p>
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
          <label className="text-sm font-medium text-gray-700 leading-none">
            Curso
          </label>
          <select
            value={idCurso}
            onChange={(e) => setIdCurso(e.target.value)}
            className="w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-2.5 text-sm text-gray-800 outline-none focus:bg-white focus:ring-2 focus:ring-violet-500 focus:border-violet-400 transition-colors duration-150"
          >
            <option value="">Selecione seu curso</option>
            {cursos.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
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
          <label className="text-sm font-medium text-gray-700 leading-none">
            Habilidades
          </label>

          {/* Campo de busca com botão + */}
          <div className="relative" ref={dropdownRef}>
            <div className="relative flex items-center">
              <input
                type="text"
                value={buscaHabilidade}
                onChange={(e) => setBuscaHabilidade(e.target.value)}
                placeholder="Adicionar habilidade..."
                className="w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-2.5 pr-10
                           text-sm text-gray-800 placeholder:text-gray-400 outline-none
                           focus:bg-white focus:border-violet-400 focus:ring-2 focus:ring-violet-500
                           transition-colors duration-150"
              />
              <span className="absolute right-3 text-violet-600 font-bold text-lg pointer-events-none select-none">
                +
              </span>
            </div>

            {/* Dropdown de sugestões */}
            {(sugestoes.length > 0 || carregandoSugestoes) && (
              <div className="absolute z-20 left-0 right-0 mt-1.5 bg-white border border-gray-200
                              rounded-xl shadow-lg overflow-hidden divide-y divide-gray-50">
                {carregandoSugestoes ? (
                  <div className="flex items-center gap-2 px-4 py-3 text-sm text-gray-400">
                    <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-gray-300 border-t-violet-500" />
                    Buscando…
                  </div>
                ) : (
                  sugestoes.slice(0, 8).map((h) => (
                    <button
                      key={h.id}
                      type="button"
                      onMouseDown={(e) => { e.preventDefault(); handleAdicionar(h); }}
                      disabled={adicionando}
                      className="w-full text-left px-4 py-2.5 text-sm text-gray-700
                                 hover:bg-violet-50 hover:text-violet-700 flex items-center gap-2
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

          {/* Badges das habilidades atuais */}
          {habilidades.length > 0 ? (
            <div className="flex flex-wrap gap-2 pt-1">
              {habilidades.map((h) => {
                const endossada = h.endossado === true;
                const estaRemovendo = removendo === h.id;

                return (
                  <span
                    key={h.id}
                    title={endossada ? 'Habilidade endossada — não pode ser removida' : undefined}
                    className={[
                      'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium',
                      endossada
                        ? 'bg-violet-50 text-violet-700 border border-violet-100'
                        : 'bg-indigo-50 text-indigo-700 border border-indigo-100',
                    ].join(' ')}
                  >
                    <span>{h.habilidade.nome}</span>

                    {/* Botão de remoção (x) — só exibido se não endossada */}
                    {!endossada && (
                      <button
                        type="button"
                        onClick={() => handleRemover(h.id)}
                        disabled={estaRemovendo || adicionando}
                        aria-label={`Remover ${h.habilidade.nome}`}
                        className="ml-0.5 rounded-full p-0.5 text-indigo-400 hover:text-indigo-700 hover:bg-indigo-100 transition-colors disabled:opacity-40"
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
                    )}
                  </span>
                );
              })}
            </div>
          ) : (
            <p className="text-xs text-gray-400">Nenhuma habilidade adicionada ainda.</p>
          )}

          <p className="text-[11px] text-gray-400">
            Habilidades endossadas por professores não podem ser removidas.
          </p>
        </div>

      </section>

      {/* ── Botões de ação no rodapé ───────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={() => navigate(`/usuarios/${id}`)}
          className="w-full sm:w-auto px-6 py-2.5 rounded-xl text-sm font-semibold text-gray-700 bg-white border border-gray-200 hover:bg-gray-50 transition-colors"
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

    </div>
  );
}
