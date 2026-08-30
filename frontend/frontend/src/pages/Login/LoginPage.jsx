import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Mail, Lock, LogIn, ArrowRight } from 'lucide-react';
import { loginUser, clearAuthError } from '../../redux/slices/authSlice';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [validationError, setValidationError] = useState('');
  
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  
  const { loading, error, isAuthenticated } = useSelector(state => state.auth);

  // Clear errors on load
  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  // Redirect if authenticated
  useEffect(() => {
    if (isAuthenticated) {
      const redirectUrl = searchParams.get('redirect') || '/';
      navigate(redirectUrl);
    }
  }, [isAuthenticated, navigate, searchParams]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setValidationError('');
    
    if (!email || !password) {
      setValidationError('Please fill in all fields.');
      return;
    }
    
    dispatch(loginUser({ email, password }));
  };

  return (
    <div className="min-h-[calc(100vh-64px)] grid grid-cols-1 lg:grid-cols-12 bg-[#FAF8F5] font-sans">
      
      {/* Left Column: Visual Cover Portrait (Hidden on Mobile) */}
      <div className="hidden lg:block lg:col-span-5 relative overflow-hidden bg-[#2C0F1F]">
        <img 
          src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=1000" 
          alt="Luxury handwoven drape model portrait" 
          className="w-full h-full object-cover object-top opacity-55 absolute inset-0"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#2C0F1F] via-[#2C0F1F]/40 to-transparent"></div>
        
        {/* Brand Crest Overlays */}
        <div className="absolute bottom-16 left-12 right-12 text-white space-y-4">
          <span className="text-[#C9A227] font-serif text-sm tracking-widest uppercase font-semibold block">
            SAREEKART SIGNATURE
          </span>
          <h2 className="text-4xl font-serif font-extrabold leading-tight text-[#FAF8F5]">
            Heritage Weaves <br />Woven for Generations
          </h2>
          <div className="w-16 h-0.5 bg-[#C9A227]"></div>
          <p className="text-xs text-white/70 font-light leading-relaxed max-w-sm">
            Experience Indian handloom luxury. Access your order history and manage your artisan showcases.
          </p>
        </div>
      </div>

      {/* Right Column: Clean Minimal Form Area */}
      <div className="col-span-1 lg:col-span-7 flex items-center justify-center p-8 sm:p-16">
        <div className="max-w-md w-full space-y-8">
          
          <div className="space-y-2">
            <h2 className="text-3xl sm:text-4xl font-serif font-extrabold text-[#3A1028] tracking-wide">
              Sign In
            </h2>
            <p className="text-sm text-text-secondary font-light">
              Welcome back to SareeKart. Access your order history and wishlist bookmarks.
            </p>
          </div>

          {/* Validation Errors */}
          {(validationError || error) && (
            <div className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-3.5 rounded-xl font-medium">
              {validationError || error}
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            
            {/* Email Field */}
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Email Address</label>
              <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-3 transition-all shadow-xs">
                <Mail className="w-4 h-4 text-text-muted mr-3 shrink-0" />
                <input
                  type="email"
                  required
                  placeholder="email@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex justify-between items-center">
                <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Password</label>
                <a href="#" className="text-[10px] font-bold text-[#C9A227] hover:underline uppercase tracking-wider">Forgot password?</a>
              </div>
              <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-3 transition-all shadow-xs">
                <Lock className="w-4 h-4 text-text-muted mr-3 shrink-0" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                />
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl text-xs uppercase tracking-widest transition-all disabled:bg-gray-400 disabled:cursor-not-allowed cursor-pointer shadow-md"
            >
              {loading ? 'Signing in...' : (
                <>Sign In <LogIn className="w-4.5 h-4.5" /></>
              )}
            </button>

          </form>

          {/* Playtester Pre-fill Shortcuts */}
          <div className="bg-white border border-[#F4F4F4] p-5 rounded-2xl space-y-3 shadow-xs">
            <p className="text-[10px] font-bold text-[#3A1028] uppercase tracking-wider border-b border-[#F4F4F4] pb-1.5">
              Playtester Quick Login
            </p>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => {
                  setEmail('customer@sareekart.com');
                  setPassword('customer123');
                }}
                className="py-2.5 bg-[#FAF8F5] hover:bg-[#F9F6F0] border border-[#F4F4F4] text-xs font-semibold rounded-xl text-text-primary transition cursor-pointer"
              >
                Customer Account
              </button>
              <button
                type="button"
                onClick={() => {
                  setEmail('admin@sareekart.com');
                  setPassword('admin123');
                }}
                className="py-2.5 bg-[#FAF8F5] hover:bg-[#F9F6F0] border border-[#F4F4F4] text-xs font-semibold rounded-xl text-text-primary transition cursor-pointer"
              >
                Admin Account
              </button>
            </div>
          </div>

          <div className="border-t border-[#F4F4F4] pt-6 text-center text-xs text-text-secondary font-light">
            <p>
              Don't have an account?{' '}
              <Link 
                to={`/register${searchParams.get('redirect') ? `?redirect=${searchParams.get('redirect')}` : ''}`} 
                className="font-bold text-[#3A1028] hover:text-[#C9A227] hover:underline flex items-center justify-center gap-1 mt-1.5 uppercase tracking-wider"
              >
                Create Account <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </p>
          </div>

        </div>
      </div>

    </div>
  );
}
