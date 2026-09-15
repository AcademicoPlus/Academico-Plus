import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import EditarProjetoRota from './editar-projeto';
import * as projetoService from '../services/projetoService';
import * as habilidadeService from '../services/habilidadeService';
import * as projetoHabilidadeService from '../services/projetoHabilidadeService';
import * as useMeuPerfilHook from '../hooks/useMeuPerfil';
import type { ProjetoDetalhe } from '../services/projetoService';
import type { UsuarioResumo } from '../services/authService';

vi.mock('../services/projetoService');
vi.mock('../services/habilidadeService');
vi.mock('../services/projetoHabilidadeService');
vi.mock('../hooks/useMeuPerfil');

const CRIADOR: UsuarioResumo = {
  id: 'user-1', nome: 'Ana Criadora', curso: 'Ciência da Computação', fotoUrl: null,
  permission: 'ALUNO', periodo: 5, notaMedia: null, totalAvaliacoes: null,
};

function projetoBase(overrides: Partial<ProjetoDetalhe> = {}): ProjetoDetalhe {
  return {
    id: 'proj-1',
    criador: CRIADOR,
    titulo: 'Projeto Original',
    descricao: 'Descrição original do projeto de teste com bastante texto.',
    bannerUrl: null,
    status: 'ABERTO',
    vagas: 3,
    vagasPreenchidas: 1,
    vagasDisponiveis: 2,
    aceitandoCandidaturas: true,
    dataFim: null,
    ativo: true,
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
    habilidadesNecessarias: [],
    totalMembros: 0,
    totalCandidaturasPendentes: 0,
    ...overrides,
  };
}

function renderPagina() {
  return render(
    <MemoryRouter initialEntries={['/editar/proj-1']}>
      <Routes>
        <Route path="/editar/:id" element={<EditarProjetoRota />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('EditarProjeto', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(habilidadeService, 'listarHabilidades').mockResolvedValue({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, last: true,
    });
  });

  it('carrega os dados existentes do projeto no formulário', async () => {
    vi.spyOn(useMeuPerfilHook, 'obterMeuPerfilCache').mockResolvedValue({ id: 'user-1' } as never);
    vi.spyOn(projetoService, 'buscarProjetoPorId').mockResolvedValue(projetoBase());

    renderPagina();

    expect(await screen.findByDisplayValue('Projeto Original')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Descrição original do projeto de teste com bastante texto.')).toBeInTheDocument();
    expect(screen.getByDisplayValue('3')).toBeInTheDocument();
  });

  it('salva as alterações do formulário chamando atualizarProjeto', async () => {
    vi.spyOn(useMeuPerfilHook, 'obterMeuPerfilCache').mockResolvedValue({ id: 'user-1' } as never);
    vi.spyOn(projetoService, 'buscarProjetoPorId').mockResolvedValue(projetoBase());
    vi.spyOn(projetoService, 'atualizarProjeto').mockResolvedValue(
      projetoBase({ titulo: 'Projeto Atualizado' }),
    );

    renderPagina();

    const campoTitulo = await screen.findByDisplayValue('Projeto Original');
    await userEvent.clear(campoTitulo);
    await userEvent.type(campoTitulo, 'Projeto Atualizado');

    await userEvent.click(screen.getByRole('button', { name: /salvar alterações/i }));

    await waitFor(() =>
      expect(projetoService.atualizarProjeto).toHaveBeenCalledWith('proj-1', {
        titulo: 'Projeto Atualizado',
        descricao: 'Descrição original do projeto de teste com bastante texto.',
        vagas: 3,
        dataFim: undefined,
      }),
    );

    expect(await screen.findByText('Alterações salvas.')).toBeInTheDocument();
  });

  it('busca habilidades no backend (debounced) e adiciona ao projeto', async () => {
    vi.spyOn(useMeuPerfilHook, 'obterMeuPerfilCache').mockResolvedValue({ id: 'user-1' } as never);
    vi.spyOn(projetoService, 'buscarProjetoPorId').mockResolvedValue(projetoBase());
    vi.spyOn(habilidadeService, 'listarHabilidades').mockResolvedValue({
      content: [{ id: 'hab-react', nome: 'React', categoria: 'Frontend', descricao: null, usuariosCount: 1, projetosCount: 1, criadoEm: '2026-01-01T00:00:00Z' }],
      totalElements: 1, totalPages: 1, number: 0, size: 8, last: true,
    });

    vi.spyOn(projetoHabilidadeService, 'vincularHabilidadeAoProjeto').mockResolvedValue({
      id: 'vinculo-1',
      habilidade: { id: 'hab-react', nome: 'React', categoria: 'Frontend' },
      obrigatoria: false,
    });

    renderPagina();
    await screen.findByDisplayValue('Projeto Original');

    expect(habilidadeService.listarHabilidades).not.toHaveBeenCalled();

    await userEvent.type(screen.getByPlaceholderText(/buscar habilidade por nome ou categoria/i), 'rea');

    await waitFor(() =>
      expect(habilidadeService.listarHabilidades).toHaveBeenCalledWith({ busca: 'rea', tamanho: 8 }),
      { timeout: 1000 },
    );

    await userEvent.click(await screen.findByRole('button', { name: /react/i }));

    await waitFor(() => expect(projetoHabilidadeService.vincularHabilidadeAoProjeto).toHaveBeenCalledWith('proj-1', 'hab-react', false));
  });
});
