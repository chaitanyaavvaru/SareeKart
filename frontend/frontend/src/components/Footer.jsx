
import { Link } from 'react-router-dom';
import { Mail, Phone, MapPin, Compass } from 'lucide-react';

const InstagramIcon = ({ className }) => (
  <svg 
    className={className} 
    viewBox="0 0 24 24" 
    fill="none" 
    stroke="currentColor" 
    strokeWidth="2" 
    strokeLinecap="round" 
    strokeLinejoin="round"
  >
    <rect x="2" y="2" width="20" height="20" rx="5" ry="5"/>
    <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/>
    <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/>
  </svg>
);

const YoutubeIcon = ({ className }) => (
  <svg 
    className={className} 
    viewBox="0 0 24 24" 
    fill="none" 
    stroke="currentColor" 
    strokeWidth="2" 
    strokeLinecap="round" 
    strokeLinejoin="round"
  >
    <path d="M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-2 29 29 0 0 0 .46-5.25 29 29 0 0 0-.46-5.33z"/>
    <polygon points="9.75 15.02 15.5 11.75 9.75 8.48 9.75 15.02"/>
  </svg>
);

export default function Footer() {
  return (
    <footer className="bg-[#2B0F1E] text-white border-t border-[#C89B3C]/30 pt-24 pb-12 font-sans">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
          
          {/* Column 1: Brand Info */}
          <div className="space-y-6">
            <div className="space-y-1">
              <h3 className="text-2xl font-bold font-serif tracking-[0.2em] uppercase text-[#C89B3C]">SareeKart</h3>
              <p className="text-[9px] text-white/40 uppercase tracking-[0.2em] font-semibold">Certified Handloom Heritage</p>
            </div>
            <p className="text-xs text-white/60 leading-relaxed font-light">
              Discover the finest handwoven sarees representing India's rich weaving heritage. From traditional Banarasi silks to elegant office wear cottons, we offer premium drapes for every story.
            </p>
            <div className="flex gap-3 pt-2">
              <a href="https://instagram.com" target="_blank" rel="noreferrer" className="w-9 h-9 rounded-full bg-white/5 hover:bg-[#C89B3C] hover:text-[#2B0F1E] transition-all flex items-center justify-center" aria-label="Instagram">
                <InstagramIcon className="w-4 h-4" />
              </a>
              <a href="https://youtube.com" target="_blank" rel="noreferrer" className="w-9 h-9 rounded-full bg-white/5 hover:bg-[#C89B3C] hover:text-[#2B0F1E] transition-all flex items-center justify-center" aria-label="YouTube">
                <YoutubeIcon className="w-4 h-4" />
              </a>
              <a href="https://pinterest.com" target="_blank" rel="noreferrer" className="w-9 h-9 rounded-full bg-white/5 hover:bg-[#C89B3C] hover:text-[#2B0F1E] transition-all flex items-center justify-center" aria-label="Pinterest">
                <Compass className="w-4 h-4" />
              </a>
            </div>
          </div>

          {/* Column 2: Collections */}
          <div>
            <h4 className="text-xs font-bold text-[#C89B3C] uppercase tracking-[0.2em] mb-6 font-serif">Collections</h4>
            <ul className="space-y-3 text-xs text-white/60 font-medium">
              <li><Link to="/products?category=Silk%20Sarees" className="hover:text-[#C89B3C] transition-colors font-light">Pure Silk</Link></li>
              <li><Link to="/products?category=Wedding%20Sarees" className="hover:text-[#C89B3C] transition-colors font-light">Wedding</Link></li>
              <li><Link to="/products?category=Banarasi%20Sarees" className="hover:text-[#C89B3C] transition-colors font-light">Royal Banarasi</Link></li>
              <li><Link to="/products?category=Cotton%20Sarees" className="hover:text-[#C89B3C] transition-colors font-light">Cotton</Link></li>
              <li><Link to="/products" className="hover:text-[#C89B3C] transition-colors font-light">Explore All</Link></li>
            </ul>
          </div>

          {/* Column 3: Customer Care */}
          <div>
            <h4 className="text-xs font-bold text-[#C89B3C] uppercase tracking-[0.2em] mb-6 font-serif">Customer Care</h4>
            <ul className="space-y-3 text-xs text-white/60 font-medium">
              <li><Link to="/orders" className="hover:text-[#C89B3C] transition-colors font-light">Track Order</Link></li>
              <li><a href="#" className="hover:text-[#C89B3C] transition-colors font-light">Shipping & Returns</a></li>
              <li><a href="#" className="hover:text-[#C89B3C] transition-colors font-light">Appreciation Support</a></li>
              <li><a href="#" className="hover:text-[#C89B3C] transition-colors font-light">Privacy Policy</a></li>
            </ul>
          </div>

          {/* Column 4: Contact */}
          <div className="space-y-4">
            <h4 className="text-xs font-bold text-[#C89B3C] uppercase tracking-[0.2em] mb-6 font-serif">Contact Us</h4>
            <div className="flex items-start gap-2.5 text-xs text-white/60">
              <MapPin className="w-4 h-4 text-[#C89B3C] shrink-0 mt-0.5" />
              <span className="font-light">12, Weaver's Colony, Handloom Avenue, Hyderabad - 500029, Telangana, India</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs text-white/60">
              <Phone className="w-4 h-4 text-[#C89B3C] shrink-0" />
              <span className="font-light">+91 40 2345 6789</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs text-white/60">
              <Mail className="w-4 h-4 text-[#C89B3C] shrink-0" />
              <span className="font-light">support@sareekart.com</span>
            </div>
          </div>

        </div>

        {/* Payment badges & Copyright info */}
        <div className="border-t border-white/10 pt-8 flex flex-col items-center justify-between gap-4 text-center">
          <p className="text-[9px] text-white/30 tracking-[0.2em] uppercase font-bold">
            Visa • Mastercard • UPI • RuPay • Net Banking
          </p>
          <p className="text-[10px] text-white/40 font-light">
            © {new Date().getFullYear()} SareeKart E-Commerce. All Rights Reserved. Crafted with love for Indian Heritage.
          </p>
        </div>
      </div>
    </footer>
  );
}
