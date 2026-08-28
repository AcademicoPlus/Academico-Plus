import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { listarProjetos, type Projeto, type StatusProjeto } from '../services/projetoService'
import { STATUS_PROJETO_BADGE, STATUS_PROJETO_LABEL, iniciaisDoNome } from '../utils/projeto'

const OPCOES_STATUS: StatusProjeto[] = ['ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO'];

// Espera o usuário parar de digitar antes de bater na API de novo.
const ATRASO_BUSCA_MS = 400;

export default function Projetos() {
  const [projetos, setProjetos] = useState<Projeto[]>([]);
  const [totalElementos, setTotalElementos] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [busca, setBusca] = useState('');
  const [buscaAplicada, setBuscaAplicada] = useState('');
  const [status, setStatus] = useState<StatusProjeto | ''>('');

  useEffect(() => {
    const temporizador = setTimeout(() => setBuscaAplicada(busca.trim()), ATRASO_BUSCA_MS);
    return () => clearTimeout(temporizador);
  }, [busca]);

  // Só busca de novo quando o filtro aplicado muda; o "carregando" liga na
  // hora em que o usuário mexe no filtro (handleBuscaChange/handleStatusChange
  // abaixo), não aqui — setState direto no corpo do efeito dispara renders
  // em cascata (regra react-hooks/set-state-in-effect).
  useEffect(() => {
    let ativo = true;
    listarProjetos({ busca: buscaAplicada || undefined, status: status || undefined, tamanho: 20 })
      .then((pagina) => {
        if (!ativo) return;
        setProjetos(pagina.content);
        setTotalElementos(pagina.totalElements);
        setErro(null);
      })
      .catch(() => {
        if (ativo) setErro('Não foi possível carregar os projetos. Tente novamente.');
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [buscaAplicada, status]);

  function handleBuscaChange(valor: string) {
    setBusca(valor);
    setCarregando(true);
  }

  function handleStatusChange(valor: StatusProjeto | '') {
    setStatus(valor);
    setCarregando(true);
  }

  return (
    <div className="pb-10">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">Feed de Projetos</h1>
          <p className="text-gray-500 text-sm">{totalElementos} projetos encontrados</p>
        </div>
        <Link
          to="/criar-projeto"
          className="bg-[#F27405] text-white px-6 py-2.5 rounded-xl font-bold w-full md:w-auto text-center shadow-md shadow-[#F27405]/20 hover:bg-[#D96704] transition-colors"
        >
          + Criar projeto
        </Link>
      </div>

      {/* Barra de Pesquisa */}
      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <input
          type="text"
          placeholder="Buscar projetos..."
          value={busca}
          onChange={(e) => handleBuscaChange(e.target.value)}
          className="flex-1 p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405]"
        />
        <select
          value={status}
          onChange={(e) => handleStatusChange(e.target.value as StatusProjeto | '')}
          className="p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405]"
        >
          <option value="">Todos os status</option>
          {OPCOES_STATUS.map((opcao) => (
            <option key={opcao} value={opcao}>{STATUS_PROJETO_LABEL[opcao]}</option>
          ))}
        </select>
      </div>

      {erro && (
        <div className="mb-6 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">
          {erro}
        </div>
      )}

      {carregando ? (
        <p className="text-sm text-gray-400">Carregando projetos...</p>
      ) : projetos.length === 0 ? (
        <p className="text-sm text-gray-400">Nenhum projeto encontrado.</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {projetos.map((proj) => (
            <div key={proj.id} className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex justify-between items-start mb-2 gap-2">
                  <h3 className="font-bold text-lg text-[#183E6C]">{proj.titulo}</h3>
                  <span className={`text-[10px] px-2 py-1 rounded-full font-bold uppercase shrink-0 ${STATUS_PROJETO_BADGE[proj.status]}`}>
                    {STATUS_PROJETO_LABEL[proj.status]}
                  </span>
                </div>
                <p className="text-sm text-gray-500 mb-4 line-clamp-2">{proj.descricao}</p>
                <div className="flex flex-wrap gap-2 mb-6">
                  {proj.habilidadesNecessarias.map((h) => (
                    <span key={h.id} className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-md">{h.habilidade.nome}</span>
                  ))}
                </div>
              </div>

              <div className="flex justify-between items-center border-t border-gray-50 pt-4">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-[#183E6C] text-white flex justify-center items-center text-xs font-bold">
                    {proj.criador ? iniciaisDoNome(proj.criador.nome) : '?'}
                  </div>
                  <span className="text-sm text-gray-600">{proj.criador?.nome ?? 'Usuário removido'}</span>
                </div>

                <Link to={`/detalhes/${proj.id}`} className="text-sm font-bold text-[#F27405] hover:underline">
                  Ver detalhes →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
