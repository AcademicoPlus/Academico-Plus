export default function Login() {
  return (
    // A caixa principal que pega a tela toda (h-screen) e fica em linha (flex)
    <div className="flex h-screen w-full">
      
      {/* Lado Esquerdo (Fundo Azul) */}
      <div className="w-1/2 bg-blue-900 text-white flex flex-col justify-center p-12">
        <h2>O curador digital da sua jornada acadêmica.</h2>
      </div>

      {/* Lado Direito (Fundo Branco com o Formulário) */}
      <div className="w-1/2 bg-white flex items-center justify-center">
        <h1>Login Institucional</h1>
      </div>

    </div>
  )
}
