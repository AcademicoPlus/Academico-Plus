// Chamadas relacionadas a avaliações entre participantes de projeto.
// "Avaliar" vem do ProjetoAvaliacaoController (POST /projetos/{id}/avaliacoes);
// "listar recebidas" vem do UsuarioController (GET /usuarios/{id}/avaliacoes) —
// os dois usam o mesmo DTO (AvaliacaoResponse), por isso ficam no mesmo arquivo.
import { apiFetch, type PaginaResposta } from './apiClient';
import type { UsuarioResumo } from './authService';
import type { ProjetoRef } from './projetoService';

// Espelha AvaliacaoResponse (dto/avaliacao). O avaliado não aparece aqui —
// ele já é o contexto da URL em GET /usuarios/{id}/avaliacoes.
export type Avaliacao = {
  id: string;
  projeto: ProjetoRef;
  avaliador: UsuarioResumo;
  nota: number;
  comentario: string | null;
  criadoEm: string;
};

// Espelha AvaliarRequest (dto/avaliacao). Só é permitido entre quem
// participou do mesmo projeto (criador ou membro), depois que o projeto
// está CONCLUIDO ou CANCELADO (validado no backend).
export function avaliarParticipante(
  projetoId: string,
  avaliadoId: string,
  nota: number,
  comentario?: string,
): Promise<Avaliacao> {
  return apiFetch<Avaliacao>(`/projetos/${projetoId}/avaliacoes`, {
    method: 'POST',
    body: { avaliadoId, nota, comentario },
  });
}

export function listarAvaliacoesRecebidas(usuarioId: string): Promise<PaginaResposta<Avaliacao>> {
  return apiFetch<PaginaResposta<Avaliacao>>(`/usuarios/${usuarioId}/avaliacoes`);
}
