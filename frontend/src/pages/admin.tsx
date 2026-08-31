import { Link } from 'react-router-dom'

const SECOES = [
  {
    to: '/admin/usuarios',
    titulo: 'Usuários',
    descricao: 'Consulte todos os usuários da plataforma e gerencie o nível de acesso de cada um.',
    icone: (
      <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-1.13a4 4 0 10-4-4 4 4 0 004 4zm6 0a4 4 0 10-4-4"></path></svg>
    ),
  },
  {
    to: '/admin/habilidades',
    titulo: 'Habilidades',
    descricao: 'Gerencie o catálogo de habilidades usado nos perfis e nos projetos.',
    icone: (
      <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
    ),
  },
  {
    to: '/admin/denuncias',
    titulo: 'Denúncias',
    descricao: 'Revise denúncias de avaliações abusivas e decida se procede a remoção.',
    icone: (
      <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
    ),
  },
]

export default function Admin() {
  return (
    <div className="pb-12 max-w-7xl mx-auto">
      <header className="mb-10">
        <h1 className="text-3xl md:text-4xl font-extrabold text-[#183E6C] tracking-tight">Administração</h1>
        <p className="text-gray-500 mt-2 font-medium">Ferramentas de gestão da plataforma, disponíveis apenas para administradores.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {SECOES.map((secao) => (
          <Link
            key={secao.to}
            to={secao.to}
            className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 hover:shadow-md hover:border-[#F27405]/30 transition-all group"
          >
            <div className="w-14 h-14 rounded-2xl bg-orange-50 text-[#F27405] flex items-center justify-center mb-6 group-hover:bg-[#F27405] group-hover:text-white transition-colors">
              {secao.icone}
            </div>
            <h2 className="text-xl font-extrabold text-[#183E6C] mb-2">{secao.titulo}</h2>
            <p className="text-gray-500 text-sm leading-relaxed">{secao.descricao}</p>
          </Link>
        ))}
      </div>
    </div>
  )
}
