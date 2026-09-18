import { useState } from 'react';
import { X, UserPlus, Copy, Check, MessageCircle, Trash2, Users, Loader2 } from 'lucide-react';

export default function CollaboratorsDrawer({
  isOpen,
  onClose,
  collaborators = [],
  onAddCollaborator,
  onRevokeCollaborator,
  boardTitle,
  shareToken,
}) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('VOTER');
  const [submitting, setSubmitting] = useState(false);
  const [copiedToken, setCopiedToken] = useState(null);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleInvite = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      await onAddCollaborator({
        name: name.trim(),
        phone: phone.trim() || null,
        email: email.trim() || null,
        role,
      });
      setName('');
      setPhone('');
      setEmail('');
    } catch (err) {
      setError(err || 'Failed to invite family member.');
    } finally {
      setSubmitting(false);
    }
  };

  const copyShareLink = (token) => {
    const activeToken = token || shareToken;
    const url = `${window.location.origin}/trousseau/share/${activeToken}`;
    navigator.clipboard.writeText(url);
    setCopiedToken(activeToken);
    setTimeout(() => setCopiedToken(null), 2500);
  };

  const getWhatsAppUrl = (col) => {
    const activeToken = col?.shareToken || shareToken;
    const shareUrl = `${window.location.origin}/trousseau/share/${activeToken}`;
    const colName = col?.name ? `Namaste ${col.name}! 🙏\n` : 'Namaste! 🙏\n';
    const message = `${colName}I have created our wedding trousseau wishlist "${boardTitle}" on SareeKart. Please take a look at the shortlisted sarees and vote on your favorite looks here:\n${shareUrl}`;
    const cleanPhone = (col?.phone || '').replace(/[^0-9]/g, '');
    return cleanPhone
      ? `https://wa.me/${cleanPhone}?text=${encodeURIComponent(message)}`
      : `https://wa.me/?text=${encodeURIComponent(message)}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white max-w-md w-full h-full flex flex-col shadow-2xl border-l border-[#E6DFD3] animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-6 border-b border-[#E6DFD3] flex items-center justify-between bg-[#FAF8F5]">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-widest text-[#C89B3C]">
              Family Collaboration & Voting
            </span>
            <h3 className="font-serif text-lg font-bold text-[#2B0F1E] flex items-center gap-2">
              <Users className="w-5 h-5 text-[#2B0F1E]" />
              Invited Family & Friends
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-black/5 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="p-3 text-xs bg-rose-50 text-rose-700 rounded-xl border border-rose-200">
              {error}
            </div>
          )}

          {/* Master Share Link Box */}
          {shareToken && (
            <div className="bg-[#FAF8F5] border border-[#C89B3C]/40 p-4 rounded-2xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[#8F6A1A]">
                  Direct Family Share Link
                </span>
                <span className="text-[10px] text-text-muted">Public voting enabled</span>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  readOnly
                  value={`${window.location.origin}/trousseau/share/${shareToken}`}
                  className="flex-1 px-3 py-1.5 rounded-lg border border-[#E6DFD3] bg-white text-xs text-text-muted font-mono select-all"
                />
                <button
                  type="button"
                  onClick={() => copyShareLink(shareToken)}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-[#2B0F1E] text-white text-xs font-bold hover:bg-[#3D142A] transition-colors cursor-pointer"
                >
                  {copiedToken === shareToken ? (
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="w-3.5 h-3.5 text-[#C89B3C]" />
                  )}
                  <span>{copiedToken === shareToken ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>
          )}

          {/* Invite New Collaborator Form */}
          <div className="bg-[#FAF8F5] border border-[#E6DFD3] p-4 rounded-2xl space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#2B0F1E] flex items-center gap-1.5">
              <UserPlus className="w-4 h-4 text-[#C89B3C]" />
              Invite New Family Member
            </h4>

            <form onSubmit={handleInvite} className="space-y-3">
              <div>
                <input
                  type="text"
                  required
                  placeholder="Full Name (e.g. Amma, Sunita Aunty)"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <input
                  type="tel"
                  placeholder="Phone (e.g. 9876543210)"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium bg-white"
                />
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium bg-white"
                >
                  <option value="VOTER">Voter</option>
                  <option value="CO_CURATOR">Co-Curator</option>
                  <option value="VIEWER">Viewer</option>
                </select>
              </div>

              <div>
                <input
                  type="email"
                  placeholder="Email (optional)"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium bg-white"
                />
              </div>

              <button
                type="submit"
                disabled={submitting || !name.trim()}
                className="w-full inline-flex items-center justify-center gap-1.5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] transition-colors disabled:opacity-50 cursor-pointer shadow-xs"
              >
                {submitting ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-[#C89B3C]" />
                ) : (
                  <UserPlus className="w-3.5 h-3.5 text-[#C89B3C]" />
                )}
                <span>Send Invitation</span>
              </button>
            </form>
          </div>

          {/* List of Current Collaborators */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-text-muted">
              Active Collaborators ({collaborators.length})
            </h4>

            {collaborators.length === 0 ? (
              <p className="text-xs text-text-muted italic text-center py-6">
                No family members invited yet. Invite your mother, sisters, and friends to vote on shortlisted looks!
              </p>
            ) : (
              <div className="space-y-2.5">
                {collaborators.map((col) => (
                  <div
                    key={col.id}
                    className="p-3 bg-white rounded-xl border border-[#E6DFD3] flex items-center justify-between gap-3 shadow-2xs"
                  >
                    <div className="min-w-0 space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="font-serif text-xs font-bold text-[#22181C] truncate">
                          {col.name}
                        </span>
                        <span className="text-[10px] px-1.5 py-0.2 rounded-md bg-[#FAF8F5] border border-[#E6DFD3] text-[#8F6A1A] font-semibold">
                          {col.role}
                        </span>
                      </div>
                      {col.phone && (
                        <p className="text-[11px] text-text-muted font-mono">{col.phone}</p>
                      )}
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      {/* WhatsApp Share Link */}
                      <a
                        href={getWhatsAppUrl(col)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-50 transition-colors"
                        title="Share via WhatsApp"
                      >
                        <MessageCircle className="w-4 h-4" />
                      </a>

                      {/* Revoke Collaborator */}
                      {onRevokeCollaborator && (
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Revoke access for ${col.name}?`)) {
                              onRevokeCollaborator(col.id);
                            }
                          }}
                          className="p-1.5 rounded-lg text-gray-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                          title="Revoke access"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
