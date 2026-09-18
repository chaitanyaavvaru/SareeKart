import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, ArrowRight, Loader2, CheckCircle2, AlertCircle, KeyRound, ExternalLink } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [devResetUrl, setDevResetUrl] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmed = email.trim();
    if (!trimmed) {
      setError('Please enter your email address.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Please enter a valid email address.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password', { email: trimmed });
      setSuccess(true);
      if (res.data?.data?.resetToken) {
        setDevResetUrl(`/reset-password?token=${res.data.data.resetToken}`);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit reset request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Forgot Password | SareeKart"
        description="Reset your SareeKart password securely."
        noindex={true}
      />

      <section className="section-shell grid min-h-[calc(100vh-116px)] justify-items-center gap-8 py-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:justify-items-stretch">
        <div className="hidden overflow-hidden rounded-[8px] bg-[#17211F] shadow-luxury lg:block">
          <div className="relative min-h-[640px]">
            <img
              src="https://kankatala.com/cdn/shop/files/1216740117_3.webp?v=1786342018&width=1200"
              alt="Heritage handloom craftsmanship"
              className="absolute inset-0 h-full w-full object-cover object-top opacity-75"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#17211F] via-[#17211F]/25 to-transparent" />
            <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#F3C56A]">Account Security</p>
              <h2 className="mt-3 max-w-md text-4xl font-bold leading-tight">Always keep your wardrobe protected.</h2>
            </div>
          </div>
        </div>

        <div className="sk-center-panel w-full max-w-md">
          <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-soft sm:p-8">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-[#71817A] hover:text-[#17211F] transition-colors mb-6"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Back to Sign In
            </Link>

            <div>
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Account Recovery</p>
              <h1 className="mt-2 text-3xl font-bold text-[#17211F]">Forgot password?</h1>
              <p className="mt-3 text-sm font-medium leading-6 text-[#71817A]">
                Enter your registered email address and we'll generate password reset instructions.
              </p>
            </div>

            {error && (
              <div
                role="alert"
                className="mt-6 flex items-start gap-3 rounded-[8px] border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-900"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {success ? (
              <div className="mt-6 space-y-4">
                <div className="flex items-start gap-3 rounded-[8px] border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-900">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                  <div>
                    <p className="font-bold">Instructions sent!</p>
                    <p className="mt-1 text-xs text-emerald-800">
                      If an account exists for <strong className="font-bold">{email}</strong>, you will receive instructions to reset your password. The link is valid for 30 minutes.
                    </p>
                  </div>
                </div>

                {devResetUrl && (
                  <div className="rounded-[8px] border border-amber-200 bg-amber-50 p-4">
                    <p className="text-xs font-black uppercase tracking-wider text-amber-800 flex items-center gap-1.5">
                      <KeyRound className="h-3.5 w-3.5 text-amber-700" /> Development Direct Shortcut
                    </p>
                    <p className="mt-1 text-xs text-amber-700">
                      In local development mode, you can proceed directly to the reset page:
                    </p>
                    <Link
                      to={devResetUrl}
                      className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-[#17211F] px-4 py-2 text-xs font-bold text-[#F3C56A] hover:bg-[#1E6A62] transition-colors"
                    >
                      Open Password Reset Form <ExternalLink className="h-3 w-3" />
                    </Link>
                  </div>
                )}

                <div className="pt-2 text-center">
                  <Link
                    to="/login"
                    className="inline-flex items-center gap-1 text-sm font-bold text-[#1E6A62] hover:text-[#B84F49]"
                  >
                    Return to login <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="mt-6 grid gap-4" noValidate>
                <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reset-email">
                  Email address
                  <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                    <Mail className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                    <input
                      id="reset-email"
                      type="email"
                      autoComplete="email"
                      placeholder="email@example.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                    />
                  </div>
                </label>

                <button
                  type="submit"
                  disabled={loading}
                  className="sk-button-primary mt-2 w-full disabled:opacity-60"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Sending Instructions...
                    </>
                  ) : (
                    <>
                      Send Reset Instructions
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>
              </form>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
