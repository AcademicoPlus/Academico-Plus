// Chamada para o NivelAcessoController do backend (rota /niveis-acesso).
// Só ADMIN (@PreAuthorize no backend) — usado pra preencher o seletor de
// promoção de usuário (ver usuarioService.alterarNivelAcesso).
import { apiFetch } from './apiClient';

// Espelha NivelAcessoResponse (dto/usuario).
export type NivelAcesso = {
  id: string;
  nome: string;
  descricao: string | null;
};

export function listarNiveisDeAcesso(): Promise<NivelAcesso[]> {
  return apiFetch<NivelAcesso[]>('/niveis-acesso');
}
