// Guard de rotas autenticadas. Envolve as rotas privadas no App.tsx e usa
// <Outlet /> do react-router para renderizar a rota filha somente quando
// existe um token JWT salvo (ver src/utils/auth.ts). Sem token, redireciona
// para /login em vez de deixar a tela renderizar e falhar as chamadas à API.
import { Navigate, Outlet } from 'react-router-dom';
import { estaAutenticado } from '../utils/auth';

export default function RotaProtegida() {
  if (!estaAutenticado()) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
