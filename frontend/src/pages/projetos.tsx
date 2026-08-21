import { Link } from 'react-router-dom'
import { projetos } from '../data/projetosdata'

export default function Projetos() {
  return (
    <div className="pb-10">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">Feed de Projetos</h1>
          <p className="text-gray-500 text-sm">{projetos.length} projetos encontrados</p>
        </div>
        <button className="bg-[#F27405] text-white px-6 py-2.5 rounded-xl font-bold w-full md:w-auto shadow-md shadow-[#F27405]/20 hover:bg-[#D96704] transition-colors">
          + Criar projeto
        </button>
      </div>

      {/* Barra de Pesquisa */}
      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <input type="text" placeholder="Buscar projetos..." className="flex-1 p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405]" />
        <select className="p-3 bg-white border border-gray-200 rounded-xl outline-none">
          <option>Todos os status</option>
        </select>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {projetos.map((proj) => (
          <div key={proj.id} className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex justify-between items-start mb-2">
                <h3 className="font-bold text-lg text-[#183E6C]">{proj.titulo}</h3>
                <span className="text-[10px] bg-green-50 text-green-600 px-2 py-1 rounded-full font-bold uppercase">{proj.status}</span>
              </div>
              <p className="text-sm text-gray-500 mb-4 line-clamp-2">{proj.descricao}</p>
              <div className="flex flex-wrap gap-2 mb-6">
                {proj.tags.map(tag => <span key={tag} className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-md">{tag}</span>)}
              </div>
            </div>
            
            <div className="flex justify-between items-center border-t border-gray-50 pt-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-[#183E6C] text-white flex justify-center items-center text-xs font-bold">{proj.autor[0]}</div>
                <span className="text-sm text-gray-600">{proj.autor}</span>
              </div>
              
              {/* O Link correto passando o ID dinâmico do projeto */}
              <Link to={`/detalhes/${proj.id}`} className="text-sm font-bold text-[#F27405] hover:underline">
                Ver detalhes →
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}