import { useEffect, useMemo, useState } from 'react';
import { 
  Wallet, 
  Coins, 
  Award, 
  Search, 
  Plus, 
  History, 
  X, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Sparkles,
  Users
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

const tierStyles = {
  SILVER: 'bg-[#E7ECE9] text-[#4E5B56] border-[#CBD5D1]',
  GOLD: 'bg-[#FEF3C7] text-[#92400E] border-[#FDE68A]',
  ROYAL_PATRON: 'bg-[#EDE9FE] text-[#6D28D9] border-[#DDD6FE]'
};

export default function ManageWallets() {
  const [wallets, setWallets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTier, setSelectedTier] = useState('ALL');

  // Ledger Drawer State
  const [activeLedgerUser, setActiveLedgerUser] = useState(null);
  const [ledgerTransactions, setLedgerTransactions] = useState([]);
  const [ledgerLoading, setLedgerLoading] = useState(false);

  // Issue Credit Modal State
  const [creditModalUser, setCreditModalUser] = useState(null);
  const [creditAmount, setCreditAmount] = useState('');
  const [creditPoints, setCreditPoints] = useState('');
  const [creditReason, setCreditReason] = useState('');
  const [submittingCredit, setSubmittingCredit] = useState(false);
  const [creditError, setCreditError] = useState(null);
  const [creditSuccess, setCreditSuccess] = useState(false);

  const fetchWallets = async () => {
    setLoading(true);
    try {
      const data = await walletService.getAllWalletsForAdmin();
      setWallets(data || []);
    } catch (err) {
      console.error('Failed to load wallets:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallets();
  }, []);

  const openLedger = async (userWallet) => {
    setActiveLedgerUser(userWallet);
    setLedgerLoading(true);
    try {
      const txs = await walletService.getUserTransactionsForAdmin(userWallet.userId);
      setLedgerTransactions(txs || []);
    } catch (err) {
      console.error('Failed to load transactions:', err);
      setLedgerTransactions([]);
    } finally {
      setLedgerLoading(false);
    }
  };

  const handleIssueCredit = async (e) => {
    e.preventDefault();
    if (!creditModalUser) return;
    if (!creditReason.trim()) {
      setCreditError('Please specify a reason or audit note.');
      return;
    }

    setSubmittingCredit(true);
    setCreditError(null);
    try {
      await walletService.issuePromotionalCredit(creditModalUser.userId, {
        amount: creditAmount ? parseFloat(creditAmount) : 0,
        points: creditPoints ? parseInt(creditPoints, 10) : 0,
        reason: creditReason.trim()
      });
      setCreditSuccess(true);
      setTimeout(() => {
        setCreditSuccess(false);
        setCreditModalUser(null);
        setCreditAmount('');
        setCreditPoints('');
        setCreditReason('');
        fetchWallets();
      }, 1200);
    } catch (err) {
      setCreditError(err.response?.data?.message || err.message || 'Failed to issue promotional credit');
    } finally {
      setSubmittingCredit(false);
    }
  };

  // Metrics
  const totalStoreCredit = useMemo(() => 
    wallets.reduce((sum, w) => sum + (w.balance || 0), 0), [wallets]
  );
  const totalLoyaltyPoints = useMemo(() => 
    wallets.reduce((sum, w) => sum + (w.loyaltyPoints || 0), 0), [wallets]
  );
  const vipCount = useMemo(() => 
    wallets.filter((w) => w.tier === 'GOLD' || w.tier === 'ROYAL_PATRON').length, [wallets]
  );

  const filteredWallets = wallets.filter((w) => {
    const matchesSearch = 
      (w.userName && w.userName.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (w.userEmail && w.userEmail.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesTier = selectedTier === 'ALL' || w.tier === selectedTier;
    return matchesSearch && matchesTier;
  });

  return (
    <div className="space-y-6">
      <SEO title="Manage Wallets & Loyalty | SareeKart Admin" description="Staff console for patron wallets." noindex={true} />

      {/* Header */}
      <div className="flex flex-col justify-between gap-4 border-b border-[#DDE4EA] pb-5 sm:flex-row sm:items-center">
        <div>
          <span className="text-[11px] font-black uppercase tracking-[0.18em] text-[#0F766E]">Commerce Governance</span>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827] sm:text-3xl">Customer Wallets & Rewards</h1>
          <p className="mt-1 text-xs font-medium text-[#64748B]">
            Audit customer balances, return refunds, loyalty tiers, and issue promotional goodwill credits.
          </p>
        </div>

        <button
          onClick={fetchWallets}
          disabled={loading}
          className="inline-flex items-center gap-2 self-start rounded-[8px] border border-[#DDE4EA] bg-white px-4 py-2 text-xs font-bold text-[#111827] shadow-sm hover:bg-[#F8FAFC] transition"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Accounts</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-soft">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#64748B]">Active Store Credit</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#E7F5F3] text-[#0F766E]">
              <Wallet className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 text-2xl font-bold text-[#111827]">{formatCurrency(totalStoreCredit)}</div>
          <p className="mt-1 text-[11px] font-medium text-[#64748B]">Total outstanding liability across accounts</p>
        </div>

        <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-soft">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#64748B]">Total Loyalty Points</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#FEF3C7] text-[#B45309]">
              <Coins className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 text-2xl font-bold text-[#111827]">{totalLoyaltyPoints.toLocaleString('en-IN')}</div>
          <p className="mt-1 text-[11px] font-medium text-[#64748B]">Accrued via post-delivery order fulfillment</p>
        </div>

        <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-soft">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#64748B]">VIP Connoisseurs</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#EDE9FE] text-[#6D28D9]">
              <Award className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 text-2xl font-bold text-[#111827]">{vipCount} Customers</div>
          <p className="mt-1 text-[11px] font-medium text-[#64748B]">Gold and Royal Silk Club members</p>
        </div>

        <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-soft">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#64748B]">Total Registered Wallets</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-50 text-blue-700">
              <Users className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 text-2xl font-bold text-[#111827]">{wallets.length} Accounts</div>
          <p className="mt-1 text-[11px] font-medium text-[#64748B]">Enabled with auto-sync and zero expiry</p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-3 rounded-[8px] border border-[#DDE4EA] bg-white p-4 shadow-soft sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#94A3B8]" />
          <input
            type="text"
            placeholder="Search by customer name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-[8px] border border-[#DDE4EA] bg-[#F8FAFC] py-2 pl-9 pr-4 text-xs font-semibold text-[#111827] outline-none focus:border-[#0F766E]"
          />
        </div>

        <div className="flex items-center gap-2">
          {['ALL', 'SILVER', 'GOLD', 'ROYAL_PATRON'].map((tier) => (
            <button
              key={tier}
              onClick={() => setSelectedTier(tier)}
              className={`rounded-[8px] px-3 py-1.5 text-xs font-bold transition ${
                selectedTier === tier
                  ? 'bg-[#111827] text-white'
                  : 'border border-[#DDE4EA] bg-white text-[#64748B] hover:text-[#111827]'
              }`}
            >
              {tier === 'ALL' ? 'All Tiers' : tier.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Wallets Table */}
      <div className="overflow-hidden rounded-[8px] border border-[#DDE4EA] bg-white shadow-soft">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-[#DDE4EA] bg-[#F8FAFC] text-[10px] font-black uppercase tracking-wider text-[#64748B]">
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Patron Tier</th>
                <th className="py-3 px-4 text-right">Store Credit</th>
                <th className="py-3 px-4 text-right">Loyalty Points</th>
                <th className="py-3 px-4 text-right">Lifetime Spend</th>
                <th className="py-3 px-4 text-center">Transactions</th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA]">
              {loading ? (
                <tr>
                  <td colSpan="7" className="py-10 text-center text-xs font-medium text-[#64748B]">
                    Loading patron wallets...
                  </td>
                </tr>
              ) : filteredWallets.length === 0 ? (
                <tr>
                  <td colSpan="7" className="py-10 text-center text-xs font-medium text-[#64748B]">
                    No customer wallets match your filter criteria.
                  </td>
                </tr>
              ) : (
                filteredWallets.map((w) => (
                  <tr key={w.id} className="hover:bg-[#F8FAFC] transition">
                    <td className="py-3 px-4">
                      <div className="font-bold text-[#111827]">{w.userName || 'Unnamed Customer'}</div>
                      <div className="text-[11px] text-[#64748B]">{w.userEmail}</div>
                    </td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                        tierStyles[w.tier] || tierStyles.SILVER
                      }`}>
                        {w.tierDisplayName || w.tier}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-[#0F766E]">
                      {formatCurrency(w.balance)}
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-[#B45309]">
                      {w.loyaltyPoints} pts
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-[#111827]">
                      {formatCurrency(w.lifetimeSpent)}
                    </td>
                    <td className="py-3 px-4 text-center font-medium text-[#64748B]">
                      {w.totalTransactions}
                    </td>
                    <td className="py-3 px-4 text-center whitespace-nowrap">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => openLedger(w)}
                          className="inline-flex items-center gap-1 rounded-[6px] border border-[#DDE4EA] bg-white px-2.5 py-1 text-[11px] font-bold text-[#111827] hover:bg-[#F8FAFC] transition"
                        >
                          <History className="h-3 w-3 text-[#64748B]" />
                          <span>Ledger</span>
                        </button>
                        <button
                          onClick={() => {
                            setCreditModalUser(w);
                            setCreditError(null);
                            setCreditSuccess(false);
                          }}
                          className="inline-flex items-center gap-1 rounded-[6px] bg-[#0F766E] px-2.5 py-1 text-[11px] font-bold text-white hover:bg-[#0D625C] transition"
                        >
                          <Plus className="h-3 w-3" />
                          <span>Add Credit</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: Issue Promotional Credit */}
      {creditModalUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-[12px] border border-[#DDE4EA] bg-white p-6 shadow-xl">
            <div className="flex items-center justify-between border-b border-[#DDE4EA] pb-3">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-[#0F766E]" />
                <h3 className="text-sm font-bold text-[#111827]">Issue Bonus / Store Credit</h3>
              </div>
              <button
                onClick={() => setCreditModalUser(null)}
                className="rounded-full p-1 text-[#64748B] hover:bg-[#F1F5F9]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 rounded-[8px] bg-[#F8FAFC] p-3 text-xs">
              <div className="font-bold text-[#111827]">{creditModalUser.userName}</div>
              <div className="text-[#64748B]">{creditModalUser.userEmail}</div>
              <div className="mt-1 flex items-center gap-2 text-[11px] font-medium text-[#0F766E]">
                <span>Current Balance: {formatCurrency(creditModalUser.balance)}</span>
                <span>•</span>
                <span>Points: {creditModalUser.loyaltyPoints}</span>
              </div>
            </div>

            {creditSuccess ? (
              <div className="mt-6 flex flex-col items-center justify-center gap-2 py-4 text-center">
                <CheckCircle2 className="h-8 w-8 text-[#0F766E]" />
                <span className="text-sm font-bold text-[#111827]">Credit Bonus Disbursed!</span>
                <p className="text-xs text-[#64748B]">Transaction has been recorded into the audit ledger.</p>
              </div>
            ) : (
              <form onSubmit={handleIssueCredit} className="mt-4 space-y-4">
                {creditError && (
                  <div className="flex items-center gap-2 rounded-[6px] border border-red-200 bg-red-50 p-2.5 text-xs font-semibold text-red-700">
                    <AlertCircle className="h-4 w-4 shrink-0" />
                    <span>{creditError}</span>
                  </div>
                )}

                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-[#64748B]">
                    Credit Amount (₹)
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="e.g. 500"
                    value={creditAmount}
                    onChange={(e) => setCreditAmount(e.target.value)}
                    className="mt-1 w-full rounded-[8px] border border-[#DDE4EA] bg-[#F8FAFC] px-3 py-2 text-xs font-bold outline-none focus:border-[#0F766E]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-[#64748B]">
                    Bonus Loyalty Points (Optional)
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    placeholder="e.g. 50"
                    value={creditPoints}
                    onChange={(e) => setCreditPoints(e.target.value)}
                    className="mt-1 w-full rounded-[8px] border border-[#DDE4EA] bg-[#F8FAFC] px-3 py-2 text-xs font-bold outline-none focus:border-[#0F766E]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-[#64748B]">
                    Reason / Administrative Note *
                  </label>
                  <textarea
                    rows="2"
                    placeholder="e.g. Goodwill credit for transit delay or festival loyalty perk..."
                    value={creditReason}
                    onChange={(e) => setCreditReason(e.target.value)}
                    required
                    className="mt-1 w-full rounded-[8px] border border-[#DDE4EA] bg-[#F8FAFC] p-2.5 text-xs outline-none focus:border-[#0F766E]"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setCreditModalUser(null)}
                    className="rounded-[8px] border border-[#DDE4EA] px-4 py-2 text-xs font-bold text-[#64748B] hover:bg-[#F8FAFC]"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submittingCredit}
                    className="rounded-[8px] bg-[#0F766E] px-4 py-2 text-xs font-bold text-white hover:bg-[#0D625C] transition disabled:opacity-60"
                  >
                    {submittingCredit ? 'Disbursing...' : 'Disburse Credit'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Slideover Drawer: User Audit Ledger */}
      {activeLedgerUser && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-xs">
          <div className="w-full max-w-xl bg-white p-6 shadow-2xl flex flex-col h-full overflow-hidden">
            <div className="flex items-center justify-between border-b border-[#DDE4EA] pb-4">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-[#0F766E]">Audit Trail</span>
                <h3 className="text-lg font-bold text-[#111827]">{activeLedgerUser.userName}'s Ledger</h3>
                <p className="text-xs text-[#64748B]">{activeLedgerUser.userEmail}</p>
              </div>
              <button
                onClick={() => setActiveLedgerUser(null)}
                className="rounded-full p-2 text-[#64748B] hover:bg-[#F1F5F9]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Quick Balance Header in Drawer */}
            <div className="mt-4 grid grid-cols-2 gap-3 rounded-[8px] bg-[#F8FAFC] p-3 text-xs border border-[#DDE4EA]">
              <div>
                <span className="text-[#64748B]">Balance</span>
                <div className="font-bold text-sm text-[#0F766E]">{formatCurrency(activeLedgerUser.balance)}</div>
              </div>
              <div>
                <span className="text-[#64748B]">Loyalty Points</span>
                <div className="font-bold text-sm text-[#B45309]">{activeLedgerUser.loyaltyPoints} pts</div>
              </div>
            </div>

            {/* Transaction List */}
            <div className="mt-4 flex-1 overflow-y-auto space-y-3 pr-1">
              {ledgerLoading ? (
                <div className="py-12 text-center text-xs text-[#64748B]">Loading audit transactions...</div>
              ) : ledgerTransactions.length === 0 ? (
                <div className="py-12 text-center text-xs text-[#64748B]">No transactions recorded for this wallet.</div>
              ) : (
                ledgerTransactions.map((tx) => {
                  const isCredit = tx.amount > 0 || (tx.points && tx.points > 0);
                  return (
                    <div key={tx.id} className="rounded-[8px] border border-[#DDE4EA] p-3 hover:bg-[#F8FAFC] transition text-xs">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider ${
                            isCredit ? 'bg-[#E7F5F3] text-[#0F766E]' : 'bg-[#FBE9E7] text-[#B84F49]'
                          }`}>
                            {isCredit ? <ArrowDownLeft className="h-2.5 w-2.5" /> : <ArrowUpRight className="h-2.5 w-2.5" />}
                            {tx.type}
                          </span>
                          <div className="mt-1 font-semibold text-[#111827]">{tx.description}</div>
                        </div>
                        <div className={`text-right font-bold whitespace-nowrap ${isCredit ? 'text-[#0F766E]' : 'text-[#B84F49]'}`}>
                          {tx.amount !== 0 ? (
                            isCredit ? `+${formatCurrency(tx.amount)}` : formatCurrency(tx.amount)
                          ) : (
                            `+${tx.points} pts`
                          )}
                        </div>
                      </div>
                      <div className="mt-2 flex items-center justify-between border-t border-[#F1F5F9] pt-1.5 text-[10px] text-[#64748B]">
                        <span>{formatDate(tx.createdAt)}</span>
                        <span>Balance after: {formatCurrency(tx.balanceAfter)}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
