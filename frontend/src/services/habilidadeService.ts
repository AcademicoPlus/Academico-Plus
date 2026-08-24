// Chamadas para o HabilidadeController do backend (rotas /habilidades/*).
// Leitura liberada a qualquer usuário autenticado (é preciso consultar o
// catálogo pra montar um projeto ou o próprio perfil); escrita (criar,
// atualizar, remover) é restrita a ADMIN no backend (@PreAuthorize).
import { apiFetch, construirQuery, type PaginaResposta } from './apiClient';

// Espelha HabilidadeResponse (dto/habilidade) — item completo do catálogo,
// diferente do HabilidadeResumo (id/nome/categoria) usado quando a
// habilidade aparece aninhada em outro recurso.
export type Habilidade = {
  id: string;
  nome: string;
  categoria: string;
  descricao: string | null;
  usuariosCount: number;
  projetosCount: number;
  criadoEm: string;
};

export type FiltroHabilidades = {
  busca?: string;
  categoria?: string;
  pagina?: number;
  tamanho?: number;
};

export function listarHabilidades(filtro: FiltroHabilidades = {}): Promise<PaginaResposta<Habilidade>> {
  const query = construirQuery({
    busca: filtro.busca,
    categoria: filtro.categoria,
    page: filtro.pagina,
    size: filtro.tamanho,
  });
  return apiFetch<PaginaResposta<Habilidade>>(`/habilidades${query}`);
}

// Categorias distintas já cadastradas — útil pra montar um filtro/select na tela.
export function listarCategoriasDeHabilidade(): Promise<string[]> {
  return apiFetch<string[]>('/habilidades/categorias');
}

export function buscarHabilidadePorId(id: string): Promise<Habilidade> {
  return apiFetch<Habilidade>(`/habilidades/${id}`);
}

// Espelha CriarHabilidadeRequest / AtualizarHabilidadeRequest (dto/habilidade)
// — os dois DTOs do backend têm os mesmos campos.
export type SalvarHabilidadeRequest = {
  nome: string;
  categoria?: string;
  descricao?: string;
};

// Só ADMIN (validado pelo backend com @PreAuthorize("hasRole('ADMIN')")).
export function criarHabilidade(dados: SalvarHabilidadeRequest): Promise<Habilidade> {
  return apiFetch<Habilidade>('/habilidades', { method: 'POST', body: dados });
}

export function atualizarHabilidade(id: string, dados: SalvarHabilidadeRequest): Promise<Habilidade> {
  return apiFetch<Habilidade>(`/habilidades/${id}`, { method: 'PUT', body: dados });
}

// Exclusão física — só funciona se a habilidade não estiver vinculada a
// nenhum projeto ou usuário (validado pelo backend).
export function removerHabilidade(id: string): Promise<{ sucesso: boolean; mensagem: string }> {
  return apiFetch(`/habilidades/${id}`, { method: 'DELETE' });
}
