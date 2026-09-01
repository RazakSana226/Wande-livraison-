import React from 'react';

interface LogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showTagline?: boolean;
  className?: string;
  onClick?: () => void;
}

export const Logo: React.FC<LogoProps> = ({
  size = 'md',
  showTagline = false,
  className = '',
  onClick,
}) => {
  const iconSizes = {
    sm: 'w-7 h-7 text-sm rounded-lg',
    md: 'w-9 h-9 text-base rounded-xl',
    lg: 'w-12 h-12 text-xl rounded-2xl',
    xl: 'w-16 h-16 text-3xl rounded-3xl',
  };

  const textSizes = {
    sm: 'text-lg',
    md: 'text-xl',
    lg: 'text-2xl',
    xl: 'text-4xl',
  };

  return (
    <div
      onClick={onClick}
      className={`inline-flex items-center gap-2.5 select-none ${onClick ? 'cursor-pointer' : ''} ${className}`}
    >
      <div
        className={`${iconSizes[size]} bg-gradient-to-tr from-blue-700 via-blue-600 to-blue-500 text-white font-black flex items-center justify-center shadow-md shadow-blue-500/25 transition-transform hover:scale-105`}
      >
        <span className="tracking-tighter">W</span>
      </div>
      <div className="flex flex-col">
        <div className="flex items-center gap-1">
          <span className={`font-black tracking-tight text-slate-900 ${textSizes[size]}`}>
            WÀNDÉ
          </span>
          <span className="w-1.5 h-1.5 rounded-full bg-blue-600 mb-2"></span>
        </div>
        {showTagline && (
          <span className="text-[10px] sm:text-xs font-semibold text-slate-500 -mt-1 tracking-normal">
            Ton livreur en quelques clics
          </span>
        )}
      </div>
    </div>
  );
};
