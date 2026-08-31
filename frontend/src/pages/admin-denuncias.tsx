import { useEffect, useState } from 'react'
import { listarDenunciasPendentes, resolverDenuncia, type Denuncia } from '../services/denunciaService'
import { ApiError } from '../services/apiClient'

function formatarData(data: string): string {
  return new Date(data).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default function AdminDenuncias() {
  const [denuncias, setDenuncias] = useState<Denuncia[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [resolvendoId, setResolvendoId] = useState<string | null>(null);
  const [erroLinha, setErroLinha] = useState<Record<string, string>>({});

  useEffect(() => {
    listarDenunciasPendentes()
      .then((pagina) => setDenuncias(pagina.content))
      .catch(() => setErro('Não foi possível carregar as denúncias. Tente novamente.'))
      .finally(() => setCarregando(false));
  }, []);

  async function handleResolver(denuncia: Denuncia, procedente: boolean) {
    if (procedente) {
      const confirmado = window.confirm(
        'Marcar como procedente remove permanentemente a avaliação denunciada. Deseja continuar?',
      );
      if (!confirmado) return;
    }

    setErroLinha((atual) => {
      const proximo = { ...atual };
      delete proximo[denuncia.id];
      return proximo;
    });
    setResolvendoId(denuncia.id);
    try {
      await resolverDenuncia(denuncia.id, procedente);
      setDenuncias((atual) => atual.filter((item) => item.id !== denuncia.id));
    } catch (erroCapturado) {
      const mensagem = erroCapturado instanceof ApiError
        ? erroCapturado.message
        : 'Não foi possível resolver a denúncia.';
      setErroLinha((atual) => ({ ...atual, [denuncia.id]: mensagem }));
    } finally {
      setResolvendoId(null);
    }
  }

  return (
    <div className="pb-12 max-w-5xl mx-auto">
      <header className="mb-8">
        <h1 className="text-2xl md:text-3xl font-extrabold text-[#183E6C] tracking-tight">Denúncias</h1>
        <p className="text-gray-500 mt-1 font-medium">{denuncias.length} denúncia(s) pendente(s) de revisão</p>
      </header>

      {erro && (
        <div className="mb-6 rounded-2xl bg-red-50 border border-red-100 px-6 py-4 text-sm font-semibold text-red-600 shadow-sm">
          {erro}
        </div>
      )}

      {carregando ? (
        <div className="flex justify-center py-20"><p className="text-lg font-bold text-gray-400 animate-pulse">Carregando denúncias...</p></div>
      ) : denuncias.length === 0 ? (
        <div className="bg-white rounded-3xl border border-gray-100 p-12 text-center shadow-sm">
          <p className="text-lg text-gray-500 font-medium">Nenhuma denúncia pendente.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {denuncias.map((denuncia) => (
            <div key={denuncia.id} className="bg-white rounded-3xl border border-gray-100 shadow-sm p-6 md:p-8">
              <div className="flex flex-col md:flex-row justify-between md:items-center gap-2 mb-5">
                <p className="text-sm font-bold text-[#183E6C]">
                  {denuncia.denunciante.nome} denunciou uma avaliação de {denuncia.avaliador.nome}
                </p>
                <span className="text-xs font-semibold text-gray-400">{formatarData(denuncia.criadoEm)}</span>
              </div>

              <div className="bg-gray-50 rounded-xl p-4 border border-gray-100 mb-4">
                <p className="text-[11px] font-extrabold text-gray-400 uppercase tracking-widest mb-2">
                  Avaliação denunciada · Nota {denuncia.notaAvaliacao}
                </p>
                <p className="text-sm text-gray-600 italic">
                  {denuncia.comentarioAvaliacao || 'Sem comentário.'}
                </p>
              </div>

              <div className="mb-6">
                <p className="text-[11px] font-extrabold text-gray-400 uppercase tracking-widest mb-2">Motivo da denúncia</p>
                <p className="text-sm text-gray-700">{denuncia.motivo}</p>
              </div>

              {erroLinha[denuncia.id] && (
                <div className="mb-4 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">
                  {erroLinha[denuncia.id]}
                </div>
              )}

              <div className="flex flex-col-reverse md:flex-row gap-3 justify-end">
                <button
                  type="button"
                  onClick={() => handleResolver(denuncia, false)}
                  disabled={resolvendoId === denuncia.id}
                  className="px-6 py-3 rounded-xl font-bold text-sm text-gray-500 bg-gray-50 hover:bg-gray-100 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  Marcar improcedente
                </button>
                <button
                  type="button"
                  onClick={() => handleResolver(denuncia, true)}
                  disabled={resolvendoId === denuncia.id}
                  className="px-6 py-3 rounded-xl font-bold text-sm text-white bg-red-500 hover:bg-red-600 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {resolvendoId === denuncia.id ? 'Processando...' : 'Marcar procedente'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
