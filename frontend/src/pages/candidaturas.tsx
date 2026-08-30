import { Link } from 'react-router-dom';
import { projetos } from '../data/projetosdata';

export default function Candidaturas() {
  return (
    <div className="pb-10 max-w-7xl mx-auto">
      
      {/* Cabeçalho Padronizado */}
      <div className="mb-8">
        {/* MESMA FONTE DO PROJETOS.TSX */}
        <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C]">
          Minhas Candidaturas
        </h1>
        <p className="text-gray-500 text-sm mt-1">2 candidaturas no total</p>
      </div>

      {/* Cards de Status */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">1</p>
          <p className="text-sm font-bold text-orange-500 bg-orange-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">🕒 Pendente</p>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">1</p>
          <p className="text-sm font-bold text-green-600 bg-green-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">✓ Aceito</p>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm text-center">
          <p className="text-3xl font-bold text-gray-900">0</p>
          <p className="text-sm font-bold text-red-500 bg-red-50 w-fit mx-auto px-3 py-1 rounded-full mt-2">✕ Rejeitado</p>
        </div>
      </div>

      {/* Lista de Candidaturas */}
      <div className="flex flex-col gap-6">
        {projetos.map((proj) => (
          <Link key={proj.id} to={`/detalhes/${proj.id}`} className="block bg-white p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md hover:border-[#F27405]/30 transition-all">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="font-bold text-lg text-[#183E6C] hover:text-[#F27405] transition-colors">{proj.titulo}</h3>
                <p className="text-sm font-medium text-gray-500">{proj.autor}</p>
              </div>
              <span className="text-xs font-bold text-orange-500 bg-orange-50 px-3 py-1 rounded-full shrink-0">{proj.status}</span>
            </div>
            
            <div className="bg-gray-50 p-4 rounded-xl mt-4 border border-gray-100">
              <p className="text-xs font-bold text-gray-400 mb-2 uppercase tracking-wider">Sua Mensagem</p>
              <p className="text-sm text-gray-600 italic">Gostaria de contribuir com este projeto pois tenho experiência...</p>
            </div>
            
            <div className="flex justify-end mt-4">
               <span className="text-xs font-bold text-[#F27405] hover:underline">Ver detalhes do projeto →</span>
            </div>
          </Link>
        ))}
      </div>
      
    </div>
  )
}