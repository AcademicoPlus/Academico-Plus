// Chamadas para o ProjetoMembroController do backend
// (rotas /projetos/{projetoId}/membros/*) — a equipe de um projeto.
import { apiFetch } from './apiClient';
import type { UsuarioResumo } from './authService';
import type { ProjetoRef } from './projetoService';

// Espelha ProjetoMembroResponse (dto/membro). "projeto" vem null nas
// listagens por projeto (o backend usa ProjetoMembroResponse.fromEntity
// sem o campo, já que o projeto é o contexto da própria URL).
export type ProjetoMembro = {
  id: string;
  projeto: ProjetoRef | null;
  usuario: UsuarioResumo | null;
  funcao: string | null;
  dataAdesao: string;
};

export function listarMembrosDoProjeto(projetoId: string): Promise<ProjetoMembro[]> {
  return apiFetch<ProjetoMembro[]>(`/projetos/${projetoId}/membros`);
}

export function buscarMembroPorId(projetoId: string, membroId: string): Promise<ProjetoMembro> {
  return apiFetch<ProjetoMembro>(`/projetos/${projetoId}/membros/${membroId}`);
}

// Espelha AdicionarMembroRequest (dto/membro) — inclusão direta pelo
// criador do projeto, sem passar por candidatura. Ocupa uma vaga.
export function adicionarMembroAoProjeto(
  projetoId: string,
  usuarioId: string,
  funcao?: string,
): Promise<ProjetoMembro> {
  return apiFetch<ProjetoMembro>(`/projetos/${projetoId}/membros`, {
    method: 'POST',
    body: { usuarioId, funcao },
  });
}

export function atualizarFuncaoDoMembro(
  projetoId: string,
  membroId: string,
  funcao: string,
): Promise<ProjetoMembro> {
  return apiFetch<ProjetoMembro>(`/projetos/${projetoId}/membros/${membroId}/funcao`, {
    method: 'PATCH',
    body: { funcao },
  });
}

// Libera a vaga ocupada. Permitido ao criador do projeto ou ao próprio
// membro (sair do projeto) — validado no backend.
export function removerMembroDoProjeto(
  projetoId: string,
  membroId: string,
): Promise<{ sucesso: boolean; mensagem: string }> {
  return apiFetch(`/projetos/${projetoId}/membros/${membroId}`, { method: 'DELETE' });
}
