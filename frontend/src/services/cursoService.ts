// Chamada para o CursoController do backend (GET /cursos).
// É pública (permitAll no SecurityConfig) porque precisa alimentar o
// dropdown de curso na tela de cadastro, antes do usuário ter um token.
import { apiFetch } from './apiClient';

export type Curso = {
  id: string;
  nome: string;
  criadoEm: string;
};

export function listarCursos(): Promise<Curso[]> {
  return apiFetch<Curso[]>('/cursos', { autenticado: false });
}
