import logo from '../assets/logo.png'

export default function Header() {
  return (
    <header className="bg-white h-16 border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-50">
      <div className="flex items-center gap-4">
        {/* Botão de menu que só aparece no celular */}
        <button className="text-gray-500 hover:text-[#F27405] transition-colors lg:hidden">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
        </button>
        <img src={logo} alt="Acadêmico+" className="h-8 object-contain" />
      </div>
      
      {/* Ícone de Notificação */}
      <button className="text-gray-400 hover:text-[#183E6C] transition-colors relative p-2">
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
        <span className="absolute top-1.5 right-1.5 bg-[#F27405] w-2.5 h-2.5 rounded-full border-2 border-white"></span>
      </button>
    </header>
  )
}