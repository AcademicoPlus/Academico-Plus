import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Importando as suas telas
import Login from './pages/login'
import Cadastro from './pages/cadastro'
import Feed from './pages/feedprojetos'
import Projeto from './pages/projetodetalhes'
// Guard que só deixa passar quem tem um token JWT salvo (ver rotaprotegida.tsx)
import RotaProtegida from './routes/rotaprotegida'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Se a pessoa acessar a raiz do site, joga ela pro login */}
        <Route path="/" element={<Navigate to="/login" />} />

        {/* Rotas públicas: não exigem login */}
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />

        {/* Rotas privadas: RotaProtegida redireciona para /login sem token */}
        <Route element={<RotaProtegida />}>
          <Route path="/feed" element={<Feed />} />
          <Route path="/projeto" element={<Projeto />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}