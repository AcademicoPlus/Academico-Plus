import { Outlet } from 'react-router-dom'
import Header from './header'
import MenuLateral from './menu-lateral'

export default function LayoutBase() {
  return (
    <div className="min-h-screen bg-[#F8F9FA] flex flex-col font-sans">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        {/* O MenuLateral fica oculto em telas pequenas (hidden lg:flex) */}
        <MenuLateral />
        <main className="flex-1 p-4 md:p-8 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}