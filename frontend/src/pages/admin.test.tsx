import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import Admin from './admin';

describe('Admin', () => {
  it('renderiza os links de navegação para as áreas administrativas', () => {
    render(
      <MemoryRouter>
        <Admin />
      </MemoryRouter>,
    );

    const linkUsuarios = screen.getByRole('link', { name: /usuários/i });
    const linkHabilidades = screen.getByRole('link', { name: /habilidades/i });
    const linkDenuncias = screen.getByRole('link', { name: /denúncias/i });

    expect(linkUsuarios).toHaveAttribute('href', '/admin/usuarios');
    expect(linkHabilidades).toHaveAttribute('href', '/admin/habilidades');
    expect(linkDenuncias).toHaveAttribute('href', '/admin/denuncias');
  });
});
