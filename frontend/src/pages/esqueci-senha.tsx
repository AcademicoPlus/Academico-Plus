import { Link, useNavigate } from 'react-router-dom';
import { useState, type FormEvent } from 'react';
import logo from '../assets/logo.png';
import {
  previewResetSenha,
  redefinirSenha,
  solicitarResetSenha,
  validarCodigoReset,
} from '../services/authService';
import { ApiError } from '../services/apiClient';

// O fluxo do backend tem 3 passos (solicitar -> validar -> redefinir), então a
// tela é um wizard: cada etapa só aparece depois que a anterior deu certo.
type Etapa = 'email' | 'codigo' | 'senha';

const MIN_SENHA = 8; // mesmo mínimo do ResetRedefinirRequest no backend

export default function EsqueciSenha() {
  const navigate = useNavigate();

  const [etapa, setEtapa] = useState<Etapa>('email');

  const [email, setEmail] = useState('');
  const [emailPreview, setEmailPreview] = useState('');
  const [codigo, setCodigo] = useState('');
  const [tokenRedefinicao, setTokenRedefinicao] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);

  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  function mensagemDeErro(erroCapturado: unknown) {
    return erroCapturado instanceof ApiError
      ? erroCapturado.message
      : 'Não foi possível conectar ao servidor. Tente novamente.';
  }

  // Passo 1 — dispara o e-mail com o OTP. O preview é só pra confirmar pro
  // usuário qual endereço recebeu o código (vem mascarado do backend).
  async function handleEnviarCodigo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErro(null);
    setAviso(null);
    setCarregando(true);

    try {
      await solicitarResetSenha(email);

      // O preview é um extra: se falhar, o código já foi enviado do mesmo
      // jeito, então seguimos em frente mostrando o e-mail digitado.
      try {
        const preview = await previewResetSenha(email);
        setEmailPreview(preview.emailPreview);
      } catch {
        setEmailPreview(email);
      }

      setEtapa('codigo');
    } catch (erroCapturado) {
      setErro(mensagemDeErro(erroCapturado));
    } finally {
      setCarregando(false);
    }
  }

  // Passo 2 — troca o OTP pelo token de curta duração usado no passo 3.
  async function handleValidarCodigo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErro(null);
    setAviso(null);
    setCarregando(true);

    try {
      const resposta = await validarCodigoReset(email, codigo);
      setTokenRedefinicao(resposta.tokenRedefinicao);
      setEtapa('senha');
    } catch (erroCapturado) {
      setErro(mensagemDeErro(erroCapturado));
    } finally {
      setCarregando(false);
    }
  }

  // Passo 3 — grava a nova senha e devolve o usuário pro login.
  async function handleRedefinir(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErro(null);
    setAviso(null);

    if (novaSenha.length < MIN_SENHA) {
      setErro(`A nova senha deve ter no mínimo ${MIN_SENHA} caracteres.`);
      return;
    }

    if (novaSenha !== confirmarSenha) {
      setErro('As senhas não coincidem.');
      return;
    }

    setCarregando(true);
    try {
      await redefinirSenha(tokenRedefinicao, novaSenha);
      navigate('/login');
    } catch (erroCapturado) {
      setErro(mensagemDeErro(erroCapturado));
    } finally {
      setCarregando(false);
    }
  }

  async function handleReenviarCodigo() {
    setErro(null);
    setAviso(null);
    setCarregando(true);

    try {
      await solicitarResetSenha(email);
      setAviso('Enviamos um novo código para o seu e-mail.');
    } catch (erroCapturado) {
      setErro(mensagemDeErro(erroCapturado));
    } finally {
      setCarregando(false);
    }
  }

  function voltarParaEmail() {
    setErro(null);
    setAviso(null);
    setCodigo('');
    setEtapa('email');
  }

  const textosDoCabecalho: Record<Etapa, { titulo: string; subtitulo: string }> = {
    email: {
      titulo: 'Esqueci minha senha',
      subtitulo: 'Informe seu e-mail institucional para receber um código',
    },
    codigo: {
      titulo: 'Digite o código',
      subtitulo: `Enviamos um código de 6 dígitos para ${emailPreview}`,
    },
    senha: {
      titulo: 'Nova senha',
      subtitulo: 'Escolha uma senha para acessar sua conta',
    },
  };

  const inputBase =
    'w-full pl-11 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-transparent rounded-xl focus:bg-white dark:focus:bg-slate-800 focus:border-[#F27405] focus:ring-2 focus:ring-[#F27405]/20 outline-none transition-all text-gray-700 dark:text-gray-100';

  return (
    <div className="flex min-h-screen w-full bg-[#F8F9FA] dark:bg-slate-950 font-sans">

      {/* Lado Esquerdo (Azul Marinho) */}
      <div className="hidden lg:flex w-1/2 bg-[#183E6C] relative flex-col justify-center px-16 xl:px-24 overflow-hidden">

        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-200 h-200 border-30 border-white/5 rounded-full pointer-events-none"></div>
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-137.5 h-137.5 border-30 border-white/5 rounded-full pointer-events-none"></div>

        <div className="relative z-10 text-white max-w-lg">

          <h1 className="text-4xl xl:text-5xl font-bold mb-6 leading-tight">
            Recupere o acesso à sua conta
          </h1>

          <p className="text-[#B2C6E0] text-lg mb-16 leading-relaxed max-w-md">
            Em três passos rápidos você volta a criar projetos e colaborar com os seus colegas.
          </p>
        </div>
      </div>

      {/* Lado Direito (Formulário) */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center items-center p-6">
        <div className="w-full max-w-md">

          <div className="mb-8 px-2">
            <img src={logo} alt="Logo Acadêmico+" className="h-12 object-contain" />
          </div>

          <div className="mb-6 px-2">
            <h2 className="text-3xl font-bold text-[#183E6C] dark:text-blue-300">{textosDoCabecalho[etapa].titulo}</h2>
            <p className="text-gray-500 dark:text-gray-300 mt-1">{textosDoCabecalho[etapa].subtitulo}</p>
          </div>

          {/* Indicador de progresso das 3 etapas */}
          <div className="flex gap-2 mb-6 px-2">
            {(['email', 'codigo', 'senha'] as Etapa[]).map((passo, indice) => {
              const indiceAtual = ['email', 'codigo', 'senha'].indexOf(etapa);
              return (
                <div
                  key={passo}
                  className={`h-1.5 flex-1 rounded-full transition-colors ${
                    indice <= indiceAtual ? 'bg-[#F27405]' : 'bg-gray-200 dark:bg-slate-700'
                  }`}
                />
              );
            })}
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 dark:border-slate-700 p-8">

            {erro && (
              <div className="mb-5 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-100 dark:border-red-900/50 px-4 py-3 text-sm text-red-600 dark:text-red-400">
                {erro}
              </div>
            )}

            {aviso && (
              <div className="mb-5 rounded-xl bg-green-50 dark:bg-green-950/40 border border-green-100 dark:border-green-900/50 px-4 py-3 text-sm text-green-700 dark:text-green-400">
                {aviso}
              </div>
            )}

            {etapa === 'email' && (
              <form className="flex flex-col gap-5" onSubmit={handleEnviarCodigo}>
                <div>
                  <label className="block text-sm font-medium text-[#183E6C] dark:text-blue-300 mb-2">E-mail institucional</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg className="h-5 w-5 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                      </svg>
                    </div>
                    <input
                      type="email"
                      placeholder="ana.silva@academico.edu.br"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      autoFocus
                      className={inputBase}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={carregando}
                  className="w-full bg-[#F27405] hover:bg-[#D96704] text-white font-bold py-3.5 rounded-xl transition-colors shadow-lg shadow-[#F27405]/30 mt-2 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {carregando ? 'Enviando...' : 'Enviar código'}
                </button>
              </form>
            )}

            {etapa === 'codigo' && (
              <form className="flex flex-col gap-5" onSubmit={handleValidarCodigo}>
                <div>
                  <label className="block text-sm font-medium text-[#183E6C] dark:text-blue-300 mb-2">Código de verificação</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="000000"
                    value={codigo}
                    // Só dígitos: o backend gera um OTP numérico de 6 posições.
                    onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    required
                    autoFocus
                    className="w-full px-4 py-3 bg-gray-50 dark:bg-slate-800 border border-transparent rounded-xl focus:bg-white dark:focus:bg-slate-800 focus:border-[#F27405] focus:ring-2 focus:ring-[#F27405]/20 outline-none transition-all text-gray-700 dark:text-gray-100 text-center text-2xl font-mono tracking-[0.5em]"
                  />
                  <p className="text-xs text-gray-400 dark:text-gray-500 mt-2">O código expira em 15 minutos.</p>
                </div>

                <button
                  type="submit"
                  disabled={carregando || codigo.length < 6}
                  className="w-full bg-[#F27405] hover:bg-[#D96704] text-white font-bold py-3.5 rounded-xl transition-colors shadow-lg shadow-[#F27405]/30 mt-2 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {carregando ? 'Validando...' : 'Validar código'}
                </button>

                <div className="flex items-center justify-between text-sm">
                  <button
                    type="button"
                    onClick={voltarParaEmail}
                    className="text-gray-500 dark:text-gray-300 hover:text-[#183E6C] dark:hover:text-blue-300 transition-colors"
                  >
                    Trocar e-mail
                  </button>
                  <button
                    type="button"
                    onClick={handleReenviarCodigo}
                    disabled={carregando}
                    className="text-[#F27405] font-medium hover:underline disabled:opacity-60"
                  >
                    Reenviar código
                  </button>
                </div>
              </form>
            )}

            {etapa === 'senha' && (
              <form className="flex flex-col gap-5" onSubmit={handleRedefinir}>
                <div>
                  <label className="block text-sm font-medium text-[#183E6C] dark:text-blue-300 mb-2">Nova senha</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg className="h-5 w-5 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                      </svg>
                    </div>
                    <input
                      type={mostrarSenha ? 'text' : 'password'}
                      placeholder="········"
                      value={novaSenha}
                      onChange={(e) => setNovaSenha(e.target.value)}
                      required
                      minLength={MIN_SENHA}
                      autoFocus
                      className={`${inputBase} pr-12 font-mono tracking-widest`}
                    />
                    <button
                      type="button"
                      onClick={() => setMostrarSenha((valor) => !valor)}
                      aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                      className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 dark:text-gray-500 hover:text-[#F27405] transition-colors"
                    >
                      <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                      </svg>
                    </button>
                  </div>
                  <p className="text-xs text-gray-400 dark:text-gray-500 mt-2">Mínimo de {MIN_SENHA} caracteres.</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#183E6C] dark:text-blue-300 mb-2">Confirmar nova senha</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <svg className="h-5 w-5 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                      </svg>
                    </div>
                    <input
                      type={mostrarSenha ? 'text' : 'password'}
                      placeholder="········"
                      value={confirmarSenha}
                      onChange={(e) => setConfirmarSenha(e.target.value)}
                      required
                      className={`${inputBase} font-mono tracking-widest`}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={carregando}
                  className="w-full bg-[#F27405] hover:bg-[#D96704] text-white font-bold py-3.5 rounded-xl transition-colors shadow-lg shadow-[#F27405]/30 mt-2 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {carregando ? 'Salvando...' : 'Redefinir senha'}
                </button>
              </form>
            )}

            <p className="text-center text-sm text-gray-500 dark:text-gray-300 mt-6">
              Lembrou a senha?{' '}
              <Link to="/login" className="text-[#F27405] font-semibold hover:underline">
                Voltar ao login
              </Link>
            </p>
          </div>

          <p className="text-center text-xs text-gray-400 dark:text-gray-500 mt-10 font-medium tracking-wide">
            UNIFAPI
          </p>

        </div>
      </div>
    </div>
  );
}
