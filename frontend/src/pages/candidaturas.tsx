import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  cancelarCandidatura,
  listarMinhasCandidaturas,
  type Candidatura,
  type StatusCandidatura,
} from '../services/candidaturaService';
import { ApiError } from '../services/apiClient';
import { STATUS_CANDIDATURA_BADGE, STATUS_CANDIDATURA_LABEL } from '../utils/candidatura';
import { formatarData } from '../utils/projeto';

const OPCOES_STATUS: StatusCandidatura[] = ['PENDENTE', 'ACEITO', 'REJEITADO'];

export default function Candidaturas() {
  const [candidaturas, setCandidaturas] = useState<Candidatura[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [filtroStatus, setFiltroStatus] = useState<StatusCandidatura | ''>('');
  const [cancelandoId, setCancelandoId] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    listarMinhasCandidaturas({ status: filtroStatus || undefined, tamanho: 50 })
      .then((pagina) => {
        if (!ativo) return;
        setCandidaturas(pagina.content);
        setErro(null);
      })
      .catch(() => {
        if (ativo) setErro('Não foi possível carregar suas candidaturas. Tente novamente.');
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => { ativo = false; };
  }, [filtroStatus]);

  const contagem = {
    PENDENTE: candidaturas.filter((c) => c.status === 'PENDENTE').length,
    ACEITO: candidaturas.filter((c) => c.status === 'ACEITO').length,
    REJEITADO: candidaturas.filter((c) => c.status === 'REJEITADO').length,
  };

  async function handleCancelar(candidatura: Candidatura) {
    const confirmado = window.confirm(`Cancelar sua candidatura ao projeto "${candidatura.projeto.titulo}"?`);
    if (!confirmado) return;

    setCancelandoId(candidatura.id);
    try {
      await cancelarCandidatura(candidatura.id);
      setCandidaturas((atual) => atual.filter((c) => c.id !== candidatura.id));
    } catch (erroCapturado) {
      setErro(erroCapturado instanceof ApiError ? erroCapturado.message : 'Não foi possível cancelar a candidatura.');
    } finally {
      setCancelandoId(null);
    }
  }

  return (
    <div className="pb-10 max-w-7xl mx-auto">

      {/* Cabeçalho Padronizado */}
      <div className="mb-8">
        <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">
          Minhas Candidaturas
        </h1>
        <p className="text-gray-500 text-sm mt-1">{candidaturas.length} candidatura(s) no total</p>
      </div>

      {/* Cards de Status */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">{contagem.PENDENTE}</p>
          <p className="text-sm font-bold text-orange-500 bg-orange-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">🕒 Pendente</p>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">{contagem.ACEITO}</p>
          <p className="text-sm font-bold text-green-600 bg-green-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">✓ Aceito</p>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">{contagem.REJEITADO}</p>
          <p className="text-sm font-bold text-red-500 bg-red-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">✕ Rejeitado</p>
        </div>
      </div>

      {/* Filtro por status */}
      <div className="flex justify-end mb-6">
        <select
          value={filtroStatus}
          onChange={(e) => setFiltroStatus(e.target.value as StatusCandidatura | '')}
          className="px-5 py-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405] focus:ring-4 focus:ring-[#F27405]/10 font-medium text-gray-700 shadow-sm cursor-pointer"
        >
          <option value="">Todos os status</option>
          {OPCOES_STATUS.map((status) => (
            <option key={status} value={status}>{STATUS_CANDIDATURA_LABEL[status]}</option>
          ))}
        </select>
      </div>

      {erro && (
        <div className="mb-6 rounded-2xl bg-red-50 border border-red-100 px-6 py-4 text-sm font-semibold text-red-600 shadow-sm">
          {erro}
        </div>
      )}

      {carregando ? (
        <div className="flex justify-center py-20"><p className="text-lg font-bold text-gray-400 animate-pulse">Carregando candidaturas...</p></div>
      ) : candidaturas.length === 0 ? (
        <div className="bg-white rounded-3xl border border-gray-100 p-12 text-center shadow-sm">
          <p className="text-lg text-gray-500 font-medium mb-2">Você ainda não se candidatou a nenhum projeto.</p>
          <Link to="/projetos" className="text-[#F27405] font-bold hover:underline">Explorar projetos →</Link>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {candidaturas.map((candidatura) => (
            <div key={candidatura.id} className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <div className="flex justify-between items-start mb-4 gap-4">
                <Link to={`/detalhes/${candidatura.projeto.id}`} className="min-w-0">
                  <h3 className="font-bold text-lg text-[#183E6C] hover:text-[#F27405] transition-colors truncate">{candidatura.projeto.titulo}</h3>
                  <p className="text-sm font-medium text-gray-500">Candidatou-se em {formatarData(candidatura.dataCandidatura)}</p>
                </Link>
                <span className={`text-xs font-bold px-3 py-1 rounded-full shrink-0 ${STATUS_CANDIDATURA_BADGE[candidatura.status]}`}>
                  {STATUS_CANDIDATURA_LABEL[candidatura.status]}
                </span>
              </div>

              {candidatura.mensagem && (
                <div className="bg-gray-50 p-4 rounded-xl mt-4 border border-gray-100">
                  <p className="text-xs font-bold text-gray-400 mb-2 uppercase tracking-wider">Sua Mensagem</p>
                  <p className="text-sm text-gray-600 italic">{candidatura.mensagem}</p>
                </div>
              )}

              {candidatura.status === 'REJEITADO' && candidatura.motivoRejeicao && (
                <div className="bg-red-50 p-4 rounded-xl mt-4 border border-red-100">
                  <p className="text-xs font-bold text-red-400 mb-2 uppercase tracking-wider">Motivo da Rejeição</p>
                  <p className="text-sm text-red-600">{candidatura.motivoRejeicao}</p>
                </div>
              )}

              <div className="flex justify-end items-center mt-4 gap-4">
                {candidatura.status === 'PENDENTE' && (
                  <button
                    type="button"
                    onClick={() => handleCancelar(candidatura)}
                    disabled={cancelandoId === candidatura.id}
                    className="text-xs font-bold text-red-500 hover:underline disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {cancelandoId === candidatura.id ? 'Cancelando...' : 'Cancelar candidatura'}
                  </button>
                )}
                <Link to={`/detalhes/${candidatura.projeto.id}`} className="text-xs font-bold text-[#F27405] hover:underline">
                  Ver detalhes do projeto →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

    </div>
  )
}
