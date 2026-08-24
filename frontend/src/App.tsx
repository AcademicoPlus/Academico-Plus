import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Importando as suas telas
import Login from './pages/login'
import Cadastro from './pages/cadastro'
import Feed from './pages/feedprojetos'
import Projeto from './pages/projetodetalhes'
import TermosDeUso from './pages/termosdeuso'
import PoliticaDePrivacidade from './pages/politicadeprivacidade'
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
        <Route path="/termos-de-uso" element={<TermosDeUso />} />
        <Route path="/politica-de-privacidade" element={<PoliticaDePrivacidade />} />

        {/* Rotas privadas: RotaProtegida redireciona para /login sem token */}
        <Route element={<RotaProtegida />}>
          <Route path="/feed" element={<Feed />} />
          {/* :id é lido em projetodetalhes.tsx via useParams para chamar GET /projetos/:id */}
          <Route path="/projeto/:id" element={<Projeto />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}