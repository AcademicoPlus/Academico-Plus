import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import Candidaturas from './candidaturas';
import * as candidaturaService from '../services/candidaturaService';
import type { Candidatura } from '../services/candidaturaService';
import type { PaginaResposta } from '../services/apiClient';

vi.mock('../services/candidaturaService');

function candidatura(overrides: Partial<Candidatura> = {}): Candidatura {
  return {
    id: 'cand-1',
    projeto: { id: 'proj-1', titulo: 'Projeto Teste', status: 'ABERTO' },
    usuario: null,
    status: 'PENDENTE',
    mensagem: null,
    motivoRejeicao: null,
    dataCandidatura: '2026-01-01T00:00:00Z',
    dataResposta: null,
    ...overrides,
  };
}

function paginaVazia(): PaginaResposta<Candidatura> {
  return { content: [], totalElements: 0, totalPages: 1, number: 0, size: 50, last: true };
}

function renderPagina() {
  return render(
    <MemoryRouter>
      <Candidaturas />
    </MemoryRouter>,
  );
}

describe('Candidaturas', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('carrega e lista as candidaturas com a contagem por status', async () => {
    vi.spyOn(candidaturaService, 'listarMinhasCandidaturas').mockResolvedValue({
      content: [
        candidatura({ id: 'cand-1', status: 'PENDENTE' }),
        candidatura({
          id: 'cand-2',
          status: 'ACEITO',
          projeto: { id: 'proj-2', titulo: 'Projeto Beta', status: 'EM_ANDAMENTO' },
        }),
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 50,
      last: true,
    });

    renderPagina();

    expect(await screen.findByText('Projeto Teste')).toBeInTheDocument();
    expect(screen.getByText('Projeto Beta')).toBeInTheDocument();
    expect(screen.getByText('2 candidatura(s) no total')).toBeInTheDocument();

    const contagemPendente = screen.getByText('🕒 Pendente').previousElementSibling;
    const contagemAceito = screen.getByText('✓ Aceito').previousElementSibling;
    expect(contagemPendente).toHaveTextContent('1');
    expect(contagemAceito).toHaveTextContent('1');
  });

  it('cancela uma candidatura pendente e remove da lista', async () => {
    vi.spyOn(candidaturaService, 'listarMinhasCandidaturas').mockResolvedValue({
      content: [candidatura()],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
      last: true,
    });
    vi.spyOn(candidaturaService, 'cancelarCandidatura').mockResolvedValue({ sucesso: true, mensagem: 'ok' });

    renderPagina();

    await screen.findByText('Projeto Teste');
    await userEvent.click(screen.getByRole('button', { name: /cancelar candidatura/i }));

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /^confirmar$/i }));

    await waitFor(() => expect(candidaturaService.cancelarCandidatura).toHaveBeenCalledWith('cand-1'));
    await waitFor(() => expect(screen.queryByText('Projeto Teste')).not.toBeInTheDocument());
  });

  it('mostra a mensagem de lista vazia quando não há candidaturas', async () => {
    vi.spyOn(candidaturaService, 'listarMinhasCandidaturas').mockResolvedValue(paginaVazia());

    renderPagina();

    expect(
      await screen.findByText('Você ainda não se candidatou a nenhum projeto.'),
    ).toBeInTheDocument();
  });
});
