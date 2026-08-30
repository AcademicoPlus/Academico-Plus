// Chamadas para o CandidaturaController do backend (rotas /candidaturas/*).
// Cobre as duas visões do fluxo de candidatura: quem se candidata a um
// projeto e quem criou o projeto e precisa aceitar/rejeitar candidatos.
import { apiFetch, construirQuery, type PaginaResposta } from './apiClient';
import type { UsuarioResumo } from './authService';
import type { ProjetoRef } from './projetoService';

// Espelha o enum StatusCandidatura do backend (domain/enums/StatusCandidatura.java).
export type StatusCandidatura = 'PENDENTE' | 'ACEITO' | 'REJEITADO';

// Espelha CandidaturaResponse (dto/candidatura).
export type Candidatura = {
  id: string;
  projeto: ProjetoRef;
  usuario: UsuarioResumo | null;
  status: StatusCandidatura;
  mensagem: string | null;
  motivoRejeicao: string | null;
  dataCandidatura: string;
  dataResposta: string | null;
};

// ============================================
// VISÃO DO CANDIDATO
// ============================================

// Espelha CriarCandidaturaRequest (dto/candidatura). O projeto precisa
// estar ABERTO e com vaga disponível (validado no backend).
export function candidatar(projetoId: string, mensagem?: string): Promise<Candidatura> {
  return apiFetch<Candidatura>('/candidaturas', {
    method: 'POST',
    body: { projetoId, mensagem },
  });
}

export type FiltroCandidaturas = {
  status?: StatusCandidatura;
  pagina?: number;
  tamanho?: number;
};

export function listarMinhasCandidaturas(
  filtro: FiltroCandidaturas = {},
): Promise<PaginaResposta<Candidatura>> {
  const query = construirQuery({ status: filtro.status, page: filtro.pagina, size: filtro.tamanho });
  return apiFetch<PaginaResposta<Candidatura>>(`/candidaturas/minhas${query}`);
}

// Desistência do próprio candidato — só funciona enquanto PENDENTE (backend valida).
export function cancelarCandidatura(id: string): Promise<{ sucesso: boolean; mensagem: string }> {
  return apiFetch(`/candidaturas/${id}`, { method: 'DELETE' });
}

// ============================================
// VISÃO DO CRIADOR DO PROJETO
// ============================================

export function listarCandidaturasDoProjeto(
  projetoId: string,
  filtro: FiltroCandidaturas = {},
): Promise<PaginaResposta<Candidatura>> {
  const query = construirQuery({ status: filtro.status, page: filtro.pagina, size: filtro.tamanho });
  return apiFetch<PaginaResposta<Candidatura>>(`/candidaturas/projeto/${projetoId}${query}`);
}

// Espelha AceitarCandidaturaRequest (dto/candidatura) — corpo opcional, só
// pra já definir a função do novo membro no projeto.
export function aceitarCandidatura(id: string, funcao?: string): Promise<Candidatura> {
  return apiFetch<Candidatura>(`/candidaturas/${id}/aceitar`, {
    method: 'PATCH',
    body: funcao !== undefined ? { funcao } : undefined,
  });
}

// Espelha RejeitarCandidaturaRequest (dto/candidatura) — motivo é obrigatório no backend.
export function rejeitarCandidatura(id: string, motivoRejeicao: string): Promise<Candidatura> {
  return apiFetch<Candidatura>(`/candidaturas/${id}/rejeitar`, {
    method: 'PATCH',
    body: { motivoRejeicao },
  });
}

// ============================================
// COMUM (acessível ao candidato e ao criador do projeto)
// ============================================

export function buscarCandidaturaPorId(id: string): Promise<Candidatura> {
  return apiFetch<Candidatura>(`/candidaturas/${id}`);
}
