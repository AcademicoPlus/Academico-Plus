import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import EsqueciSenha from './esqueci-senha';
import * as authService from '../services/authService';
import { ApiError } from '../services/apiClient';

vi.mock('../services/authService');

describe('EsqueciSenha', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('avança para a etapa de código quando o envio é bem-sucedido', async () => {
    vi.spyOn(authService, 'solicitarResetSenha').mockResolvedValue({ sucesso: true, mensagem: '' });
    vi.spyOn(authService, 'previewResetSenha').mockResolvedValue({
      sucesso: true,
      mensagem: '',
      emailPreview: 'a***@academico.edu.br',
    });

    render(
      <MemoryRouter>
        <EsqueciSenha />
      </MemoryRouter>,
    );

    await userEvent.type(
      screen.getByPlaceholderText('ana.silva@academico.edu.br'),
      'ana@academico.edu.br',
    );
    await userEvent.click(screen.getByRole('button', { name: /enviar código/i }));

    await waitFor(() => expect(authService.solicitarResetSenha).toHaveBeenCalledWith('ana@academico.edu.br'));
    expect(await screen.findByRole('heading', { name: 'Digite o código' })).toBeInTheDocument();
    expect(screen.getByText(/a\*\*\*@academico\.edu\.br/)).toBeInTheDocument();
  });

  it('mostra a mensagem de erro quando o backend falha ao solicitar o código', async () => {
    vi.spyOn(authService, 'solicitarResetSenha').mockRejectedValue(
      new ApiError(404, 'E-mail não encontrado.'),
    );

    render(
      <MemoryRouter>
        <EsqueciSenha />
      </MemoryRouter>,
    );

    await userEvent.type(
      screen.getByPlaceholderText('ana.silva@academico.edu.br'),
      'inexistente@academico.edu.br',
    );
    await userEvent.click(screen.getByRole('button', { name: /enviar código/i }));

    expect(await screen.findByText('E-mail não encontrado.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Esqueci minha senha' })).toBeInTheDocument();
  });
});
