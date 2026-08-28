import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { recomendarProjetos, type ProjetoRecomendado } from '../services/recomendacaoService'
import { listarMeusProjetos, type Projeto } from '../services/projetoService'
import { obterMeuPerfil } from '../services/usuarioService'
import { STATUS_PROJETO_BADGE, STATUS_PROJETO_LABEL } from '../utils/projeto'

export default function Dashboard() {
  const [nome, setNome] = useState<string | null>(null);
  const [meusProjetos, setMeusProjetos] = useState<Projeto[]>([]);
  const [carregandoMeusProjetos, setCarregandoMeusProjetos] = useState(true);
  const [recomendados, setRecomendados] = useState<ProjetoRecomendado[]>([]);
  const [carregandoRecomendados, setCarregandoRecomendados] = useState(true);

  useEffect(() => {
    obterMeuPerfil()
      .then((perfil) => setNome(perfil.nome))
      .catch(() => {});

    listarMeusProjetos({ tamanho: 3 })
      .then((pagina) => setMeusProjetos(pagina.content))
      .catch(() => {})
      .finally(() => setCarregandoMeusProjetos(false));

    recomendarProjetos(5)
      .then(setRecomendados)
      .catch(() => {})
      .finally(() => setCarregandoRecomendados(false));
  }, []);

  const primeiroNome = nome?.split(' ')[0];

  return (
    <div className="flex flex-col gap-6 pb-10 max-w-7xl mx-auto">
      
      {/* Cabeçalho Compacto */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end border-b border-gray-100 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-[#183E6C]">Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">
            Bem-vindo(a) de volta, {primeiroNome ? primeiroNome : 'ao Acadêmico+'}
          </p>
        </div>
        <Link to="/criar-projeto" className="mt-4 md:mt-0 text-sm bg-[#183E6C] text-white px-5 py-2 rounded-lg font-semibold hover:bg-blue-900 transition-colors">
          + Novo Projeto
        </Link>
      </div>

      {/* Métricas Minimalistas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { label: 'Projetos ativos', value: '1', border: 'border-blue-200' },
          { label: 'Candidaturas', value: '1', border: 'border-orange-200' },
          { label: 'Recomendações', value: recomendados.length.toString(), border: 'border-green-200' },
        ].map((item, i) => (
          <div key={i} className={`bg-white p-5 rounded-xl border-l-4 ${item.border} border-y border-r border-gray-100 shadow-sm flex items-center justify-between`}>
            <div>
              <p className="text-sm font-semibold text-gray-500 uppercase tracking-wide">{item.label}</p>
              <p className="text-2xl font-bold text-[#183E6C] mt-1">{item.value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Grid Principal: 2/3 Esquerda, 1/3 Direita */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-2">
        
        {/* Coluna Principal: Meus Projetos */}
        <div className="lg:col-span-2 flex flex-col gap-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-bold text-[#183E6C] flex items-center gap-2">
              <svg className="w-5 h-5 text-[#F27405]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002 2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
              Meus Projetos
            </h2>
            <Link to="/projetos" className="text-sm text-[#F27405] font-semibold hover:underline">Ver todos</Link>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-1">
            {carregandoMeusProjetos ? (
              <p className="text-sm text-gray-400 p-5">Carregando...</p>
            ) : meusProjetos.length === 0 ? (
              <div className="p-8 text-center text-sm text-gray-500">
                Você ainda não possui projetos em andamento.<br/><br/>
                <Link to="/criar-projeto" className="text-[#F27405] font-semibold hover:underline">Comece criando o primeiro →</Link>
              </div>
            ) : (
              <div className="divide-y divide-gray-50">
                {meusProjetos.map((proj) => {
                  const percentual = proj.vagas > 0 ? Math.round((proj.vagasPreenchidas / proj.vagas) * 100) : 0;
                  return (
                    <Link key={proj.id} to={`/detalhes/${proj.id}`} className="p-5 block hover:bg-gray-50 transition-colors">
                      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
                        <div className="flex-1">
                          <h3 className="font-bold text-gray-800 text-base">{proj.titulo}</h3>
                          <div className="flex flex-wrap gap-2 mt-2">
                            {proj.habilidadesNecessarias.slice(0, 3).map((h) => (
                              <span key={h.id} className="text-[10px] bg-gray-100 text-gray-600 px-2 py-1 rounded border border-gray-200">
                                {h.habilidade.nome}
                              </span>
                            ))}
                            {proj.habilidadesNecessarias.length > 3 && (
                              <span className="text-[10px] text-gray-400 py-1 font-medium">+{proj.habilidadesNecessarias.length - 3}</span>
                            )}
                          </div>
                        </div>
                        
                        <div className="flex flex-col items-end shrink-0 w-32">
                          <span className={`text-[10px] px-2.5 py-1 rounded-full font-bold uppercase ${STATUS_PROJETO_BADGE[proj.status]}`}>
                            {STATUS_PROJETO_LABEL[proj.status]}
                          </span>
                          <div className="w-full bg-gray-100 h-1.5 rounded-full mt-3">
                            <div className="bg-[#183E6C] h-full rounded-full" style={{ width: `${percentual}%` }}></div>
                          </div>
                          <p className="text-[10px] text-gray-400 mt-1">{proj.vagasPreenchidas}/{proj.vagas} vagas</p>
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
          <h2 className="text-lg font-bold text-[#183E6C] flex items-center gap-2">
            <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            Recomendados
          </h2>
          
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden divide-y divide-gray-50">
            {carregandoRecomendados ? (
              <p className="p-5 text-sm text-gray-400">Buscando oportunidades...</p>
            ) : recomendados.length === 0 ? (
              <p className="p-5 text-sm text-gray-400 text-center">Nenhuma recomendação baseada nas suas habilidades.</p>
            ) : (
              recomendados.map(({ projeto }) => (
                <Link key={projeto.id} to={`/detalhes/${projeto.id}`} className="p-4 block hover:bg-gray-50 transition-colors group">
                  <h4 className="font-bold text-sm text-[#183E6C] group-hover:text-[#F27405] transition-colors leading-tight line-clamp-2">
                    {projeto.titulo}
                  </h4>
                  <div className="flex justify-between items-center mt-3">
                    <p className="text-xs text-gray-500 truncate pr-2">{projeto.criador?.nome ?? 'Autor desconhecido'}</p>
                    <span className="text-[10px] font-bold text-green-600 bg-green-50 px-2 py-1 rounded whitespace-nowrap">
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