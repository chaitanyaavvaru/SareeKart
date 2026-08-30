
import { motion } from 'framer-motion';

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

const MASONRY_COLUMNS = [
  [
    { id: 1, image: "https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=600", height: "h-[450px]", caption: "Bridal Red Heritage" },
    { id: 2, image: "https://images.unsplash.com/photo-1590050752117-238cb0612b1b?w=600", height: "h-[280px]", caption: "Palace Corridor Details" }
  ],
  [
    { id: 3, image: "https://images.unsplash.com/photo-1566552881560-0be862a7c445?w=600", height: "h-[300px]", caption: "Royal Weft Threads" },
    { id: 4, image: "https://images.unsplash.com/photo-1561376377-2690fb0d3113?w=600", height: "h-[480px]", caption: "Temple Architecture Textures" }
  ],
  [
    { id: 5, image: "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=600", height: "h-[420px]", caption: "Artisan Courtyard Life" },
    { id: 6, image: "https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?w=600", height: "h-[320px]", caption: "Mulberry Silk Folds" }
  ]
];

export default function InstagramGallery() {
  return (
    <section className="w-full min-h-screen bg-[#F9F6F1] flex items-center justify-center py-32 border-t border-[#F4F2EB]">
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full space-y-16">
        
        {/* Header Block */}
        <div className="text-center space-y-3">
          <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
            Visual Journal
          </span>
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
            Visual Tales of Silk
          </h2>
          <p className="text-sm text-text-secondary font-sans font-light">
            Follow our journey <a href="https://instagram.com" target="_blank" rel="noreferrer" className="font-bold text-[#C89B3C] hover:underline inline-flex items-center gap-1">@SareeKartLuxe <InstagramIcon className="w-3.5 h-3.5" /></a> for behind-the-scenes handloom stories.
          </p>
        </div>

        {/* Pinterest-style Masonry Columns Grid (Height 100vh overall) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">
          {MASONRY_COLUMNS.map((column, colIdx) => (
            <div key={colIdx} className="flex flex-col gap-6 w-full">
              {column.map((item, itemIdx) => (
                <motion.div 
                  key={item.id}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.8, delay: (colIdx * 2 + itemIdx) * 0.1, ease: "easeOut" }}
                  className={`group relative ${item.height} rounded-3xl overflow-hidden shadow-luxury border border-[#E6DFD3] bg-white cursor-pointer`}
                >
                  <img 
                    src={item.image} 
                    alt={item.caption} 
                    className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-[1.5s] ease-out object-top"
                    loading="lazy"
                  />
                  {/* Hover Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/35 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 flex flex-col justify-end p-6 text-left text-white">
                    <span className="text-[9px] uppercase font-bold text-[#C89B3C] tracking-widest font-sans flex items-center gap-1.5 mb-1">
                      <InstagramIcon className="w-3 h-3" /> Instagram Diary
                    </span>
                    <h4 className="text-md font-serif italic">{item.caption}</h4>
                  </div>
                </motion.div>
              ))}
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
