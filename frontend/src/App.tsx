import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Importando as suas telas
import Login from './pages/login'
import Cadastro from './pages/cadastro'
import Feed from './pages/feedprojetos'
import Projeto from './pages/projetodetalhes'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Se a pessoa acessar a raiz do site, joga ela pro login */}
        <Route path="/" element={<Navigate to="/login" />} />
        
        {/* Aqui estão os endereços de cada tela */}
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/feed" element={<Feed />} />
        <Route path="/projeto" element={<Projeto />} />
      </Routes>
    </BrowserRouter>
  )
}