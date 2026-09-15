import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import AdminHabilidades from './admin-habilidades';
import * as habilidadeService from '../services/habilidadeService';
import type { Habilidade } from '../services/habilidadeService';

vi.mock('../services/habilidadeService');

const HABILIDADE_1: Habilidade = {
  id: 'hab-1',
  nome: 'React',
  categoria: 'Frontend',
  descricao: 'Biblioteca para interfaces',
  usuariosCount: 5,
  projetosCount: 2,
  criadoEm: '2026-01-01T00:00:00Z',
};

const PAGINA_VAZIA = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10, last: true };

function renderPagina() {
  return render(
    <MemoryRouter>
      <AdminHabilidades />
    </MemoryRouter>,
  );
}

describe('AdminHabilidades', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(habilidadeService, 'listarCategoriasDeHabilidade').mockResolvedValue(['Frontend', 'Backend']);
  });

  it('carrega e lista as habilidades do catálogo', async () => {
    vi.spyOn(habilidadeService, 'listarHabilidades').mockResolvedValue({
      content: [HABILIDADE_1],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 10,
      last: true,
    });

    renderPagina();

    expect(await screen.findByText('React')).toBeInTheDocument();
    expect(screen.getByText('1 habilidades no catálogo')).toBeInTheDocument();
  });

  it('cria uma nova habilidade preenchendo o formulário', async () => {
    const listar = vi
      .spyOn(habilidadeService, 'listarHabilidades')
      .mockResolvedValueOnce(PAGINA_VAZIA)
      .mockResolvedValueOnce({ content: [HABILIDADE_1], totalElements: 1, totalPages: 1, number: 0, size: 10, last: true });
    vi.spyOn(habilidadeService, 'criarHabilidade').mockResolvedValue(HABILIDADE_1);

    renderPagina();

    await waitFor(() => expect(listar).toHaveBeenCalledTimes(1));

    await userEvent.type(screen.getByPlaceholderText('Ex.: React'), 'React');
    await userEvent.type(screen.getByPlaceholderText('Ex.: Frontend'), 'Frontend');
    await userEvent.type(screen.getByPlaceholderText(/breve descrição/i), 'Biblioteca para interfaces');

    await userEvent.click(screen.getByRole('button', { name: /criar habilidade/i }));

    await waitFor(() =>
      expect(habilidadeService.criarHabilidade).toHaveBeenCalledWith({
        nome: 'React',
        categoria: 'Frontend',
        descricao: 'Biblioteca para interfaces',
      }),
    );
  });
});
