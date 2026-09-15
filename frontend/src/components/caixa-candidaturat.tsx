type CaixaCandidaturatProps = {
  mensagem: string;
  onChangeMensagem: (valor: string) => void;
  onEnviar: () => void;
  onCancelar: () => void;
  enviando: boolean;
  erro?: string | null;
};

export default function CaixaCandidaturat({
  mensagem,
  onChangeMensagem,
  onEnviar,
  onCancelar,
  enviando,
  erro,
}: CaixaCandidaturatProps) {
  return (
    <div className="bg-gray-50 dark:bg-slate-800 rounded-2xl p-6 border border-gray-200 dark:border-slate-700 mt-6 shadow-inner">
      <h4 className="font-bold text-[#183E6C] dark:text-blue-300 mb-2">Mensagem de Candidatura</h4>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">Apresente-se brevemente para o criador do projeto. Diga como pode contribuir.</p>
      <textarea
        rows={3}
        placeholder="Olá! Gostaria de contribuir com este projeto pois tenho experiência em..."
        value={mensagem}
        onChange={(e) => onChangeMensagem(e.target.value)}
        disabled={enviando}
        className="w-full p-3 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 rounded-xl outline-none focus:border-[#F27405] focus:ring-1 focus:ring-[#F27405] resize-none text-sm text-gray-700 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all disabled:opacity-60"
      ></textarea>
      {erro && <p className="text-xs text-red-500 dark:text-red-400 mt-2">{erro}</p>}
      <div className="flex justify-end gap-3 mt-4">
        <button
          type="button"
          onClick={onCancelar}
          disabled={enviando}
          className="px-6 py-2.5 rounded-xl font-bold text-sm text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-slate-700 transition-colors disabled:opacity-60"
        >
          Cancelar
        </button>
        <button
          type="button"
          onClick={onEnviar}
          disabled={enviando}
          className="bg-[#183E6C] text-white px-6 py-2.5 rounded-xl font-bold text-sm hover:bg-[#102a4a] transition-colors w-full md:w-auto shadow-md disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {enviando ? 'Enviando...' : 'Confirmar Envio'}
        </button>
      </div>
    </div>
  )
}
