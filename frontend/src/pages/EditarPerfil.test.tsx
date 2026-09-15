import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import EditarPerfil from './EditarPerfil';
import * as useMeuPerfilHook from '../hooks/useMeuPerfil';
import * as cursoService from '../services/cursoService';
import * as usuarioHabilidadeService from '../services/usuarioHabilidadeService';
import { apiFetch } from '../services/apiClient';
import type { UsuarioPerfil } from '../services/usuarioService';

vi.mock('../hooks/useMeuPerfil');
vi.mock('../services/cursoService');
vi.mock('../services/usuarioHabilidadeService');
vi.mock('../services/apiClient', async () => {
  const original = await vi.importActual<typeof import('../services/apiClient')>('../services/apiClient');
  return {
    ...original,
    apiFetch: vi.fn(),
  };
});

function perfilBase(overrides: Partial<UsuarioPerfil> = {}): UsuarioPerfil {
  return {
    id: 'user-1',
    email: 'ana@academico.edu.br',
    nome: 'Ana Silva',
    nivelAcesso: null,
    curso: null,
    periodo: 3,
    bio: '',
    fotoUrl: null,
    linkedinUrl: null,
    githubUrl: null,
    habilidades: [],
    notaMedia: null,
    totalAvaliacoes: null,
    termosAceitosEm: null,
    ativo: true,
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

function renderPagina() {
  return render(
    <MemoryRouter initialEntries={['/usuarios/user-1/editar']}>
      <Routes>
        <Route path="/usuarios/:id/editar" element={<EditarPerfil />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('EditarPerfil', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(cursoService, 'listarCursos').mockResolvedValue([]);
  });

  it('adiciona uma nova habilidade ao perfil', async () => {
    vi.spyOn(useMeuPerfilHook, 'obterMeuPerfilCache').mockResolvedValue(perfilBase());
    vi.mocked(apiFetch).mockResolvedValue({
      content: [{ id: 'hab-react', nome: 'React', categoria: 'Tecnologia' }],
      totalElements: 1, totalPages: 1, number: 0, size: 50, last: true,
    });
    vi.spyOn(usuarioHabilidadeService, 'adicionarHabilidadeAoPerfil').mockResolvedValue({
      id: 'vinculo-1',
      habilidade: { id: 'hab-react', nome: 'React', categoria: 'Tecnologia' },
      nivel: 'INICIANTE',
    });

    renderPagina();

    expect(await screen.findByRole('heading', { name: 'Editar perfil' })).toBeInTheDocument();

    const buscaInput = screen.getByPlaceholderText('Adicionar habilidade...');
    await userEvent.type(buscaInput, 'Rea');

    const sugestao = await screen.findByRole('button', { name: /react/i });
    await userEvent.click(sugestao);

    await waitFor(() =>
      expect(usuarioHabilidadeService.adicionarHabilidadeAoPerfil).toHaveBeenCalledWith('hab-react', 'INICIANTE'),
    );
    expect(await screen.findByText('React')).toBeInTheDocument();
  });
});
