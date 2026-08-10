export default function FiltrosBusca() {
  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
      <div>
        <h1 className="text-2xl font-bold text-[#183E6C]">Explorar Projetos</h1>
        <p className="text-gray-500 text-sm mt-1">Encontre a equipe ideal para o seu perfil</p>
      </div>
      
      <div className="flex w-full md:w-auto gap-3">
        <div className="relative flex-1 md:w-80">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
            <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
          </div>
          <input 
            type="text" 
            placeholder="Buscar por título, tag ou curso..." 
            className="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-transparent rounded-xl focus:bg-white focus:border-[#F27405] focus:ring-2 focus:ring-[#F27405]/20 outline-none transition-all text-sm text-gray-700"
          />
        </div>
        <button className="bg-gray-100 hover:bg-gray-200 text-[#183E6C] p-2.5 rounded-xl transition-colors flex items-center justify-center" title="Filtros">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path></svg>
        </button>
      </div>
    </div>
  )
}