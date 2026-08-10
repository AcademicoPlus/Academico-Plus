import { Link } from 'react-router-dom'

// Aqui definimos o formato dos dados que o card vai receber
interface ProjetoProps {
  projeto: {
    id: number;
    titulo: string;
    descricao: string;
    autor: string;
    curso: string;
    vagas: number;
    tags: string[];
  }
}

export default function CardProjeto({ projeto }: ProjetoProps) {
  return (
    <div className="bg-white rounded-2xl p-6 shadow-[0_4px_20px_rgb(0,0,0,0.03)] border border-gray-100 flex flex-col hover:shadow-md transition-shadow cursor-default relative overflow-hidden">
      
      {/* Detalhezinho laranja no topo do card */}
      <div className="absolute top-0 left-0 w-full h-1 bg-[#F27405]"></div>

      <div className="flex justify-between items-start mb-4">
        <span className="bg-blue-50 text-[#183E6C] text-xs font-bold px-3 py-1 rounded-full">
          {projeto.vagas} {projeto.vagas === 1 ? 'vaga' : 'vagas'}
        </span>
        <button className="text-gray-300 hover:text-[#F27405] transition-colors">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>
        </button>
      </div>
      
      <h3 className="text-xl font-bold text-[#183E6C] mb-2 leading-tight">
        {projeto.titulo}
      </h3>
      
      <p className="text-gray-500 text-sm mb-6 flex-1 line-clamp-3">
        {projeto.descricao}
      </p>
      
      <div className="flex flex-wrap gap-2 mb-6">
        {projeto.tags.map((tag, index) => (
          <span key={index} className="bg-gray-100 text-gray-600 text-xs px-2.5 py-1 rounded-md font-medium">
            {tag}
          </span>
        ))}
      </div>
      
      <div className="flex items-center justify-between border-t border-gray-100 pt-4 mt-auto">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-[#183E6C] text-white flex items-center justify-center font-bold text-xs">
            {projeto.autor.charAt(0)}
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900">{projeto.autor}</p>
            <p className="text-xs text-gray-500">{projeto.curso}</p>
          </div>
        </div>
        
        {/* Link que leva para a tela de Detalhes do Projeto */}
        <Link to="/projeto" className="text-[#F27405] hover:bg-orange-50 p-2 rounded-lg transition-colors" title="Ver detalhes">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path></svg>
        </Link>
      </div>
    </div>
  )
}