import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../services/apiClient'
import { buscarProjetoPorId, type ProjetoDetalhe } from '../services/projetoService'
import { listarMembrosDoProjeto, type ProjetoMembro } from '../services/projetoMembroService'
import { obterMeuPerfilCache } from '../hooks/useMeuPerfil'
import {
  aceitarCandidatura,
  candidatar,
  cancelarCandidatura,
  listarCandidaturasDoProjeto,
  listarMinhasCandidaturas,
  rejeitarCandidatura,
  type Candidatura,
} from '../services/candidaturaService'
import { STATUS_PROJETO_BADGE, STATUS_PROJETO_LABEL, formatarData, iniciaisDoNome } from '../utils/projeto'
import CaixaCandidaturat from '../components/caixa-candidaturat'

export default function DetalhesProjetoRota() {
  const { id } = useParams<{ id: string }>();

  if (!id) {
    return (
      <div className="max-w-5xl mx-auto pb-10">
        <p className="text-gray-500 mb-4 font-bold text-center">Projeto não encontrado.</p>
        <Link to="/projetos" className="text-[#F27405] font-semibold hover:underline block text-center">← Voltar para projetos</Link>
      </div>
    );
  }

  return <DetalhesProjeto key={id} id={id} />;
}

function DetalhesProjeto({ id }: { id: string }) {
  const [projeto, setProjeto] = useState<ProjetoDetalhe | null>(null);
  const [membros, setMembros] = useState<ProjetoMembro[]>([]);
  const [meuId, setMeuId] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // Visão do candidato: minha própria candidatura a este projeto (se existir).
  const [minhaCandidatura, setMinhaCandidatura] = useState<Candidatura | null>(null);
  const [mostrandoFormulario, setMostrandoFormulario] = useState(false);
  const [mensagem, setMensagem] = useState('');
  const [enviandoCandidatura, setEnviandoCandidatura] = useState(false);
  const [erroCandidatura, setErroCandidatura] = useState<string | null>(null);
  const [cancelando, setCancelando] = useState(false);

  // Visão do criador: candidaturas pendentes recebidas neste projeto.
  const [candidaturasPendentes, setCandidaturasPendentes] = useState<Candidatura[]>([]);
  const [processandoId, setProcessandoId] = useState<string | null>(null);
  const [rejeitandoId, setRejeitandoId] = useState<string | null>(null);
  const [motivoRejeicao, setMotivoRejeicao] = useState('');
  const [erroAcao, setErroAcao] = useState<string | null>(null);

  // Reutilizada tanto no carregamento inicial quanto depois de qualquer ação
  // (candidatar, cancelar, aceitar, rejeitar) — assim vagas/membros/status
  // continuam consistentes sem precisar atualizar cada pedaço à mão.
  async function carregarDados() {
    const [detalhe, listaDeMembros, meuPerfil] = await Promise.all([
      buscarProjetoPorId(id),
      listarMembrosDoProjeto(id).catch(() => [] as ProjetoMembro[]),
      obterMeuPerfilCache().catch(() => null),
    ]);

    setProjeto(detalhe);
    setMembros(listaDeMembros);
    setMeuId(meuPerfil?.id ?? null);

    const souCriador = meuPerfil !== null && detalhe.criador?.id === meuPerfil.id;

    if (souCriador) {
      const pagina = await listarCandidaturasDoProjeto(id, { status: 'PENDENTE', tamanho: 50 }).catch(() => null);
      setCandidaturasPendentes(pagina?.content ?? []);
    } else {
      const pagina = await listarMinhasCandidaturas({ tamanho: 100 }).catch(() => null);
      setMinhaCandidatura(pagina?.content.find((c) => c.projeto.id === id) ?? null);
    }
  }

  useEffect(() => {
    setCarregando(true);
    setErro(null);
    carregarDados()
      .catch((erroCapturado) => {
        setErro(erroCapturado instanceof ApiError && erroCapturado.status === 404 ? 'Projeto não encontrado.' : 'Não foi possível carregar este projeto.');
      })
      .finally(() => setCarregando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleCandidatar() {
    setErroCandidatura(null);
    setEnviandoCandidatura(true);
    try {
      const nova = await candidatar(id, mensagem.trim() || undefined);
      setMinhaCandidatura(nova);
      setMostrandoFormulario(false);
      setMensagem('');
    } catch (erroCapturado) {
      setErroCandidatura(
        erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível enviar a candidatura.',
      );
    } finally {
      setEnviandoCandidatura(false);
    }
  }

  async function handleCancelarCandidatura() {
    if (!minhaCandidatura) return;
    const confirmado = window.confirm('Cancelar sua candidatura a este projeto?');
    if (!confirmado) return;

    setErroCandidatura(null);
    setCancelando(true);
    try {
      await cancelarCandidatura(minhaCandidatura.id);
      setMinhaCandidatura(null);
    } catch (erroCapturado) {
      setErroCandidatura(
        erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível cancelar a candidatura.',
      );
    } finally {
      setCancelando(false);
    }
  }

  async function handleAceitar(candidatura: Candidatura) {
    const confirmado = window.confirm(`Aceitar ${candidatura.usuario?.nome ?? 'este candidato'} no projeto?`);
    if (!confirmado) return;

    setErroAcao(null);
    setProcessandoId(candidatura.id);
    try {
      await aceitarCandidatura(candidatura.id);
      await carregarDados();
    } catch (erroCapturado) {
      setErroAcao(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível aceitar a candidatura.');
    } finally {
      setProcessandoId(null);
    }
  }

  async function handleRejeitar(candidatura: Candidatura) {
    const motivo = motivoRejeicao.trim();
    if (motivo.length < 5) {
      setErroAcao('O motivo da rejeição precisa ter pelo menos 5 caracteres.');
      return;
    }

    setErroAcao(null);
    setProcessandoId(candidatura.id);
    try {
      await rejeitarCandidatura(candidatura.id, motivo);
      setRejeitandoId(null);
      setMotivoRejeicao('');
      await carregarDados();
    } catch (erroCapturado) {
      setErroAcao(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível rejeitar a candidatura.');
    } finally {
      setProcessandoId(null);
    }
  }

  if (carregando) return <div className="flex justify-center py-20"><p className="text-lg font-bold text-gray-400 animate-pulse">Carregando projeto...</p></div>;

  if (erro || !projeto) {
    return (
      <div className="max-w-5xl mx-auto pb-10">
        <Link to="/projetos" className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-bold transition-colors">← Voltar para projetos</Link>
        <div className="bg-white rounded-3xl p-12 border border-gray-100 shadow-sm text-center text-gray-500 font-bold text-lg">{erro ?? 'Projeto não encontrado.'}</div>
      </div>
    );
  }

  const prazo = formatarData(projeto.dataFim);
  const souCriador = meuId !== null && projeto.criador?.id === meuId;

  return (
    <div className="max-w-5xl mx-auto pb-12 animate-fade-in">
      <Link to="/projetos" className="inline-flex items-center gap-2 mb-8 text-gray-500 hover:text-[#F27405] text-sm font-bold transition-colors">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
        Voltar para explorar
      </Link>

      <div className="bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 overflow-hidden mb-8">

        {/* Capa do Projeto (Hero Section) */}
        <div className="h-64 sm:h-80 w-full relative bg-gray-100">
          <img src={`https://picsum.photos/seed/${projeto.id}projeto/1200/400`} alt="Capa" className="w-full h-full object-cover" />
          <div className="absolute inset-0 bg-linear-to-t from-[#0B1D33]/90 via-[#0B1D33]/40 to-transparent"></div>
          <div className="absolute bottom-6 left-6 md:bottom-10 md:left-10 pr-6">
            <span className={`px-4 py-1.5 rounded-xl text-xs font-black uppercase tracking-wider mb-4 inline-block shadow-sm ${STATUS_PROJETO_BADGE[projeto.status]}`}>
              ● {STATUS_PROJETO_LABEL[projeto.status]}
            </span>
            <h1 className="text-3xl md:text-5xl font-extrabold text-white leading-tight drop-shadow-lg">{projeto.titulo}</h1>
          </div>
        </div>

        {/* Conteúdo Principal */}
        <div className="p-6 md:p-10 flex flex-col lg:flex-row gap-10">

          <div className="lg:w-2/3">
            <h2 className="text-xl font-bold text-[#183E6C] mb-4">Sobre o Projeto</h2>
            <p className="text-gray-600 text-base md:text-lg leading-relaxed mb-10 whitespace-pre-line">{projeto.descricao}</p>

            <h2 className="text-xl font-bold text-[#183E6C] mb-4">Habilidades Desejadas</h2>
            {projeto.habilidadesNecessarias.length > 0 ? (
              <div className="flex flex-wrap gap-3 mb-8">
                {projeto.habilidadesNecessarias.map((h) => (
                  <span key={h.id} className={`px-4 py-2 rounded-xl text-sm font-bold border ${h.obrigatoria ? 'bg-[#183E6C] text-white border-[#183E6C]' : 'bg-orange-50 text-[#F27405] border-orange-100'}`}>
                    {h.habilidade.nome} {h.obrigatoria && <span className="opacity-75 font-medium ml-1">• Obrigatória</span>}
                  </span>
                ))}
              </div>
            ) : (
              <p className="text-gray-400 italic mb-8">Nenhuma habilidade específica requerida.</p>
            )}

            {/* Membros do Projeto */}
            <h2 className="text-xl font-bold text-[#183E6C] mb-4 mt-8">Equipe Atual ({projeto.totalMembros})</h2>
            {membros.length === 0 ? (
              <p className="text-sm text-gray-400">Nenhum membro no momento.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {membros.map((membro) => (
                  <div key={membro.id} className="bg-gray-50 border border-gray-100 p-4 rounded-2xl flex items-center gap-4">
                    <div className="w-12 h-12 rounded-full bg-[#183E6C] text-white flex items-center justify-center text-sm font-black shadow-sm">
                      {membro.usuario ? iniciaisDoNome(membro.usuario.nome) : '?'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-[#183E6C] truncate">{membro.usuario?.nome ?? 'Usuário removido'}</p>
                      <p className="text-xs font-medium text-gray-500 truncate">{membro.funcao ?? membro.usuario?.curso ?? '—'}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Candidaturas recebidas (só o criador do projeto vê) */}
            {souCriador && (
              <>
                <h2 className="text-xl font-bold text-[#183E6C] mb-4 mt-8">
                  Candidaturas Pendentes {candidaturasPendentes.length > 0 && `(${candidaturasPendentes.length})`}
                </h2>

                {erroAcao && (
                  <div className="mb-4 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">
                    {erroAcao}
                  </div>
                )}

                {candidaturasPendentes.length === 0 ? (
                  <p className="text-sm text-gray-400">Nenhuma candidatura pendente no momento.</p>
                ) : (
                  <div className="flex flex-col gap-4">
                    {candidaturasPendentes.map((candidatura) => (
                      <div key={candidatura.id} className="bg-gray-50 border border-gray-100 p-5 rounded-2xl">
                        <div className="flex items-center gap-3 mb-3">
                          <div className="w-10 h-10 rounded-full bg-[#183E6C] text-white flex items-center justify-center text-sm font-black shrink-0">
                            {candidatura.usuario ? iniciaisDoNome(candidatura.usuario.nome) : '?'}
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-bold text-[#183E6C] truncate">{candidatura.usuario?.nome ?? 'Usuário removido'}</p>
                            <p className="text-xs text-gray-500 truncate">{candidatura.usuario?.curso ?? '—'}</p>
                          </div>
                        </div>

                        {candidatura.mensagem && (
                          <p className="text-sm text-gray-600 italic bg-white border border-gray-100 rounded-xl p-3 mb-3">
                            "{candidatura.mensagem}"
                          </p>
                        )}

                        {rejeitandoId === candidatura.id ? (
                          <div className="flex flex-col gap-2">
                            <textarea
                              rows={2}
                              placeholder="Motivo da rejeição (mínimo 5 caracteres)..."
                              value={motivoRejeicao}
                              onChange={(e) => setMotivoRejeicao(e.target.value)}
                              className="w-full p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-red-400 focus:ring-1 focus:ring-red-400 resize-none text-sm"
                            />
                            <div className="flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={() => { setRejeitandoId(null); setMotivoRejeicao(''); setErroAcao(null); }}
                                className="px-4 py-2 rounded-xl text-sm font-bold text-gray-500 hover:bg-gray-100 transition-colors"
                              >
                                Cancelar
                              </button>
                              <button
                                type="button"
                                onClick={() => handleRejeitar(candidatura)}
                                disabled={processandoId === candidatura.id}
                                className="px-4 py-2 rounded-xl text-sm font-bold text-white bg-red-500 hover:bg-red-600 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                              >
                                Confirmar rejeição
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div className="flex justify-end gap-3">
                            <button
                              type="button"
                              onClick={() => { setRejeitandoId(candidatura.id); setMotivoRejeicao(''); setErroAcao(null); }}
                              disabled={processandoId === candidatura.id}
                              className="px-4 py-2 rounded-xl text-sm font-bold text-red-500 hover:bg-red-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                              Rejeitar
                            </button>
                            <button
                              type="button"
                              onClick={() => handleAceitar(candidatura)}
                              disabled={processandoId === candidatura.id}
                              className="px-4 py-2 rounded-xl text-sm font-bold text-white bg-[#183E6C] hover:bg-[#102a4a] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                              {processandoId === candidatura.id ? 'Processando...' : 'Aceitar'}
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>

          {/* Sidebar */}
          <div className="lg:w-1/3 flex flex-col gap-6">
            <div className="bg-gray-50 rounded-3xl p-6 border border-gray-100">
              <p className="text-xs text-gray-400 uppercase font-bold tracking-wider mb-2">Liderado por</p>
              <div className="flex items-center gap-4 mt-2">
                <div className="w-14 h-14 rounded-full bg-orange-50 text-[#F27405] flex justify-center items-center font-black text-xl border border-orange-100 shadow-sm">
                  {projeto.criador ? iniciaisDoNome(projeto.criador.nome) : '?'}
                </div>
                <div>
                  <p className="font-extrabold text-[#183E6C] text-lg leading-tight">{projeto.criador?.nome ?? 'Usuário removido'}</p>
                  <p className="text-sm text-gray-500 font-medium mt-1">Autor do Projeto</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 rounded-3xl p-6 border border-gray-100 grid grid-cols-2 gap-6">
              <div>
                <p className="text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Vagas</p>
                <p className="font-black text-2xl text-[#183E6C]">{projeto.vagasPreenchidas}<span className="text-gray-400 text-lg">/{projeto.vagas}</span></p>
              </div>
              <div>
                <p className="text-xs text-gray-400 uppercase font-bold tracking-wider mb-1">Prazo</p>
                <p className="font-bold text-[#183E6C] text-sm mt-1.5">{prazo ?? 'Sem prazo'}</p>
              </div>
            </div>

            {souCriador ? (
              <Link to={`/editar-projeto/${projeto.id}`} className="w-full text-center py-4 rounded-xl font-extrabold transition-all shadow-sm bg-white border-2 border-[#183E6C] text-[#183E6C] hover:bg-[#183E6C] hover:text-white">
                ✎ Editar Detalhes
              </Link>
            ) : minhaCandidatura ? (
              minhaCandidatura.status === 'PENDENTE' ? (
                <div className="flex flex-col gap-3">
                  <div className="text-center py-3 px-4 rounded-xl bg-orange-50 text-[#F27405] font-bold text-sm">
                    Candidatura enviada — aguardando resposta
                  </div>
                  {erroCandidatura && <p className="text-xs text-red-500 text-center">{erroCandidatura}</p>}
                  <button
                    onClick={handleCancelarCandidatura}
                    disabled={cancelando}
                    className="w-full py-4 rounded-xl font-extrabold transition-all shadow-sm bg-white border-2 border-red-500 text-red-500 hover:bg-red-50 disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {cancelando ? 'Cancelando...' : '✕ Cancelar Candidatura'}
                  </button>
                </div>
              ) : minhaCandidatura.status === 'ACEITO' ? (
                <div className="text-center py-4 px-4 rounded-xl bg-green-50 text-green-600 font-extrabold">
                  ✓ Você faz parte da equipe
                </div>
              ) : (
                <div className="py-4 px-4 rounded-xl bg-red-50 text-red-500">
                  <p className="font-extrabold text-center mb-1">Candidatura rejeitada</p>
                  {minhaCandidatura.motivoRejeicao && (
                    <p className="text-sm text-center opacity-90">{minhaCandidatura.motivoRejeicao}</p>
                  )}
                </div>
              )
            ) : !projeto.aceitandoCandidaturas ? (
              <button disabled className="w-full py-4 rounded-xl font-extrabold bg-gray-200 text-gray-500 cursor-not-allowed">
                🚫 Candidaturas Fechadas
              </button>
            ) : mostrandoFormulario ? (
              <CaixaCandidaturat
                mensagem={mensagem}
                onChangeMensagem={setMensagem}
                onEnviar={handleCandidatar}
                onCancelar={() => { setMostrandoFormulario(false); setErroCandidatura(null); }}
                enviando={enviandoCandidatura}
                erro={erroCandidatura}
              />
            ) : (
              <button
                onClick={() => setMostrandoFormulario(true)}
                className="w-full py-4 rounded-xl font-extrabold transition-all duration-300 shadow-lg bg-[#F27405] text-white hover:bg-[#D96704] hover:-translate-y-0.5 shadow-[#F27405]/30"
              >
                ✓ Quero me Candidatar
              </button>
            )}
          </div>

        </div>
      </div>
    </div>
  )
}
