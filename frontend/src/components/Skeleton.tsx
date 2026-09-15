// Bloco genérico de carregamento (placeholder animado). Componha vários
// para montar o skeleton de uma tela inteira, em vez de reimplementar o
// mesmo `animate-pulse` em cada página.
type SkeletonProps = {
  className?: string;
};

export default function Skeleton({ className = 'h-4 w-full rounded' }: SkeletonProps) {
  return <div className={`animate-pulse bg-gray-200 dark:bg-slate-700 ${className}`} />;
}
