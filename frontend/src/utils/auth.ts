// Funções utilitárias para guardar/ler o token JWT emitido pelo backend
// (AuthController -> LoginResponse.token) e decidir se o usuário está logado.
// Centralizar isso aqui evita espalhar "localStorage.getItem('token')" pelo app.

const TOKEN_KEY = 'academico:token';

export function salvarToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function obterToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function removerToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function estaAutenticado(): boolean {
  return obterToken() !== null;
}
