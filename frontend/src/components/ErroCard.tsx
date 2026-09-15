// Bloco padrão de mensagem de erro inline, reutilizado nas páginas que hoje
// reimplementavam a mesma classe repetidamente.
import type { ReactNode } from 'react';

type ErroCardProps = {
  children: ReactNode;
  className?: string;
};

export default function ErroCard({ children, className = '' }: ErroCardProps) {
  return (
    <div
      role="alert"
      className={`rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-semibold text-red-600 dark:bg-red-950/40 dark:border-red-900/50 dark:text-red-400 ${className}`}
    >
      {children}
    </div>
  );
}
