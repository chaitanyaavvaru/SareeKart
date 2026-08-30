

export default function Button({ 
  children, 
  onClick, 
  type = "button", 
  variant = "primary", 
  disabled = false,
  className = "" 
}) {
  const baseStyle = "px-6 py-3 font-bold rounded-lg transition-all focus:outline-none cursor-pointer flex items-center justify-center gap-2";
  
  const variants = {
    primary: "bg-maroon hover:bg-maroon-light text-white shadow-lg",
    secondary: "border border-maroon text-maroon hover:bg-maroon hover:text-white",
    gold: "bg-gold hover:bg-gold-light text-[#2C0F1F] shadow-lg hover:shadow-gold/25",
    dark: "bg-[#2C0F1F] hover:bg-[#3A1028] text-white shadow-md"
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseStyle} ${variants[variant] || variants.primary} ${disabled ? "opacity-50 cursor-not-allowed" : ""} ${className}`}
    >
      {children}
    </button>
  );
}
