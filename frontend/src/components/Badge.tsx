import React from "react";

interface BadgeProps {
  children: React.ReactNode;
  variant?: "status" | "skill";
  status?: "ABERTO" | "EM_ANDAMENTO" | "CONCLUIDO" | "PENDENTE" | "ACEITO" | "REJEITADO" | "CANCELADO";
}

// Status → [bg, text] Tailwind classes (cores suaves, fundo claro + texto combinando)
const statusClasses: Record<NonNullable<BadgeProps["status"]>, string> = {
  ABERTO: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",
  EM_ANDAMENTO: "bg-amber-100   text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
  CONCLUIDO: "bg-violet-100  text-violet-700 dark:bg-violet-900/40 dark:text-violet-300",
  PENDENTE: "bg-yellow-100  text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",
  ACEITO: "bg-green-100   text-green-700 dark:bg-green-900/40 dark:text-green-300",
  REJEITADO: "bg-red-100     text-red-600 dark:bg-red-900/40 dark:text-red-300",
  CANCELADO: "bg-gray-100 text-gray-500 dark:bg-slate-800 dark:text-gray-400"
};

// Rótulos legíveis para exibição quando variant="status"
const statusLabel: Record<NonNullable<BadgeProps["status"]>, string> = {
  ABERTO: "Aberto",
  EM_ANDAMENTO: "Em andamento",
  CONCLUIDO: "Concluído",
  PENDENTE: "Pendente",
  ACEITO: "Aceito",
  REJEITADO: "Rejeitado",
  CANCELADO: "Cancelado",
};

// Paleta de cores suaves para badges de habilidade (cicla pelo índice do texto)
const skillPalette = [
  "bg-rose-100    text-rose-700 dark:bg-rose-900/40 dark:text-rose-300",
  "bg-sky-100     text-sky-700 dark:bg-sky-900/40 dark:text-sky-300",
  "bg-violet-100  text-violet-700 dark:bg-violet-900/40 dark:text-violet-300",
  "bg-teal-100    text-teal-700 dark:bg-teal-900/40 dark:text-teal-300",
  "bg-orange-100  text-orange-700 dark:bg-orange-900/40 dark:text-orange-300",
];

/**
 * Gera um índice estável a partir do conteúdo textual do children,
 * para que a mesma habilidade receba sempre a mesma cor.
 */
function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 31 + str.charCodeAt(i)) >>> 0;
  }
  return hash;
}

function extractText(children: React.ReactNode): string {
  if (typeof children === "string" || typeof children === "number") {
    return String(children);
  }
  return "";
}

export default function Badge({
  children,
  variant = "status",
  status,
}: BadgeProps) {
  const base =
    "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium leading-none select-none";

  if (variant === "status") {
    const key = status ?? "ABERTO";
    const colorClass = statusClasses[key];
    const label = status ? statusLabel[status] : String(children);

    return (
      <span className={`${base} ${colorClass}`}>
        <span
          aria-hidden="true"
          className="mr-1 inline-block h-1.5 w-1.5 rounded-full bg-current opacity-80"
        />
        {label}
      </span>
    );
  }

  // variant === "skill"
  const text = extractText(children);
  const paletteIndex = hashString(text) % skillPalette.length;
  const colorClass = skillPalette[paletteIndex];

  return (
    <span className={`${base} ${colorClass}`}>
      {children}
    </span>
  );
}
