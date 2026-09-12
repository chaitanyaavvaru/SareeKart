import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AlertCircle, ArrowRight, Loader2, Lock, Mail, Phone, User, UserPlus } from 'lucide-react';
import { clearAuthError, registerUser } from '../../redux/slices/authSlice';
import SEO from '../../components/common/SEO';

function getSafeRedirect(value) {
  if (!value || !value.startsWith('/') || value.startsWith('//')) return '/';
  return value;
}

export default function RegisterPage() {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [validationError, setValidationError] = useState('');

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loading, error, isAuthenticated } = useSelector((state) => state.auth);

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate(getSafeRedirect(searchParams.get('redirect')), { replace: true });
    }
  }, [isAuthenticated, navigate, searchParams]);

  const handleSubmit = (event) => {
    event.preventDefault();
    setValidationError('');

    const normalizedFirstName = firstName.trim();
    const normalizedLastName = lastName.trim();
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedMobile = mobile.trim();
    const normalizedPassword = password.trim();
    const normalizedConfirmPassword = confirmPassword.trim();

    if (!normalizedFirstName || !normalizedEmail || !normalizedMobile || !normalizedPassword || !normalizedConfirmPassword) {
      setValidationError('Please fill in all required fields.');
      return;
    }

    if (normalizedFirstName.length < 2 || normalizedFirstName.length > 50) {
      setValidationError('First name must be between 2 and 50 characters.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setValidationError('Please enter a valid email address.');
      return;
    }

    if (normalizedPassword !== normalizedConfirmPassword) {
      setValidationError('Passwords do not match.');
      return;
    }

    if (normalizedPassword.length < 6 || normalizedPassword.length > 40) {
      setValidationError('Password must be between 6 and 40 characters.');
      return;
    }

    if (!/^[0-9]{10,15}$/.test(normalizedMobile)) {
      setValidationError('Phone number must be between 10 and 15 digits.');
      return;
    }

    dispatch(registerUser({
      firstName: normalizedFirstName,
      lastName: normalizedLastName,
      email: normalizedEmail,
      mobile: normalizedMobile,
      password: normalizedPassword,
    }));
  };

  const displayError = validationError || error;

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Your Edit Starts Here | SareeKart"
        description="Create a SareeKart account to save the drapes you love and move through checkout with less fuss."
      />

      <section className="section-shell grid min-h-[calc(100vh-116px)] justify-items-center gap-8 py-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:justify-items-stretch">
        <div className="hidden overflow-hidden rounded-[8px] bg-[#17211F] shadow-luxury lg:block">
          <div className="relative min-h-[760px]">
            <img
              src="https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=1200&q=80"
              alt="Colorful saree collection"
              className="absolute inset-0 h-full w-full object-cover object-top opacity-78"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#17211F] via-[#17211F]/25 to-transparent" />
            <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#F3C56A]">A better way to browse</p>
              <h2 className="mt-3 max-w-md text-4xl font-bold leading-tight">Save your favorites for later.</h2>
            </div>
          </div>
        </div>

        <div className="sk-center-panel w-full max-w-lg">
          <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-soft sm:p-8">
            <div>
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Your edit starts here</p>
              <h1 className="mt-2 text-4xl font-bold text-[#17211F]">Make room for color.</h1>
              <p className="mt-3 text-sm font-medium leading-7 text-[#71817A]">
                Save favorites, track orders, and check out faster.
              </p>
            </div>

            {displayError && (
              <div role="alert" className="mt-6 flex items-center gap-3 rounded-[8px] border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-900">
                <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
                <span>{displayError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="mt-6 grid gap-4" noValidate>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-firstname">
                  First name *
                  <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                    <User className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                    <input
                      id="reg-firstname"
                      type="text"
                      required
                      placeholder="First name"
                      value={firstName}
                      onChange={(event) => setFirstName(event.target.value)}
                      className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                    />
                  </div>
                </label>
                <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-lastname">
                  Last name
                  <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                    <input
                      id="reg-lastname"
                      type="text"
                      placeholder="Last name"
                      value={lastName}
                      onChange={(event) => setLastName(event.target.value)}
                      className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                    />
                  </div>
                </label>
              </div>

              <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-email">
                Email address *
                <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                  <Mail className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                  <input
                    id="reg-email"
                    type="email"
                    required
                    placeholder="email@example.com"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                  />
                </div>
              </label>

              <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-mobile">
                Mobile number *
                <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                  <Phone className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                  <input
                    id="reg-mobile"
                    type="tel"
                    required
                    placeholder="9876543210"
                    value={mobile}
                    onChange={(event) => setMobile(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                  />
                </div>
              </label>

              <div className="grid gap-4 sm:grid-cols-2">
                <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-password">
                  Password *
                  <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                    <Lock className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                    <input
                      id="reg-password"
                      type="password"
                      required
                      placeholder="Password"
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                    />
                  </div>
                </label>
                <label className="grid gap-2 text-sm font-black text-[#17211F]" htmlFor="reg-confirm-password">
                  Confirm *
                  <div className="flex h-12 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 focus-within:border-[#1E6A62]">
                    <Lock className="mr-3 h-4 w-4 shrink-0 text-[#71817A]" />
                    <input
                      id="reg-confirm-password"
                      type="password"
                      required
                      placeholder="Password"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                    className="w-full bg-transparent text-sm font-semibold outline-none placeholder:text-[#9AA5A5]"
                    />
                  </div>
                </label>
              </div>

              <button type="submit" disabled={loading} className="sk-button-primary mt-2 w-full disabled:opacity-60">
                {loading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Creating account...
                  </>
                ) : (
                  <>
                    Create Account
                    <UserPlus className="h-4 w-4" />
                  </>
                )}
              </button>
            </form>

            <p className="mt-6 text-center text-sm font-semibold text-[#71817A]">
              Already have an account?{' '}
              <Link
                to={`/login${searchParams.get('redirect') ? `?redirect=${searchParams.get('redirect')}` : ''}`}
                className="inline-flex items-center gap-1 font-black text-[#1E6A62] hover:text-[#B84F49]"
              >
                Sign In
                <ArrowRight className="h-4 w-4" />
              </Link>
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
