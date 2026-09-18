import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Wallet, 
  Sparkles, 
  Coins, 
  ArrowUpRight, 
  ArrowDownLeft, 
  RefreshCw, 
  CheckCircle2
} from 'lucide-react';
import walletService from '../../services/walletService';
import SEO from '../../components/common/SEO';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(val || 0);

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const tierConfigs = {
  SILVER: {
    name: 'Silver Patron',
    badgeClass: 'bg-[#E7ECE9] text-[#4E5B56] border-[#CBD5D1]',
    gradient: 'from-[#E7ECE9] to-[#D5DED9]',
    multiplier: '1.0x Points',
    nextThreshold: '₹25,000',
    nextTier: 'Gold Connoisseur'
  },
  GOLD: {
    name: 'Gold Connoisseur',
    badgeClass: 'bg-[#FEF3C7] text-[#92400E] border-[#FDE68A]',
    gradient: 'from-[#FEF3C7] to-[#FDE68A]',
    multiplier: '1.25x Points',
    nextThreshold: '₹1,00,000',
    nextTier: 'Royal Silk Club'
  },
  ROYAL_PATRON: {
    name: 'Royal Silk Club',
    badgeClass: 'bg-[#EDE9FE] text-[#6D28D9] border-[#DDD6FE]',
    gradient: 'from-[#EDE9FE] to-[#DDD6FE]',
    multiplier: '1.5x Points',
    nextThreshold: null,
    nextTier: 'Highest Level'
  }
};

