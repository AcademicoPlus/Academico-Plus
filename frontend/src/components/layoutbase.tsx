import { Outlet } from 'react-router-dom'
import Header from './header'
import MenuLateral from './menulateral'

export default function LayoutBase() {
  return (
    <div className="min-h-screen bg-[#F8F9FA] flex flex-col font-sans">
      
      {/* O Cabeçalho fica no topo */}
      <Header />
      
      <div className="flex flex-1">
        {/* O Menu fica na lateral esquerda */}
        <MenuLateral />
        
        {/* Aqui é o espaço livre onde as telas vão renderizar! */}
        <main className="flex-1 p-8 overflow-y-auto">
          <Outlet />
        </main>
      </div>

    </div>
  )
}