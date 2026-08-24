// Tela de Feed conectada ao backend: busca a lista real de projetos em
// GET /projetos (ProjetoController) e renderiza um card por projeto.
// Serve de exemplo pro grupo de como conectar uma tela que só lista dados
// (sem formulário) — o padrão é o mesmo do cadastro.tsx: service -> useEffect
// -> useState -> JSX. Ver docs/CONECTANDO_FRONT_BACKEND.md para o passo a
// passo completo.
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../services/apiClient';
import { listarProjetos, type Projeto, type StatusProjeto } from '../services/projetoService';
import { logout } from '../services/usuarioService';
import { removerToken } from '../utils/auth';

// Label em português pra cada valor do enum StatusProjeto do backend
// (o JSON manda "ABERTO", "EM_ANDAMENTO" etc., não o texto já traduzido).
const STATUS_LABEL: Record<StatusProjeto, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

// Cor do badge de status, no mesmo estilo dos cards (borda + fundo clarinho).
const STATUS_COR: Record<StatusProjeto, string> = {
  ABERTO: 'bg-green-50 text-green-700 border-green-200',
  EM_ANDAMENTO: 'bg-blue-50 text-blue-700 border-blue-200',
  CONCLUIDO: 'bg-gray-100 text-gray-600 border-gray-200',
  CANCELADO: 'bg-red-50 text-red-600 border-red-200',
};

function formatarData(data: string | null): string | null {
  if (!data) return null;
  return new Date(data).toLocaleDateString('pt-BR');
}

export default function Feed() {
  const navigate = useNavigate();

  const [projetos, setProjetos] = useState<Projeto[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // Carrega os projetos uma vez, assim que a tela abre (mesmo padrão da
  // lista de cursos em cadastro.tsx).
  useEffect(() => {
    listarProjetos()
      .then((pagina) => setProjetos(pagina.content))
      .catch((erroCapturado) => {
        // ApiError vem do apiClient com a mensagem que o backend retornou;
        // qualquer outro erro aqui é de rede/CORS/backend fora do ar.
        const mensagem =
          erroCapturado instanceof ApiError
            ? erroCapturado.message
            : 'Não foi possível carregar os projetos. Tente novamente.';
        setErro(mensagem);
      })
      .finally(() => setCarregando(false));
  }, []);

  // Rota é protegida (RotaProtegida em App.tsx), então sem token o usuário
  // nem chega aqui — mas ele ainda precisa de um jeito de sair e trocar de
  // conta, então o Feed cuida do logout até existir um header compartilhado.
  async function handleSair() {
    try {
      // Revoga o token no backend (POST /usuarios/logout). Mesmo se essa
      // chamada falhar (token já expirado, backend fora do ar), ainda
      // limpamos o token local abaixo — o usuário não pode ficar preso
      // numa tela que exige token sem conseguir sair.
      await logout();
    } catch {
      // Erro intencionalmente ignorado — ver comentário acima.
    }
    removerToken();
    navigate('/login');
  }

  return (
    <div className="min-h-screen w-full bg-[#F8F9FA] font-sans">
      {/* Barra superior */}
      <header className="bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Projetos</h1>
          <p className="text-sm text-gray-500">Projetos abertos para colaboração</p>
        </div>
        <button
          onClick={handleSair}
          className="text-sm font-medium text-gray-500 hover:text-[#1472dd] transition-colors"
        >
          Sair
        </button>
      </header>

      <main className="max-w-5xl mx-auto p-6">
        {/* Estado de carregamento */}
        {carregando && (
          <p className="text-center text-gray-500 py-10">Carregando projetos...</p>
        )}

        {/* Estado de erro (backend fora do ar, CORS, etc.) */}
        {!carregando && erro && (
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3">
            {erro}
          </div>
        )}

        {/* Lista vazia: backend respondeu certo, só não tem projeto ativo ainda */}
        {!carregando && !erro && projetos.length === 0 && (
          <p className="text-center text-gray-500 py-10">Nenhum projeto encontrado.</p>
        )}

        {/* Grade de cards, um por projeto */}
        {!carregando && !erro && projetos.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {projetos.map((projeto) => (
              // Link pra GET /projetos/:id (tela de detalhes) — o id do
              // projeto vira parâmetro de rota, lido em projetodetalhes.tsx.
              <Link
                to={`/projeto/${projeto.id}`}
                key={projeto.id}
                className="bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-6 flex flex-col gap-3"
              >
                <div className="flex items-start justify-between gap-3">
                  <h2 className="text-lg font-bold text-gray-900">{projeto.titulo}</h2>
                  <span
                    className={`text-xs font-medium border rounded-full px-3 py-1 whitespace-nowrap ${STATUS_COR[projeto.status]}`}
                  >
                    {STATUS_LABEL[projeto.status]}
                  </span>
                </div>

                <p className="text-sm text-gray-500 line-clamp-3">{projeto.descricao}</p>

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

                <div className="flex items-center justify-between text-sm text-gray-500 mt-2 pt-3 border-t border-gray-100">
                  <span>{projeto.criador?.nome ?? 'Criador não informado'}</span>
                  <span>
                    {projeto.vagasPreenchidas}/{projeto.vagas} vagas
                  </span>
                </div>

                {formatarData(projeto.dataFim) && (
                  <p className="text-xs text-gray-400">
                    Prazo: {formatarData(projeto.dataFim)}
                  </p>
                )}
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
