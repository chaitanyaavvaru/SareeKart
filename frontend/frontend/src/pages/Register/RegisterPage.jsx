import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Mail, Lock, Phone, UserPlus, ArrowRight } from 'lucide-react';
import { registerUser, clearAuthError } from '../../redux/slices/authSlice';

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
    
    if (!firstName || !email || !mobile || !password || !confirmPassword) {
      setValidationError('Please fill in all required fields.');
      return;
    }

    if (password !== confirmPassword) {
      setValidationError('Passwords do not match.');
      return;
    }

    if (password.length < 6) {
      setValidationError('Password must be at least 6 characters.');
      return;
    }

    if (!/^[0-9]{10,15}$/.test(mobile)) {
      setValidationError('Phone number must be between 10 and 15 digits.');
      return;
    }

    dispatch(registerUser({
      firstName,
      lastName,
      email,
      phone: mobile,
      password
    }));
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
        <div className="max-w-md w-full space-y-6">
          
          <div className="space-y-2">
            <h2 className="text-3xl sm:text-4xl font-serif font-extrabold text-[#3A1028] tracking-wide">
              Create Account
            </h2>
            <p className="text-sm text-text-secondary font-light">
              Join SareeKart to start your collection of handwoven artisanal sarees.
            </p>
          </div>

          {/* Validation Errors */}
          {(validationError || error) && (
            <div className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-3.5 rounded-xl font-medium">
              {validationError || error}
            </div>
          )}

          {/* Register Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Name Row */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">First Name *</label>
                <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-2.5 transition-all shadow-xs">
                  <input
                    type="text"
                    required
                    placeholder="First name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Last Name</label>
                <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-2.5 transition-all shadow-xs">
                  <input
                    type="text"
                    placeholder="Last name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                  />
                </div>
              </div>
            </div>

            {/* Email Field */}
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Email Address *</label>
              <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-2.5 transition-all shadow-xs">
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

            {/* Mobile Number */}
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Mobile Number *</label>
              <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-4 py-2.5 transition-all shadow-xs">
                <Phone className="w-4 h-4 text-text-muted mr-3 shrink-0" />
                <input
                  type="tel"
                  required
                  placeholder="9876543210"
                  value={mobile}
                  onChange={(e) => setMobile(e.target.value)}
                  className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                />
              </div>
            </div>

            {/* Password Row */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Password *</label>
                <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-3 py-2.5 transition-all shadow-xs">
                  <Lock className="w-4 h-4 text-text-muted mr-2 shrink-0" />
                  <input
                    type="password"
                    required
                    placeholder="••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-text-secondary uppercase tracking-wider">Confirm *</label>
                <div className="flex items-center bg-white border border-[#F4F4F4] focus-within:border-[#C9A227] rounded-xl px-3 py-2.5 transition-all shadow-xs">
                  <Lock className="w-4 h-4 text-text-muted mr-2 shrink-0" />
                  <input
                    type="password"
                    required
                    placeholder="••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="bg-transparent border-none outline-none text-sm w-full text-[#1C1C1C] placeholder-text-muted font-sans"
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl text-xs uppercase tracking-widest transition-all disabled:bg-gray-400 disabled:cursor-not-allowed cursor-pointer shadow-md mt-2"
            >
              {loading ? 'Creating account...' : (
                <>Create Account <UserPlus className="w-4.5 h-4.5" /></>
              )}
            </button>

          </form>

          <div className="border-t border-[#F4F4F4] pt-6 text-center text-xs text-text-secondary font-light">
            <p>
              Already have an account?{' '}
              <Link 
                to={`/login${searchParams.get('redirect') ? `?redirect=${searchParams.get('redirect')}` : ''}`} 
                className="font-bold text-[#3A1028] hover:text-[#C9A227] hover:underline flex items-center justify-center gap-1 mt-1.5 uppercase tracking-wider"
              >
                Sign In <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </p>
          </div>

        </div>
      </div>

    </div>
  );
}
