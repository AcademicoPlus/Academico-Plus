import React from "react";

interface CardProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export default function Card({ children, onClick, className = "" }: CardProps) {
  const base =
    "bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 p-6";

  const interactive = onClick
    ? "cursor-pointer transition-shadow duration-150 hover:shadow-md"
    : "";

  if (onClick) {
    return (
      <div
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") onClick();
        }}
        className={`${base} ${interactive} ${className}`.trim()}
      >
        {children}
      </div>
    );
  }

  return (
    <div className={`${base} ${className}`.trim()}>
      {children}
    </div>
  );
}
