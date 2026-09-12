import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Badge from './Badge';

describe('Badge', () => {
  it('mostra o rótulo em português para um status conhecido', () => {
    render(<Badge variant="status" status="EM_ANDAMENTO">Em andamento</Badge>);
    expect(screen.getByText('Em andamento')).toBeInTheDocument();
  });

  it('renderiza o texto da habilidade quando variant="skill"', () => {
    render(<Badge variant="skill">React</Badge>);
    expect(screen.getByText('React')).toBeInTheDocument();
  });

  it('atribui sempre a mesma cor para o mesmo texto de habilidade', () => {
    const { container: primeiro } = render(<Badge variant="skill">React</Badge>);
    const { container: segundo } = render(<Badge variant="skill">React</Badge>);
    expect(primeiro.querySelector('span')?.className).toBe(segundo.querySelector('span')?.className);
  });
});
