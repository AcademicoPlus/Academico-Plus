import React from "react";

interface ButtonProps {
  variant?: "primary" | "secondary";
  children: React.ReactNode;
  onClick?: () => void;
  type?: "button" | "submit";
}

const variantClasses: Record<NonNullable<ButtonProps["variant"]>, string> = {
  primary: [
    "bg-indigo-600 text-white",
    "shadow-sm shadow-indigo-200",
    "hover:bg-indigo-700",
    "focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2",
  ].join(" "),

  secondary: [
    "bg-white text-indigo-700 border border-gray-200",
    "hover:bg-gray-50",
    "focus-visible:ring-2 focus-visible:ring-indigo-400 focus-visible:ring-offset-2",
  ].join(" "),
};

const baseClasses = [
  "inline-flex items-center justify-center",
  "w-full px-6 py-2.5",
  "rounded-xl",
  "text-sm font-semibold",
  "cursor-pointer select-none",
  "transition-colors duration-150 ease-in-out",
  "outline-none focus-visible:outline-none",
].join(" ");

export default function Button({
  variant = "primary",
  children,
  onClick,
  type = "button",
}: ButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      className={`${baseClasses} ${variantClasses[variant]}`}
    >
      {children}
    </button>
  );
}
