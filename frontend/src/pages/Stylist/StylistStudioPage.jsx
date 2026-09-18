import { useEffect, useState, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Sparkles,
  Scissors,
  Crown,
  Heart,
  Calendar,
  Gem,
  ArrowRight,
  RefreshCw,
  ShoppingBag,
  CheckCircle2,
  Filter,
  Layers,
  Video,
  MessageSquare,
  Send,
  User as UserIcon,
  Bot
} from 'lucide-react';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl } from '../../utils/seoUtils';
import aiStylistService from '../../services/aiStylistService';
import AiStylistModal from '../../components/stylist/AiStylistModal';
import { useCurrency } from '../../context/CurrencyContext';

const OCCASIONS = [
  { id: 'Wedding', label: 'Bridal / Muhurtham', desc: 'Heirloom heavy zari silks, regal temple grandeur' },
  { id: 'Reception', label: 'Sangeet & Reception', desc: 'Luminous shimmer, contemporary cuts, rich colors' },
  { id: 'Temple', label: 'Temple Festival & Puja', desc: 'Sacred hues, auspicious Korvai borders, pure mulberry' },
  { id: 'Gala', label: 'Royal Gala / Dinner', desc: 'Understated luxury, tissue organza, fine brocade' },
  { id: 'Festive', label: 'Festive / Diwali', desc: 'Vibrant celebratory silks, celebratory palettes' }
];

const UNDERTONES = [
  { id: 'WARM', label: 'Warm Undertone', hint: 'Glows in Temple Gold, Vermilion, Emerald Green & Mustard', hex: '#D4AF37' },
  { id: 'COOL', label: 'Cool Undertone', hint: 'Radiates in Pastel Lilac, Silver Brocade, Royal Blue & Rose', hex: '#4A6FA5' },
  { id: 'JEWEL', label: 'Jewel Tones / Neutral', hint: 'Stunning in Ruby Red, Midnight Navy & Deep Peacock Teal', hex: '#800020' }
];

const WEAVES = [
  { id: 'ANY', label: 'All Heritage Weaves' },
  { id: 'KANCHIPURAM', label: 'Kanchipuram Silk' },
  { id: 'BANARASI', label: 'Banarasi Brocade' },
  { id: 'PAITHANI', label: 'Paithani Peacock' },
  { id: 'ORGANZA', label: 'Tissue Organza' }
];

const BUDGETS = [
  { id: 'ANY', label: 'Any Budget' },
  { id: 'UNDER_15K', label: 'Under ₹15,000' },
  { id: '15K_TO_30K', label: '₹15,000 – ₹30,000' },
  { id: 'LUXURY_30K_PLUS', label: '₹30,000+ Heirloom' }
];

const QUICK_PROMPTS = [
  'Wedding saree under ₹20,000 with contrast blouse',
  'Royal Kanchipuram silk for muhurtham in maroon',
  'Lightweight festive organza for cocktail reception',
  'Banarasi brocade under ₹15,000'
];