export default function WalletPage() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');

  const loadWalletData = async () => {
    setLoading(true);
    try {
      const [walletData, txData] = await Promise.all([
        walletService.getMyWallet(),
        walletService.getMyTransactions()
      ]);
      setWallet(walletData);
      setTransactions(txData || []);
    } catch (err) {
      console.error('Failed to load wallet data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWalletData();
  }, []);

  const tier = tierConfigs[wallet?.tier] || tierConfigs.SILVER;
  const lifetimeSpent = wallet?.lifetimeSpent || 0;
  const progressPercent = wallet?.tier === 'ROYAL_PATRON' 
    ? 100 
    : wallet?.tier === 'GOLD' 
      ? Math.min(100, Math.round((lifetimeSpent / 100000) * 100))
      : Math.min(100, Math.round((lifetimeSpent / 25000) * 100));

  const filteredTransactions = transactions.filter((tx) => {
    if (activeTab === 'ALL') return true;
    if (activeTab === 'CREDITS') return tx.amount > 0 || (tx.points && tx.points > 0);
    if (activeTab === 'DEBITS') return tx.amount < 0;
    return true;
  });

  return (
    <div className="min-h-screen bg-[#F7F4EE] py-8 text-[#17211F]">
      <SEO 
        title="Silk Patron Wallet & Loyalty Rewards | SareeKart" 
        description="View your SareeKart store credit balance, earned loyalty points, and Silk Patron Club tier benefits." 
        noindex={true}
      />

      <div className="section-shell max-w-6xl">
        {/* Breadcrumb & Header */}
        <div className="flex flex-col justify-between gap-4 border-b border-[#DDD8CF] pb-6 sm:flex-row sm:items-center">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-[#71817A]">
              <Link to="/" className="hover:text-[#1E6A62]">Home</Link>
              <span>/</span>
              <span className="text-[#1E6A62]">Patron Wallet & Rewards</span>
            </div>
            <h1 className="mt-2 font-serif text-3xl font-medium tracking-tight sm:text-4xl">
              Silk Patron Club & Store Credit
            </h1>
            <p className="mt-1 text-sm text-[#71817A]">
              Manage your store credit balance, post-return refunds, and loyalty points.
            </p>
          </div>

          <button
            onClick={loadWalletData}
            disabled={loading}
            className="inline-flex items-center gap-2 self-start rounded-full border border-[#DDD8CF] bg-white px-4 py-2 text-xs font-bold text-[#1E6A62] shadow-sm hover:bg-[#FAF8F5] transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh Ledger</span>
          </button>
        </div>

        {/* Top Metric Cards */}
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {/* Card 1: Available Store Credit */}
          <div className="relative overflow-hidden rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#71817A]">
                Available Store Credit
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#E7F3EE] text-[#1E6A62]">
                <Wallet className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-4">
              <span className="font-serif text-4xl font-semibold tracking-tight text-[#17211F]">
                {formatCurrency(wallet?.balance)}
              </span>
              <p className="mt-1.5 text-xs text-[#71817A]">
                Usable immediately at checkout on any handloom drape.
              </p>
            </div>
            <div className="mt-5 border-t border-[#F0ECE1] pt-4">
              <Link 
                to="/products"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#1E6A62] hover:text-[#17524C]"
              >
                <span>Redeem in Catalog</span>
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>

          {/* Card 2: Loyalty Rewards Points */}
          <div className="relative overflow-hidden rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#71817A]">
                Accumulated Loyalty Points
              </span>
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#FEF3C7] text-[#B45309]">
                <Coins className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-4">
              <div className="flex items-baseline gap-2">
                <span className="font-serif text-4xl font-semibold tracking-tight text-[#17211F]">
                  {wallet?.loyaltyPoints || 0}
                </span>
                <span className="text-xs font-bold text-[#71817A]">Points (₹{wallet?.loyaltyPoints || 0})</span>
              </div>
              <p className="mt-1.5 text-xs text-[#71817A]">
                Earned: 1 pt per ₹100 spent on delivered orders.
              </p>
            </div>
            <div className="mt-5 border-t border-[#F0ECE1] pt-4 flex items-center justify-between text-xs">
              <span className="font-medium text-[#71817A]">Multiplier Rate:</span>
              <span className="font-bold text-[#B45309]">{tier.multiplier}</span>
            </div>
          </div>

          {/* Card 3: Membership Tier */}
          <div className="relative overflow-hidden rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#71817A]">
                Patron Membership
              </span>
              <span className={`rounded-full border px-3 py-1 text-[11px] font-bold uppercase tracking-wider ${tier.badgeClass}`}>
                {tier.name}
              </span>
            </div>
            <div className="mt-4">
              <div className="flex items-center justify-between text-xs font-bold">
                <span>Lifetime Patronage</span>
                <span>{formatCurrency(wallet?.lifetimeSpent)}</span>
              </div>
              {/* Progress Bar */}
              <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-[#EFECE6]">
                <div 
                  className="h-full bg-[#1E6A62] transition-all duration-500"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
              <p className="mt-2 text-xs text-[#71817A]">
                {wallet?.tier === 'ROYAL_PATRON' ? (
                  <span className="text-[#1E6A62] font-semibold">★ Royal Silk Club Premier Privileges</span>
                ) : (
                  <>Spend <span className="font-bold text-[#17211F]">{formatCurrency(wallet?.nextTierSpendRemaining)}</span> more to achieve <span className="font-bold text-[#1E6A62]">{tier.nextTier}</span></>
                )}
              </p>
            </div>
            <div className="mt-4 border-t border-[#F0ECE1] pt-3 text-[11px] text-[#71817A]">
              Includes 5% Patronage Bonus on all Store Credit returns.
            </div>
          </div>
        </div>

        {/* Patron Benefits Banner */}
        <div className="mt-8 rounded-[8px] border border-[#C9C1B5] bg-gradient-to-r from-[#FAF8F5] via-[#F4EFE6] to-[#FAF8F5] p-6 shadow-sm">
          <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-[#C4A052]" />
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-[#C4A052]">
                  SareeKart Patron Privileges
                </span>
              </div>
              <h3 className="font-serif text-xl font-medium">Why Store Credit is Better Than Card Refunds</h3>
              <p className="max-w-2xl text-xs text-[#52605B] leading-relaxed">
                When exchanging or returning a drape, choosing <strong>Store Credit</strong> gives you an instant 
                <strong> 5% Patronage Bonus</strong>, zero banking delays (2-5 business days bypassed), and 1-click checkout application.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:flex sm:items-center sm:gap-4 shrink-0">
              <div className="flex items-center gap-2 rounded-[6px] border border-[#DDD8CF] bg-white px-3 py-2 text-xs font-semibold">
                <CheckCircle2 className="h-4 w-4 text-[#1E6A62]" />
                <span>Zero Expiry</span>
              </div>
              <div className="flex items-center gap-2 rounded-[6px] border border-[#DDD8CF] bg-white px-3 py-2 text-xs font-semibold">
                <CheckCircle2 className="h-4 w-4 text-[#1E6A62]" />
                <span>1-Click Checkout</span>
              </div>
            </div>
          </div>
        </div>

        {/* Transaction History Ledger */}
        <div className="mt-10 rounded-[8px] border border-[#DDD8CF] bg-white p-6 shadow-sm">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div>
              <h2 className="font-serif text-2xl font-medium">Wallet Audit Ledger</h2>
              <p className="text-xs text-[#71817A]">
                Comprehensive record of return disbursements, order loyalty accruals, and checkout debits.
              </p>
            </div>

            {/* Filter Tabs */}
            <div className="inline-flex rounded-full border border-[#DDD8CF] bg-[#FAF8F5] p-1 text-xs font-bold">
              {['ALL', 'CREDITS', 'DEBITS'].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`rounded-full px-4 py-1.5 transition ${
                    activeTab === tab 
                      ? 'bg-[#1E6A62] text-white shadow-sm' 
                      : 'text-[#71817A] hover:text-[#17211F]'
                  }`}
                >
                  {tab === 'ALL' ? 'All Activity' : tab === 'CREDITS' ? 'Credits (+)' : 'Debits (-)'}
                </button>
              ))}
            </div>
          </div>

          {/* Ledger Table */}
          <div className="mt-6 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-[#E8E2D9] text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                  <th className="pb-3 pr-4">Date & Time</th>
                  <th className="pb-3 pr-4">Transaction Type</th>
                  <th className="pb-3 pr-4">Description</th>
                  <th className="pb-3 pr-4">Reference</th>
                  <th className="pb-3 pr-4 text-right">Credit / Debit</th>
                  <th className="pb-3 text-right">Balance After</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F0ECE1]">
                {filteredTransactions.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="py-8 text-center text-sm font-medium text-[#71817A]">
                      No transactions found for this filter.
                    </td>
                  </tr>
                ) : (
                  filteredTransactions.map((tx) => {
                    const isCredit = tx.amount > 0 || (tx.points && tx.points > 0);
                    return (
                      <tr key={tx.id} className="hover:bg-[#FAF8F5] transition">
                        <td className="py-3.5 pr-4 text-[#71817A] whitespace-nowrap">
                          {formatDate(tx.createdAt)}
                        </td>
                        <td className="py-3.5 pr-4 whitespace-nowrap">
                          <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                            tx.type?.includes('CREDIT')
                              ? 'bg-[#E7F3EE] text-[#17644F]'
                              : 'bg-[#FBE9E7] text-[#B84F49]'
                          }`}>
                            {isCredit ? (
                              <ArrowDownLeft className="h-3 w-3" />
                            ) : (
                              <ArrowUpRight className="h-3 w-3" />
                            )}
                            {tx.type?.replace(/_/g, ' ')}
                          </span>
                        </td>
                        <td className="py-3.5 pr-4 max-w-xs truncate font-medium text-[#17211F]" title={tx.description}>
                          {tx.description}
                        </td>
                        <td className="py-3.5 pr-4 text-[#71817A] whitespace-nowrap">
                          {tx.referenceType ? `${tx.referenceType} #${tx.referenceId}` : '—'}
                        </td>
                        <td className={`py-3.5 pr-4 text-right font-bold whitespace-nowrap ${
                          isCredit ? 'text-[#17644F]' : 'text-[#B84F49]'
                        }`}>
                          {tx.amount !== 0 ? (
                            isCredit ? `+${formatCurrency(tx.amount)}` : formatCurrency(tx.amount)
                          ) : (
                            `+${tx.points} pts`
                          )}
                        </td>
                        <td className="py-3.5 text-right font-semibold text-[#17211F] whitespace-nowrap">
                          {formatCurrency(tx.balanceAfter)}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
