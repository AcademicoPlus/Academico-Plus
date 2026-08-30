// Helpers de exibição compartilhados entre as telas que mostram candidaturas
// (detalhes do projeto, minhas candidaturas) — evita repetir o mapeamento de
// status em cada uma.
import type { StatusCandidatura } from '../services/candidaturaService';

export const STATUS_CANDIDATURA_LABEL: Record<StatusCandidatura, string> = {
  PENDENTE: 'Pendente',
  ACEITO: 'Aceito',
  REJEITADO: 'Rejeitado',
};

export const STATUS_CANDIDATURA_BADGE: Record<StatusCandidatura, string> = {
  PENDENTE: 'bg-orange-50 text-[#F27405]',
  ACEITO: 'bg-green-50 text-green-600',
  REJEITADO: 'bg-red-50 text-red-500',
};