export default function StylistStudioPage() {
  const { formatPrice } = useCurrency();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('chat'); // 'chat' | 'quiz'

  // Quiz state
  const [selectedOccasion, setSelectedOccasion] = useState('Wedding');
  const [selectedUndertone, setSelectedUndertone] = useState('WARM');
  const [selectedWeave, setSelectedWeave] = useState('ANY');
  const [selectedBudget, setSelectedBudget] = useState('ANY');

  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');

  // Active modal for styling a specific saree
  const [stylingProduct, setStylingProduct] = useState(null);

  // Chat State
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [sessionId] = useState(() => 'sess_' + Math.random().toString(36).substring(2, 10));
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Namaste! I am your AI Luxury Saree Stylist and Drape Concierge. Tell me about your upcoming occasion, preferred weave, color, or budget, and I will curate complete handloom ensembles with bespoke contrast blouse pairings and temple jewelry coordinates.',
      recommendedSarees: [],
      primaryLook: null,
      consultationId: null
    }
  ]);

  const chatEndRef = useRef(null);

  const runConsultation = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await aiStylistService.consultQuiz({
        occasion: selectedOccasion,
        skinUndertone: selectedUndertone,
        preferredWeave: selectedWeave,
        budgetRange: selectedBudget
      });
      if (res && res.data) {
        setResults(res.data);
      }
    } catch (err) {
      setError(err.message || 'Unable to fetch personalized recommendations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'quiz') {
      runConsultation();
    }
  }, [activeTab]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, chatLoading]);

  const handleSendMessage = async (textToSend) => {
    const query = (textToSend || chatInput).trim();
    if (!query || chatLoading) return;

    const userMsg = { role: 'user', content: query };
    setMessages((prev) => [...prev, userMsg]);
    setChatInput('');
    setChatLoading(true);

    try {
      const history = messages
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map((m) => ({ role: m.role, content: m.content }));

      const res = await aiStylistService.chat({
        message: query,
        sessionId,
        occasion: selectedOccasion,
        budgetRange: selectedBudget,
        preferredWeave: selectedWeave,
        skinUndertone: selectedUndertone,
        conversationHistory: history
      });

      if (res && res.data) {
        const assistantMsg = {
          role: 'assistant',
          content: res.data.reply,
          recommendedSarees: res.data.recommendedSarees || [],
          primaryLook: res.data.primaryLook || null,
          consultationId: res.data.consultationId,
          fallbackUsed: res.data.fallbackUsed
        };
        setMessages((prev) => [...prev, assistantMsg]);
      }
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'I apologize, but our styling atelier is momentarily busy. Please try asking again or explore our curated lookbook above.',
          recommendedSarees: []
        }
      ]);
    } finally {
      setChatLoading(false);
    }
  };

  const handleHandoffToTailoring = (consultationId, look) => {
    if (consultationId) {
      aiStylistService.trackTailoring(consultationId);
    }
    const searchParams = new URLSearchParams();
    if (consultationId) searchParams.set('consultationId', consultationId);
    if (look?.blouse?.contrastColor) searchParams.set('contrastColor', look.blouse.contrastColor);
    if (look?.blouse?.frontNeck) searchParams.set('frontNeck', look.blouse.frontNeck);
    if (look?.blouse?.sleeve) searchParams.set('sleeve', look.blouse.sleeve);
    navigate(`/tailoring?${searchParams.toString()}`);
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#17211F] py-10">
      <SEO
        title="AI Luxury Saree Stylist & Drape Concierge | SareeKart"
        description="Curate bespoke bridal and festive saree ensembles with AI styling grounded in authentic handloom inventory, blouse pairings, and jewelry."
        canonical={getCanonicalUrl('/stylist')}
      />
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 space-y-10">
        
        {/* Studio Hero */}
        <div className="relative overflow-hidden rounded-[20px] bg-gradient-to-r from-[#17211F] via-[#1E6A62] to-[#12423D] px-8 py-12 text-white shadow-xl">
          <div className="relative z-10 max-w-3xl space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-black uppercase tracking-widest text-[#E3F5F0]">
              <Sparkles className="h-3.5 w-3.5 text-[#FDE68A]" />
              Phase 9: AI Luxury Saree Stylist & Drape Concierge
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-serif font-black tracking-tight text-[#FAF8F5]">
              AI Luxury Saree Stylist & Visual Drape Concierge
            </h1>
            <p className="text-sm sm:text-base text-[#E3F5F0]/90 leading-relaxed">
              Curate complete bridal and festive ensembles grounded in authentic SareeKart handloom inventory. 
              Converse with our atelier concierge to discover contrast blouses, temple jewelry pairings, and professional draping techniques.
            </p>

            {/* Tab Switcher */}
            <div className="pt-4 flex items-center gap-3">
              <button
                onClick={() => setActiveTab('chat')}
                className={`inline-flex items-center gap-2 rounded-full px-5 py-2.5 text-xs font-black tracking-wide transition-all shadow-sm ${
                  activeTab === 'chat'
                    ? 'bg-white text-[#17211F]'
                    : 'bg-white/15 text-white hover:bg-white/25'
                }`}
              >
                <MessageSquare className="h-4 w-4 text-[#1E6A62]" />
                Conversational Stylist Concierge
              </button>
              <button
                onClick={() => setActiveTab('quiz')}
                className={`inline-flex items-center gap-2 rounded-full px-5 py-2.5 text-xs font-black tracking-wide transition-all shadow-sm ${
                  activeTab === 'quiz'
                    ? 'bg-white text-[#17211F]'
                    : 'bg-white/15 text-white hover:bg-white/25'
                }`}
              >
                <Filter className="h-4 w-4 text-[#FDE68A]" />
                Guided Style Selector
              </button>
            </div>
          </div>
        </div>

        {/* TAB 1: CONVERSATIONAL STYLIST CHAT */}
        {activeTab === 'chat' && (
          <div className="rounded-[16px] border border-[#DDD8CF] bg-white shadow-xs overflow-hidden flex flex-col min-h-[640px]">
            {/* Chat Header */}
            <div className="border-b border-[#F0ECE1] bg-[#FAF8F5] px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#1E6A62] text-white shadow-xs">
                  <Bot className="h-5 w-5 text-[#FDE68A]" />
                </div>
                <div>
                  <h2 className="font-serif font-black text-sm text-[#17211F]">
                    SareeKart Drape Concierge & Stylist
                  </h2>
                  <p className="text-[11px] text-[#71817A] flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-emerald-500 inline-block animate-pulse" />
                    Online • Grounded in authentic catalog inventory
                  </p>
                </div>
              </div>
            </div>

            {/* Quick Prompt Chips */}
            <div className="px-6 py-3 border-b border-[#F0ECE1] bg-white flex flex-wrap items-center gap-2">
              <span className="text-[10px] font-black uppercase tracking-wider text-[#71817A] mr-1">
                Suggested Prompts:
              </span>
              {QUICK_PROMPTS.map((prompt, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendMessage(prompt)}
                  disabled={chatLoading}
                  className="rounded-full border border-[#DDD8CF] bg-[#FAF8F5] px-3 py-1 text-[11px] font-bold text-[#17211F] hover:border-[#1E6A62] hover:bg-[#E7F5F3] hover:text-[#1E6A62] transition-colors disabled:opacity-50"
                >
                  {prompt}
                </button>
              ))}
            </div>

            {/* Chat Messages Stream */}
            <div className="flex-1 p-6 overflow-y-auto space-y-6 max-h-[600px] bg-[#FCFAF7]">
              {messages.map((msg, i) => (
                <div
                  key={i}
                  className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  {msg.role === 'assistant' && (
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1E6A62] text-white shadow-2xs">
                      <Bot className="h-4 w-4 text-[#FDE68A]" />
                    </div>
                  )}

                  <div className={`max-w-2xl space-y-4 ${msg.role === 'user' ? 'text-right' : 'text-left'}`}>
                    <div
                      className={`inline-block rounded-[14px] p-4 text-xs sm:text-sm leading-relaxed shadow-2xs ${
                        msg.role === 'user'
                          ? 'bg-[#1E6A62] text-white rounded-br-none'
                          : 'bg-white border border-[#E7E2D8] text-[#17211F] rounded-bl-none whitespace-pre-line'
                      }`}
                    >
                      {msg.content}
                    </div>

                    {/* Grounded Recommended Sarees Cards */}
                    {msg.recommendedSarees && msg.recommendedSarees.length > 0 && (
                      <div className="pt-2 space-y-2 text-left">
                        <div className="flex items-center gap-1.5 text-[11px] font-black uppercase tracking-wider text-[#1E6A62]">
                          <Sparkles className="h-3 w-3 text-[#FDE68A]" />
                          Verified In-Stock Recommendations:
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          {msg.recommendedSarees.map((item, idx) => {
                            const p = item.product;
                            return (
                              <div
                                key={idx}
                                className="rounded-[12px] border border-[#DDD8CF] bg-white p-3 shadow-2xs flex gap-3 items-center justify-between"
                              >
                                <div className="h-16 w-16 shrink-0 rounded-[8px] bg-[#F7F4EE] overflow-hidden">
                                  <img
                                    src={p.imageUrl || p.images?.[0] || 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=800&q=80'}
                                    alt={p.name}
                                    className="h-full w-full object-cover"
                                  />
                                </div>
                                <div className="flex-1 min-w-0">
                                  <p className="font-serif font-black text-xs text-[#17211F] truncate">
                                    {p.name}
                                  </p>
                                  <p className="text-[10px] text-[#71817A] truncate">
                                    {p.fabric || 'Pure Silk'} • {p.color || 'Artisanal'}
                                  </p>
                                  <p className="text-xs font-black text-[#1E6A62] mt-0.5">
                                    {formatPrice(p.price)}
                                  </p>
                                </div>
                                <div className="flex flex-col gap-1 shrink-0">
                                  <button
                                    onClick={() => setStylingProduct(p)}
                                    className="rounded-[6px] bg-[#E7F5F3] px-2.5 py-1 text-[10px] font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition-all"
                                  >
                                    Ensemble
                                  </button>
                                  <Link
                                    to={`/products/${p.id}`}
                                    className="text-[10px] font-bold text-[#71817A] text-center hover:text-[#17211F]"
                                  >
                                    View
                                  </Link>
                                </div>
                              </div>
                            );
                          })}
                        </div>

                        {/* Direct Tailoring Studio Action Bar */}
                        {msg.primaryLook && (
                          <div className="rounded-[10px] bg-gradient-to-r from-[#E7F5F3] to-[#FAF8F5] border border-[#B8D8D1] p-3 flex items-center justify-between gap-3">
                            <div className="text-[11px] text-[#17211F]">
                              <span className="font-black text-[#1E6A62]">Stylist Preset:</span>{' '}
                              Pair with {msg.primaryLook.blouse.contrastColor} {msg.primaryLook.blouse.fabric}
                            </div>
                            <button
                              onClick={() => handleHandoffToTailoring(msg.consultationId, msg.primaryLook)}
                              className="inline-flex items-center gap-1.5 rounded-[6px] bg-[#1E6A62] px-3 py-1.5 text-xs font-bold text-white hover:bg-[#154e48] transition-all shrink-0 shadow-2xs"
                            >
                              <Scissors className="h-3 w-3" />
                              Customize Blouse
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {msg.role === 'user' && (
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#FAF8F5] border border-[#DDD8CF] text-[#17211F]">
                      <UserIcon className="h-4 w-4 text-[#71817A]" />
                    </div>
                  )}
                </div>
              ))}

              {chatLoading && (
                <div className="flex gap-3 items-center text-xs text-[#71817A]">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#1E6A62] text-white">
                    <RefreshCw className="h-4 w-4 animate-spin text-[#FDE68A]" />
                  </div>
                  <span>Atelier Stylist is curating your bespoke ensemble...</span>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>

            {/* Chat Input Bar */}
            <div className="border-t border-[#F0ECE1] bg-white p-4">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
                className="flex items-center gap-2"
              >
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Ask our stylist: e.g. 'Wedding saree in pink under ₹20,000' or 'What blouse pairs with emerald green?'"
                  className="flex-1 rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] px-4 py-3 text-xs sm:text-sm text-[#17211F] placeholder-[#9CA3AF] focus:border-[#1E6A62] focus:bg-white focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={!chatInput.trim() || chatLoading}
                  className="inline-flex items-center gap-1.5 rounded-[10px] bg-[#1E6A62] px-5 py-3 text-xs font-black text-white hover:bg-[#154e48] transition-all disabled:opacity-40 shadow-xs"
                >
                  <Send className="h-4 w-4" />
                  <span className="hidden sm:inline">Consult</span>
                </button>
              </form>
            </div>
          </div>
        )}

        {/* TAB 2: GUIDED STYLE SELECTOR (QUIZ) */}
        {activeTab === 'quiz' && (
          <div className="space-y-8">
            <div className="rounded-[16px] border border-[#DDD8CF] bg-white p-6 sm:p-8 shadow-xs space-y-8">
              {/* Step 1: Occasion */}
              <div>
                <div className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-[#1E6A62] mb-3">
                  <span className="flex h-5 w-5 items-center justify-center rounded-full bg-[#E7F5F3] text-[10px]">1</span>
                  <span>Select Celebratory Occasion</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
                  {OCCASIONS.map((occ) => (
                    <button
                      key={occ.id}
                      onClick={() => setSelectedOccasion(occ.id)}
                      className={`rounded-[10px] p-4 text-left border transition-all ${
                        selectedOccasion === occ.id
                          ? 'border-[#1E6A62] bg-[#E7F5F3] shadow-xs'
                          : 'border-[#E7E2D8] bg-[#FAF8F5] hover:bg-white'
                      }`}
                    >
                      <p className="font-serif font-black text-xs text-[#17211F]">{occ.label}</p>
                      <p className="text-[10px] text-[#71817A] mt-1 line-clamp-2">{occ.desc}</p>
                    </button>
                  ))}
                </div>
              </div>

              {/* Step 2: Skin Undertone & Color Palette */}
              <div>
                <div className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-[#1E6A62] mb-3">
                  <span className="flex h-5 w-5 items-center justify-center rounded-full bg-[#E7F5F3] text-[10px]">2</span>
                  <span>Select Color Harmony & Undertone</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {UNDERTONES.map((tone) => (
                    <button
                      key={tone.id}
                      onClick={() => setSelectedUndertone(tone.id)}
                      className={`rounded-[10px] p-4 text-left border transition-all ${
                        selectedUndertone === tone.id
                          ? 'border-[#1E6A62] bg-[#E7F5F3] shadow-xs'
                          : 'border-[#E7E2D8] bg-[#FAF8F5] hover:bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <span className="h-4 w-4 rounded-full shrink-0 shadow-2xs" style={{ backgroundColor: tone.hex }} />
                        <p className="font-serif font-black text-xs text-[#17211F]">{tone.label}</p>
                      </div>
                      <p className="text-[10px] text-[#71817A] mt-1.5">{tone.hint}</p>
                    </button>
                  ))}
                </div>
              </div>

              {/* Step 3: Weave & Budget Filter */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-4 border-t border-[#F0ECE1]">
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-2 uppercase tracking-wider">Preferred Weave</label>
                  <div className="flex flex-wrap gap-2">
                    {WEAVES.map((w) => (
                      <button
                        key={w.id}
                        onClick={() => setSelectedWeave(w.id)}
                        className={`rounded-full px-3.5 py-1.5 text-xs font-bold transition-all border ${
                          selectedWeave === w.id
                            ? 'bg-[#1E6A62] text-white border-[#1E6A62]'
                            : 'bg-white text-[#71817A] border-[#DDD8CF] hover:border-[#1E6A62]'
                        }`}
                      >
                        {w.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-2 uppercase tracking-wider">Investment Budget</label>
                  <div className="flex flex-wrap gap-2">
                    {BUDGETS.map((b) => (
                      <button
                        key={b.id}
                        onClick={() => setSelectedBudget(b.id)}
                        className={`rounded-full px-3.5 py-1.5 text-xs font-bold transition-all border ${
                          selectedBudget === b.id
                            ? 'bg-[#1E6A62] text-white border-[#1E6A62]'
                            : 'bg-white text-[#71817A] border-[#DDD8CF] hover:border-[#1E6A62]'
                        }`}
                      >
                        {b.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Consultation Trigger Button */}
              <div className="flex justify-end pt-2">
                <button
                  onClick={runConsultation}
                  disabled={loading}
                  className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 py-3 text-xs font-black text-white hover:bg-[#154e48] shadow-sm transition-all disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      Styling Lookbook...
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-4 w-4 text-[#FDE68A]" />
                      Generate Curated Lookbook
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Results Showcase Grid */}
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-4">
                <div>
                  <h2 className="text-xl sm:text-2xl font-serif font-black text-[#17211F]">
                    Your Bespoke Styled Lookbook
                  </h2>
                  <p className="text-xs text-[#71817A] mt-0.5">
                    Handpicked heirloom catalog items matching {selectedOccasion} and {selectedUndertone.toLowerCase()} palette.
                  </p>
                </div>
                <span className="text-xs font-bold text-[#1E6A62]">
                  {results.length} Heirloom Sarees Found
                </span>
              </div>

              {error && (
                <div className="rounded-[10px] bg-red-50 border border-red-200 p-4 text-center text-xs font-bold text-red-700">
                  {error}
                </div>
              )}

              {loading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {[1, 2, 3].map((n) => (
                    <div key={n} className="h-80 rounded-[12px] bg-white border border-[#E7E2D8] animate-pulse p-4 space-y-4">
                      <div className="h-48 bg-[#F7F4EE] rounded-[8px]" />
                      <div className="h-4 bg-[#F7F4EE] rounded w-3/4" />
                      <div className="h-3 bg-[#F7F4EE] rounded w-1/2" />
                    </div>
                  ))}
                </div>
              ) : results.length === 0 ? (
                <div className="rounded-[12px] border border-[#DDD8CF] bg-white p-12 text-center text-xs text-[#71817A]">
                  No sarees match the selected filter combination. Try selecting "All Heritage Weaves" or "Any Budget".
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {results.map((product) => (
                    <div
                      key={product.id}
                      className="group rounded-[14px] border border-[#DDD8CF] bg-white shadow-2xs hover:shadow-md transition-all overflow-hidden flex flex-col justify-between"
                    >
                      <div className="relative aspect-4/5 overflow-hidden bg-[#F7F4EE]">
                        <img
                          src={product.imageUrl || product.images?.[0] || 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=800&q=80'}
                          alt={product.name}
                          className="h-full w-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
                        />
                        <div className="absolute top-3 left-3">
                          <span className="rounded-full bg-white/90 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-[#17211F] backdrop-blur-xs shadow-xs">
                            {product.fabric || 'Pure Silk'}
                          </span>
                        </div>
                      </div>

                      <div className="p-5 space-y-3 flex-1 flex flex-col justify-between">
                        <div>
                          <p className="text-[10px] font-bold uppercase tracking-wider text-[#B45309]">{product.categoryName || 'Heritage Weave'}</p>
                          <h3 className="font-serif font-black text-sm text-[#17211F] line-clamp-1 mt-0.5">
                            {product.name}
                          </h3>
                          <p className="text-xs font-black text-[#1E6A62] mt-1">
                            {formatPrice(product.price)}
                          </p>
                        </div>

                        <div className="pt-2 border-t border-[#F0ECE1] space-y-2">
                          <button
                            onClick={() => setStylingProduct(product)}
                            className="w-full inline-flex items-center justify-center gap-1.5 rounded-[8px] bg-[#E7F5F3] px-4 py-2.5 text-xs font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition-all shadow-2xs"
                          >
                            <Sparkles className="h-3.5 w-3.5" />
                            Style Complete Ensemble
                          </button>
                          <Link
                            to={`/products/${product.id}`}
                            className="w-full inline-flex items-center justify-center gap-1 text-[11px] font-bold text-[#71817A] hover:text-[#17211F] py-1"
                          >
                            View Saree Details
                            <ArrowRight className="h-3 w-3" />
                          </Link>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Video Appointment Banner */}
        <div className="rounded-[16px] border border-[#B8D8D1] bg-gradient-to-r from-[#E7F5F3] via-white to-[#E7F5F3] p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xs">
          <div className="space-y-1 text-center sm:text-left">
            <h3 className="text-lg font-serif font-black text-[#17211F] flex items-center justify-center sm:justify-start gap-2">
              <Video className="h-5 w-5 text-[#1E6A62]" />
              Need 1-on-1 Guidance with a Master Draper?
            </h3>
            <p className="text-xs text-[#71817A] max-w-xl">
              Book a complimentary private virtual appointment with our flagship boutique stylists in Bengaluru or Hyderabad.
            </p>
          </div>
          <Link
            to="/stores"
            className="inline-flex items-center gap-2 rounded-[8px] bg-[#17211F] px-5 py-2.5 text-xs font-bold text-white hover:bg-black transition-colors shrink-0 shadow-sm"
          >
            <Calendar className="h-4 w-4" />
            Book Video Appointment
          </Link>
        </div>

      </div>

      {/* AiStylistModal instance */}
      {stylingProduct && (
        <AiStylistModal
          isOpen={true}
          onClose={() => setStylingProduct(null)}
          product={stylingProduct}
          onCustomizeTailoring={(preset) => {
            const searchParams = new URLSearchParams();
            if (preset.frontNeck) searchParams.set('frontNeck', preset.frontNeck);
            if (preset.sleeve) searchParams.set('sleeve', preset.sleeve);
            if (preset.notes) searchParams.set('notes', preset.notes);
            navigate(`/tailoring?${searchParams.toString()}`);
          }}
        />
      )}
    </div>
  );
}
