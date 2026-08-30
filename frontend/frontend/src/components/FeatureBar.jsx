
import { ShieldCheck, Truck, Star } from 'lucide-react';
import { motion } from 'framer-motion';

const features = [
  {
    icon: <ShieldCheck className="w-6 h-6 text-[#3A1028]" />,
    title: "100% Authentic Handloom",
    description: "Certified silk mark products straight from artisan looms."
  },
  {
    icon: <Truck className="w-6 h-6 text-[#3A1028]" />,
    title: "Free Insured Shipping",
    description: "Get free delivery on orders exceeding ₹5,000 across India."
  },
  {
    icon: <Star className="w-6 h-6 text-[#3A1028]" />,
    title: "Artisan Empowerment",
    description: "We pay fair trade wages directly to our network of master weavers."
  }
];

export default function FeatureBar() {
  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 bg-white border border-[#F4F4F4] p-6 sm:p-8 rounded-2xl shadow-xs">
        {features.map((item, idx) => (
          <motion.div 
            key={idx}
            whileHover={{ y: -5, shadow: "0 10px 25px -5px rgba(0,0,0,0.05)" }}
            className="flex gap-4 items-start p-4 rounded-xl hover:bg-[#F9F6F0]/30 transition-all duration-300"
          >
            <div className="p-3 bg-[#F9F6F0] rounded-xl text-[#3A1028] shrink-0 border border-[#F4F4F4]">
              {item.icon}
            </div>
            <div className="space-y-1">
              <h4 className="font-bold text-text-primary text-sm sm:text-base font-serif tracking-wide">{item.title}</h4>
              <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-sans">{item.description}</p>
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
