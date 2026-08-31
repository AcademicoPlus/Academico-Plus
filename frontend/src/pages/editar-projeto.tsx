import { Link, useParams } from 'react-router-dom'
import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../services/apiClient'
import {
  atualizarProjeto,
  alterarStatusProjeto,
  buscarProjetoPorId,
  type ProjetoDetalhe,
  type StatusProjeto,
} from '../services/projetoService'
import { listarHabilidades, type Habilidade } from '../services/habilidadeService'
import {
  alterarObrigatoriedadeHabilidade,
  desvincularHabilidadeDoProjeto,
  vincularHabilidadeAoProjeto,
} from '../services/projetoHabilidadeService'
import { obterMeuPerfilCache } from '../hooks/useMeuPerfil'
import {
  ANOS_MAXIMOS_PRAZO,
  STATUS_PROJETO_BADGE,
  STATUS_PROJETO_LABEL,
  adicionarAnos,
  formatarDataISO,
} from '../utils/projeto'

const TODOS_STATUS: StatusProjeto[] = ['ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO'];

// Espelha ProjetoService.validarTransicaoStatus (backend): só sai de status
// final quem nunca entrou nele — ABERTO/EM_ANDAMENTO trocam livremente entre
// si e para qualquer status; CONCLUIDO/CANCELADO não saem mais de lá.
function transicoesDisponiveis(atual: StatusProjeto): StatusProjeto[] {
  if (atual === 'CONCLUIDO' || atual === 'CANCELADO') return [];
  return TODOS_STATUS.filter((status) => status !== atual);
}

export default function EditarProjetoRota() {
  const { id } = useParams<{ id: string }>();

  if (!id) {
    return (
      <div className="max-w-3xl mx-auto pb-10">
        <p className="text-gray-500 mb-4">Projeto não encontrado.</p>
        <Link to="/projetos" className="text-[#F27405] font-semibold hover:underline">← Voltar para projetos</Link>
      </div>
    );
  }

  // key={id} força remontar ao trocar de projeto — mesmo padrão da tela de detalhes.
  return <EditarProjeto key={id} id={id} />;
}

