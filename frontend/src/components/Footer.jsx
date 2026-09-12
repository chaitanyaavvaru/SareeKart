import { Link } from 'react-router-dom';
import { Mail, MapPin, MessageCircle, Send } from 'lucide-react';

const columns = [
  {
    title: 'Shop',
    links: [
      ['New arrivals', '/products'],
      ['Kanchipuram', '/products?search=Kanchipuram'],
      ['Silk sarees', '/products?search=silk'],
      ['Cotton sarees', '/products?search=cotton'],
      ['Wedding edit', '/products?search=bridal'],
      ['Trousseau planner', '/trousseau-planner'],
    ],
  },
  {
    title: 'Discover',
    links: [
      ['Banarasi', '/products?search=Banarasi'],
      ['Paithani', '/products?search=Paithani'],
      ['Ikat', '/products?search=Ikat'],
      ['Organza', '/products?search=Organza'],
      ['Linen & tussar', '/products?search=Linen'],
    ],
  },
  {
    title: 'Support',
    links: [
      ['Track order', '/track-order'],
      ['Shipping & delivery', '/products'],
      ['Returns', '/products'],
      ['Saree care', '/saree-care'],
      ['Contact us', 'mailto:hello@sareekart.com'],
    ],
  },
  {
    title: 'About',
    links: [
      ['Our story', '/'],
      ['The weavers', '/artisans'],
      ['Heritage weaves', '/heritage-weaves'],
      ['Store locator', '/stores'],
      ['Events', '/products'],
    ],
  },
];

function FooterLink({ href, children }) {
  if (href.startsWith('mailto:')) {
    return <a href={href} className="text-sm font-semibold text-[#71817A] transition hover:text-[#1E6A62]">{children}</a>;
  }
  return <Link to={href} className="text-sm font-semibold text-[#71817A] transition hover:text-[#1E6A62]">{children}</Link>;
}

export default function Footer() {
  return (
    <footer className="border-t border-[#DDD8CF] bg-white text-[#17211F]">
      <section className="border-b border-[#DDD8CF] bg-[#F3E6C7] py-12 sm:py-16">
        <div className="section-shell grid gap-7 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.22em] text-[#1E6A62]">From the loom to your inbox</p>
            <h2 className="mt-2 font-serif text-4xl font-medium text-[#17211F]">Discover elegant sarees first.</h2>
            <p className="mt-3 max-w-lg text-sm leading-6 text-[#4E5B56]">New arrivals, weave notes, styling ideas, and early access to the pieces we are most excited about.</p>
          </div>
          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={(event) => event.preventDefault()}>
            <label htmlFor="footer-email" className="sr-only">Email address</label>
            <input id="footer-email" type="email" placeholder="Your email address" className="h-13 min-w-0 flex-1 border border-[#C9C1B5] bg-white px-5 text-sm text-[#17211F] outline-none focus:border-[#1E6A62]" />
            <button type="submit" className="inline-flex h-13 items-center justify-center gap-2 bg-[#17211F] px-6 text-sm font-bold uppercase tracking-[0.1em] text-white transition hover:bg-[#1E6A62]">Subscribe <Send className="h-4 w-4" /></button>
          </form>
        </div>
      </section>

      <div className="section-shell py-12 sm:py-16">
        <div className="grid gap-10 lg:grid-cols-[1.25fr_repeat(4,1fr)]">
          <div className="max-w-xs">
            <Link to="/" className="font-serif text-3xl font-medium">SareeKart</Link>
            <p className="mt-4 text-sm leading-6 text-[#71817A]">A considered edit of Indian handlooms, chosen for texture, color, and the way you actually dress.</p>
            <div className="mt-6 grid gap-3 text-sm font-semibold text-[#4E5B56]">
              <a href="mailto:hello@sareekart.com" className="inline-flex items-center gap-2 transition hover:text-[#1E6A62]"><Mail className="h-4 w-4 text-[#B84F49]" /> hello@sareekart.com</a>
              <a href="https://wa.me/919059564499" target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 transition hover:text-[#1E6A62]"><MessageCircle className="h-4 w-4 text-[#1E6A62]" /> +91 90595 64499</a>
              <span className="inline-flex items-start gap-2"><MapPin className="mt-0.5 h-4 w-4 text-[#C48B3C]" /> Bengaluru, India</span>
            </div>
          </div>

          {columns.map((column) => (
            <div key={column.title}>
              <h3 className="text-xs font-black uppercase tracking-[0.16em] text-[#17211F]">{column.title}</h3>
              <ul className="mt-5 grid gap-3">
                {column.links.map(([label, href]) => <li key={label}><FooterLink href={href}>{label}</FooterLink></li>)}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 grid gap-5 border-t border-[#DDD8CF] pt-6 text-sm font-semibold text-[#71817A] sm:flex sm:items-center sm:justify-between">
          <p>Copyright &copy; {new Date().getFullYear()} SareeKart. All rights reserved.</p>
          <div className="flex items-center gap-5">
            <a href="#" className="transition hover:text-[#1E6A62]">Privacy</a>
            <a href="#" className="transition hover:text-[#1E6A62]">Terms</a>
            <a href="https://instagram.com" target="_blank" rel="noreferrer" aria-label="Instagram" className="transition hover:text-[#B84F49]"><svg className="h-4 w-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><path d="M17.5 6.5h.01" /></svg></a>
          </div>
        </div>
      </div>
    </footer>
  );
}
