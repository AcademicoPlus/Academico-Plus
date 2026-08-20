export default function FiltrosBusca() {
  return (
    <div className="flex flex-col md:flex-row gap-4 mb-8 w-full">
      <input 
        type="text" 
        placeholder="Buscar projetos..." 
        className="flex-1 p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405] focus:ring-1 focus:ring-[#F27405] transition-all" 
      />
      <div className="flex gap-4 w-full md:w-auto">
        <select className="flex-1 md:w-40 p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405] text-sm text-gray-600">
          <option>Todos os status</option>
          <option>Aberto</option>
          <option>Em andamento</option>
        </select>
        <select className="flex-1 md:w-48 p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405] text-sm text-gray-600">
          <option>Todas as habilidades</option>
          <option>UI/UX</option>
          <option>Inteligência Artificial</option>
        </select>
      </div>
    </div>
  )
}