export default function CaixaCandidaturat() {
  return (
    <div className="bg-gray-50 rounded-2xl p-6 border border-gray-200 mt-6 shadow-inner">
      <h4 className="font-bold text-[#183E6C] mb-2">Mensagem de Candidatura</h4>
      <p className="text-sm text-gray-500 mb-4">Apresente-se brevemente para o criador do projeto. Diga como pode contribuir.</p>
      <textarea 
        rows={3}
        placeholder="Olá! Gostaria de contribuir com este projeto pois tenho experiência em..."
        className="w-full p-3 bg-white border border-gray-200 rounded-xl outline-none focus:border-[#F27405] focus:ring-1 focus:ring-[#F27405] resize-none text-sm transition-all"
      ></textarea>
      <div className="flex justify-end mt-4">
        <button className="bg-[#183E6C] text-white px-6 py-2.5 rounded-xl font-bold text-sm hover:bg-[#102a4a] transition-colors w-full md:w-auto shadow-md">
          Confirmar Envio
        </button>
      </div>
    </div>
  )
}