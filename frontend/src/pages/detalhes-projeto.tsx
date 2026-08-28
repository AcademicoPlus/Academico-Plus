import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../services/apiClient'
import { buscarProjetoPorId, type ProjetoDetalhe } from '../services/projetoService'
import { listarMembrosDoProjeto, type ProjetoMembro } from '../services/projetoMembroService'
import { obterMeuPerfil } from '../services/usuarioService'
import { STATUS_PROJETO_BADGE, STATUS_PROJETO_LABEL, formatarData, iniciaisDoNome } from '../utils/projeto'

export default function DetalhesProjetoRota() {
  const { id } = useParams<{ id: string }>();

  if (!id) {
    return (
      <div className="max-w-4xl mx-auto pb-10">
        <p className="text-gray-500 mb-4">Projeto não encontrado.</p>
        <Link to="/projetos" className="text-[#F27405] font-semibold hover:underline">← Voltar para projetos</Link>
      </div>
    );
  }

  // key={id} força remontar a tela ao trocar de projeto — cada instância já
  // nasce com o loading certo, sem precisar "resetar" estado num efeito.
  return <DetalhesProjeto key={id} id={id} />;
}

function DetalhesProjeto({ id }: { id: string }) {
  const [projeto, setProjeto] = useState<ProjetoDetalhe | null>(null);
  const [membros, setMembros] = useState<ProjetoMembro[]>([]);
  const [meuId, setMeuId] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // Lógica do botão de candidatura (ainda só visual — a candidatura de fato
  // fica pra quando o fluxo de candidaturas for implementado nesta tela).
  const [candidatado, setCandidatado] = useState(false);

  useEffect(() => {
    let ativo = true;
    Promise.all([
      buscarProjetoPorId(id),
      listarMembrosDoProjeto(id).catch(() => [] as ProjetoMembro[]),
      obterMeuPerfil().catch(() => null),
    ])
      .then(([detalhe, listaDeMembros, meuPerfil]) => {
        if (!ativo) return;
        setProjeto(detalhe);
        setMembros(listaDeMembros);
        setMeuId(meuPerfil?.id ?? null);
      })
      .catch((erroCapturado) => {
        if (!ativo) return;
        setErro(
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

  if (carregando) {
    return <p className="text-sm text-gray-400">Carregando projeto...</p>;
  }

  if (erro || !projeto) {
    return (
      <div className="max-w-4xl mx-auto pb-10">
        <Link to="/projetos" className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
          ← Voltar para projetos
        </Link>
        <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-sm text-center text-gray-500">
          {erro ?? 'Projeto não encontrado.'}
        </div>
      </div>
    );
  }

  const prazo = formatarData(projeto.dataFim);
  const souCriador = meuId !== null && projeto.criador?.id === meuId;

  return (
    <div className="max-w-4xl mx-auto pb-10">
      <Link to="/projetos" className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
        ← Voltar para projetos
      </Link>

      <div className="bg-white rounded-3xl p-6 md:p-8 border border-gray-100 shadow-sm mb-6">
        <div className="flex flex-col md:flex-row justify-between items-start mb-6 gap-4">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C] leading-tight mb-2">{projeto.titulo}</h1>
            <span className={`px-3 py-1 rounded-full text-xs font-bold ${STATUS_PROJETO_BADGE[projeto.status]}`}>
              ● {STATUS_PROJETO_LABEL[projeto.status]}
            </span>
          </div>

          {/* Quem criou o projeto edita em vez de se candidatar ao próprio projeto */}
          {souCriador ? (
            <Link
              to={`/editar-projeto/${projeto.id}`}
              className="w-full md:w-auto text-center px-6 py-3 rounded-xl font-bold transition-all shadow-sm bg-white border-2 border-[#183E6C] text-[#183E6C] hover:bg-[#183E6C] hover:text-white"
            >
              ✎ Editar projeto
            </Link>
          ) : (
            <button
              onClick={() => setCandidatado((atual) => !atual)}
              disabled={!projeto.aceitandoCandidaturas}
              className={`w-full md:w-auto px-6 py-3 rounded-xl font-bold transition-all shadow-sm disabled:opacity-50 disabled:cursor-not-allowed ${
                candidatado
                  ? 'bg-white border-2 border-red-500 text-red-500 hover:bg-red-50'
                  : 'bg-[#F27405] text-white hover:bg-[#D96704] shadow-[#F27405]/20'
              }`}
            >
              {!projeto.aceitandoCandidaturas
                ? 'Candidaturas fechadas'
                : candidatado
                  ? '✕ Cancelar candidatura'
                  : '✓ Candidatar-se'}
            </button>
          )}
        </div>

        <p className="text-gray-600 text-sm md:text-base mb-6 leading-relaxed">{projeto.descricao}</p>

        {projeto.habilidadesNecessarias.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-6">
            {projeto.habilidadesNecessarias.map((h) => (
              <span
                key={h.id}
                className={`text-xs px-3 py-1 rounded-full font-medium ${
                  h.obrigatoria ? 'bg-[#183E6C] text-white' : 'bg-gray-100 text-gray-600'
                }`}
              >
                {h.habilidade.nome}
                {h.obrigatoria ? ' • obrigatória' : ''}
              </span>
            ))}
          </div>
        )}

        {/* Informações */}
        <div className="bg-gray-50 rounded-2xl p-6 grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Vagas</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">👥 {projeto.vagasPreenchidas}/{projeto.vagas}</p>
          </div>
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Prazo</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">📅 {prazo ?? 'Sem prazo definido'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Candidaturas</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">
              {projeto.aceitandoCandidaturas ? '📬 Recebendo candidaturas' : '🚫 Fechado'}
            </p>
          </div>
        </div>

        {/* Autor */}
        <div className="flex items-center gap-3 pt-6 border-t border-gray-100">
          <div className="w-12 h-12 rounded-full bg-[#183E6C] text-white flex justify-center items-center font-bold">
            {projeto.criador ? iniciaisDoNome(projeto.criador.nome) : '?'}
          </div>
          <div>
            <p className="text-xs text-gray-400">Criado por</p>
            <p className="font-bold text-gray-900">{projeto.criador?.nome ?? 'Usuário removido'}</p>
          </div>
        </div>
      </div>

      {/* Membros do Projeto */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-gray-100 shadow-sm">
        <h3 className="font-bold text-[#183E6C] text-lg mb-4">Membros do projeto ({projeto.totalMembros})</h3>
        {membros.length === 0 ? (
          <p className="text-sm text-gray-400">Nenhum membro no momento.</p>
        ) : (
          <div className="flex flex-col md:flex-row flex-wrap gap-4">
            {membros.map((membro) => (
              <div key={membro.id} className="bg-gray-50 p-3 rounded-xl flex items-center gap-3 w-full md:w-64">
                <div className="w-10 h-10 rounded-full bg-[#183E6C] text-white flex items-center justify-center text-xs font-bold shrink-0">
                  {membro.usuario ? iniciaisDoNome(membro.usuario.nome) : '?'}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-bold truncate">{membro.usuario?.nome ?? 'Usuário removido'}</p>
                  <p className="text-xs text-gray-500 truncate">{membro.funcao ?? membro.usuario?.curso ?? '—'}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
