import { Link } from 'react-router-dom'
import CaixaCandidaturat from '../components/caixacandidatura'

export default function ProjetoDetalhes() {
  return (
    <div className="flex flex-col gap-8 pb-10 max-w-6xl mx-auto">
      
      {/* Botão de Voltar para o Feed */}
      <div>
        <Link to="/feed" className="inline-flex items-center gap-2 text-gray-500 hover:text-[#183E6C] font-medium text-sm transition-colors">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
          Voltar para o Feed
        </Link>
      </div>

      {/* Grid Principal dividida em colunas */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Lado Esquerdo: Informações do Projeto (Ocupa 2 colunas) */}
        <div className="lg:col-span-2 flex flex-col gap-6">
          
          {/* Card de Cabeçalho do Projeto */}
          <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 flex flex-col gap-4 relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-1.5 bg-[#F27405]"></div>
            
            <div className="flex flex-wrap items-center justify-between gap-4">
              <span className="bg-blue-50 text-[#183E6C] text-xs font-bold px-3 py-1 rounded-full">
                3 vagas disponíveis
              </span>
              <span className="text-gray-400 text-sm">Publicado recentemente</span>
            </div>

            <h1 className="text-3xl font-bold text-[#183E6C] leading-tight">
              Sistema Inteligente de Proteção Web
            </h1>

            <div className="flex items-center gap-3 pt-2">
              <div className="w-10 h-10 rounded-full bg-[#183E6C] text-white flex items-center justify-center font-bold text-sm">
                L
              </div>
              <div>
                <p className="text-sm font-semibold text-gray-900">Luana Caldas</p>
                <p className="text-xs text-gray-500">Engenharia de Software • UNIFAPI</p>
              </div>
            </div>
          </div>

          {/* Descrição Completa */}
          <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 flex flex-col gap-4">
            <h2 className="text-xl font-bold text-[#183E6C]">Sobre o Projeto</h2>
            <p className="text-gray-600 leading-relaxed">
              Este projeto propõe o desenvolvimento de um sistema inteligente voltado para a segurança online, utilizando análise de comportamento visual, técnicas de eye-tracking e identificação avançada de conteúdo para proteger os usuários durante a navegação.
            </p>
            <p className="text-gray-600 leading-relaxed">
              Buscamos colaboradores engajados para atuar em conjunto na validação dos testes, modelagem de dados e refinamento da interface de usuário da aplicação acadêmica.
            </p>

            <h3 className="text-lg font-bold text-[#183E6C] mt-4">Tecnologias e Tags</h3>
            <div className="flex flex-wrap gap-2">
              <span className="bg-gray-100 text-gray-700 text-xs px-3 py-1.5 rounded-lg font-medium">Inteligência Artificial</span>
              <span className="bg-gray-100 text-gray-700 text-xs px-3 py-1.5 rounded-lg font-medium">Segurança</span>
              <span className="bg-gray-100 text-gray-700 text-xs px-3 py-1.5 rounded-lg font-medium">Visão Computacional</span>
            </div>
          </div>

        </div>

        {/* Lado Direito: Caixa de Candidatura e Requisitos (Ocupa 1 coluna) */}
        <div className="flex flex-col gap-6">
          
          {/* Aqui entra o componente que você criou */}
          <CaixaCandidaturat />

          {/* Card de Requisitos */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col gap-4">
            <h3 className="text-lg font-bold text-[#183E6C]">Requisitos</h3>
            <ul className="flex flex-col gap-3 text-sm text-gray-600">
              <li className="flex items-start gap-2">
                <svg className="w-5 h-5 text-[#F27405] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                Estar matriculado em cursos afins à tecnologia ou design.
              </li>
              <li className="flex items-start gap-2">
                <svg className="w-5 h-5 text-[#F27405] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                Disponibilidade para reuniões quinzenais.
              </li>
            </ul>
          </div>

        </div>

      </div>
    </div>
  )
}