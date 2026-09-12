import { useEffect, useRef, useState } from 'react';
import { Bot, Check, RotateCcw, Send, ShoppingBag, Sparkles, X } from 'lucide-react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { addToCart, closeAddedModal } from '../../redux/slices/cartSlice';
import { sendGeminiMessage } from '../../services/geminiService';

const initialMessages = [
  {
    id: 1,
    sender: 'bot',
    text: 'Namaste! I am your AI Saree Stylist, powered by Google Gemini. Tell me your occasion, favorite colors, fabric, or budget, and I will recommend and help you book the perfect handcrafted saree.',
    suggestions: [
      'Wedding silk under ₹20,000',
      'Daily office cotton saree',
      'Gift for my mother',
      'Tell me about Kanchipuram weave',
    ],
  },
];

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

export default function AiAssistantModal() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState(initialMessages);
  const [inputQuery, setInputQuery] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [recentlyBookedId, setRecentlyBookedId] = useState(null);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const chatEndRef = useRef(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  const handleSendMessage = async (textToSend) => {
    const query = textToSend || inputQuery.trim();
    if (!query || isTyping) return;

    const userMsg = { id: Date.now(), sender: 'user', text: query };
    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputQuery('');
    setIsTyping(true);

    try {
      const response = await sendGeminiMessage(messages, query);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: response.text,
          products: response.products,
          suggestions: response.suggestions,
        },
      ]);
    } catch (err) {
      console.error('Error fetching AI stylist response:', err);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: 'I apologize, I encountered a brief connection issue. Please feel free to rephrase or browse our curated catalog.',
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleSendMessageRef = useRef(handleSendMessage);
  useEffect(() => {
    handleSendMessageRef.current = handleSendMessage;
  });

  useEffect(() => {
    const handleOpenWithPrompt = (e) => {
      setIsOpen(true);
      if (e.detail?.prompt) {
        // Small delay to ensure modal mounts before sending message
        setTimeout(() => {
          handleSendMessageRef.current?.(e.detail.prompt);
        }, 150);
      }
    };
    window.addEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
    return () => window.removeEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
  }, []);

  const handleBookProduct = (product) => {
    dispatch(addToCart(product));
    // Dismiss full-screen cart modal to maintain seamless in-chat concierge booking
    dispatch(closeAddedModal());
    setRecentlyBookedId(product.id);
    setTimeout(() => setRecentlyBookedId(null), 4000);
  };

  const handleClearChat = () => {
    setMessages(initialMessages);
  };

  return (
    <>
      {/* Floating Trigger Button */}
      <motion.button
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-40 flex items-center gap-2.5 rounded-full border border-white/60 bg-[#17211F] px-4 py-3.5 text-white shadow-2xl transition hover:bg-[#1E6A62]"
        aria-label="Open AI Saree Stylist and Booking Assistant"
      >
        <Sparkles className="h-5 w-5 text-[#F3C56A] animate-pulse" />
        <span className="text-xs font-bold uppercase tracking-wider">AI Stylist</span>
      </motion.button>

      {/* Slide-Over Chat Modal */}
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-[200] flex items-end justify-end p-0 text-left sm:p-6">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="absolute inset-0 bg-[#17211F]/50 backdrop-blur-xs"
            />

            <motion.section
              initial={{ y: 40, opacity: 0, scale: 0.98 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: 40, opacity: 0, scale: 0.98 }}
              transition={{ duration: 0.22 }}
              className="relative z-10 flex h-[90vh] w-full flex-col overflow-hidden rounded-t-2xl border border-[#DDD8CF] bg-[#F7F4EE] shadow-2xl sm:h-[680px] sm:w-[450px] sm:rounded-2xl"
            >
              {/* Header */}
              <header className="flex items-center justify-between border-b border-white/10 bg-[#17211F] px-5 py-4 text-white">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#1E6A62]/60 border border-[#F3C56A]/40">
                    <Sparkles className="h-5 w-5 text-[#F3C56A]" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-serif font-bold">SareeKart AI Stylist</h3>
                      <span className="rounded-full bg-emerald-900/60 border border-emerald-400/40 px-2 py-0.5 text-[9px] font-bold uppercase tracking-widest text-emerald-300">
                        Gemini 2.5
                      </span>
                    </div>
                    <p className="text-[11px] text-[#DDD8CF]/80">Curated recommendations & direct booking</p>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={handleClearChat}
                    title="Clear Conversation"
                    className="flex h-8 w-8 items-center justify-center rounded-full text-white/70 hover:bg-white/10 hover:text-white transition"
                    aria-label="Reset chat"
                  >
                    <RotateCcw className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => setIsOpen(false)}
                    className="flex h-8 w-8 items-center justify-center rounded-full text-white/70 hover:bg-white/10 hover:text-white transition"
                    aria-label="Close assistant"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              </header>

              {/* Chat Scroll View */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {messages.map((message) => (
                  <div
                    key={message.id}
                    className={`flex ${message.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[88%] ${message.sender === 'user' ? 'text-right' : 'text-left'}`}
                    >
                      {/* Message Bubble */}
                      <div
                        className={`rounded-2xl p-3.5 text-xs font-medium leading-relaxed shadow-xs ${
                          message.sender === 'user'
                            ? 'bg-[#1E6A62] text-white rounded-br-none'
                            : 'bg-white text-[#17211F] border border-[#DDD8CF] rounded-bl-none'
                        }`}
                      >
                        <p className="whitespace-pre-line">{message.text}</p>
                      </div>

                      {/* Recommended Catalog Product Cards */}
                      {message.products && message.products.length > 0 && (
                        <div className="mt-3 space-y-2">
                          <p className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] ml-1">
                            Recommended Handloom Drapes:
                          </p>
                          {message.products.map((product) => {
                            const isJustAdded = recentlyBookedId === product.id;
                            return (
                              <article
                                key={product.id}
                                className="flex items-center justify-between gap-3 rounded-xl border border-[#DDD8CF] bg-white p-2.5 shadow-xs transition hover:border-[#1E6A62]"
                              >
                                <div className="flex items-center gap-3 min-w-0">
                                  <img
                                    src={product.image}
                                    alt={product.name}
                                    className="h-16 w-14 rounded-lg object-cover object-top shrink-0 border border-[#E5E0D8]"
                                  />
                                  <div className="min-w-0">
                                    <span className="text-[9px] font-bold uppercase tracking-wider text-[#1E6A62] block">
                                      {product.category || product.fabric}
                                    </span>
                                    <h4 className="truncate text-xs font-bold text-[#17211F]">
                                      {product.name}
                                    </h4>
                                    <p className="text-xs font-serif font-bold text-[#17211F] mt-0.5">
                                      {formatCurrency(product.price)}
                                    </p>
                                  </div>
                                </div>

                                <button
                                  onClick={() => handleBookProduct(product)}
                                  className={`shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-lg text-[11px] font-bold uppercase tracking-wider transition ${
                                    isJustAdded
                                      ? 'bg-emerald-700 text-white'
                                      : 'bg-[#17211F] hover:bg-[#1E6A62] text-white shadow-xs'
                                  }`}
                                  aria-label={`Book ${product.name} now`}
                                >
                                  {isJustAdded ? (
                                    <>
                                      <Check className="h-3.5 w-3.5" /> Booked!
                                    </>
                                  ) : (
                                    <>
                                      <ShoppingBag className="h-3.5 w-3.5 text-[#F3C56A]" /> Book Now
                                    </>
                                  )}
                                </button>
                              </article>
                            );
                          })}

                          {/* Quick Checkout Link */}
                          {recentlyBookedId && (
                            <motion.div
                              initial={{ opacity: 0, y: 5 }}
                              animate={{ opacity: 1, y: 0 }}
                              className="mt-2 flex items-center justify-between bg-emerald-50 border border-emerald-200 rounded-lg p-2.5 text-xs text-emerald-900"
                            >
                              <span className="font-semibold">Saree added to your bag!</span>
                              <button
                                onClick={() => {
                                  setIsOpen(false);
                                  navigate('/checkout');
                                }}
                                className="font-bold underline text-emerald-800 hover:text-emerald-950"
                              >
                                Go to Checkout →
                              </button>
                            </motion.div>
                          )}
                        </div>
                      )}

                      {/* Interactive Suggestion Chips */}
                      {message.suggestions && message.suggestions.length > 0 && (
                        <div className="mt-3 flex flex-wrap gap-1.5">
                          {message.suggestions.map((suggestion, idx) => (
                            <button
                              key={idx}
                              onClick={() => handleSendMessage(suggestion)}
                              className="rounded-full border border-[#DDD8CF] bg-white px-3 py-1 text-[11px] font-medium text-[#17211F] shadow-xs hover:border-[#1E6A62] hover:bg-[#FAF8F5] transition"
                            >
                              {suggestion}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {/* Typing Indicator */}
                {isTyping && (
                  <div className="flex items-center gap-2 w-fit rounded-2xl border border-[#DDD8CF] bg-white px-4 py-3 text-xs font-semibold text-[#71817A] shadow-xs">
                    <Sparkles className="h-3.5 w-3.5 text-[#F3C56A] animate-spin" />
                    <span>Gemini is curating sarees...</span>
                  </div>
                )}
                <div ref={chatEndRef} />
              </div>

              {/* Chat Input Bar */}
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  handleSendMessage();
                }}
                className="flex items-center gap-2 border-t border-[#DDD8CF] bg-white p-3"
              >
                <input
                  type="text"
                  placeholder="Ask Gemini: occasion, color, budget, or book a saree..."
                  value={inputQuery}
                  onChange={(event) => setInputQuery(event.target.value)}
                  disabled={isTyping}
                  className="h-11 min-w-0 flex-1 rounded-full border border-[#DDD8CF] bg-[#F7F4EE] px-4 text-xs font-medium outline-none focus:border-[#1E6A62] disabled:opacity-60"
                />
                <button
                  type="submit"
                  disabled={!inputQuery.trim() || isTyping}
                  className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#1E6A62] text-white hover:bg-[#16524C] transition disabled:opacity-40 shadow-xs"
                  aria-label="Send message to AI Stylist"
                >
                  <Send className="h-4 w-4" />
                </button>
              </form>
            </motion.section>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
