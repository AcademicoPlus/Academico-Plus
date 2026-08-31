// Client único do TanStack React Query para o app. O banco de dados fica
// remoto (Render, Oregon) mesmo em dev — ver docs/MEMORIA — então evitamos
// refetch automático agressivo (foco de janela, remontagem) para não
// multiplicar requisições desnecessárias sobre uma conexão já mais lenta.
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
