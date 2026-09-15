import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import Projetos from './projetos';
import * as projetoService from '../services/projetoService';
import type { Projeto } from '../services/projetoService';
import type { PaginaResposta } from '../services/apiClient';

vi.mock('../services/projetoService');

function projeto(overrides: Partial<Projeto> = {}): Projeto {
  return {
    id: 'proj-1',
    criador: {
      id: 'user-1', nome: 'Ana Silva', curso: null, fotoUrl: null,
      permission: 'ALUNO', periodo: null, notaMedia: null, totalAvaliacoes: null,
    },
    titulo: 'Projeto Teste',
    descricao: 'Descrição do projeto de teste.',
    bannerUrl: null,
    status: 'ABERTO',
    habilidadesNecessarias: [],
    vagas: 4,
    vagasPreenchidas: 1,
    dataFim: null,
    ...overrides,
  };
}

function paginaVazia(): PaginaResposta<Projeto> {
  return { content: [], totalElements: 0, totalPages: 1, number: 0, size: 20, last: true };
}

function renderPagina() {
  return render(
    <MemoryRouter>
      <Projetos />
    </MemoryRouter>,
  );
}

describe('Projetos', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('carrega e lista os projetos disponíveis', async () => {
    vi.spyOn(projetoService, 'listarProjetos').mockResolvedValue({
      content: [projeto()],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      last: true,
    });

    renderPagina();

    expect(await screen.findByText('Projeto Teste')).toBeInTheDocument();
    expect(screen.getByText(/1 oportunidades disponíveis/i)).toBeInTheDocument();
  });

  it('busca novamente com o texto digitado após o debounce', async () => {
    const listar = vi.spyOn(projetoService, 'listarProjetos').mockResolvedValue(paginaVazia());

    renderPagina();

    await waitFor(() =>
      expect(listar).toHaveBeenCalledWith({ busca: undefined, status: undefined, tamanho: 20 }),
    );

    await userEvent.type(screen.getByPlaceholderText(/buscar por título/i), 'IA');

    await waitFor(
      () => expect(listar).toHaveBeenLastCalledWith({ busca: 'IA', status: undefined, tamanho: 20 }),
      { timeout: 1000 },
    );
  });

  it('mostra mensagem de erro quando a busca falha', async () => {
    vi.spyOn(projetoService, 'listarProjetos').mockRejectedValue(new Error('erro de rede'));

    renderPagina();

    expect(
      await screen.findByText('Não foi possível carregar os projetos. Tente novamente.'),
    ).toBeInTheDocument();
  });
});
