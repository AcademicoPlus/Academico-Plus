import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { recomendarProjetos, type ProjetoRecomendado } from '../services/recomendacaoService'
import { listarMeusProjetos, type Projeto } from '../services/projetoService'
import { useMeuPerfil } from '../hooks/useMeuPerfil'
import { listarMinhasCandidaturas } from '../services/candidaturaService'
import { STATUS_PROJETO_BADGE, STATUS_PROJETO_LABEL } from '../utils/projeto'

export default function Dashboard() {
  const { data: meuPerfil } = useMeuPerfil();
  const [meusProjetos, setMeusProjetos] = useState<Projeto[]>([]);
  const [carregandoMeusProjetos, setCarregandoMeusProjetos] = useState(true);
  const [recomendados, setRecomendados] = useState<ProjetoRecomendado[]>([]);
  const [carregandoRecomendados, setCarregandoRecomendados] = useState(true);
  const [totalCandidaturasPendentes, setTotalCandidaturasPendentes] = useState<number | null>(null);

  useEffect(() => {
    listarMeusProjetos({ tamanho: 3 })
      .then((pagina) => setMeusProjetos(pagina.content))
      .catch(() => {})
      .finally(() => setCarregandoMeusProjetos(false));

    recomendarProjetos(5)
      .then(setRecomendados)
      .catch(() => {})
      .finally(() => setCarregandoRecomendados(false));

    listarMinhasCandidaturas({ status: 'PENDENTE', tamanho: 1 })
      .then((pagina) => setTotalCandidaturasPendentes(pagina.totalElements))
      .catch(() => {});
  }, []);

  const primeiroNome = meuPerfil?.nome.split(' ')[0];

  return (
    <div className="flex flex-col gap-6 pb-10 max-w-7xl mx-auto">
      
      {/* Cabeçalho Padronizado */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-4 gap-4">
        <div>
          {/* MESMA FONTE DO PROJETOS.TSX */}
          <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">
            Olá{primeiroNome ? `, ${primeiroNome}` : ''} 👋
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Bem-vindo(a) de volta ao Acadêmico+
          </p>
        </div>
        <Link to="/criar-projeto" className="text-sm bg-[#183E6C] text-white px-5 py-2.5 rounded-xl font-bold hover:bg-blue-900 transition-colors shadow-sm">
          + Novo Projeto
        </Link>
      </div>

      {/* Métricas Minimalistas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { label: 'Projetos ativos', value: '1', border: 'border-blue-200' },
          { label: 'Candidaturas', value: totalCandidaturasPendentes?.toString() ?? '—', border: 'border-orange-200' },
          { label: 'Recomendações', value: recomendados.length.toString(), border: 'border-green-200' },
        ].map((item, i) => (
          <div key={i} className={`bg-white p-5 rounded-2xl border-l-4 ${item.border} border-y border-r border-gray-100 shadow-sm flex items-center justify-between`}>
            <div>
              <p className="text-sm font-bold text-gray-400 uppercase tracking-wide">{item.label}</p>
              <p className="text-2xl font-bold text-[#183E6C] mt-1">{item.value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Grid Principal */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-2">
        
        {/* Coluna Principal: Meus Projetos */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-bold text-[#183E6C]">Meus Projetos</h2>
            <Link to="/projetos" className="text-sm text-[#F27405] font-bold hover:underline">Ver todos</Link>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-2">
            {carregandoMeusProjetos ? (
              <p className="text-sm text-gray-400 p-5">Carregando...</p>
            ) : meusProjetos.length === 0 ? (
              <div className="p-8 text-center text-sm text-gray-500">
                Você ainda não criou nenhum projeto.<br/><br/>
                <Link to="/criar-projeto" className="text-[#F27405] font-bold hover:underline">Criar o primeiro →</Link>
              </div>
            ) : (
              <div className="divide-y divide-gray-50">
                {meusProjetos.map((proj) => {
                  const percentual = proj.vagas > 0 ? Math.round((proj.vagasPreenchidas / proj.vagas) * 100) : 0;
                  return (
                    <Link key={proj.id} to={`/detalhes/${proj.id}`} className="p-5 block hover:bg-gray-50 rounded-xl transition-colors">
                      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
                        <div className="flex-1">
                          <h3 className="font-bold text-[#183E6C] text-base">{proj.titulo}</h3>
                          <div className="flex flex-wrap gap-2 mt-2">
                            {proj.habilidadesNecessarias.slice(0, 3).map((h) => (
                              <span key={h.id} className="text-[10px] bg-blue-50 text-[#183E6C] px-2 py-1 rounded font-bold border border-blue-100">
                                {h.habilidade.nome}
                              </span>
                            ))}
                          </div>
                        </div>
                        
                        <div className="flex flex-col items-end shrink-0 w-32">
                          <span className={`text-[10px] px-2.5 py-1 rounded-full font-bold uppercase ${STATUS_PROJETO_BADGE[proj.status]}`}>
                            {STATUS_PROJETO_LABEL[proj.status]}
                          </span>
                          <div className="w-full bg-gray-100 h-1.5 rounded-full mt-3">
                            <div className="bg-[#183E6C] h-full rounded-full" style={{ width: `${percentual}%` }}></div>
                          </div>
                          <p className="text-[10px] text-gray-400 mt-1 font-medium">{proj.vagasPreenchidas}/{proj.vagas} vagas</p>
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Coluna Lateral: Recomendações */}
        <div className="flex flex-col gap-4">
          <h2 className="text-lg font-bold text-[#183E6C]">Recomendado para você</h2>
          
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden divide-y divide-gray-50">
            {carregandoRecomendados ? (
              <p className="p-5 text-sm text-gray-400">Buscando oportunidades...</p>
            ) : recomendados.length === 0 ? (
              <p className="p-5 text-sm text-gray-400 text-center">Nenhuma recomendação baseada nas suas habilidades.</p>
            ) : (
              recomendados.map(({ projeto }) => (
                <Link key={projeto.id} to={`/detalhes/${projeto.id}`} className="p-5 block hover:bg-gray-50 transition-colors group">
                  <h4 className="font-bold text-sm text-[#183E6C] group-hover:text-[#F27405] transition-colors leading-tight line-clamp-2">
                    {projeto.titulo}
                  </h4>
                  <div className="flex justify-between items-center mt-3">
                    <p className="text-xs text-gray-500 font-medium truncate pr-2">{projeto.criador?.nome ?? 'Autor desconhecido'}</p>
                    <span className="text-[10px] font-bold text-green-600 bg-green-50 border border-green-100 px-2 py-1 rounded whitespace-nowrap">
                      Visualizar
                    </span>
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>

      </div>
    </div>
  )
}