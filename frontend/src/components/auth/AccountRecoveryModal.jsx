import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  KeyRound,
  Mail,
  MessageCircle,
  X,
  Loader2,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  ExternalLink,
  Sparkles,
} from 'lucide-react';
import api from '../../api/axiosConfig';

export default function AccountRecoveryModal({ isOpen, onClose, initialEmail = '' }) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('key'); // 'key' | 'email' | 'whatsapp'
  const [email, setEmail] = useState(initialEmail);
  const [recoveryKey, setRecoveryKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [resetToken, setResetToken] = useState('');
  const [resetUrl, setResetUrl] = useState('');

  useEffect(() => {
    if (isOpen) {
      setEmail(initialEmail || '');
      setError('');
      setSuccess(false);
      setResetToken('');
      setResetUrl('');
    }
  }, [isOpen, initialEmail]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleKeyRecovery = async (e) => {
    e.preventDefault();
    setError('');
    const trimmedEmail = email.trim();
    const trimmedKey = recoveryKey.trim();

    if (!trimmedEmail) {
      setError('Please enter your email address.');
      return;
    }
    if (!trimmedKey) {
      setError('Please enter your Emergency Security Recovery Key.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/verify-recovery-key', {
        email: trimmedEmail,
        recoveryKey: trimmedKey,
      });

      setSuccess(true);
      const token = res.data?.data?.resetToken;
      const url = res.data?.data?.resetUrl || `/reset-password?token=${token}`;
      setResetToken(token);
      setResetUrl(url);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          'Failed to verify recovery key. Please ensure your email and key are accurate.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleEmailRecovery = async (e) => {
    e.preventDefault();
    setError('');
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      setError('Please enter your email address.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setError('Please enter a valid email address.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password', { email: trimmedEmail });
      setSuccess(true);
      const token = res.data?.data?.resetToken;
      if (token) {
        setResetToken(token);
        setResetUrl(`/reset-password?token=${token}`);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit reset request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleProceedToReset = () => {
    onClose();
    if (resetToken) {
      navigate(`/reset-password?token=${resetToken}`);
    } else if (resetUrl) {
      navigate(resetUrl);
    }
  };

  const handleFillDemoKey = (demoEmail, demoKey) => {
    setEmail(demoEmail);
    setRecoveryKey(demoKey);
    setError('');
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="recovery-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative w-full max-w-lg rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#1E6A62]/10 text-[#1E6A62]">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <h2 id="recovery-modal-title" className="text-xl font-bold text-[#17211F]">
                Login Security & Recovery
              </h2>
              <p className="text-xs font-semibold text-[#71817A]">
                Restore access using your preferred verified recovery channel
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close recovery dialog"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div
          role="tablist"
          aria-label="Recovery Options"
          className="mt-5 grid grid-cols-3 gap-1 rounded-[8px] bg-[#F7F4EE] p-1 text-xs font-bold"
        >
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'key'}
            onClick={() => {
              setActiveTab('key');
              setError('');
              setSuccess(false);
            }}
            className={`flex items-center justify-center gap-1.5 rounded-[6px] py-2 px-2 transition-all ${
              activeTab === 'key'
                ? 'bg-white text-[#1E6A62] shadow-xs'
                : 'text-[#71817A] hover:text-[#17211F]'
            }`}
          >
            <KeyRound className="h-3.5 w-3.5" />
            <span>Security Key</span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'email'}
            onClick={() => {
              setActiveTab('email');
              setError('');
              setSuccess(false);
            }}
            className={`flex items-center justify-center gap-1.5 rounded-[6px] py-2 px-2 transition-all ${
              activeTab === 'email'
                ? 'bg-white text-[#1E6A62] shadow-xs'
                : 'text-[#71817A] hover:text-[#17211F]'
            }`}
          >
            <Mail className="h-3.5 w-3.5" />
            <span>Email Link</span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'whatsapp'}
            onClick={() => {
              setActiveTab('whatsapp');
              setError('');
              setSuccess(false);
            }}
            className={`flex items-center justify-center gap-1.5 rounded-[6px] py-2 px-2 transition-all ${
              activeTab === 'whatsapp'
                ? 'bg-white text-[#1E6A62] shadow-xs'
                : 'text-[#71817A] hover:text-[#17211F]'
            }`}
          >
            <MessageCircle className="h-3.5 w-3.5" />
            <span>WhatsApp</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            role="alert"
            className="mt-4 flex items-start gap-2.5 rounded-[8px] border border-red-200 bg-red-50 p-3.5 text-xs font-bold text-red-900"
          >
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
            <span>{error}</span>
          </div>
        )}

        {/* Success Alert */}
        {success && (
          <div
            role="alert"
            className="mt-4 rounded-[8px] border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-950"
          >
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              <div>
                <p className="font-bold">Identity verified successfully!</p>
                <p className="mt-1 font-medium text-emerald-800">
                  {activeTab === 'key'
                    ? 'Your Emergency Security Recovery Key is verified. You can now choose a new password.'
                    : 'A secure reset link has been dispatched to your email address.'}
                </p>
              </div>
            </div>

            {(resetToken || resetUrl) && (
              <div className="mt-3 border-t border-emerald-200/60 pt-3">
                <button
                  type="button"
                  onClick={handleProceedToReset}
                  className="flex w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] py-2.5 px-4 text-xs font-black text-white hover:bg-[#154e48] transition-colors"
                >
                  Proceed to Set New Password
                  <ArrowRight className="h-4 w-4" />
                </button>
              </div>
            )}
          </div>
        )}

        {/* Tab 1: Emergency Recovery Key */}
        {activeTab === 'key' && !success && (
          <form onSubmit={handleKeyRecovery} className="mt-5 grid gap-4">
            <div className="rounded-[8px] bg-[#F7F4EE] p-3 text-xs text-[#71817A]">
              <p className="font-semibold text-[#17211F]">Instant Bypass via Security Key</p>
              <p className="mt-1">
                Enter your registered email and 16-character Emergency Recovery Key to bypass email
                delays and reset your password immediately.
              </p>
            </div>

            <label className="grid gap-1.5 text-xs font-black text-[#17211F]" htmlFor="recovery-email">
              Registered Email
              <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                <Mail className="mr-2.5 h-4 w-4 shrink-0 text-[#71817A]" />
                <input
                  id="recovery-email"
                  type="email"
                  required
                  placeholder="customer@sareekart.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-transparent text-xs font-semibold outline-none placeholder:text-[#9AA5A5]"
                />
              </div>
            </label>

            <label className="grid gap-1.5 text-xs font-black text-[#17211F]" htmlFor="recovery-key-input">
              Emergency Security Recovery Key
              <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                <KeyRound className="mr-2.5 h-4 w-4 shrink-0 text-[#71817A]" />
                <input
                  id="recovery-key-input"
                  type="text"
                  required
                  placeholder="SK-REC-XXXX-XXXX"
                  value={recoveryKey}
                  onChange={(e) => setRecoveryKey(e.target.value.toUpperCase())}
                  className="w-full bg-transparent font-mono text-xs font-bold tracking-wider outline-none placeholder:text-[#9AA5A5]"
                />
              </div>
            </label>

            {/* Demo Quick-Fill Buttons */}
            <div className="rounded-[8px] border border-[#F3C56A]/40 bg-[#FFFDF7] p-3">
              <div className="flex items-center gap-1.5 text-[11px] font-bold text-[#9B6A27]">
                <Sparkles className="h-3.5 w-3.5 text-[#F3C56A]" />
                <span>Evaluation Demo Keys</span>
              </div>
              <div className="mt-2 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => handleFillDemoKey('customer@sareekart.com', 'SK-REC-CUST-2026')}
                  className="rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 py-1 text-[11px] font-bold text-[#17211F] hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Customer: SK-REC-CUST-2026
                </button>
                <button
                  type="button"
                  onClick={() => handleFillDemoKey('admin@sareekart.com', 'SK-REC-ADMN-2026')}
                  className="rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 py-1 text-[11px] font-bold text-[#17211F] hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Admin: SK-REC-ADMN-2026
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="mt-1 flex h-11 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-bold text-white hover:bg-[#154e48] transition-colors disabled:opacity-60"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Verifying Recovery Key...
                </>
              ) : (
                <>
                  <KeyRound className="h-4 w-4" />
                  Verify Key & Unlock Account
                </>
              )}
            </button>
          </form>
        )}

        {/* Tab 2: Email Reset Link */}
        {activeTab === 'email' && !success && (
          <form onSubmit={handleEmailRecovery} className="mt-5 grid gap-4">
            <div className="rounded-[8px] bg-[#F7F4EE] p-3 text-xs text-[#71817A]">
              <p className="font-semibold text-[#17211F]">Cryptographic Reset Token</p>
              <p className="mt-1">
                We will dispatch a secure, single-use password reset link to your inbox valid for 30
                minutes.
              </p>
            </div>

            <label className="grid gap-1.5 text-xs font-black text-[#17211F]" htmlFor="tab-email-input">
              Registered Email Address
              <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                <Mail className="mr-2.5 h-4 w-4 shrink-0 text-[#71817A]" />
                <input
                  id="tab-email-input"
                  type="email"
                  required
                  placeholder="email@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-transparent text-xs font-semibold outline-none placeholder:text-[#9AA5A5]"
                />
              </div>
            </label>

            <button
              type="submit"
              disabled={loading}
              className="mt-1 flex h-11 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-bold text-white hover:bg-[#154e48] transition-colors disabled:opacity-60"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Sending Link...
                </>
              ) : (
                <>
                  <Mail className="h-4 w-4" />
                  Send Reset Link
                </>
              )}
            </button>
          </form>
        )}

        {/* Tab 3: WhatsApp Concierge Assisted Recovery */}
        {activeTab === 'whatsapp' && (
          <div className="mt-5 space-y-4">
            <div className="rounded-[8px] border border-emerald-200 bg-emerald-50/70 p-4 text-xs text-emerald-950">
              <div className="flex items-center gap-2 font-bold text-[#1E6A62]">
                <MessageCircle className="h-4 w-4" />
                <span>Direct Concierge Verification</span>
              </div>
              <p className="mt-1.5 leading-relaxed text-[#71817A]">
                Need immediate human assistance? Connect with our dedicated SareeKart customer security
                concierge on WhatsApp. We will securely verify your account identity, look up your recent
                handloom reservations, and help you regain access within minutes.
              </p>
            </div>

            <div className="rounded-[8px] bg-[#F7F4EE] p-3 text-xs text-[#17211F]">
              <p className="font-black">Official Security Concierge Desk</p>
              <p className="mt-1 font-mono text-sm font-bold text-[#1E6A62]">+91 90595 64499</p>
              <p className="mt-1 text-[11px] text-[#71817A]">
                Hours: 24/7 Security Escalations & VIP Handloom Support
              </p>
            </div>

            <a
              href={`https://wa.me/919059564499?text=${encodeURIComponent(
                `Hello SareeKart Support, I need assistance recovering my account for email: ${
                  email || '(not specified)'
                }.`
              )}`}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-11 w-full items-center justify-center gap-2 rounded-[8px] bg-[#25D366] text-xs font-bold text-white hover:bg-[#20b858] transition-colors"
            >
              <MessageCircle className="h-4 w-4" />
              Contact Security Concierge on WhatsApp
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>
        )}

        {/* Footer */}
        <div className="mt-6 flex items-center justify-between border-t border-[#EAE6DF] pt-4 text-xs">
          <span className="text-[#71817A]">Remembered your credentials?</span>
          <button
            type="button"
            onClick={onClose}
            className="font-bold text-[#1E6A62] hover:text-[#B84F49] transition-colors"
          >
            Back to Sign In
          </button>
        </div>
      </div>
    </div>
  );
}
