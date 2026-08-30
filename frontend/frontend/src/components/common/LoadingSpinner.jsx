import { Loader2 } from 'lucide-react';

/**
 * Reusable loading spinner component
 */
export default function LoadingSpinner({ size = 'md', message = 'Loading...' }) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  };

  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3">
      <Loader2 className={`${sizeClasses[size]} animate-spin text-maroon`} />
      {message && (
        <p className="text-text-secondary text-sm">{message}</p>
      )}
    </div>
  );
}
