// Chamadas para o RecomendacaoController do backend (rotas /recomendacoes/*).
// Ranqueia projetos/candidatos pela cobertura dos requisitos de habilidade
// (compara tags do usuário com as habilidades necessárias do projeto).
import { apiFetch, construirQuery } from './apiClient';
import type { UsuarioResumo } from './authService';
import type { HabilidadeResumo } from './projetoHabilidadeService';
import type { Projeto } from './projetoService';

// Espelha ProjetoRecomendadoResponse (dto/recomendacao).
export type ProjetoRecomendado = {
  projeto: Projeto;
  // 0.0 a 1.0 — cobertura ponderada dos requisitos do projeto pelas habilidades do usuário logado.
  compatibilidade: number;
  habilidadesEmComum: HabilidadeResumo[];
};

// Espelha UsuarioRecomendadoResponse (dto/recomendacao).
export type UsuarioRecomendado = {
  usuario: UsuarioResumo;
  // 0.0 a 1.0 — cobertura ponderada dos requisitos do projeto pelas habilidades do candidato.
  compatibilidade: number;
  habilidadesEmComum: HabilidadeResumo[];
};

// Projetos ABERTOS recomendados pra o usuário logado, a partir das
// habilidades cadastradas no próprio perfil.
export function recomendarProjetos(limite?: number): Promise<ProjetoRecomendado[]> {
  return apiFetch<ProjetoRecomendado[]>(`/recomendacoes/projetos${construirQuery({ limite })}`);
}

// Só o criador do projeto pode chamar (validado no backend).
export function recomendarCandidatos(
  projetoId: string,
  limite?: number,
): Promise<UsuarioRecomendado[]> {
  return apiFetch<UsuarioRecomendado[]>(
    `/recomendacoes/projetos/${projetoId}/candidatos${construirQuery({ limite })}`,
  );
}
