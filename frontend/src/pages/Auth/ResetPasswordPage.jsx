import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, Eye, EyeOff, Loader2, CheckCircle2, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [verifying, setVerifying] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setVerifying(false);
      setTokenValid(false);
      setError('Password reset token is missing. Please request a new link.');
      return;
    }

    const checkToken = async () => {
      try {
        const res = await api.get(`/auth/verify-reset-token?token=${encodeURIComponent(token)}`);
        if (res.data?.success) {
          setTokenValid(true);
        } else {
          setTokenValid(false);
          setError('This password reset link is invalid or has expired.');
        }
      } catch (err) {
        setTokenValid(false);
        setError(err.response?.data?.message || 'This password reset link is invalid or has expired.');
      } finally {
        setVerifying(false);
      }
    };

    checkToken();
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await api.post('/auth/reset-password', {
        token: token.trim(),
        newPassword: newPassword.trim(),
      });
      if (res.data?.success) {
        setSuccess(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset password. The link may have expired.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Reset Password | SareeKart"
        description="Choose a secure new password for your SareeKart account."
      />

      <section className="section-shell grid min-h-[calc(100vh-116px)] justify-items-center gap-8 py-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:justify-items-stretch">
        <div className="hidden overflow-hidden rounded-[8px] bg-[#17211F] shadow-luxury lg:block">
          <div className="relative min-h-[640px]">
            <img
              src="https://kankatala.com/cdn/shop/files/1216740117_3.webp?v=1786342018&width=1200"
              alt="Heritage handloom styling"
              className="absolute inset-0 h-full w-full object-cover object-top opacity-75"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#17211F] via-[#17211F]/25 to-transparent" />
            <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#F3C56A]">Account Security</p>
              <h2 className="mt-3 max-w-md text-4xl font-bold leading-tight">Create your new secure credentials.</h2>
            </div>
          </div>
        </div>

        <div className="sk-center-panel w-full max-w-md">
          <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-soft sm:p-8">
            {verifying ? (
              <div className="py-12 text-center">
                <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#1E6A62]" />
                <p className="mt-4 text-xs font-bold uppercase tracking-wider text-[#71817A]">
                  Verifying reset token...
                </p>
              </div>
            ) : !tokenValid ? (
              <div className="space-y-6">
                <div>
                  <p className="text-[12px] font-black uppercase tracking-[0.18em] text-red-700">Link Expired or Invalid</p>
                  <h1 className="mt-2 text-2xl font-bold text-[#17211F]">Unable to reset password</h1>
                  <p className="mt-3 text-sm font-medium leading-6 text-[#71817A]">
                    {error || 'This reset link has expired or has already been used. Please request a fresh reset link.'}
                  </p>
                </div>

                <div className="pt-2">
                  <Link
                    to="/forgot-password"
                    className="sk-button-primary inline-flex w-full items-center justify-center gap-2"
                  >
                    Request New Reset Link <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            ) : success ? (
              <div className="space-y-6">
                <div className="flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                  <ShieldCheck className="h-7 w-7" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-[#17211F]">Password updated successfully</h1>
                  <p className="mt-2 text-sm font-medium leading-6 text-[#71817A]">
                    Your password has been changed. You can now sign in to your SareeKart account using your new password.
                  </p>
                </div>

                <Link
                  to="/login"
                  className="sk-button-primary inline-flex w-full items-center justify-center gap-2"
                >
                  Sign In With New Password <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            ) : (
              <div>
                <div>
                  <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Account Security</p>
                  <h1 className="mt-2 text-3xl font-bold text-[#17211F]">Set new password</h1>
                  <p className="mt-3 text-sm font-medium leading-6 text-[#71817A]">
                    Choose a strong password with at least 6 characters.
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

                <form onSubmit={handleSubmit} className="mt-6 grid gap-4" noValidate>
                  <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="new-password">
                    New Password
                    <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                      <Lock className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                      <input
                        id="new-password"
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        placeholder="At least 6 characters"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="text-[#71817A] hover:text-[#17211F] transition-colors p-1"
                      >
                        {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </label>

                  <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="confirm-password">
                    Confirm New Password
                    <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                      <Lock className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                      <input
                        id="confirm-password"
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        placeholder="Re-enter password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                      />
                    </div>
                  </label>

                  <button
                    type="submit"
                    disabled={submitting}
                    className="sk-button-primary mt-2 w-full disabled:opacity-60"
                  >
                    {submitting ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Updating Password...
                      </>
                    ) : (
                      <>
                        Update Password
                        <ArrowRight className="h-4 w-4" />
                      </>
                    )}
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
