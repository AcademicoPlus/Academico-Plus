import { Link, useLocation } from 'react-router-dom'
import { projetos } from '../data/projetosdata'

export default function MenuLateral() {
  const location = useLocation();
  const activeClass = "bg-[#183E6C] text-white shadow-md";
  const inactiveClass = "text-gray-600 hover:bg-gray-50 hover:text-[#F27405]";

  return (
    <aside className="hidden lg:flex w-72 bg-white border-r border-gray-100 h-[calc(100vh-4rem)] flex-col justify-between overflow-y-auto">
      <div className="py-6">
        <nav className="flex flex-col gap-2 px-4">
          
          {/* Dashboard */}
          <Link to="/dashboard" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium ${location.pathname === '/dashboard' ? activeClass : inactiveClass}`}>
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
            </svg>
            Dashboard
          </Link>

          {/* Projetos */}
          <Link to="/projetos" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium ${location.pathname === '/projetos' ? activeClass : inactiveClass}`}>
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002 2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
            </svg>
            Projetos
          </Link>

          {/* Explorar Pessoas */}
          <Link to="/pessoas" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium ${location.pathname === '/pessoas' ? activeClass : inactiveClass}`}>
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
            Explorar Pessoas
          </Link>

          {/* Candidaturas */}
          <Link to="/candidaturas" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium ${location.pathname === '/candidaturas' ? activeClass : inactiveClass}`}>
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            Candidaturas
          </Link>

          {/* Criar Projeto */}
          <Link to="/criar-projeto" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium ${location.pathname === '/criar-projeto' ? activeClass : inactiveClass}`}>
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path>
            </svg>
            Criar Projeto
          </Link>
        </nav>

        {/* Meus Projetos (Dinamicos) */}
        <div className="px-8 mt-10">
          <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-4">Meus Projetos</p>
          <ul className="flex flex-col gap-4">
            {projetos.map(proj => (
              <li key={proj.id}>
                <Link to={`/detalhes/${proj.id}`} className="flex items-center gap-3 text-sm text-gray-500 hover:text-[#F27405] transition-colors">
                  <div className="w-2 h-2 rounded-full bg-[#183E6C] shrink-0"></div>
                  <span className="truncate">{proj.titulo}</span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Perfil na Base do Menu */}
      <div className="p-6 border-t border-gray-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#183E6C] text-white flex items-center justify-center font-bold text-sm">
            LC
          </div>
          <div>
            <p className="text-sm font-bold text-gray-900">Luana Caldas</p>
            <p className="text-xs text-gray-500">Engenharia de Software</p>
          </div>
        </div>
      </div>
    </aside>
  )
}