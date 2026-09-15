import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import CriarProjeto from './criar-projeto';
import * as projetoService from '../services/projetoService';
import * as habilidadeService from '../services/habilidadeService';
import type { ProjetoDetalhe } from '../services/projetoService';

vi.mock('../services/projetoService');
vi.mock('../services/habilidadeService');

function projetoCriado(): ProjetoDetalhe {
  return {
    id: 'proj-1',
    criador: null,
    titulo: 'Sistema Inteligente de Proteção Web',
    descricao: 'Descrição com mais de vinte caracteres para passar na validação.',
    bannerUrl: null,
    status: 'ABERTO',
    vagas: 3,
    vagasPreenchidas: 0,
    vagasDisponiveis: 3,
    aceitandoCandidaturas: true,
    dataFim: null,
    ativo: true,
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
    habilidadesNecessarias: [],
    totalMembros: 0,
    totalCandidaturasPendentes: 0,
  };
}

function renderPagina() {
  return render(
    <MemoryRouter initialEntries={['/criar-projeto']}>
      <Routes>
        <Route path="/criar-projeto" element={<CriarProjeto />} />
        <Route path="/detalhes/:id" element={<p>Página de detalhes do projeto</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CriarProjeto', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(habilidadeService, 'listarHabilidades').mockResolvedValue({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, last: true,
    });
  });

  it('preenche o formulário mínimo e cria o projeto, navegando para os detalhes', async () => {
    vi.spyOn(projetoService, 'criarProjeto').mockResolvedValue(projetoCriado());

    renderPagina();

    await userEvent.type(
      screen.getByPlaceholderText('Ex.: Sistema Inteligente de Proteção Web'),
      'Sistema Inteligente de Proteção Web',
    );
    await userEvent.type(
      screen.getByPlaceholderText(/conte do que se trata o projeto/i),
      'Descrição com mais de vinte caracteres para passar na validação.',
    );

    await userEvent.click(screen.getByRole('button', { name: /criar projeto/i }));

    await waitFor(() =>
      expect(projetoService.criarProjeto).toHaveBeenCalledWith({
        titulo: 'Sistema Inteligente de Proteção Web',
        descricao: 'Descrição com mais de vinte caracteres para passar na validação.',
        vagas: 1,
        dataFim: undefined,
        habilidades: [],
      }),
    );

    expect(await screen.findByText('Página de detalhes do projeto')).toBeInTheDocument();
  });

  it('busca habilidades no backend (debounced) em vez de carregar o catálogo inteiro', async () => {
    vi.spyOn(habilidadeService, 'listarHabilidades').mockResolvedValue({
      content: [{ id: 'hab-react', nome: 'React', categoria: 'Frontend', descricao: null, usuariosCount: 1, projetosCount: 1, criadoEm: '2026-01-01T00:00:00Z' }],
      totalElements: 1, totalPages: 1, number: 0, size: 8, last: true,
    });

    renderPagina();

    // Não deve buscar nada até o usuário digitar (nunca carrega o catálogo inteiro).
    expect(habilidadeService.listarHabilidades).not.toHaveBeenCalled();

    await userEvent.type(screen.getByPlaceholderText(/buscar habilidade por nome ou categoria/i), 'rea');

    await waitFor(() =>
      expect(habilidadeService.listarHabilidades).toHaveBeenCalledWith({ busca: 'rea', tamanho: 8 }),
      { timeout: 1000 },
    );

    const sugestao = await screen.findByRole('button', { name: /react/i });
    await userEvent.click(sugestao);

    expect(screen.getByText('Obrigatória')).toBeInTheDocument();
  });
});
