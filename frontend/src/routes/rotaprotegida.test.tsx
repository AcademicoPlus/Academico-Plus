import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RotaProtegida from './rotaprotegida';
import { salvarToken } from '../utils/auth';

function renderComGuard(caminhoInicial: string) {
  return render(
    <MemoryRouter initialEntries={[caminhoInicial]}>
      <Routes>
        <Route path="/login" element={<div>Tela de login</div>} />
        <Route element={<RotaProtegida />}>
          <Route path="/dashboard" element={<div>Tela protegida</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RotaProtegida', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redireciona para /login quando não há token', () => {
    renderComGuard('/dashboard');
    expect(screen.getByText('Tela de login')).toBeInTheDocument();
  });

  it('renderiza a rota filha quando há token salvo', () => {
    salvarToken('token-valido');
    renderComGuard('/dashboard');
    expect(screen.getByText('Tela protegida')).toBeInTheDocument();
  });
});
