import { useCallback, useState, useRef } from 'react';
import { 
  Upload, 
  X, 
  Trash2, 
  ArrowLeft, 
  ArrowRight, 
  Star, 
  RefreshCw, 
  ImageIcon, 
  Loader2, 
  CheckCircle2, 
  AlertTriangle 
} from 'lucide-react';
import api from '../../api/axiosConfig';

/**
 * SareePhotoDropzone
 * Drag-and-drop + click-to-browse photo uploader for saree products with reordering,
 * cover selection, replacement, and deletion.
 * Props:
 *   productId   – optional; passed to backend to prefix filenames
 *   sku         – optional; passed to backend
 *   value       – current list of image URLs (string[])
 *   onChange    – called with updated URL list
 *   maxPhotos   – max photos allowed (default 6)
 */
export default function SareePhotoDropzone({ productId, sku, value = [], onChange, maxPhotos = 6 }) {
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [replaceIndex, setReplaceIndex] = useState(null);
  
  const inputRef = useRef(null);
  const replaceInputRef = useRef(null);

  const uploadFiles = useCallback(async (files) => {
    if (!files || files.length === 0) return;
    const remaining = maxPhotos - value.length;
    if (remaining <= 0) {
      setError(`Maximum ${maxPhotos} photos allowed.`);
      return;
    }

    const toUpload = Array.from(files).slice(0, remaining);
    setUploading(true);
    setError(null);

    try {
      if (toUpload.length === 1) {
        // Single upload
        const fd = new FormData();
        fd.append('file', toUpload[0]);
        if (productId) fd.append('productId', productId);
        if (sku) fd.append('sku', sku);
        const res = await api.post('/admin/photos/upload', fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        if (res.data?.url) {
          onChange([...value, res.data.url]);
        } else {
          setError(res.data?.error || res.data?.message || 'Upload failed');
        }
      } else {
        // Bulk upload
        const fd = new FormData();
        toUpload.forEach((f) => fd.append('files', f));
        if (productId) fd.append('productId', productId);
        if (sku) fd.append('sku', sku);
        const res = await api.post('/admin/photos/upload/bulk', fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        if (res.data?.uploaded?.length) {
          onChange([...value, ...res.data.uploaded]);
        }
        if (res.data?.errors?.length) {
          setError('Some files failed: ' + res.data.errors.join(', '));
        }
      }
    } catch (e) {
      setError(e.response?.data?.error || e.response?.data?.message || e.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  }, [value, onChange, productId, sku, maxPhotos]);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setDragging(false);
    uploadFiles(e.dataTransfer.files);
  }, [uploadFiles]);

  const handleDragOver = (e) => { e.preventDefault(); setDragging(true); };
  const handleDragLeave = () => setDragging(false);
  const handleInputChange = (e) => uploadFiles(e.target.files);

  const removePhoto = async (url, idx) => {
    // Attempt to delete from server if it's a local upload
    if (url && typeof url === 'string' && url.startsWith('/uploads/')) {
      const filename = url.split('/').pop();
      try { await api.delete(`/admin/photos/${filename}`); } catch (_) { /* ignore */ }
    }
    onChange(value.filter((_, i) => i !== idx));
  };

  const makeCover = (idx) => {
    if (idx <= 0 || idx >= value.length) return;
    const updated = [value[idx], ...value.filter((_, i) => i !== idx)];
    onChange(updated);
  };

  const movePhoto = (idx, direction) => {
    const targetIdx = idx + direction;
    if (targetIdx < 0 || targetIdx >= value.length) return;
    const updated = [...value];
    const [moved] = updated.splice(idx, 1);
    updated.splice(targetIdx, 0, moved);
    onChange(updated);
  };

  const triggerReplace = (idx) => {
    setReplaceIndex(idx);
    if (replaceInputRef.current) {
      replaceInputRef.current.value = '';
      replaceInputRef.current.click();
    }
  };

  const handleReplaceFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file || replaceIndex === null) return;
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      if (productId) fd.append('productId', productId);
      if (sku) fd.append('sku', sku);
      const res = await api.post('/admin/photos/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.data?.url) {
        const updated = [...value];
        updated[replaceIndex] = res.data.url;
        onChange(updated);
      } else {
        setError(res.data?.error || res.data?.message || 'Replace upload failed');
      }
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || err.message || 'Replace failed');
    } finally {
      setUploading(false);
      setReplaceIndex(null);
    }
  };

  return (
    <div className="space-y-3">
      {/* Hidden file input for replacement */}
      <input
        ref={replaceInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        className="hidden"
        onChange={handleReplaceFile}
      />

      {/* Drop Zone */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => !uploading && inputRef.current?.click()}
        className={`relative flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed p-7 cursor-pointer transition-all
          ${dragging
            ? 'border-[#1E6A62] bg-[#E3F0ED] scale-[1.01]'
            : 'border-[#DDE4EA] bg-[#F5F7FA] hover:border-[#1E6A62] hover:bg-[#EEF7F5]'}
          ${uploading ? 'pointer-events-none opacity-70' : ''}
          ${value.length >= maxPhotos ? 'pointer-events-none opacity-50' : ''}
        `}
      >
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          multiple
          className="hidden"
          onChange={handleInputChange}
        />
        {uploading ? (
          <>
            <Loader2 className="h-9 w-9 animate-spin text-[#1E6A62]" />
            <p className="text-xs font-bold text-[#1E6A62]">Uploading saree imagery…</p>
          </>
        ) : value.length >= maxPhotos ? (
          <>
            <CheckCircle2 className="h-9 w-9 text-emerald-600" />
            <p className="text-xs font-bold text-emerald-700">Maximum {maxPhotos} photos added</p>
          </>
        ) : (
          <>
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow-xs border border-[#DDE4EA]">
              <Upload className="h-5 w-5 text-[#1E6A62]" />
            </div>
            <div className="text-center">
              <p className="text-xs font-bold text-[#121212]">
                {dragging ? 'Drop photos here' : 'Drag & drop saree photos'}
              </p>
              <p className="mt-1 text-[11px] text-[#71817A]">
                or <span className="text-[#1E6A62] font-bold underline underline-offset-2">click to browse</span>
                {' '}· JPG, PNG, WebP · max 10 MB each · {value.length}/{maxPhotos} added
              </p>
            </div>
          </>
        )}
      </div>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-semibold text-red-700">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span className="flex-1">{error}</span>
          <button type="button" onClick={() => setError(null)} className="ml-auto text-red-400 hover:text-red-700 cursor-pointer">
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Preview Grid with Reorder, Cover, Replace, Delete */}
      {value.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-[11px] font-bold text-[#71817A]">
            <span>Product Gallery ({value.length}/{maxPhotos})</span>
            <span className="text-[10px] font-normal text-[#888888]">Hover photo to reorder or set primary cover</span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-2.5">
            {value.map((url, idx) => (
              <div 
                key={`${url}-${idx}`} 
                className={`group relative aspect-square overflow-hidden rounded-xl border bg-[#F5F7FA] transition-all shadow-2xs ${
                  idx === 0 ? 'ring-2 ring-[#B8860B] border-[#B8860B]' : 'border-[#DDE4EA]'
                }`}
              >
                <img
                  src={url}
                  alt={`Saree photo ${idx + 1}`}
                  className="h-full w-full object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    if (e.currentTarget.nextElementSibling) {
                      e.currentTarget.nextElementSibling.classList.remove('hidden');
                    }
                  }}
                />
                {/* Fallback container shown only on load error */}
                <div className="hidden absolute inset-0 flex flex-col items-center justify-center bg-[#F5F7FA] p-2 text-center text-[#71817A]">
                  <ImageIcon className="w-5 h-5 text-gray-400 mb-1" />
                  <span className="text-[9px] font-bold">Image Failed</span>
                </div>

                {/* Primary Cover Badge or Make Cover Button */}
                {idx === 0 ? (
                  <span className="absolute top-1.5 left-1.5 z-10 flex items-center gap-1 rounded-md bg-[#17211F]/90 px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-[#F3C56A] shadow-xs backdrop-blur-xs">
                    <Star className="w-2.5 h-2.5 fill-[#F3C56A]" /> Cover
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={(e) => { e.stopPropagation(); makeCover(idx); }}
                    className="absolute top-1.5 left-1.5 z-10 hidden group-hover:flex items-center gap-1 rounded-md bg-white/95 px-1.5 py-0.5 text-[9px] font-bold text-[#17211F] hover:bg-white shadow-xs cursor-pointer border border-[#DDE4EA]"
                    title="Make primary cover image"
                  >
                    <Star className="w-2.5 h-2.5 text-[#B8860B]" /> Set Cover
                  </button>
                )}

                {/* Action Toolbar on Hover */}
                <div className="absolute inset-0 bg-black/45 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-1.5">
                  <div className="flex items-center justify-between gap-1 w-full bg-white/95 p-1 rounded-lg shadow-sm">
                    {/* Move Left */}
                    <button
                      type="button"
                      disabled={idx === 0}
                      onClick={(e) => { e.stopPropagation(); movePhoto(idx, -1); }}
                      className="flex h-5 w-5 items-center justify-center rounded text-[#17211F] hover:bg-gray-200 disabled:opacity-20 disabled:pointer-events-none cursor-pointer"
                      title="Move left in sequence"
                    >
                      <ArrowLeft className="w-3 h-3" />
                    </button>

                    {/* Replace */}
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); triggerReplace(idx); }}
                      className="flex h-5 w-5 items-center justify-center rounded text-[#1E6A62] hover:bg-[#E3F0ED] cursor-pointer"
                      title="Replace photo"
                    >
                      <RefreshCw className="w-3 h-3" />
                    </button>

                    {/* Delete */}
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); removePhoto(url, idx); }}
                      className="flex h-5 w-5 items-center justify-center rounded text-red-600 hover:bg-red-50 cursor-pointer"
                      title="Remove photo"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>

                    {/* Move Right */}
                    <button
                      type="button"
                      disabled={idx === value.length - 1}
                      onClick={(e) => { e.stopPropagation(); movePhoto(idx, 1); }}
                      className="flex h-5 w-5 items-center justify-center rounded text-[#17211F] hover:bg-gray-200 disabled:opacity-20 disabled:pointer-events-none cursor-pointer"
                      title="Move right in sequence"
                    >
                      <ArrowRight className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
