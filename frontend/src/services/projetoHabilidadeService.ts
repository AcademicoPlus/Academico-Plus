// Chamadas para o ProjetoHabilidadeController do backend
// (rotas /projetos/{projetoId}/habilidades/*) — habilidades exigidas ou
// desejáveis para participar de um projeto.
import { apiFetch } from './apiClient';

// Espelha HabilidadeResumoResponse (dto/habilidade) — referência mínima ao
// catálogo, usada aninhada aqui e em usuarioHabilidadeService.
export type HabilidadeResumo = {
  id: string;
  nome: string;
  categoria: string;
};

// Espelha ProjetoHabilidadeResponse (dto/habilidade) — vínculo entre o
// projeto e uma habilidade necessária, com a flag de obrigatória ou não.
export type ProjetoHabilidade = {
  id: string;
  habilidade: HabilidadeResumo;
  obrigatoria: boolean;
};

export function listarHabilidadesDoProjeto(projetoId: string): Promise<ProjetoHabilidade[]> {
  return apiFetch<ProjetoHabilidade[]>(`/projetos/${projetoId}/habilidades`);
}

// Espelha VincularHabilidadeRequest (dto/habilidade). "obrigatoria" nulo
// vira false no backend (habilidade desejável, não obrigatória).
export function vincularHabilidadeAoProjeto(
  projetoId: string,
  habilidadeId: string,
  obrigatoria?: boolean,
): Promise<ProjetoHabilidade> {
  return apiFetch<ProjetoHabilidade>(`/projetos/${projetoId}/habilidades`, {
    method: 'POST',
    body: { habilidadeId, obrigatoria },
  });
}

export function alterarObrigatoriedadeHabilidade(
  projetoId: string,
  habilidadeId: string,
  obrigatoria: boolean,
): Promise<ProjetoHabilidade> {
  return apiFetch<ProjetoHabilidade>(
    `/projetos/${projetoId}/habilidades/${habilidadeId}?obrigatoria=${obrigatoria}`,
    { method: 'PATCH' },
  );
}

export function desvincularHabilidadeDoProjeto(
  projetoId: string,
  habilidadeId: string,
): Promise<{ sucesso: boolean; mensagem: string }> {
  return apiFetch(`/projetos/${projetoId}/habilidades/${habilidadeId}`, { method: 'DELETE' });
}
