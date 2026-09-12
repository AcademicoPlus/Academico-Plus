import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export type Tema = 'light' | 'dark';

const CHAVE_ARMAZENAMENTO = 'academico:tema';

type ThemeContextValor = {
  tema: Tema;
  alternarTema: () => void;
};

const ThemeContext = createContext<ThemeContextValor | null>(null);

function lerTemaInicial(): Tema {
  const salvo = localStorage.getItem(CHAVE_ARMAZENAMENTO);
  if (salvo === 'light' || salvo === 'dark') {
    return salvo;
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(lerTemaInicial);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'dark');
    localStorage.setItem(CHAVE_ARMAZENAMENTO, tema);
  }, [tema]);

  function alternarTema() {
    setTema((atual) => (atual === 'dark' ? 'light' : 'dark'));
  }

  return <ThemeContext.Provider value={{ tema, alternarTema }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const contexto = useContext(ThemeContext);
  if (!contexto) {
    throw new Error('useTheme precisa ser usado dentro de um ThemeProvider');
  }
  return contexto;
}
