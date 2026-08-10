import logo from '../assets/logo.png'

export default function Header() {
  return (
    <header className="bg-white h-20 border-b border-gray-200 flex items-center justify-between px-8 sticky top-0 z-50 shadow-sm">
      
      {/* Lado Esquerdo: Logo */}
      <div className="flex items-center">
        <img src={logo} alt="Logo Acadêmico+" className="h-10 object-contain" />
      </div>
      
      {/* Lado Direito: Notificações e Perfil */}
      <div className="flex items-center gap-6">
        
        {/* Ícone de Sino (Notificação) */}
        <button className="text-gray-400 hover:text-[#F27405] transition-colors relative">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
          </svg>
          {/* Bolinha vermelha/laranja de notificação não lida */}
          <span className="absolute -top-1 -right-1 bg-[#F27405] w-3 h-3 rounded-full border-2 border-white"></span>
        </button>
        
        {/* Divisória e Perfil do Usuário */}
        <div className="flex items-center gap-3 border-l border-gray-200 pl-6 cursor-pointer hover:opacity-80 transition-opacity">
          <div className="text-right hidden md:block">
            <p className="text-sm font-bold text-[#183E6C]">Luana Caldas</p>
            <p className="text-xs text-gray-500">Aluna</p>
          </div>
          {/* Avatar (Iniciais) */}
          <div className="w-10 h-10 rounded-full bg-[#183E6C] text-white flex items-center justify-center font-bold text-sm shadow-inner">
            LC
          </div>
        </div>

      </div>
    </header>
  )
}