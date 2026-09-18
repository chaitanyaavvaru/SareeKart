import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Loader2, Heart, Calendar, AlertCircle, User, Check, Sparkles } from 'lucide-react';
import trousseauService from '../../services/trousseauService';
import ItemCard from './components/ItemCard';
import SEO from '../../components/common/SEO';

export default function TrousseauSharedViewPage() {
  const { token } = useParams();
  const [board, setBoard] = useState(null);
  const [activeCeremonyId, setActiveCeremonyId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userVotes, setUserVotes] = useState({}); // itemId -> 'LOVE' | 'LIKE' | 'PASS'
  const [voteSubmitting, setVoteSubmitting] = useState(false);

  // Voter identity state (persisted in localStorage for convenience)
  const [voterName, setVoterName] = useState(() => localStorage.getItem('trousseau_voter_name') || '');
  const [voterPhone, setVoterPhone] = useState(() => localStorage.getItem('trousseau_voter_phone') || '');
  const [isVoterPromptOpen, setIsVoterPromptOpen] = useState(false);
  const [pendingVote, setPendingVote] = useState(null); // { itemId, reaction }

  useEffect(() => {
    let isMounted = true;
    const loadSharedBoard = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await trousseauService.getSharedView(token);
        const data = res?.data !== undefined ? res.data : res;
        if (isMounted) {
          setBoard(data);
          if (data?.ceremonies?.length > 0) {
            setActiveCeremonyId(data.ceremonies[0].id);
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(err.response?.data?.message || 'Invalid or expired trousseau share link.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    if (token) {
      loadSharedBoard();
    }
    return () => {
      isMounted = false;
    };
  }, [token]);

  const activeCeremony =
    board?.ceremonies?.find((c) => c.id === activeCeremonyId) ||
    board?.ceremonies?.[0];

  const handleVoteClick = (itemId, reaction) => {
    if (voteSubmitting) return;

    // Check if voterName is available
    const savedName = voterName.trim() || localStorage.getItem('trousseau_voter_name');
    if (!savedName) {
      setPendingVote({ itemId, reaction });
      setIsVoterPromptOpen(true);
      return;
    }

    executeVote(itemId, reaction, savedName, voterPhone.trim() || null);
  };

  const executeVote = async (itemId, reaction, nameToUse, phoneToUse) => {
    setVoteSubmitting(true);
    const oldVote = userVotes[itemId];
    setUserVotes((prev) => ({ ...prev, [itemId]: reaction }));

    // Optimistic tally update
    setBoard((prevBoard) => {
      if (!prevBoard) return prevBoard;
      return {
        ...prevBoard,
        ceremonies: (prevBoard.ceremonies || []).map((ceremony) => ({
          ...ceremony,
          items: (ceremony.items || []).map((item) => {
            if (item.id !== itemId) return item;
            const updated = { ...item };
            if (reaction === 'LOVE') updated.loveCount = (updated.loveCount || 0) + 1;
            else if (reaction === 'LIKE') updated.likeCount = (updated.likeCount || 0) + 1;
            else if (reaction === 'PASS') updated.passCount = (updated.passCount || 0) + 1;
            return updated;
          }),
        })),
      };
    });

    try {
      const payload = {
        voterName: nameToUse,
        voterPhone: phoneToUse || null,
        reaction,
        note: null,
      };
      await trousseauService.castVote(token, itemId, payload);
    } catch {
      // Revert on error
      setUserVotes((prev) => ({ ...prev, [itemId]: oldVote }));
      alert('Could not record your vote. Please check your network connection.');
    } finally {
      setVoteSubmitting(false);
    }
  };

  const handleSaveVoterProfile = (e) => {
    e.preventDefault();
    if (!voterName.trim()) return;

    localStorage.setItem('trousseau_voter_name', voterName.trim());
    if (voterPhone.trim()) {
      localStorage.setItem('trousseau_voter_phone', voterPhone.trim());
    }
    setIsVoterPromptOpen(false);

    if (pendingVote) {
      executeVote(pendingVote.itemId, pendingVote.reaction, voterName.trim(), voterPhone.trim() || null);
      setPendingVote(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3 text-text-muted">
        <SEO
          title="Family Bridal Trousseau Collaboration | SareeKart"
          description="Collaborative family trousseau curation and voting."
          noindex={true}
        />
        <Loader2 className="w-8 h-8 animate-spin text-[#C89B3C]" />
        <span className="font-serif text-sm tracking-wider uppercase">Loading Family Trousseau...</span>
      </div>
    );
  }

  if (error || !board) {
    return (
      <div className="max-w-md mx-auto my-16 p-8 bg-white rounded-3xl border border-[#E6DFD3] text-center space-y-4 shadow-sm">
        <SEO
          title="Share Link Unavailable | SareeKart"
          description="This trousseau board link has expired or was revoked."
          noindex={true}
          nofollow={true}
        />
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h2 className="font-serif text-xl font-bold text-[#2B0F1E]">Share Link Unavailable</h2>
        <p className="text-xs text-text-muted">{error || 'This trousseau board link has expired or was revoked by the bride.'}</p>
        <Link
          to="/"
          className="inline-block px-5 py-2.5 rounded-full text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A]"
        >
          Explore SareeKart
        </Link>
      </div>
    );
  }

  const formattedWeddingDate = board.weddingDate
    ? new Date(board.weddingDate).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : null;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <SEO
        title="Family Bridal Trousseau Collaboration | SareeKart"
        description="Collaborative family trousseau curation and voting."
        noindex={true}
      />
      {/* Family Invitation Banner */}
      <div className="bg-gradient-to-r from-[#FAF8F5] via-[#F4EFE6] to-[#FAF8F5] border border-[#E6DFD3] rounded-3xl p-6 sm:p-10 space-y-4 text-center relative overflow-hidden shadow-xs">
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#C89B3C]/15 text-[#8F6A1A] text-xs font-bold uppercase tracking-widest">
          <Heart className="w-3.5 h-3.5 fill-current text-rose-500" />
          <span>Family Collaboration & Voting</span>
        </div>

        <h1 className="font-serif text-3xl sm:text-4xl font-bold text-[#2B0F1E]">
          {board.title}
        </h1>

        <p className="text-xs sm:text-sm text-text-muted max-w-lg mx-auto">
          Curated by <span className="font-bold text-[#22181C]">{board.brideOrOwnerName || board.ownerName || 'the Bride'}</span>. Vote on the saree designs you love most to help assemble the perfect bridal ensemble!
        </p>

        <div className="flex items-center justify-center gap-3 flex-wrap pt-1">
          {formattedWeddingDate && (
            <div className="inline-flex items-center gap-1.5 text-xs text-[#8F6A1A] font-semibold bg-white/80 px-3 py-1 rounded-full border border-[#E6DFD3]">
              <Calendar className="w-3.5 h-3.5 text-[#C89B3C]" />
              Wedding Date: {formattedWeddingDate}
            </div>
          )}

          {voterName ? (
            <div className="inline-flex items-center gap-1.5 text-xs text-[#22181C] font-semibold bg-white/80 px-3 py-1 rounded-full border border-[#E6DFD3]">
              <User className="w-3.5 h-3.5 text-[#C89B3C]" />
              Voting as: <span className="font-bold">{voterName}</span>
              <button
                type="button"
                onClick={() => setIsVoterPromptOpen(true)}
                className="ml-1 text-[10px] text-[#C89B3C] underline cursor-pointer"
              >
                Change
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsVoterPromptOpen(true)}
              className="inline-flex items-center gap-1.5 text-xs text-[#2B0F1E] font-semibold bg-white px-3 py-1 rounded-full border border-[#C89B3C] hover:bg-[#FAF8F5] cursor-pointer"
            >
              <User className="w-3.5 h-3.5 text-[#C89B3C]" />
              <span>Set Your Voter Name</span>
            </button>
          )}
        </div>
      </div>

      {/* Ceremony Tabs Navigation */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-[#E6DFD3] no-scrollbar">
        {(board.ceremonies || []).map((ceremony) => {
          const isActive = ceremony.id === activeCeremonyId;
          const count = ceremony.items?.length || ceremony.itemCount || 0;
          const ceremonyTitle = ceremony.title || ceremony.name || 'Ceremony';

          return (
            <button
              key={ceremony.id}
              type="button"
              onClick={() => setActiveCeremonyId(ceremony.id)}
              className={`flex items-center gap-2 shrink-0 rounded-xl px-4 py-2.5 transition-all text-xs font-semibold cursor-pointer border ${
                isActive
                  ? 'bg-[#2B0F1E] text-white border-[#2B0F1E] shadow-sm'
                  : 'bg-white text-[#22181C] border-[#E6DFD3] hover:border-[#C89B3C] hover:bg-[#FAF8F5]'
              }`}
            >
              <span className="tracking-wide uppercase font-serif text-sm">{ceremonyTitle}</span>
              <span
                className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                  isActive ? 'bg-[#C89B3C] text-[#2B0F1E]' : 'bg-black/5 text-[#22181C]'
                }`}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Sarees Grid */}
      {activeCeremony && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="font-serif text-xl font-bold text-[#2B0F1E]">
              {activeCeremony.title || activeCeremony.name} Looks
            </h2>
            <span className="text-xs text-text-muted">
              Click ❤️ Love, 👍 Like, or 🤔 Pass to cast your vote
            </span>
          </div>

          {(!activeCeremony.items || activeCeremony.items.length === 0) ? (
            <div className="p-12 text-center text-xs text-text-muted bg-[#FAF8F5] rounded-3xl border border-dashed border-[#E6DFD3]">
              No sarees added to this ceremony yet.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {activeCeremony.items.map((item) => (
                <ItemCard
                  key={item.id}
                  item={item}
                  isOwner={false}
                  onVote={(reaction) => handleVoteClick(item.id, reaction)}
                  userVote={userVotes[item.id]}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Voter Profile Modal */}
      {isVoterPromptOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-sm w-full shadow-2xl border border-[#E6DFD3] overflow-hidden">
            <div className="p-5 border-b border-[#E6DFD3] bg-[#FAF8F5]">
              <span className="text-[10px] font-bold uppercase tracking-widest text-[#C89B3C]">
                Family Voting
              </span>
              <h3 className="font-serif text-lg font-bold text-[#2B0F1E]">
                Who is Voting?
              </h3>
            </div>

            <form onSubmit={handleSaveVoterProfile} className="p-5 space-y-4">
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Your Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Amma, Sunita Aunty, Ananya"
                  value={voterName}
                  onChange={(e) => setVoterName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Phone (Optional)
                </label>
                <input
                  type="tel"
                  placeholder="10-digit mobile number"
                  value={voterPhone}
                  onChange={(e) => setVoterPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-2 border-t border-[#E6DFD3]">
                <button
                  type="button"
                  onClick={() => {
                    setIsVoterPromptOpen(false);
                    setPendingVote(null);
                  }}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!voterName.trim()}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] disabled:opacity-50"
                >
                  Save & Vote
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
