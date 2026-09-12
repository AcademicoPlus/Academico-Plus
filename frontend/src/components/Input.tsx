import { useId } from "react";

interface InputProps {
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string;
  required?: boolean;
}

const fieldBase =
  "w-full rounded-xl border px-4 py-2.5 text-sm text-gray-800 dark:text-gray-100 " +
  "placeholder:text-gray-400 dark:placeholder:text-gray-500 bg-gray-50 dark:bg-slate-800 " +
  "transition-colors duration-150 outline-none " +
  "focus:bg-white dark:focus:bg-slate-800 focus:ring-2 focus:ring-indigo-500 focus:ring-offset-0 focus:border-indigo-400";

const fieldNormal = "border-gray-200 dark:border-slate-700";
const fieldError = "border-red-400 focus:ring-red-400 focus:border-red-400";

export default function Input({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  error,
  required = false,
}: InputProps) {
  const id = useId();
  const errorId = `${id}-error`;
  const isTextarea = type === "textarea";

  const fieldClass =
    `${fieldBase} ${error ? fieldError : fieldNormal}` +
    (isTextarea ? " resize-none min-h-[100px]" : "");

  const sharedProps = {
    id,
    value,
    placeholder,
    required,
    "aria-required": required,
    "aria-describedby": error ? errorId : undefined,
    "aria-invalid": !!error,
    className: fieldClass,
  };

  return (
    <div className="flex flex-col gap-1.5">
      {/* Label */}
      <label
        htmlFor={id}
        className="text-sm font-medium text-gray-700 dark:text-gray-200 leading-none"
      >
        {label}
        {required && (
          <span className="ml-0.5 text-red-500" aria-hidden="true">
            *
          </span>
        )}
      </label>

      {/* Field */}
      {isTextarea ? (
        <textarea
          {...sharedProps}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : (
        <input
          {...sharedProps}
          type={type}
          onChange={(e) => onChange(e.target.value)}
        />
      )}

      {/* Error message */}
      {error && (
        <p id={errorId} role="alert" className="text-xs text-red-500 dark:text-red-400 leading-none">
          {error}
        </p>
      )}
    </div>
  );
}
