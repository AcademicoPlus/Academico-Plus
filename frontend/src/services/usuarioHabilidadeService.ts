// Chamadas para o UsuarioHabilidadeController do backend
// (rotas /usuarios/me/habilidades/*) — tags de competência do próprio
// usuário logado, usadas pela recomendação de projetos (RecomendacaoController).
import { apiFetch } from './apiClient';
import type { HabilidadeResumo } from './projetoHabilidadeService';

// Espelha o enum NivelHabilidade do backend (domain/enums/NivelHabilidade.java).
export type NivelHabilidade = 'INICIANTE' | 'INTERMEDIARIO' | 'AVANCADO' | 'EXPERT';

// Espelha UsuarioHabilidadeResponse (dto/habilidade).
export type UsuarioHabilidade = {
  id: string;
  habilidade: HabilidadeResumo;
  nivel: NivelHabilidade;
};

export function listarMinhasHabilidades(): Promise<UsuarioHabilidade[]> {
  return apiFetch<UsuarioHabilidade[]>('/usuarios/me/habilidades');
}

// Espelha AdicionarHabilidadeUsuarioRequest (dto/habilidade). "nivel" nulo
// vira NivelHabilidade.INICIANTE no backend.
export function adicionarHabilidadeAoPerfil(
  habilidadeId: string,
  nivel?: NivelHabilidade,
): Promise<UsuarioHabilidade> {
  return apiFetch<UsuarioHabilidade>('/usuarios/me/habilidades', {
    method: 'POST',
    body: { habilidadeId, nivel },
  });
}

export function atualizarNivelHabilidade(
  habilidadeId: string,
  nivel: NivelHabilidade,
): Promise<UsuarioHabilidade> {
  return apiFetch<UsuarioHabilidade>(`/usuarios/me/habilidades/${habilidadeId}`, {
    method: 'PATCH',
    body: { nivel },
  });
}

export function removerHabilidadeDoPerfil(
  habilidadeId: string,
): Promise<{ sucesso: boolean; mensagem: string }> {
  return apiFetch(`/usuarios/me/habilidades/${habilidadeId}`, { method: 'DELETE' });
}
