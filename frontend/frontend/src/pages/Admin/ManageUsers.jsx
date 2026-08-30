import { useEffect, useState } from 'react';
import { 
  Users, 
  Search, 
  Loader2, 
  MapPin, 
  Calendar,
  ShoppingBag,
  IndianRupee
} from 'lucide-react';
import api from '../../api/axiosConfig';

export default function ManageUsers() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const loadCustomers = async () => {
      try {
        setLoading(true);
        setError(null);

        // Registered customers with lifetime aggregates from /admin/users
        const response = await api.get('/admin/users');
        if (!response.data?.success) {
          throw new Error(response.data?.message || 'Failed to load customers');
        }

        const dbCustomers = (response.data.data || []).map((u) => ({
          id: u.id,
          name: u.fullName || u.email,
          email: u.email,
          phone: u.phone || '—',
          orders: u.ordersCount ?? 0,
          totalSpent: u.totalSpent ?? 0,
          city: '—',
          joined: u.joinedAt
            ? new Date(u.joinedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
            : '—',
        }));

        setCustomers(dbCustomers);
      } catch (err) {
        console.error('Error fetching customers:', err);
        setError(err.response?.data?.message || 'Could not load customers. Please retry.');
        setCustomers([]);
      } finally {
        setLoading(false);
      }
    };

    loadCustomers();
  }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  const filteredCustomers = customers.filter(c => 
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.phone.includes(searchQuery)
  );

  return (
    <div className="space-y-6 animate-fade-in text-sm text-text-primary">
      
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold font-serif text-maroon">Customers</h1>
        <p className="text-xs text-text-secondary mt-0.5">Track customer list, purchase frequencies, and customer lifetime values</p>
      </div>

      {/* Search Filter */}
      <div className="bg-white border border-border rounded-xl p-4 flex gap-4 items-center justify-between shadow-xs">
        <div className="relative w-full md:max-w-md">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted">
            <Search className="w-4 h-4" />
          </span>
          <input 
            type="text" 
            placeholder="Search customers by name, email, city, or phone..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-1.5 bg-white border border-[#F4F4F4] rounded-xl outline-none text-xs focus:border-[#C9A227] shadow-xs"
          />
        </div>
        <div className="text-xs font-semibold text-text-secondary shrink-0">
          Total customers: {filteredCustomers.length}
        </div>
      </div>

      {/* Customer Table */}
      {loading ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-4">
          <Loader2 className="w-8 h-8 text-maroon animate-spin" />
          <p className="text-xs text-text-secondary">Generating user telemetry...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-xl font-medium">
          {error}
        </div>
      ) : filteredCustomers.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl">
          <Users className="w-10 h-10 text-text-muted mx-auto mb-3" />
          <h3 className="font-bold text-maroon">No Customers Found</h3>
          <p className="text-xs text-text-secondary mt-1">Try updating your search keywords.</p>
        </div>
      ) : (
        <div className="bg-white border border-[#F4F4F4] rounded-2xl shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#FAF8F5] border-b border-border text-xs">
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Customer</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Contact Info</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">City</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Bookings count</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Lifetime spent</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Joined Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-xs">
                {filteredCustomers.map((c) => (
                  <tr key={c.id} className="hover:bg-[#FAF8F5] transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-maroon text-gold font-bold flex items-center justify-center text-xs shrink-0 ring-2 ring-[#C9A227]/30 ring-offset-1">
                          {c.name ? c.name[0].toUpperCase() : 'C'}
                        </div>
                        <div className="min-w-0">
                          <p className="font-bold text-text-primary truncate">{c.name}</p>
                          <p className="text-[9px] text-text-muted mt-0.5 font-mono">UID: {c.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="font-semibold text-text-secondary">{c.email}</p>
                      <p className="text-[10px] text-text-muted mt-0.5">{c.phone}</p>
                    </td>
                    <td className="px-6 py-4 font-semibold text-text-secondary flex items-center gap-1 mt-2.5">
                      <MapPin className="w-3.5 h-3.5 text-gold-dark" />
                      {c.city}
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-bold text-text-secondary flex items-center gap-1.5">
                        <ShoppingBag className="w-3.5 h-3.5 text-maroon/60" /> {c.orders}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-bold text-green-700 flex items-center gap-0.5 mt-2.5">
                      <IndianRupee className="w-3.5 h-3.5 text-green-700" />
                      {formatCurrency(c.totalSpent).replace('₹', '')}
                    </td>
                    <td className="px-6 py-4 text-text-secondary text-xs flex items-center gap-1.5 mt-2.5">
                      <Calendar className="w-3.5 h-3.5 text-gold-dark" />
                      {c.joined}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}
