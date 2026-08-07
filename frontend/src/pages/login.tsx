export default function Login() {
  return (
    // Fundo geral cinza super claro para o card branco destacar
    <div className="flex min-h-screen w-full bg-[#F8F9FA] font-sans">
      
      {/* Lado Esquerdo (Azul Vibrante) - Oculto no celular */}
      <div className="hidden lg:flex w-1/2 bg-[#1472dd] relative flex-col justify-center px-16 xl:px-24 overflow-hidden">
        
        {/* Efeito de círculos concêntricos sutis no fundo */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-200 h-200 border-30 border-white/5 rounded-full pointer-events-none"></div>
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-137.5 h-137.5 border-30 border-white/5 rounded-full pointer-events-none"></div>
        
        <div className="relative z-10 text-white max-w-lg">
          {/* Logo A+ */}
          <div className="bg-white/20 w-16 h-16 rounded-2xl flex items-center justify-center text-2xl font-bold mb-10 shadow-sm backdrop-blur-sm">
            A+
          </div>
          
          <h1 className="text-4xl xl:text-5xl font-bold mb-6 leading-tight">
            Conecte-se com projetos multidisciplinares
          </h1>
          
          <p className="text-[#a7ccfd] text-lg mb-16 leading-relaxed max-w-md">
            Crie projetos, recrute colaboradores por habilidades e faça parte de algo maior com colegas de outros cursos.
          </p>
          
          {/* Estatísticas */}
          <div className="flex gap-10">
            <div>
              <h3 className="text-3xl font-bold">120+</h3>
              <p className="text-[#a7ccfd] text-sm mt-1">Projetos ativos</p>
            </div>
            <div>
              <h3 className="text-3xl font-bold">850+</h3>
              <p className="text-[#a7ccfd] text-sm mt-1">Estudantes</p>
            </div>
            <div>
              <h3 className="text-3xl font-bold">45+</h3>
              <p className="text-[#a7ccfd] text-sm mt-1">Professores</p>
            </div>
          </div>
        </div>
      </div>

      {/* Lado Direito (Formulário) */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center items-center p-6">
        <div className="w-full max-w-md">
          
          {/* Cabeçalho do Formulário */}
          <div className="flex justify-between items-start mb-6 px-2">
            <div>
              <h2 className="text-3xl font-bold text-gray-900">Bem-vindo de volta</h2>
              <p className="text-gray-500 mt-1">Entre na sua conta institucional</p>
            </div>
            {/* Botão de Tema Escuro (Lua) */}
            <button className="p-2.5 bg-gray-100 rounded-full text-gray-500 hover:bg-gray-200 transition-colors">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path>
              </svg>
            </button>
          </div>

          {/* Card Branco */}
          <div className="bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-8">
            
            <form className="flex flex-col gap-5">
              {/* Campo E-mail */}
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-2">E-mail institucional</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    {/* Ícone de Envelope */}
                    <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                    </svg>
                  </div>
                  <input 
                    type="email" 
                    placeholder="ana.silva@academico.edu.br" 
                    className="w-full pl-11 pr-4 py-3 bg-gray-50 border border-transparent rounded-xl focus:bg-white focus:border-[#1472dd] focus:ring-2 focus:ring-[#1472dd]/20 outline-none transition-all text-gray-700"
                  />
                </div>
              </div>

              {/* Campo Senha */}
              <div>
                <label className="block text-sm font-medium text-gray-900 mb-2">Senha</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    {/* Ícone de Cadeado */}
                    <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                    </svg>
                  </div>
                  <input 
                    type="password" 
                    defaultValue="........"
                    className="w-full pl-11 pr-12 py-3 bg-gray-50 border border-transparent rounded-xl focus:bg-white focus:border-[#1472dd] focus:ring-2 focus:ring-[#1472dd]/20 outline-none transition-all text-gray-700 font-mono tracking-widest"
                  />
                  <div className="absolute inset-y-0 right-0 pr-4 flex items-center cursor-pointer">
                    {/* Ícone de Olho */}
                    <svg className="h-5 w-5 text-gray-400 hover:text-gray-600 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                  </div>
                </div>
              </div>

              {/* Lembrar-me e Esqueci a Senha */}
              <div className="flex items-center justify-between mt-1 mb-2">
                <label className="flex items-center cursor-pointer">
                  <input type="checkbox" defaultChecked className="w-4 h-4 rounded text-[#1472dd] border-gray-300 focus:ring-[#1472dd]" />
                  <span className="ml-2 text-sm text-gray-500">Lembrar-me</span>
                </label>
                <a href="#" className="text-sm text-[#1472dd] hover:underline font-medium">Esqueci a senha</a>
              </div>

              {/* Botão Entrar */}
              <button type="button" className="w-full bg-[#1472dd] hover:bg-[#105db8] text-white font-medium py-3.5 rounded-xl transition-colors shadow-md shadow-[#1472dd]/20">
                Entrar na plataforma
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              Não tem conta? <a href="#" className="text-[#1472dd] font-semibold hover:underline">Cadastre-se</a>
            </p>
          </div>
          
          {/* Rodapé */}
          <p className="text-center text-xs text-gray-400 mt-10">
            UNIFAPI - Sistema Acadêmico Integrado
          </p>
          
        </div>
      </div>
    </div>
  )
}