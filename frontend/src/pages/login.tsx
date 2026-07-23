export default function Login() {
  return (
    <div className="flex flex-col md:flex-row min-h-screen w-full font-sans">
      
      {/* Lado Esquerdo (Fundo Azul) - Oculto no celular (hidden), aparece no PC (md:flex) */}
      <div className="hidden md:flex w-full md:w-1/2 bg-blue-950 text-white flex-col justify-center p-12 lg:p-24">
        <h2 className="text-4xl lg:text-5xl font-bold max-w-lg mb-6 leading-tight">
          O curador digital da sua jornada acadêmica.
        </h2>
        <p className="text-blue-200 text-lg max-w-md leading-relaxed">
          Organize, colabore e descubra. Um ambiente focado, projetado para afastar distrações e elevar o seu processo de pesquisa.
        </p>
      </div>

      {/* Lado Direito (Fundo Branco com o Formulário) */}
      <div className="w-full md:w-1/2 bg-white flex flex-col items-center justify-center p-6 md:p-8">
        
        {/* Container interno para alinhar tudo bonitinho */}
        <div className="w-full max-w-sm flex flex-col items-center">
          
          {/* Logo Academico+ simulada */}
          <div className="mb-12 flex items-center gap-2">
            <span className="text-3xl">⚲</span> {/* Ícone que lembra o Compasso do Figma */}
            <span className="text-2xl font-bold text-gray-900 tracking-tight">Academico +</span>
          </div>

          {/* Títulos proporcionais */}
          <h1 className="text-2xl font-semibold text-gray-900 mb-2 text-center">
            Login Institucional
          </h1>
          <p className="text-sm text-gray-500 mb-8 text-center">
            Acesse seu ambiente acadêmico curado.
          </p>

          {/* Botões Modernizados */}
          <button className="bg-blue-700 hover:bg-blue-800 text-white font-medium py-3 px-4 rounded-lg w-full mb-6 transition-all shadow-sm">
            Entrar com E-mail Acadêmico
          </button>

          <div className="flex items-center w-full mb-6">
            <div className="flex-grow border-t border-gray-200"></div>
            <span className="px-4 text-gray-400 text-xs font-medium uppercase tracking-wider">OU</span>
            <div className="flex-grow border-t border-gray-200"></div>
          </div>

          <button className="bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium py-3 px-4 rounded-lg w-full transition-all shadow-sm flex justify-center items-center gap-2">
            {/* Ícone SVG do Google desenhado via código */}
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="currentColor" d="M21.35,11.1H12.18V13.83H18.69C18.36,17.64 15.19,19.27 12.19,19.27C8.36,19.27 5,16.25 5,12C5,7.9 8.2,4.73 12.2,4.73C15.29,4.73 17.1,6.7 17.1,6.7L19,4.72C19,4.72 16.56,2 12.1,2C6.42,2 2.03,6.8 2.03,12C2.03,17.05 6.16,22 12.25,22C17.6,22 21.5,18.33 21.5,12.91C21.5,11.76 21.35,11.1 21.35,11.1V11.1Z" />
            </svg>
            Acessar via Google Workspace
          </button>

          {/* Links de Rodapé */}
          <div className="mt-8 flex flex-col items-center text-center">
            <a href="#" className="text-sm text-gray-600 hover:text-blue-700 hover:underline mb-8 transition-colors">
              Problemas para acessar?
            </a>
            <p className="text-xs text-gray-400">
              Ao entrar, você concorda com nossos{' '}
              <a href="#" className="font-medium text-gray-500 hover:underline">Termos</a> e{' '}
              <a href="#" className="font-medium text-gray-500 hover:underline">Privacidade</a>
            </p>
          </div>

        </div>
      </div>
    </div>
  )
}