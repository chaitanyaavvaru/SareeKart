import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AlertCircle, ArrowRight, Loader2, Lock, LogIn, Mail, ShieldCheck, WifiOff } from 'lucide-react';
import { clearAuthError, loginUser } from '../../redux/slices/authSlice';
import SEO from '../../components/common/SEO';
import AccountRecoveryModal from '../../components/auth/AccountRecoveryModal';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [validationError, setValidationError] = useState('');
  const [showRecoveryModal, setShowRecoveryModal] = useState(false);

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loading, error, isAuthenticated, user } = useSelector((state) => state.auth);

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  useEffect(() => {
    if (isAuthenticated) {
      const explicitRedirect = searchParams.get('redirect');
      let redirectUrl = explicitRedirect;
      if (!redirectUrl) {
        redirectUrl = ['ADMIN', 'OWNER', 'MANAGER'].includes(user?.role) ? '/admin' : '/';
      }
      if (!redirectUrl.startsWith('/')) redirectUrl = `/${redirectUrl}`;
      navigate(redirectUrl, { replace: true });
    }
  }, [isAuthenticated, user, navigate, searchParams]);

  const handleSubmit = (event) => {
    event.preventDefault();
    setValidationError('');

    const trimmedEmail = email.trim();
    const trimmedPassword = password.trim();

    if (!trimmedEmail || !trimmedPassword) {
      setValidationError('Please fill in both email and password.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setValidationError('Please enter a valid email address.');
      return;
    }

    dispatch(loginUser({ email: trimmedEmail, password: trimmedPassword }));
  };

  const displayError = validationError || error;
  const isNetworkError = error && error.includes('Cannot connect to server');

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Welcome Back | SareeKart"
        description="Sign in to SareeKart to keep saved colors, checkout details, and order history together."
        noindex={true}
      />

      <section className="section-shell grid min-h-[calc(100vh-116px)] justify-items-center gap-8 py-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:justify-items-stretch">
        <div className="hidden overflow-hidden rounded-[8px] bg-[#17211F] shadow-luxury lg:block">
          <div className="relative min-h-[680px]">
            <img
              src="https://kankatala.com/cdn/shop/files/1216740117_3.webp?v=1786342018&width=1200"
              alt="Modern saree styling"
              className="absolute inset-0 h-full w-full object-cover object-top opacity-75"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#17211F] via-[#17211F]/25 to-transparent" />
            <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#F3C56A]">Welcome back</p>
              <h2 className="mt-3 max-w-md text-4xl font-bold leading-tight">Your next drape is waiting.</h2>
            </div>
          </div>
        </div>

        <div className="sk-center-panel w-full max-w-md">
          <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-soft sm:p-8">
            <div>
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Welcome back</p>
              <h1 className="mt-2 text-4xl font-bold text-[#17211F]">Pick up where you left off.</h1>
              <p className="mt-3 text-sm font-medium leading-7 text-[#71817A]">
                Your saved sarees, orders, and checkout details are here.
              </p>
            </div>

            {displayError && (
              <div
                role="alert"
                className={`mt-6 flex items-start gap-3 rounded-[8px] border p-4 text-sm font-bold ${
                  isNetworkError
                    ? 'border-orange-200 bg-orange-50 text-orange-900'
                    : 'border-red-200 bg-red-50 text-red-900'
                }`}
              >
                {isNetworkError ? <WifiOff className="mt-0.5 h-4 w-4 shrink-0" /> : <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />}
                <span>{displayError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="mt-6 grid gap-4" noValidate>
              <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="login-email">
                Email address
                <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                  <Mail className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                  <input
                    id="login-email"
                    type="email"
                    autoComplete="email"
                    placeholder="email@example.com"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                  />
                </div>
              </label>

              <div className="grid gap-2">
                <div className="flex items-center justify-between">
                  <label className="text-sm font-black text-[#17211F]" htmlFor="login-password">
                    Password
                  </label>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setShowRecoveryModal(true)}
                      className="inline-flex items-center gap-1 text-xs font-bold text-[#1E6A62] hover:text-[#B84F49] transition-colors"
                    >
                      <ShieldCheck className="h-3.5 w-3.5" />
                      Security & Recovery
                    </button>
                    <span className="text-[#DDD8CF]">•</span>
                    <Link
                      to="/forgot-password"
                      className="text-xs font-bold text-[#71817A] hover:text-[#17211F] transition-colors"
                    >
                      Forgot password?
                    </Link>
                  </div>
                </div>
                <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                  <Lock className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                  <input
                    id="login-password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="Password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="sk-button-primary mt-2 w-full disabled:opacity-60">
                {loading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Signing in...
                  </>
                ) : (
                  <>
                    Sign In
                    <LogIn className="h-4 w-4" />
                  </>
                )}
              </button>
            </form>

            <div className="mt-6 rounded-[8px] bg-[#F3E6C7] p-4">
              <p className="text-[12px] font-black uppercase tracking-[0.14em] text-[#9B6A27]">Quick access</p>
              <div className="mt-3 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setEmail('owner@sareekart.com');
                    setPassword('owner123');
                  }}
                  className="h-10 rounded-full bg-white px-4 text-xs font-black text-[#17211F] shadow-xs hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Owner
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEmail('manager@sareekart.com');
                    setPassword('manager123');
                  }}
                  className="h-10 rounded-full bg-white px-4 text-xs font-black text-[#17211F] shadow-xs hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Manager
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEmail('admin@sareekart.com');
                    setPassword('admin123');
                  }}
                  className="h-10 rounded-full bg-white px-4 text-xs font-black text-[#17211F] shadow-xs hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Admin
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEmail('customer@sareekart.com');
                    setPassword('customer123');
                  }}
                  className="h-10 rounded-full bg-white px-4 text-xs font-black text-[#17211F] shadow-xs hover:bg-[#1E6A62] hover:text-white transition-colors"
                >
                  Customer
                </button>
              </div>
            </div>

            <p className="mt-6 text-center text-sm font-semibold text-[#71817A]">
              New here?{' '}
              <Link
                to={`/register${searchParams.get('redirect') ? `?redirect=${searchParams.get('redirect')}` : ''}`}
                className="inline-flex items-center gap-1 font-black text-[#1E6A62] hover:text-[#B84F49]"
              >
                Create an account
                <ArrowRight className="h-4 w-4" />
              </Link>
            </p>
          </div>
        </div>
      </section>

      <AccountRecoveryModal
        isOpen={showRecoveryModal}
        onClose={() => setShowRecoveryModal(false)}
        initialEmail={email}
      />
    </div>
  );
}
