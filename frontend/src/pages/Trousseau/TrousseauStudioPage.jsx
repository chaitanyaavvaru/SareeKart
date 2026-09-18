import { useEffect, useState, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Loader2, CheckCircle2, AlertCircle } from 'lucide-react';
import {
  fetchMyBoard,
  createBoard,
  updateBoard,
  archiveBoard,
  addCeremony,
  removeCeremony,
  addItem,
  removeItem,
  aiCurateCeremony,
  addCollaborator,
  revokeCollaborator,
  convertToCart,
  setActiveCeremony,
  setSseConnected,
  applyLiveEvent,
  clearConversionResult,
} from '../../redux/slices/trousseauSlice';

import BoardHeader from './components/BoardHeader';
import CeremonyTabs from './components/CeremonyTabs';
import CeremonyPanel from './components/CeremonyPanel';
import EmptyStudioState from './components/EmptyStudioState';
import AddCeremonyModal from './components/AddCeremonyModal';
import AddItemModal from './components/AddItemModal';
import CollaboratorsDrawer from './components/CollaboratorsDrawer';

export default function TrousseauStudioPage() {
  const dispatch = useDispatch();
  const {
    board,
    activeCeremonyId,
    loading,
    actionLoading,
    aiCurating,
    sseConnected,
    conversionResult,
    error,
  } = useSelector((state) => state.trousseau);

  // Modals & Drawers state
  const [isAddCeremonyOpen, setIsAddCeremonyOpen] = useState(false);
  const [isAddItemOpen, setIsAddItemOpen] = useState(false);
  const [isCollaboratorsOpen, setIsCollaboratorsOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // Edit Board Form State
  const [editTitle, setEditTitle] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editNotes, setEditNotes] = useState('');

  // Initial fetch on mount
  useEffect(() => {
    dispatch(fetchMyBoard());
  }, [dispatch]);

  const handleOpenEditModal = () => {
    if (board) {
      setEditTitle(board.title || '');
      setEditDate(board.weddingDate || '');
      setEditNotes(board.notes || '');
    }
    setIsEditModalOpen(true);
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!board?.id || !editTitle.trim()) return;
    await dispatch(
      updateBoard({
        boardId: board.id,
        data: {
          title: editTitle.trim(),
          weddingDate: editDate || null,
          notes: editNotes.trim() || null,
        },
      })
    );
    setIsEditModalOpen(false);
  };

  // ── SSE Live Updates Subscription via Fetch Stream (sends JWT Bearer) ───────
  useEffect(() => {
    if (!board?.id) return;
    const token = localStorage.getItem('sareekart_token');
    const abortController = new AbortController();

    async function startSse() {
      try {
        const response = await fetch(`/api/trousseau/${board.id}/stream`, {
          headers: {
            Accept: 'text/event-stream, application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          signal: abortController.signal,
        });

        if (!response.ok) {
          dispatch(setSseConnected(false));
          return;
        }

        dispatch(setSseConnected(true));
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split('\n\n');
          buffer = parts.pop() || '';

          for (const part of parts) {
            const lines = part.split('\n');
            let eventType = 'message';
            let eventData = '';
            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.replace('event:', '').trim();
              } else if (line.startsWith('data:')) {
                eventData += line.replace('data:', '').trim();
              }
            }
            if (eventType === 'CONNECTED') {
              dispatch(setSseConnected(true));
            } else if (eventType && eventData) {
              try {
                const parsed = JSON.parse(eventData);
                dispatch(applyLiveEvent({ type: eventType, data: parsed }));
              } catch (e) {
                console.error('SSE parse error', e);
              }
            }
          }
        }
      } catch (err) {
        if (err.name !== 'AbortError') {
          dispatch(setSseConnected(false));
        }
      }
    }

    startSse();

    return () => {
      abortController.abort();
      dispatch(setSseConnected(false));
    };
  }, [board?.id, dispatch]);

  // Active Ceremony Resolution
  const activeCeremony = useMemo(() => {
    if (!board?.ceremonies || board.ceremonies.length === 0) return null;
    return (
      board.ceremonies.find((c) => c.id === activeCeremonyId) ||
      board.ceremonies[0]
    );
  }, [board, activeCeremonyId]);

  // Handlers
  const handleCreateBoard = async (data) => {
    await dispatch(createBoard(data)).unwrap();
  };

  const handleAddCeremony = async (data) => {
    if (!board?.id) return;
    await dispatch(addCeremony({ boardId: board.id, data })).unwrap();
  };

  const handleRemoveCeremony = async (ceremonyId) => {
    if (!board?.id) return;
    await dispatch(removeCeremony({ boardId: board.id, ceremonyId })).unwrap();
  };

  const handleAddItem = async (data) => {
    if (!board?.id || !activeCeremony?.id) return;
    await dispatch(
      addItem({
        boardId: board.id,
        ceremonyId: activeCeremony.id,
        data,
      })
    ).unwrap();
  };

  const handleRemoveItem = async (itemId) => {
    if (!board?.id || !activeCeremony?.id) return;
    await dispatch(
      removeItem({
        boardId: board.id,
        ceremonyId: activeCeremony.id,
        itemId,
      })
    ).unwrap();
  };

  const handleAiCurate = async (ceremonyId) => {
    if (!board?.id || !ceremonyId) return;
    await dispatch(aiCurateCeremony({ boardId: board.id, ceremonyId })).unwrap();
  };

  const handleAddCollaborator = async (data) => {
    if (!board?.id) return;
    await dispatch(addCollaborator({ boardId: board.id, data })).unwrap();
  };

  const handleRevokeCollaborator = async (collaboratorId) => {
    if (!board?.id) return;
    await dispatch(
      revokeCollaborator({ boardId: board.id, collaboratorId })
    ).unwrap();
  };

  const handleConvertToCart = async (ceremonyId, itemIds) => {
    if (!board?.id) return;
    await dispatch(convertToCart({ boardId: board.id, ceremonyId, itemIds })).unwrap();
  };

  const handleShareWhatsApp = () => {
    if (!board) return;
    const shareUrl = board.shareUrl || `${window.location.origin}/trousseau/share/${board.shareToken}`;
    const message = `Namaste! 🙏\nI'm planning our bridal trousseau "${board.title}" on SareeKart. Take a look at the shortlisted sarees and vote on your favorites:\n${shareUrl}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(message)}`, '_blank');
  };

  // Existing product IDs in the current active ceremony to prevent duplicate add
  const existingProductIds = useMemo(() => {
    return (activeCeremony?.items || []).map((i) => i.productId);
  }, [activeCeremony]);

  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3 text-text-muted">
        <Loader2 className="w-8 h-8 animate-spin text-[#C89B3C]" />
        <span className="font-serif text-sm tracking-wider uppercase">Loading Bridal Studio...</span>
      </div>
    );
  }

  if (!board) {
    return (
      <EmptyStudioState
        onCreateBoard={handleCreateBoard}
        loading={actionLoading}
      />
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* 1-Click Cart Conversion Success Toast / Banner */}
      {conversionResult && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-center justify-between gap-4 animate-in fade-in slide-in-from-top-2 duration-300">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="text-xs font-semibold">
              {conversionResult.message || 'Selected trousseau sarees transferred to your shopping cart!'}
            </span>
          </div>
          <button
            type="button"
            onClick={() => dispatch(clearConversionResult())}
            className="text-xs underline font-bold hover:text-emerald-900 cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Error alert if any */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 flex items-center gap-2 text-xs">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Studio Header */}
      <BoardHeader
        board={board}
        sseConnected={sseConnected}
        onOpenCollaborators={() => setIsCollaboratorsOpen(true)}
        onArchiveBoard={(id) => dispatch(archiveBoard(id))}
        onOpenEditModal={handleOpenEditModal}
        onShareWhatsApp={handleShareWhatsApp}
        isOwner={true}
      />

      {/* Ceremonies Navigation Tabs */}
      <CeremonyTabs
        ceremonies={board.ceremonies || []}
        activeCeremonyId={activeCeremony?.id}
        onSelectCeremony={(id) => dispatch(setActiveCeremony(id))}
        onOpenAddCeremony={() => setIsAddCeremonyOpen(true)}
        onRemoveCeremony={handleRemoveCeremony}
        isOwner={true}
      />

      {/* Active Ceremony Item Grid & Actions */}
      <CeremonyPanel
        ceremony={activeCeremony}
        onOpenAddItem={() => setIsAddItemOpen(true)}
        onRemoveItem={handleRemoveItem}
        onConvertToCart={handleConvertToCart}
        onAiCurate={handleAiCurate}
        isOwner={true}
        actionLoading={actionLoading}
        aiCurating={aiCurating}
      />

      {/* Modals & Drawers */}
      <AddCeremonyModal
        isOpen={isAddCeremonyOpen}
        onClose={() => setIsAddCeremonyOpen(false)}
        onAddCeremony={handleAddCeremony}
      />

      <AddItemModal
        isOpen={isAddItemOpen}
        onClose={() => setIsAddItemOpen(false)}
        ceremonyName={activeCeremony?.title || activeCeremony?.name || 'Ceremony'}
        onAddItem={handleAddItem}
        existingProductIds={existingProductIds}
      />

      <CollaboratorsDrawer
        isOpen={isCollaboratorsOpen}
        onClose={() => setIsCollaboratorsOpen(false)}
        collaborators={board.collaborators || []}
        onAddCollaborator={handleAddCollaborator}
        onRevokeCollaborator={handleRevokeCollaborator}
        boardTitle={board.title}
        shareToken={board.shareToken}
      />

      {/* Edit Board Modal */}
      {isEditModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-[#E6DFD3] overflow-hidden">
            <div className="p-5 border-b border-[#E6DFD3] bg-[#FAF8F5]">
              <h3 className="font-serif text-lg font-bold text-[#2B0F1E]">Edit Trousseau Details</h3>
            </div>
            <form onSubmit={handleSaveEdit} className="p-5 space-y-4">
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Title *
                </label>
                <input
                  type="text"
                  required
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Wedding Date
                </label>
                <input
                  type="date"
                  value={editDate}
                  onChange={(e) => setEditDate(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={editNotes}
                  onChange={(e) => setEditNotes(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#E6DFD3]">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A]"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
