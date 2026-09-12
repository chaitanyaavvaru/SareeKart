import { Check, ShoppingBag, X } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import { closeAddedModal, setCartOpen } from '../../redux/slices/cartSlice';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

export default function AddedToCartModal() {
  const dispatch = useDispatch();
  const { addedItem, showAddedModal } = useSelector((state) => state.cart);

  if (!showAddedModal || !addedItem) return null;

  return (
    <div className="fixed inset-0 z-[150] flex items-center justify-center bg-[#111827]/55 px-4 backdrop-blur-sm">
      <button
        onClick={() => dispatch(closeAddedModal())}
        className="absolute inset-0 cursor-default"
        aria-label="Close added to bag dialog"
      />

      <section className="relative w-full max-w-md rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-luxury sm:p-6">
        <button
          onClick={() => dispatch(closeAddedModal())}
          className="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full bg-[#F5F7FA] text-[#64748B] transition hover:text-[#111827]"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="mb-5 flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[#E7F5F3] text-[#0F766E]">
            <Check className="h-6 w-6" />
          </div>
          <div>
            <p className="text-[11px] font-black uppercase tracking-[0.16em] text-[#0F766E]">
              Added to bag
            </p>
            <h2 className="text-2xl font-bold text-[#111827]">Nice pick.</h2>
          </div>
        </div>

        <div className="grid grid-cols-[96px_1fr] gap-4 rounded-[8px] bg-[#F5F7FA] p-3">
          <div className="h-28 overflow-hidden rounded-[8px] bg-white">
            <img
              src={addedItem.image || 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=240&q=80'}
              alt={addedItem.name}
              className="h-full w-full object-cover object-top"
            />
          </div>
          <div className="flex min-w-0 flex-col justify-center">
            <h3 className="line-clamp-2 text-base font-extrabold leading-snug text-[#111827]">{addedItem.name}</h3>
            <p className="mt-2 text-lg font-black text-[#243B6B]">{formatCurrency(addedItem.price)}</p>
            <p className="mt-1 text-xs font-bold text-[#64748B]">Quantity 1</p>
          </div>
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          <button
            onClick={() => dispatch(closeAddedModal())}
            className="sk-button-secondary w-full"
          >
            Continue
          </button>
          <button
            onClick={() => {
              dispatch(closeAddedModal());
              dispatch(setCartOpen(true));
            }}
            className="sk-button-primary w-full"
          >
            <ShoppingBag className="h-4 w-4" />
            View bag
          </button>
        </div>
      </section>
    </div>
  );
}
