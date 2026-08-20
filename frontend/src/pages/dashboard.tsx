import { Link } from 'react-router-dom'
import { projetos } from '../data/projetosdata'

export default function Dashboard() {
  return (
    <div className="flex flex-col gap-8 pb-10">
      {/* Cabeçalho */}
      <div>
        <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">Olá, Luana 👋</h1>
        <p className="text-gray-500 text-sm md:text-base">Bem-vinda de volta ao Acadêmico+</p>
      </div>

      {/* Cards de Resumo (No celular fica um embaixo do outro, no PC ficam 3 lado a lado) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-6">
        {[
          { label: 'Projetos ativos', value: '1', iconColor: 'text-[#183E6C]', bg: 'bg-blue-50' },
          { label: 'Candidaturas pendentes', value: '1', iconColor: 'text-[#F27405]', bg: 'bg-orange-50' },
          { label: 'Projetos recomendados', value: projetos.length.toString(), iconColor: 'text-green-600', bg: 'bg-green-50' },
        ].map((item, i) => (
          <div key={i} className="bg-white p-5 md:p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
            <div className={`p-3 rounded-xl ${item.bg} ${item.iconColor}`}>
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{item.value}</p>
              <p className="text-sm text-gray-500">{item.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Meus Projetos */}
      <div>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold text-[#183E6C]">Meus Projetos</h2>
          <Link to="/projetos" className="text-sm text-[#F27405] font-semibold hover:underline">Ver todos</Link>
        </div>
        <div className="bg-white p-5 md:p-6 rounded-2xl shadow-sm border border-gray-100">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-2 mb-2">
            <div>
              <h3 className="font-bold text-[#183E6C]">Sistema Inteligente de Proteção Web</h3>
              <div className="flex flex-wrap gap-2 mt-2">
                <span className="text-[10px] bg-blue-50 text-[#183E6C] px-2 py-0.5 rounded-full font-bold">Inteligência Artificial</span>
                <span className="text-[10px] bg-orange-50 text-[#F27405] px-2 py-0.5 rounded-full font-bold">Visão Computacional</span>
              </div>
            </div>
            <span className="text-xs bg-orange-50 text-[#F27405] px-3 py-1 rounded-full font-bold w-fit">Em andamento</span>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full mt-4">
            <div className="bg-[#183E6C] h-2 rounded-full w-[67%]"></div>
          </div>
          <p className="text-xs text-gray-400 mt-2 font-medium">2/3 vagas preenchidas • TCC</p>
        </div>
      </div>

      {/* Projetos Recomendados (Agora puxando do banco de dados e clicáveis!) */}
      <div>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold text-[#183E6C]">Recomendado para o seu perfil</h2>
        </div>
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 divide-y divide-gray-50">
          {projetos.map((proj) => (
            <Link 
              key={proj.id} 
              to={`/detalhes/${proj.id}`} 
              className="p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-2 hover:bg-gray-50 transition-colors"
            >
              <p className="font-medium text-gray-700 text-sm md:text-base">{proj.titulo}</p>
              <span className="bg-green-50 text-green-600 text-xs font-bold px-3 py-1 rounded-lg w-fit">
                Ver detalhes →
              </span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}