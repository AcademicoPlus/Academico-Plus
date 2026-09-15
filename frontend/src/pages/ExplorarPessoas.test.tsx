import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import ExplorarPessoas from './ExplorarPessoas';
import * as usuarioService from '../services/usuarioService';
import * as cursoService from '../services/cursoService';
import * as apiClient from '../services/apiClient';

vi.mock('../services/usuarioService');
vi.mock('../services/cursoService');
vi.mock('../services/apiClient', async () => {
  const real = await vi.importActual<typeof import('../services/apiClient')>('../services/apiClient');
  return { ...real, apiFetch: vi.fn() };
});

function renderPagina() {
  return render(
    <MemoryRouter>
      <ExplorarPessoas />
    </MemoryRouter>,
  );
}

describe('ExplorarPessoas', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(cursoService, 'listarCursos').mockResolvedValue([{ id: 'curso-1', nome: 'Ciência da Computação', criadoEm: '2026-01-01T00:00:00Z' }]);
    vi.spyOn(apiClient, 'apiFetch').mockResolvedValue({
      content: [{ id: 'hab-1', nome: 'React' }],
      totalElements: 1, totalPages: 1, number: 0, size: 100, last: true,
    } as never);
  });

  it('busca pessoas ao carregar e mostra os resultados', async () => {
    vi.spyOn(usuarioService, 'explorarPerfis').mockResolvedValue({
      content: [
        { id: 'user-1', nome: 'Bruno Alves', curso: 'Ciência da Computação', fotoUrl: null, permission: 'ALUNO', periodo: 6, notaMedia: null, totalAvaliacoes: null },
      ],
      totalElements: 1, totalPages: 1, number: 0, size: 20, last: true,
    });

    renderPagina();

    expect(await screen.findByText('Bruno Alves')).toBeInTheDocument();
    expect(usuarioService.explorarPerfis).toHaveBeenCalledWith({ tamanho: 20 });
  });

  it('envia o idHabilidade escolhido ao backend (filtro server-side, não mais client-side)', async () => {
    vi.spyOn(usuarioService, 'explorarPerfis').mockResolvedValue({
      content: [], totalElements: 0, totalPages: 1, number: 0, size: 20, last: true,
    });

    renderPagina();

    await waitFor(() => expect(usuarioService.explorarPerfis).toHaveBeenCalledTimes(1));

    const selectHabilidade = await screen.findByDisplayValue('Todas as habilidades');
    await userEvent.selectOptions(selectHabilidade, 'hab-1');

    await waitFor(() =>
      expect(usuarioService.explorarPerfis).toHaveBeenLastCalledWith({
        idHabilidade: 'hab-1',
        pagina: undefined,
        tamanho: 20,
      }),
    );
  });

  it('mostra mensagem de erro quando a busca falha', async () => {
    vi.spyOn(usuarioService, 'explorarPerfis').mockRejectedValue(new Error('Falha ao buscar pessoas.'));

    renderPagina();

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao buscar pessoas.');
  });
});
