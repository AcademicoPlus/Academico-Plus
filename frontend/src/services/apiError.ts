// Formato de erro de campo do GlobalExceptionHandler do backend (validação
// de DTO), usado tanto pelo apiClient real quanto pelos mocks (mocks.ts).
export type ErroCampo = {
  campo: string;
  mensagem: string;
};

// Erro lançado quando o backend (ou o mock) responde com status >= 400, já
// com a mensagem pronta para mostrar ao usuário (ex.: "Email ou senha
// inválidos"). Fica em arquivo próprio para o apiClient e os mocks poderem
// importar sem criar dependência circular entre os dois.
export class ApiError extends Error {
  status: number;
  erros?: ErroCampo[];

  constructor(status: number, mensagem: string, erros?: ErroCampo[]) {
    super(mensagem);
    this.status = status;
    this.erros = erros;
  }
}