function EditarProjeto({ id }: { id: string }) {
  const [projeto, setProjeto] = useState<ProjetoDetalhe | null>(null);
  const [souCriador, setSouCriador] = useState<boolean | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erroCarregamento, setErroCarregamento] = useState<string | null>(null);

  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [vagas, setVagas] = useState('1');
  const [dataFim, setDataFim] = useState('');

  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});

  const [habilidades, setHabilidades] = useState<Habilidade[]>([]);
  const [carregandoHabilidades, setCarregandoHabilidades] = useState(true);
  const [habilidadesErro, setHabilidadesErro] = useState<string | null>(null);
  const [buscaHabilidade, setBuscaHabilidade] = useState('');
  const [habilidadeEmAcao, setHabilidadeEmAcao] = useState<string | null>(null);
  const [erroHabilidade, setErroHabilidade] = useState<string | null>(null);

  const [alterandoStatus, setAlterandoStatus] = useState(false);
  const [erroStatus, setErroStatus] = useState<string | null>(null);
  const [confirmandoCancelamento, setConfirmandoCancelamento] = useState(false);
  const [motivoCancelamento, setMotivoCancelamento] = useState('');

  useEffect(() => {
    let ativo = true;
    Promise.all([buscarProjetoPorId(id), obterMeuPerfilCache().catch(() => null)])
      .then(([detalhe, meuPerfil]) => {
        if (!ativo) return;
        setProjeto(detalhe);
        setSouCriador(meuPerfil !== null && detalhe.criador?.id === meuPerfil.id);
        setTitulo(detalhe.titulo);
        setDescricao(detalhe.descricao);
        setVagas(String(detalhe.vagas));
        setDataFim(detalhe.dataFim ? detalhe.dataFim.slice(0, 10) : '');
      })
      .catch((erroCapturado) => {
        if (!ativo) return;
        setErroCarregamento(
          erroCapturado instanceof ApiError && erroCapturado.status === 404
            ? 'Projeto não encontrado.'
            : 'Não foi possível carregar este projeto. Tente novamente.',
        );
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [id]);

  useEffect(() => {
    listarHabilidades({ tamanho: 100 })
      .then((pagina) => setHabilidades(pagina.content))
      .catch(() => setHabilidadesErro('Não foi possível carregar o catálogo de habilidades.'))
      .finally(() => setCarregandoHabilidades(false));
  }, []);

  function validar(): Record<string, string> {
    const erros: Record<string, string> = {};

    const tituloAparado = titulo.trim();
    if (!tituloAparado) {
      erros.titulo = 'Título é obrigatório.';
    } else if (tituloAparado.length < 5 || tituloAparado.length > 255) {
      erros.titulo = 'Título deve ter entre 5 e 255 caracteres.';
    }

    const descricaoAparada = descricao.trim();
    if (!descricaoAparada) {
      erros.descricao = 'Descrição é obrigatória.';
    } else if (descricaoAparada.length < 20) {
      erros.descricao = 'Descrição deve ter no mínimo 20 caracteres.';
    }

    const vagasNumero = Number(vagas);
    const minimoVagas = projeto?.totalMembros ?? 1;
    if (!vagas.trim() || Number.isNaN(vagasNumero)) {
      erros.vagas = 'Número de vagas é obrigatório.';
    } else if (vagasNumero < 1) {
      erros.vagas = 'Deve ter no mínimo 1 vaga.';
    } else if (vagasNumero < minimoVagas) {
      erros.vagas = `Não pode ser menor que os ${minimoVagas} membros já no projeto.`;
    }

    // Diferente da criação, uma data de fim já vencida é permitida aqui (um
    // projeto em andamento pode manter o prazo original) — só barramos lixo
    // de input (ver ANOS_MAXIMOS_PRAZO) e datas impossíveis.
    if (dataFim) {
      if (!/^\d{4}-\d{2}-\d{2}$/.test(dataFim)) {
        erros.dataFim = 'Data de fim inválida.';
      } else {
        const fim = new Date(`${dataFim}T23:59:59`);
        const limite = adicionarAnos(new Date(), ANOS_MAXIMOS_PRAZO);
        if (Number.isNaN(fim.getTime())) {
          erros.dataFim = 'Data de fim inválida.';
        } else if (fim.getTime() > limite.getTime()) {
          erros.dataFim = `Data de fim não pode passar de ${ANOS_MAXIMOS_PRAZO} anos a partir de hoje.`;
        }
      }
    }

    return erros;
  }

  function limparErroCampo(campo: string) {
    setErrosCampo((atual) => {
      if (!(campo in atual)) return atual;
      const proximo = { ...atual };
      delete proximo[campo];
      return proximo;
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);

    const errosValidacao = validar();
    setErrosCampo(errosValidacao);
    if (Object.keys(errosValidacao).length > 0) {
      setErro('Corrija os campos destacados antes de continuar.');
      return;
    }

    setSalvando(true);
    try {
      const atualizado = await atualizarProjeto(id, {
        titulo,
        descricao,
        vagas: Number(vagas),
        dataFim: dataFim ? `${dataFim}T23:59:59` : undefined,
      });
      setProjeto(atualizado);
      setSucesso('Alterações salvas.');
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
        if (erroCapturado.erros) {
          const mapa: Record<string, string> = {};
          for (const { campo, mensagem } of erroCapturado.erros) {
            mapa[campo] = mensagem;
          }
          setErrosCampo(mapa);
        }
      } else {
        setErro('Não foi possível conectar ao servidor. Tente novamente.');
      }
    } finally {
      setSalvando(false);
    }
  }

  const habilidadesSelecionadas = projeto?.habilidadesNecessarias ?? [];
  const idsSelecionados = new Set(habilidadesSelecionadas.map((h) => h.habilidade.id));
  const habilidadesFiltradas = habilidades.filter((habilidade) => {
    if (idsSelecionados.has(habilidade.id)) return false;
    const termo = buscaHabilidade.trim().toLowerCase();
    if (!termo) return true;
    return (
      habilidade.nome.toLowerCase().includes(termo) ||
      habilidade.categoria.toLowerCase().includes(termo)
    );
  });

  async function handleAdicionarHabilidade(habilidadeId: string) {
    setErroHabilidade(null);
    setHabilidadeEmAcao(habilidadeId);
    try {
      const vinculo = await vincularHabilidadeAoProjeto(id, habilidadeId, false);
      setProjeto((atual) => (atual ? { ...atual, habilidadesNecessarias: [...atual.habilidadesNecessarias, vinculo] } : atual));
    } catch (erroCapturado) {
      setErroHabilidade(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível adicionar a habilidade.');
    } finally {
      setHabilidadeEmAcao(null);
    }
  }

  async function handleRemoverHabilidade(habilidadeId: string) {
    setErroHabilidade(null);
    setHabilidadeEmAcao(habilidadeId);
    try {
      await desvincularHabilidadeDoProjeto(id, habilidadeId);
      setProjeto((atual) =>
        atual
          ? { ...atual, habilidadesNecessarias: atual.habilidadesNecessarias.filter((h) => h.habilidade.id !== habilidadeId) }
          : atual,
      );
    } catch (erroCapturado) {
      setErroHabilidade(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível remover a habilidade.');
    } finally {
      setHabilidadeEmAcao(null);
    }
  }

  async function handleAlternarObrigatoria(habilidadeId: string, obrigatoriaAtual: boolean) {
    setErroHabilidade(null);
    setHabilidadeEmAcao(habilidadeId);
    try {
      const atualizado = await alterarObrigatoriedadeHabilidade(id, habilidadeId, !obrigatoriaAtual);
      setProjeto((atual) =>
        atual
          ? {
              ...atual,
              habilidadesNecessarias: atual.habilidadesNecessarias.map((h) =>
                h.habilidade.id === habilidadeId ? atualizado : h,
              ),
            }
          : atual,
      );
    } catch (erroCapturado) {
      setErroHabilidade(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível atualizar a habilidade.');
    } finally {
      setHabilidadeEmAcao(null);
    }
  }

  async function handleMudarStatus(novoStatus: StatusProjeto, motivo?: string) {
    setErroStatus(null);
    setAlterandoStatus(true);
    try {
      const atualizado = await alterarStatusProjeto(id, novoStatus, motivo);
      setProjeto(atualizado);
      setConfirmandoCancelamento(false);
      setMotivoCancelamento('');
    } catch (erroCapturado) {
      setErroStatus(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível alterar o status.');
    } finally {
      setAlterandoStatus(false);
    }
  }

  if (carregando) {
    return <p className="text-sm text-gray-400">Carregando projeto...</p>;
  }

  if (erroCarregamento || !projeto) {
    return (
      <div className="max-w-3xl mx-auto pb-10">
        <Link to="/projetos" className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
          ← Voltar para projetos
        </Link>
        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-sm text-center text-gray-500">
          {erroCarregamento ?? 'Projeto não encontrado.'}
        </div>
      </div>
    );
  }

  if (souCriador === false) {
    return (
      <div className="max-w-3xl mx-auto pb-10">
        <Link to={`/detalhes/${id}`} className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
          ← Voltar para o projeto
        </Link>
        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-sm text-center text-gray-500">
          Você não tem permissão para editar este projeto.
        </div>
      </div>
    );
  }

  const statusFinal = projeto.status === 'CONCLUIDO' || projeto.status === 'CANCELADO';

  return (
    <div className="max-w-3xl mx-auto pb-10">
      <Link to={`/detalhes/${id}`} className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
        ← Voltar para o projeto
      </Link>

      <div className="mb-6">
        <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">Editar projeto</h1>
        <p className="text-gray-500 text-sm mt-1">{projeto.titulo}</p>
      </div>

      {/* Informações básicas */}
      <div className="bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-6 md:p-8 mb-6">
        {erro && (
          <div className="mb-5 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">{erro}</div>
        )}
        {sucesso && (
          <div className="mb-5 rounded-xl bg-green-50 border border-green-100 px-4 py-3 text-sm text-green-700">{sucesso}</div>
        )}

        {statusFinal && (
          <div className="mb-5 rounded-xl bg-gray-50 border border-gray-100 px-4 py-3 text-sm text-gray-500">
            Projetos com status {STATUS_PROJETO_LABEL[projeto.status]} não podem ser editados.
          </div>
        )}

        <fieldset disabled={statusFinal} className="flex flex-col gap-4 disabled:opacity-50">
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            <div>
              <label className="block text-sm font-medium text-[#183E6C] mb-2">Título do projeto</label>
              <input
                type="text"
                value={titulo}
                onChange={(e) => { setTitulo(e.target.value); limparErroCampo('titulo'); }}
                className={`w-full px-4 py-3 bg-gray-50 border rounded-xl focus:bg-white focus:ring-2 outline-none transition-all text-gray-700 ${
                  errosCampo.titulo
                    ? 'border-red-400 focus:border-red-400 focus:ring-red-400/20'
                    : 'border-transparent focus:border-[#F27405] focus:ring-[#F27405]/20'
                }`}
              />
              {errosCampo.titulo && <p className="text-xs text-red-500 mt-1.5">{errosCampo.titulo}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-[#183E6C] mb-2">Descrição</label>
              <textarea
                value={descricao}
                onChange={(e) => { setDescricao(e.target.value); limparErroCampo('descricao'); }}
                rows={5}
                className={`w-full px-4 py-3 bg-gray-50 border rounded-xl focus:bg-white focus:ring-2 outline-none transition-all text-gray-700 resize-none ${
                  errosCampo.descricao
                    ? 'border-red-400 focus:border-red-400 focus:ring-red-400/20'
                    : 'border-transparent focus:border-[#F27405] focus:ring-[#F27405]/20'
                }`}
              />
              {errosCampo.descricao && <p className="text-xs text-red-500 mt-1.5">{errosCampo.descricao}</p>}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#183E6C] mb-2">Número de vagas</label>
                <input
                  type="number"
                  min={1}
                  value={vagas}
                  onChange={(e) => { setVagas(e.target.value); limparErroCampo('vagas'); }}
                  className={`w-full px-4 py-3 bg-gray-50 border rounded-xl focus:bg-white focus:ring-2 outline-none transition-all text-gray-700 ${
                    errosCampo.vagas
                      ? 'border-red-400 focus:border-red-400 focus:ring-red-400/20'
                      : 'border-transparent focus:border-[#F27405] focus:ring-[#F27405]/20'
                  }`}
                />
                {errosCampo.vagas && <p className="text-xs text-red-500 mt-1.5">{errosCampo.vagas}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-[#183E6C] mb-2">Prazo final (opcional)</label>
                <input
                  type="date"
                  value={dataFim}
                  max={formatarDataISO(adicionarAnos(new Date(), ANOS_MAXIMOS_PRAZO))}
                  onChange={(e) => { setDataFim(e.target.value); limparErroCampo('dataFim'); }}
                  className={`w-full px-4 py-3 bg-gray-50 border rounded-xl focus:bg-white focus:ring-2 outline-none transition-all text-gray-700 ${
                    errosCampo.dataFim
                      ? 'border-red-400 focus:border-red-400 focus:ring-red-400/20'
                      : 'border-transparent focus:border-[#F27405] focus:ring-[#F27405]/20'
                  }`}
                />
                {errosCampo.dataFim && <p className="text-xs text-red-500 mt-1.5">{errosCampo.dataFim}</p>}
              </div>
            </div>

            <button
              type="submit"
              disabled={salvando}
              className="w-full md:w-auto self-end bg-[#F27405] hover:bg-[#D96704] text-white font-bold px-8 py-3 rounded-xl transition-colors shadow-lg shadow-[#F27405]/30 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {salvando ? 'Salvando...' : 'Salvar alterações'}
            </button>
          </form>
        </fieldset>
      </div>

      {/* Habilidades necessárias */}
      <div className="bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-6 md:p-8 mb-6">
        <h2 className="text-lg font-bold text-[#183E6C] mb-4">Habilidades necessárias</h2>

        {erroHabilidade && (
          <div className="mb-4 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">{erroHabilidade}</div>
        )}

        {habilidadesSelecionadas.length > 0 && (
          <div className="flex flex-col gap-2 mb-4">
            {habilidadesSelecionadas.map((vinculo) => (
              <div
                key={vinculo.id}
                className="flex items-center justify-between gap-3 px-4 py-2.5 rounded-xl border border-[#F27405]/30 bg-orange-50"
              >
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-gray-700">{vinculo.habilidade.nome}</span>
                  <span className="text-[10px] text-gray-400 uppercase font-semibold">{vinculo.habilidade.categoria}</span>
                </div>

                <div className="flex items-center gap-4 shrink-0">
                  <label className="flex items-center gap-1.5 text-xs text-gray-500 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={vinculo.obrigatoria}
                      disabled={habilidadeEmAcao === vinculo.habilidade.id}
                      onChange={() => handleAlternarObrigatoria(vinculo.habilidade.id, vinculo.obrigatoria)}
                      className="h-3.5 w-3.5 rounded border-gray-300 text-[#183E6C] focus:ring-[#183E6C]/20"
                    />
                    Obrigatória
                  </label>
                  <button
                    type="button"
                    disabled={habilidadeEmAcao === vinculo.habilidade.id}
                    onClick={() => handleRemoverHabilidade(vinculo.habilidade.id)}
                    aria-label={`Remover ${vinculo.habilidade.nome}`}
                    className="text-gray-400 hover:text-red-500 transition-colors disabled:opacity-40"
                  >
                    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {carregandoHabilidades ? (
          <p className="text-sm text-gray-400">Carregando habilidades...</p>
        ) : habilidadesErro ? (
          <p className="text-sm text-red-500">{habilidadesErro}</p>
        ) : (
          <>
            <div className="relative mb-3">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z"></path>
                </svg>
              </div>
              <input
                type="text"
                placeholder="Buscar habilidade por nome ou categoria..."
                value={buscaHabilidade}
                onChange={(e) => setBuscaHabilidade(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-transparent rounded-xl focus:bg-white focus:border-[#F27405] focus:ring-2 focus:ring-[#F27405]/20 outline-none transition-all text-sm text-gray-700"
              />
            </div>

            {habilidadesFiltradas.length === 0 ? (
              <p className="text-sm text-gray-400">
                {buscaHabilidade
                  ? `Nenhuma habilidade encontrada para "${buscaHabilidade}".`
                  : 'Todas as habilidades do catálogo já foram adicionadas.'}
              </p>
            ) : (
              <div className="flex flex-col gap-2 max-h-64 overflow-y-auto pr-1">
                {habilidadesFiltradas.map((habilidade) => (
                  <button
                    type="button"
                    key={habilidade.id}
                    disabled={habilidadeEmAcao === habilidade.id}
                    onClick={() => handleAdicionarHabilidade(habilidade.id)}
                    className="flex items-center justify-between gap-3 px-4 py-2.5 rounded-xl bg-gray-50 hover:bg-gray-100 border border-transparent hover:border-[#F27405]/30 transition-colors text-left disabled:opacity-50"
                  >
                    <span className="flex items-center gap-2">
                      <span className="text-sm text-gray-700">{habilidade.nome}</span>
                      <span className="text-[10px] text-gray-400 uppercase font-semibold">{habilidade.categoria}</span>
                    </span>
                    <span className="text-[#F27405] text-lg leading-none font-bold shrink-0">+</span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>

      {/* Status do projeto */}
      <div className="bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-6 md:p-8">
        <h2 className="text-lg font-bold text-[#183E6C] mb-4">Status do projeto</h2>

        <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold mb-4 ${STATUS_PROJETO_BADGE[projeto.status]}`}>
          {STATUS_PROJETO_LABEL[projeto.status]}
        </span>

        {erroStatus && (
          <div className="mb-4 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">{erroStatus}</div>
        )}

        {statusFinal ? (
          <p className="text-sm text-gray-400">Projetos com status final não podem ter o status alterado.</p>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap gap-3">
              {transicoesDisponiveis(projeto.status)
                .filter((status) => status !== 'CANCELADO')
                .map((status) => (
                  <button
                    key={status}
                    type="button"
                    disabled={alterandoStatus}
                    onClick={() => handleMudarStatus(status)}
                    className="px-5 py-2.5 rounded-xl font-bold text-sm border-2 border-[#183E6C] text-[#183E6C] hover:bg-[#183E6C] hover:text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Marcar como {STATUS_PROJETO_LABEL[status]}
                  </button>
                ))}

              {!confirmandoCancelamento && (
                <button
                  type="button"
                  disabled={alterandoStatus}
                  onClick={() => setConfirmandoCancelamento(true)}
                  className="px-5 py-2.5 rounded-xl font-bold text-sm border-2 border-red-500 text-red-500 hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancelar projeto
                </button>
              )}
            </div>

            {confirmandoCancelamento && (
              <div className="rounded-xl border border-red-100 bg-red-50 p-4 flex flex-col gap-3">
                <p className="text-sm text-red-700">
                  Cancelar avisa todos os membros do projeto. Essa ação não pode ser desfeita.
                </p>
                <textarea
                  placeholder="Motivo do cancelamento (opcional)"
                  value={motivoCancelamento}
                  onChange={(e) => setMotivoCancelamento(e.target.value)}
                  rows={2}
                  className="w-full px-4 py-2.5 bg-white border border-red-200 rounded-xl outline-none focus:border-red-400 focus:ring-2 focus:ring-red-400/20 transition-all text-sm text-gray-700 resize-none"
                />
                <div className="flex gap-3">
                  <button
                    type="button"
                    disabled={alterandoStatus}
                    onClick={() => handleMudarStatus('CANCELADO', motivoCancelamento.trim() || undefined)}
                    className="px-5 py-2.5 rounded-xl font-bold text-sm bg-red-500 text-white hover:bg-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {alterandoStatus ? 'Cancelando...' : 'Confirmar cancelamento'}
                  </button>
                  <button
                    type="button"
                    disabled={alterandoStatus}
                    onClick={() => { setConfirmandoCancelamento(false); setMotivoCancelamento(''); }}
                    className="px-5 py-2.5 rounded-xl font-bold text-sm text-gray-500 hover:bg-gray-100 transition-colors disabled:opacity-50"
                  >
                    Voltar
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
