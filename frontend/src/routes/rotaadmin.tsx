// Guard das rotas /admin/*. Além de exigir login (RotaProtegida já cuida
// disso por fora), confere se o usuário logado tem nivelAcesso "ADMIN" antes
// de liberar o <Outlet />. Sem isso, qualquer usuário autenticado poderia
// acessar as telas de admin digitando a URL direto.
import { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { obterMeuPerfilCache } from '../hooks/useMeuPerfil';

export default function RotaAdmin() {
  const [carregando, setCarregando] = useState(true);
  const [ehAdmin, setEhAdmin] = useState(false);

  useEffect(() => {
    let ativo = true;
    obterMeuPerfilCache()
      .then((perfil) => {
        if (ativo) setEhAdmin(perfil.nivelAcesso?.nome?.toUpperCase() === 'ADMIN');
      })
      .catch(() => {
        if (ativo) setEhAdmin(false);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => { ativo = false; };
  }, []);

  if (carregando) {
    return (
      <div className="flex justify-center py-20">
        <p className="text-lg font-bold text-gray-400 animate-pulse">Verificando permissões...</p>
      </div>
    );
  }

  if (!ehAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
