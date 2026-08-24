import { Link } from 'react-router-dom'

interface ProjetoProps {
  projeto: {
    id: number;
    titulo: string;
    descricao: string;
    vagas: string;
    tags: string[];
    autor: string;
    status: string;
  }
}

export default function CardProjeto({ projeto }: ProjetoProps) {
  // Define a cor da bolinha dependendo do status do projeto
  const statusColor = projeto.status === 'Aberto' ? 'text-green-600 bg-green-50' : 
                      projeto.status === 'Em andamento' ? 'text-orange-600 bg-orange-50' : 
                      'text-gray-600 bg-gray-100';

  return (
    <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between hover:shadow-md transition-shadow">
      <div>
        <div className="flex justify-between items-start mb-2 gap-2">
          <h3 className="font-bold text-lg text-[#183E6C] leading-tight">{projeto.titulo}</h3>
          <span className={`text-[10px] px-2 py-1 rounded-full font-bold uppercase whitespace-nowrap ${statusColor}`}>
            ● {projeto.status}
          </span>
        </div>
        <p className="text-sm text-gray-500 mb-4 line-clamp-2">{projeto.descricao}</p>
        <div className="flex flex-wrap gap-2 mb-6">
          {projeto.tags.map(tag => (
            <span key={tag} className="text-xs bg-gray-100 text-gray-600 px-2.5 py-1 rounded-md font-medium">
              {tag}
            </span>
          ))}
        </div>
      </div>
      
      <div className="flex justify-between items-center border-t border-gray-50 pt-4 mt-auto">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-[#183E6C] text-white flex justify-center items-center text-xs font-bold">
            {projeto.autor.charAt(0)}
          </div>
          <span className="text-sm font-medium text-gray-600">{projeto.autor}</span>
        </div>
        <Link to="/detalhes" className="text-sm font-bold text-[#F27405] hover:underline flex items-center gap-1">
          Ver detalhes
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path></svg>
        </Link>
      </div>
    </div>
  )
}