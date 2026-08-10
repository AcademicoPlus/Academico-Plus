export default function CaixaCandidaturat() {
  return (
    <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col gap-4">
      <h3 className="text-xl font-bold text-[#183E6C]">Inscreva-se na vaga</h3>
      <p className="text-gray-500 text-sm">
        Envie uma breve mensagem destacando suas habilidades e por que deseja participar deste projeto.
      </p>
      
      <div className="flex flex-col gap-2">
        <label className="text-sm font-medium text-[#183E6C]">Mensagem para o criador</label>
        <textarea 
          rows={4}
          placeholder="Olá! Tenho experiência com desenvolvimento e gostaria de ajudar na equipe..."
          className="w-full p-3 bg-gray-50 border border-transparent rounded-xl focus:bg-white focus:border-[#F27405] focus:ring-2 focus:ring-[#F27405]/20 outline-none transition-all text-sm text-gray-700 resize-none"
        ></textarea>
      </div>

      <button 
        type="button" 
        onClick={() => alert("Candidatura enviada com sucesso! O autor entrará em contato.")}
        className="w-full bg-[#F27405] hover:bg-[#D96704] text-white font-bold py-3.5 rounded-xl transition-colors shadow-lg shadow-[#F27405]/30 cursor-pointer"
      >
        Enviar Candidatura
      </button>
    </div>
  )
}