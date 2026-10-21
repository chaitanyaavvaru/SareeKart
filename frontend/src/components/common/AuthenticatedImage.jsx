import React, { useState, useEffect } from 'react';
import api from '../../api/axiosConfig';

/**
 * Renders an image element, handling Bearer token authentication and Blob URL
 * lifecycle management for private media paths (/uploads/return-photos/**).
 *
 * Public paths (/uploads/saree-photos/**, https://...) bypass Axios and render directly.
 *
 * Memory Safety:
 * - Uses AbortController to cancel stale requests when src changes or component unmounts.
 * - Invokes URL.revokeObjectURL() on cleanup to prevent browser memory leaks.
 */
export default function AuthenticatedImage({
  src,
  alt = 'Evidence photo',
  className = '',
  fallbackSrc = 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=400&q=80',
  ...props
}) {
  const [blobUrl, setBlobUrl] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  const isPrivate = typeof src === 'string' && src.startsWith('/uploads/return-photos/');

  useEffect(() => {
    if (!src) {
      setBlobUrl(null);
      return;
    }

    // Public catalog media or external URLs render directly without Axios
    if (!isPrivate) {
      setBlobUrl(src);
      return;
    }

    const abortController = new AbortController();
    let activeObjectUrl = null;

    setLoading(true);
    setError(false);

    api.get(src, {
      responseType: 'blob',
      signal: abortController.signal,
    })
      .then((response) => {
        activeObjectUrl = URL.createObjectURL(response.data);
        setBlobUrl(activeObjectUrl);
        setLoading(false);
      })
      .catch((err) => {
        // Ignore deliberate aborts during unmount / prop change
        if (err.name === 'CanceledError' || err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
          return;
        }
        setError(true);
        setLoading(false);
      });

    return () => {
      abortController.abort();
      if (activeObjectUrl) {
        URL.revokeObjectURL(activeObjectUrl);
      }
    };
  }, [src, isPrivate]);

  if (error || (!blobUrl && !loading)) {
    return (
      <img
        src={fallbackSrc}
        alt={alt}
        className={className}
        {...props}
      />
    );
  }

  if (loading && !blobUrl) {
    return (
      <div
        className={`animate-pulse bg-[#E2E8F0] ${className}`}
        aria-label="Loading image..."
      />
    );
  }

  return (
    <img
      src={blobUrl}
      alt={alt}
      className={className}
      {...props}
    />
  );
}
