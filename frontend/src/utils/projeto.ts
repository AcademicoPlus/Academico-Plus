// Helpers de exibição compartilhados entre as telas que mostram projetos
// (feed, detalhes, sidebar) — evita repetir o mapeamento de status/data em cada uma.
import type { StatusProjeto } from '../services/projetoService';

export const STATUS_PROJETO_LABEL: Record<StatusProjeto, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

export const STATUS_PROJETO_BADGE: Record<StatusProjeto, string> = {
  ABERTO: 'bg-green-50 text-green-600',
  EM_ANDAMENTO: 'bg-orange-50 text-[#F27405]',
  CONCLUIDO: 'bg-blue-50 text-[#183E6C]',
  CANCELADO: 'bg-red-50 text-red-500',
};

export function formatarData(data: string | null | undefined): string | null {
  if (!data) return null;
  return new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(data));
}

export function iniciaisDoNome(nome: string): string {
  return nome
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase();
}

// O input nativo type="date" aceita ano com mais de 4 dígitos (ex.: "12312")
// sem reclamar visualmente, e por causa disso um prazo maluco ainda "parece"
// uma data válida. Limitamos a uma janela razoável pra pegar esses casos —
// usado tanto ao criar quanto ao editar um projeto.
export const ANOS_MAXIMOS_PRAZO = 5;

export function formatarDataISO(data: Date): string {
  return data.toISOString().split('T')[0];
}

export function adicionarAnos(data: Date, anos: number): Date {
  const copia = new Date(data);
  copia.setFullYear(copia.getFullYear() + anos);
  return copia;
}
