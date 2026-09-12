import { describe, it, expect, beforeEach } from 'vitest';
import { salvarToken, obterToken, removerToken, estaAutenticado } from './auth';

describe('utils/auth', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('não está autenticado quando não há token salvo', () => {
    expect(estaAutenticado()).toBe(false);
    expect(obterToken()).toBeNull();
  });

  it('salva e recupera o token', () => {
    salvarToken('meu-jwt');
    expect(obterToken()).toBe('meu-jwt');
    expect(estaAutenticado()).toBe(true);
  });

  it('remove o token', () => {
    salvarToken('meu-jwt');
    removerToken();
    expect(obterToken()).toBeNull();
    expect(estaAutenticado()).toBe(false);
  });
});
