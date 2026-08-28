import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// 1. Importando o "Esqueleto" (Header e Menu)
import LayoutBase from './components/layout-base'

// 2. Importando as Telas Públicas (Fora do sistema)
import Login from './pages/login'
import Cadastro from './pages/cadastro'
import TermosDeUso from './pages/termosdeuso'
import PoliticaDePrivacidade from './pages/politicadeprivacidade'

// 3. Importando as Telas Logadas (Dentro do sistema)
import Dashboard from './pages/dashboard'
import Projetos from './pages/projetos'
import DetalhesProjeto from './pages/detalhes-projeto'
import Candidaturas from './pages/candidaturas'
import CriarProjeto from './pages/criar-projeto'
import EditarProjeto from './pages/editar-projeto'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Redireciona quem entra no site direto para a tela de Login */}
        <Route path="/" element={<Navigate to="/login" />} />
        
        {/* ROTAS PÚBLICAS (Ocupam a tela toda, sem o menu) */}
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/termos-de-uso" element={<TermosDeUso />} />
        <Route path="/politica-de-privacidade" element={<PoliticaDePrivacidade />} />
        
        {/* ROTAS DA ÁREA LOGADA (O "LayoutBase" abraça todas essas telas) */}
        <Route element={<LayoutBase />}>
          
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/projetos" element={<Projetos />} />
          
          {/* ALTERADO AQUI: Adicionado o /:id para carregar o projeto certo */}
          <Route path="/detalhes/:id" element={<DetalhesProjeto />} />
          
          <Route path="/candidaturas" element={<Candidaturas />} />
          
          {/* Telas que faremos no futuro (Aparece só um texto provisório por enquanto) */}
          <Route path="/pessoas" element={
            <div className="p-8 text-[#183E6C] font-bold text-xl">Tela "Explorar Pessoas" em construção...</div>
          } />
          <Route path="/criar-projeto" element={<CriarProjeto />} />
          <Route path="/editar-projeto/:id" element={<EditarProjeto />} />
          
        </Route>
      </Routes>
    </BrowserRouter>
  )
}