// Mensagem padrão para listas sem resultados, reutilizada nas páginas que
// hoje reimplementavam o mesmo bloco centralizado cinza.
import type { ReactNode } from 'react';

type EstadoVazioProps = {
  titulo: string;
  descricao?: string;
  icone?: string;
  acao?: ReactNode;
  className?: string;
};

export default function EstadoVazio({ titulo, descricao, icone, acao, className = '' }: EstadoVazioProps) {
  return (
    <div className={`flex flex-col items-center justify-center py-16 gap-2 text-center ${className}`}>
      {icone && <span className="text-4xl mb-1">{icone}</span>}
      <p className="text-gray-500 dark:text-gray-400 text-sm font-medium">{titulo}</p>
      {descricao && <p className="text-gray-400 dark:text-gray-500 text-xs">{descricao}</p>}
      {acao}
    </div>
  );
}
