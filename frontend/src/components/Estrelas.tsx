// Exibe (e opcionalmente permite escolher) uma nota de 1 a 5 estrelas.
// Usado tanto para mostrar avaliações recebidas (Perfil.tsx) quanto para
// o formulário de nova avaliação (detalhes-projeto.tsx).
type EstrelasProps = {
  nota: number;
  onSelecionar?: (nota: number) => void;
  tamanho?: string;
};

export default function Estrelas({ nota, onSelecionar, tamanho = 'h-3.5 w-3.5' }: EstrelasProps) {
  const interativo = !!onSelecionar;

  return (
    <div className="flex items-center gap-0.5" aria-label={`Nota ${nota} de 5`}>
      {[1, 2, 3, 4, 5].map((posicao) => {
        const preenchida = posicao <= Math.round(nota);
        const estrela = (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className={`${tamanho} ${preenchida ? 'text-amber-400' : 'text-gray-200 dark:text-slate-700'}`}
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.958a1 1 0 00.95.69h4.162c.969 0 1.371 1.24.588 1.81l-3.368 2.448a1 1 0 00-.364 1.118l1.287 3.957c.299.922-.756 1.688-1.54 1.118l-3.367-2.447a1 1 0 00-1.176 0l-3.368 2.447c-.783.57-1.838-.196-1.539-1.118l1.286-3.957a1 1 0 00-.363-1.118L2.98 9.385c-.783-.57-.38-1.81.588-1.81h4.162a1 1 0 00.951-.69l1.286-3.958z" />
          </svg>
        );

        if (!interativo) return <span key={posicao}>{estrela}</span>;

        return (
          <button
            key={posicao}
            type="button"
            onClick={() => onSelecionar(posicao)}
            aria-label={`Dar nota ${posicao}`}
            className="p-0.5 hover:scale-110 transition-transform"
          >
            {estrela}
          </button>
        );
      })}
    </div>
  );
}
