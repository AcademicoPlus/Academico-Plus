// Chamadas para o AuthController do backend (rotas /auth/*).
// Os tipos abaixo espelham os records Java em academico-backend
// (dto/auth/LoginRequest, LoginResponse, CadastroRequest, CadastroResponse).
import { apiFetch } from './apiClient';

export type LoginRequest = {
  email: string;
  senha: string;
};

export type UsuarioResumo = {
  id: string;
  nome: string;
  email: string;
  curso: string | null;
  fotoUrl: string | null;
  permission: string | null;
  periodo: number | null;
  notaMedia: number | null;
  totalAvaliacoes: number | null;
};

export type LoginResponse = {
  sucesso: boolean;
  mensagem: string;
  token: string;
  usuario: UsuarioResumo;
  // Avisos de projetos cancelados exibidos ao usuário após o login;
  // não é usado pelo formulário de login em si, então fica tipado de forma solta.
  avisosCancelamento: unknown[];
};

export type CadastroRequest = {
  email: string;
  nome: string;
  periodo?: number;
  idCurso: string;
  senha: string;
};

export type CadastroResponse = {
  sucesso: boolean;
  mensagem: string;
};

export function login(dados: LoginRequest): Promise<LoginResponse> {
  // autenticado: false porque essa rota é pública (permitAll no SecurityConfig)
  // e ainda não existe token nesse momento.
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: dados,
    autenticado: false,
  });
}

export function cadastrar(dados: CadastroRequest): Promise<CadastroResponse> {
  return apiFetch<CadastroResponse>('/auth/cadastro', {
    method: 'POST',
    body: dados,
    autenticado: false,
  });
}
