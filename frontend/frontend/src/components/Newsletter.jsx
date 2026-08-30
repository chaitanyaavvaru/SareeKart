
import { useState } from 'react';
import { Mail, Check } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Newsletter() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (email.trim()) {
      setSubmitted(true);
      setEmail('');
    }
  };

  return (
    <section className="w-full min-h-[90vh] bg-[#2B0F1E] text-white flex items-center justify-center relative overflow-hidden py-32 border-t border-white/5 select-none">
      {/* Background silk ribbons/patterns */}
      <div className="absolute inset-0 opacity-15 bg-cover bg-center pointer-events-none" style={{ backgroundImage: "url('https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=1600')" }}></div>
      <div className="absolute inset-0 bg-black/55 z-0"></div>

      <div className="relative z-10 max-w-[1480px] mx-auto px-10 sm:px-20 w-full text-center space-y-8">
        <div className="flex items-center justify-center gap-2">
          <span className="h-px w-8 bg-[#C89B3C]"></span>
          <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">
            Patron Registration
          </span>
        </div>
        
        <div className="space-y-4">
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-white leading-tight">
            Join the Weaver Guild
          </h2>
          <p className="text-sm text-white/70 max-w-xl mx-auto font-sans leading-relaxed font-light">
            Subscribe to receive private weaver previews, editorial updates on limited loom drops, and invitations to brand trunk shows. Connect directly with artisanal weavers.
          </p>
        </div>

        {!submitted ? (
          <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-4 pt-4 max-w-lg mx-auto w-full">
            <div className="relative flex-grow">
              <input
                type="email"
                required
                placeholder="Enter your email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-white/10 hover:bg-white/15 focus:bg-white focus:text-[#22181C] border border-white/20 focus:border-[#C89B3C] outline-none px-6 h-[56px] rounded-xl text-sm placeholder-white/60 focus:placeholder-gray-400 font-sans transition-all text-white font-semibold"
              />
              <Mail className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-white/40 pointer-events-none" />
            </div>
            <button
              type="submit"
              className="px-8 h-[56px] bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl text-xs uppercase tracking-widest transition-colors cursor-pointer shadow-lg shrink-0"
            >
              Request Invitation
            </button>
          </form>
        ) : (
          <motion.div 
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="p-5 bg-white/10 border border-[#C89B3C]/40 rounded-2xl flex items-center justify-center gap-2 max-w-md mx-auto text-xs font-bold uppercase tracking-widest text-[#C89B3C]"
          >
            <Check className="w-4 h-4" /> Registration Successful! Welcome to the Guild.
          </motion.div>
        )}
      </div>
    </section>
  );
}
