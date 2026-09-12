import { describe, it, expect } from 'vitest';
import { construirQuery } from './apiClient';

describe('services/apiClient construirQuery', () => {
  it('retorna string vazia quando não há parâmetros definidos', () => {
    expect(construirQuery({})).toBe('');
    expect(construirQuery({ status: undefined, busca: undefined })).toBe('');
  });

  it('monta a query string ignorando valores undefined', () => {
    const query = construirQuery({ status: 'ABERTO', busca: undefined, page: 2 });
    expect(query).toBe('?status=ABERTO&page=2');
  });

  it('inclui valores booleanos e numéricos convertidos para string', () => {
    const query = construirQuery({ ativo: true, tamanho: 10 });
    expect(query).toBe('?ativo=true&tamanho=10');
  });
});
