import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Importando as páginas
import Login from './pages/login'
import Cadastro from './pages/cadastro'
import Feed from './pages/feedprojetos'
import Projeto from './pages/projetodetalhes'

// Importando o Layout (Esqueleto)
import LayoutBase from './components/layoutbase'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rotas Públicas (Sem Menu e sem Header) */}
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        
        {/* Rotas Protegidas (COM Menu e Header) */}
        {/* Tudo que estiver dentro desta Route vai herdar o layout! */}
        <Route element={<LayoutBase />}>
          <Route path="/feed" element={<Feed />} />
          <Route path="/projeto" element={<Projeto />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}