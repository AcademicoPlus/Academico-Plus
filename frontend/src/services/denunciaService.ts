// Chamadas para o DenunciaController do backend — denúncia de avaliações
// abusivas e a fila de revisão administrativa (rotas /denuncias e
// /avaliacoes/{id}/denuncias). Listar pendentes e resolver são só ADMIN
// (@PreAuthorize no backend).
import { apiFetch, type PaginaResposta } from './apiClient';
import type { UsuarioResumo } from './authService';

// Espelha o enum StatusDenuncia do backend (domain/enums/StatusDenuncia.java).
export type StatusDenuncia = 'PENDENTE' | 'PROCEDENTE' | 'IMPROCEDENTE';

// Espelha DenunciaResponse (dto/avaliacao) — inclui os dados da avaliação
// denunciada (nota, comentário, quem avaliou) pra dar contexto no admin.
export type Denuncia = {
  id: string;
  avaliacaoId: string;
  avaliador: UsuarioResumo;
  notaAvaliacao: number;
  comentarioAvaliacao: string | null;
  denunciante: UsuarioResumo;
  motivo: string;
  status: StatusDenuncia;
  criadoEm: string;
  analisadoEm: string | null;
};

// Só a pessoa avaliada pode denunciar a avaliação, uma vez por avaliação
// (validado no backend).
export function denunciarAvaliacao(avaliacaoId: string, motivo: string): Promise<Denuncia> {
  return apiFetch<Denuncia>(`/avaliacoes/${avaliacaoId}/denuncias`, {
    method: 'POST',
    body: { motivo },
  });
}

export function listarDenunciasPendentes(): Promise<PaginaResposta<Denuncia>> {
  return apiFetch<PaginaResposta<Denuncia>>('/denuncias');
}

// Se procedente=true, a avaliação denunciada é removida e a nota do
// avaliado é recalculada pelo backend.
export function resolverDenuncia(id: string, procedente: boolean): Promise<Denuncia> {
  return apiFetch<Denuncia>(`/denuncias/${id}/resolver?procedente=${procedente}`, {
    method: 'PATCH',
  });
}
