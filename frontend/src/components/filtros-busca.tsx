export default function FiltrosBusca() {
  return (
    <div className="flex flex-col md:flex-row gap-4 mb-8 w-full">
      <input
        type="text"
        placeholder="Buscar projetos..."
        className="flex-1 p-3 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 rounded-xl outline-none focus:border-[#F27405] focus:ring-1 focus:ring-[#F27405] transition-all"
      />
      <div className="flex gap-4 w-full md:w-auto">
        <select className="flex-1 md:w-40 p-3 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 rounded-xl outline-none focus:border-[#F27405] text-sm text-gray-600 dark:text-gray-300">
          <option>Todos os status</option>
          <option>Aberto</option>
          <option>Em andamento</option>
        </select>
        <select className="flex-1 md:w-48 p-3 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 rounded-xl outline-none focus:border-[#F27405] text-sm text-gray-600 dark:text-gray-300">
          <option>Todas as habilidades</option>
          <option>UI/UX</option>
          <option>Inteligência Artificial</option>
        </select>
      </div>
    </div>
  )
}