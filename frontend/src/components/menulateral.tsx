import { Link, useLocation } from 'react-router-dom'

export default function MenuLateral() {
  const location = useLocation(); // Ferramenta para saber em qual tela estamos
  
  return (
    <aside className="w-64 bg-white border-r border-gray-200 h-[calc(100vh-5rem)] sticky top-20 flex flex-col justify-between py-6">
      
      {/* Links Principais */}
      <nav className="flex flex-col gap-2 px-4">
        
        <Link to="/feed" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${location.pathname === '/feed' ? 'bg-[#183E6C] text-white shadow-md' : 'text-gray-500 hover:bg-gray-50 hover:text-[#F27405]'}`}>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
          <span className="font-medium">Explorar Projetos</span>
        </Link>

        <Link to="/projeto" className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${location.pathname === '/projeto' ? 'bg-[#183E6C] text-white shadow-md' : 'text-gray-500 hover:bg-gray-50 hover:text-[#F27405]'}`}>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002 2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
          <span className="font-medium">Detalhes (Teste)</span>
        </Link>
        
      </nav>
      
      {/* Botão de Sair no final */}
      <div className="px-4">
        <Link to="/login" className="flex items-center gap-3 px-4 py-3 rounded-xl text-red-500 hover:bg-red-50 transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
          <span className="font-medium">Sair</span>
        </Link>
      </div>

    </aside>
  )
}