interface NotificationDropdownProps {
  onClose: () => void;
}

export default function NotificationDropdown({ onClose }: NotificationDropdownProps) {
  // Exemplo de lista de notificações
  const notifications = [
    { id: 1, text: "Lucas Mendes enviou uma mensagem." },
    { id: 2, text: "Sua candidatura foi visualizada." }
  ];

  return (
    <div className="absolute top-14 right-0 w-80 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-700 shadow-lg rounded-xl z-50 overflow-hidden">
      <div className="p-4 border-b border-gray-100 dark:border-slate-700 flex justify-between items-center">
        <h3 className="font-semibold text-[#183E6C] dark:text-blue-300">Notificações</h3>
        <button
          onClick={onClose}
          className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 text-sm font-medium"
        >
          Fechar
        </button>
      </div>

      <div className="max-h-80 overflow-y-auto">
        {notifications.length > 0 ? (
          notifications.map(notif => (
            <div key={notif.id} className="p-4 border-b border-gray-50 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 cursor-pointer">
              <p className="text-sm text-gray-700 dark:text-gray-200">{notif.text}</p>
            </div>
          ))
        ) : (
          <div className="p-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Você não tem novas notificações no momento.
          </div>
        )}
      </div>
    </div>
  )
}