// Modal de confirmação genérico — substitui window.confirm() e formulários
// inline de confirmação espalhados pelas telas, para um diálogo consistente
// (e legível no dark mode) em qualquer ação que precise de confirmação.
import type { ReactNode } from 'react';

type ConfirmModalProps = {
  titulo: string;
  mensagem?: ReactNode;
  children?: ReactNode;
  textoConfirmar?: string;
  textoCancelar?: string;
  confirmando?: boolean;
  confirmarDesabilitado?: boolean;
  variante?: 'padrao' | 'perigo';
  onConfirmar: () => void;
  onCancelar: () => void;
};

export default function ConfirmModal({
  titulo,
  mensagem,
  children,
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Cancelar',
  confirmando = false,
  confirmarDesabilitado = false,
  variante = 'padrao',
  onConfirmar,
  onCancelar,
}: ConfirmModalProps) {
  const corConfirmar =
    variante === 'perigo'
      ? 'bg-red-500 hover:bg-red-600'
      : 'bg-[#183E6C] hover:bg-[#102a4a]';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-titulo"
      onClick={() => !confirmando && onCancelar()}
    >
      <div
        className="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-gray-100 dark:border-slate-700 shadow-xl p-6 flex flex-col gap-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="confirm-modal-titulo" className="text-lg font-bold text-[#183E6C] dark:text-blue-300">
          {titulo}
        </h2>

        {mensagem && <p className="text-sm text-gray-600 dark:text-gray-300">{mensagem}</p>}

        {children}

        <div className="flex justify-end gap-3 mt-2">
          <button
            type="button"
            onClick={onCancelar}
            disabled={confirmando}
            className="px-4 py-2 rounded-xl text-sm font-bold text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
          >
            {textoCancelar}
          </button>
          <button
            type="button"
            onClick={onConfirmar}
            disabled={confirmando || confirmarDesabilitado}
            className={`px-4 py-2 rounded-xl text-sm font-bold text-white transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${corConfirmar}`}
          >
            {confirmando ? 'Processando…' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
