// Tela de Detalhes do Projeto conectada ao backend: lê o id na URL
// (rota "/projeto/:id" em App.tsx, acessada a partir do card do feed) e
// busca os dados completos em GET /projetos/{id} (ProjetoController ->
// ProjetoResponse). Layout mantido simples de propósito — o objetivo aqui é
// só ligar os dados reais; quem for cuidar do visual troca o JSX abaixo sem
// mexer na busca dos dados. Ver docs/CONECTANDO_FRONT_BACKEND.md.
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../services/apiClient';
import { buscarProjetoPorId, type ProjetoDetalhe } from '../services/projetoService';

// Mesmo mapeamento de label usado no feed (o JSON traz "ABERTO", não o texto
// em português) — duplicado aqui de propósito, são só 4 linhas; se crescer,
// vale mover pra um arquivo compartilhado tipo utils/status.ts.
const STATUS_LABEL: Record<string, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

function formatarData(data: string | null): string | null {
  if (!data) return null;
  return new Date(data).toLocaleDateString('pt-BR');
}

export default function Projeto() {
  // :id vem da rota "/projeto/:id" (App.tsx)
  const { id } = useParams<{ id: string }>();

  const [projeto, setProjeto] = useState<ProjetoDetalhe | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    // Sem id não há o que buscar — cai no "if (!id)" abaixo, sem tocar
    // nesses estados (setState direto no corpo do efeito é evitado de
    // propósito: dispara um render em cascata que o React recomenda não fazer).
    if (!id) return;

    buscarProjetoPorId(id)
      .then(setProjeto)
      .catch((erroCapturado) => {
        // ApiError vem do apiClient com a mensagem do backend (ex.: projeto
        // não encontrado); qualquer outro erro aqui é de rede/CORS.
        const mensagem =
          erroCapturado instanceof ApiError
            ? erroCapturado.message
            : 'Não foi possível carregar o projeto. Tente novamente.';
        setErro(mensagem);
      })
      .finally(() => setCarregando(false));
  }, [id]);

  if (!id) {
    return (
      <div className="min-h-screen w-full bg-[#F8F9FA] font-sans p-6">
        <div className="max-w-3xl mx-auto flex flex-col gap-4">
          <Link to="/feed" className="text-sm text-[#1472dd] hover:underline">
            ← Voltar para o feed
          </Link>
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3">
            Projeto não informado na URL.
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-[#F8F9FA] font-sans p-6">
      <div className="max-w-3xl mx-auto flex flex-col gap-4">
        <Link to="/feed" className="text-sm text-[#1472dd] hover:underline">
          ← Voltar para o feed
        </Link>

        {carregando && (
          <p className="text-center text-gray-500 py-10">Carregando projeto...</p>
        )}

        {!carregando && erro && (
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3">
            {erro}
          </div>
        )}

        {!carregando && !erro && projeto && (
          <div className="bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-6 flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <h1 className="text-2xl font-bold text-gray-900">{projeto.titulo}</h1>
              <span className="text-xs font-medium border rounded-full px-3 py-1 whitespace-nowrap bg-gray-100 text-gray-600 border-gray-200">
                {STATUS_LABEL[projeto.status] ?? projeto.status}
              </span>
            </div>

            <p className="text-sm text-gray-600">{projeto.descricao}</p>

            {/* Habilidades necessárias, se o criador cadastrou alguma */}
            {projeto.habilidadesNecessarias.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {projeto.habilidadesNecessarias.map((vinculo) => (
                  <span
                    key={vinculo.id}
                    className="text-xs bg-[#1472dd]/10 text-[#1472dd] rounded-full px-3 py-1"
                  >
                    {vinculo.habilidade.nome}
                    {vinculo.obrigatoria ? ' *' : ''}
                  </span>
                ))}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 text-sm text-gray-500 pt-3 border-t border-gray-100">
              <span>Criador: {projeto.criador?.nome ?? 'Não informado'}</span>
              <span>
                Vagas: {projeto.vagasPreenchidas}/{projeto.vagas} ({projeto.vagasDisponiveis} disponíveis)
              </span>
              <span>Membros na equipe: {projeto.totalMembros}</span>
              <span>Candidaturas pendentes: {projeto.totalCandidaturasPendentes}</span>
              <span>Aceitando candidaturas: {projeto.aceitandoCandidaturas ? 'Sim' : 'Não'}</span>
              {formatarData(projeto.dataFim) && (
                <span>Prazo: {formatarData(projeto.dataFim)}</span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
