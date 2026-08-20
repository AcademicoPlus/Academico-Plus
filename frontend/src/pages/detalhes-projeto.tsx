import { useState } from 'react'
import { Link } from 'react-router-dom'

export default function DetalhesProjeto() {
  // Lógica do botão de candidatura
  const [candidatado, setCandidatado] = useState(false);

  return (
    <div className="max-w-4xl mx-auto pb-10">
      <Link to="/projetos" className="inline-block mb-6 text-gray-500 hover:text-[#F27405] text-sm font-medium transition-colors">
        ← Voltar para projetos
      </Link>

      <div className="bg-white rounded-3xl p-6 md:p-8 border border-gray-100 shadow-sm mb-6">
        <div className="flex flex-col md:flex-row justify-between items-start mb-6 gap-4">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-[#183E6C] leading-tight mb-2">App de Saúde Mental para Universitários</h1>
            <span className="bg-green-100 text-green-700 px-3 py-1 rounded-full text-xs font-bold">● Aberto</span>
          </div>
          
          {/* Botão de Candidatura que muda de cor e texto */}
          <button 
            onClick={() => setCandidatado(!candidatado)}
            className={`w-full md:w-auto px-6 py-3 rounded-xl font-bold transition-all shadow-sm ${
              candidatado 
              ? 'bg-white border-2 border-red-500 text-red-500 hover:bg-red-50' 
              : 'bg-[#F27405] text-white hover:bg-[#D96704] shadow-[#F27405]/20'
            }`}
          >
            {candidatado ? "✕ Cancelar candidatura" : "✓ Candidatar-se"}
          </button>
        </div>

        <p className="text-gray-600 text-sm md:text-base mb-6 leading-relaxed">
          Desenvolvimento de uma aplicação móvel focada no bem-estar e saúde mental de estudantes universitários, com recursos de mindfulness, tracking de humor e conexão com suporte psicológico institucional.
        </p>

        {/* Informações */}
        <div className="bg-gray-50 rounded-2xl p-6 grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Vagas</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">👥 2/5</p>
          </div>
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Prazo</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">📅 15 de mar. de 2025</p>
          </div>
          <div>
            <p className="text-xs text-gray-400 uppercase font-semibold mb-1">Área</p>
            <p className="font-bold text-[#183E6C] flex items-center gap-2">📖 Ciência da Computação</p>
          </div>
        </div>

        {/* Autor */}
        <div className="flex items-center gap-3 pt-6 border-t border-gray-100">
          <div className="w-12 h-12 rounded-full bg-[#183E6C] text-white flex justify-center items-center font-bold">LM</div>
          <div>
            <p className="text-xs text-gray-400">Criado por</p>
            <p className="font-bold text-gray-900">Lucas Mendes</p>
          </div>
        </div>
      </div>

      {/* Membros do Projeto */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-gray-100 shadow-sm">
        <h3 className="font-bold text-[#183E6C] text-lg mb-4">Membros do projeto (2)</h3>
        <div className="flex flex-col md:flex-row gap-4">
          <div className="bg-gray-50 p-3 rounded-xl flex items-center gap-3 w-full md:w-64">
            <div className="w-10 h-10 rounded-full bg-blue-200"></div> 
            <div>
              <p className="text-sm font-bold">Lucas Mendes</p>
              <p className="text-xs text-gray-500">Computação</p>
            </div>
          </div>
          <div className="bg-gray-50 p-3 rounded-xl flex items-center gap-3 w-full md:w-64">
            <div className="w-10 h-10 rounded-full bg-pink-200"></div> 
            <div>
              <p className="text-sm font-bold">Carla Rodrigues</p>
              <p className="text-xs text-gray-500">Psicologia</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}