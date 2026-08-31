// Hook + acesso em cache para GET /usuarios/me. Antes, cada tela/guard que
// precisava saber quem é o usuário logado chamava obterMeuPerfil() por conta
// própria (menu lateral, dashboard, guards de rota, páginas de perfil...),
// disparando várias requisições idênticas e simultâneas a cada navegação.
// Com o QueryClient compartilhado, todas essas chamadas caem no mesmo cache
// e requisições concorrentes para a mesma chave são deduplicadas.
import { useQuery } from '@tanstack/react-query';
import { obterMeuPerfil, type UsuarioPerfil } from '../services/usuarioService';
import { queryClient } from '../services/queryClient';

export const MEU_PERFIL_QUERY_KEY = ['meuPerfil'] as const;

export function useMeuPerfil() {
  return useQuery({
    queryKey: MEU_PERFIL_QUERY_KEY,
    queryFn: obterMeuPerfil,
  });
}

// Para fluxos imperativos (fora de componentes React, ou dentro de um
// Promise.all junto com outras chamadas) que ainda precisam do valor direto
// em vez do estado reativo do hook — reaproveita o mesmo cache do useQuery.
// revalidateIfStale garante que, se o cache já estiver marcado como stale
// (por invalidateQueries ou pelo staleTime), uma revalidação é disparada em
// segundo plano — sem isso, ensureQueryData devolveria o valor em cache pra
// sempre, mesmo depois de invalidado.
export function obterMeuPerfilCache(): Promise<UsuarioPerfil> {
  return queryClient.ensureQueryData({
    queryKey: MEU_PERFIL_QUERY_KEY,
    queryFn: obterMeuPerfil,
    revalidateIfStale: true,
  });
}

// Escreve direto no cache o perfil já atualizado que voltou de uma mutação
// (PUT /usuarios/me, upload de foto). Preferível a invalidar e esperar um
// novo fetch: o dado já veio fresco na resposta, então não há por que pedir
// de novo — e evita a tela seguinte mostrar dado velho enquanto um refetch
// em segundo plano ainda não terminou (o banco é remoto e pode demorar).
export function atualizarCacheMeuPerfil(perfil: UsuarioPerfil): void {
  queryClient.setQueryData(MEU_PERFIL_QUERY_KEY, perfil);
}

// Mesma ideia, mas para mutações de habilidade (adicionar/remover), que só
// devolvem o registro afetado, não o perfil inteiro — atualiza só a lista.
export function atualizarHabilidadesNoCacheMeuPerfil(
  habilidades: UsuarioPerfil['habilidades'],
): void {
  queryClient.setQueryData<UsuarioPerfil>(MEU_PERFIL_QUERY_KEY, (atual) =>
    atual ? { ...atual, habilidades } : atual,
  );
}
