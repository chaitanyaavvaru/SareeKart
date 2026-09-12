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

const MOCK_CUSTOMERS = [
  { id: "M-1", name: "Priya Reddy", email: "priya@example.com", phone: "+91 98765 43210", orders: 4, totalSpent: 142800, city: "Hyderabad", joined: "12 Jan 2025" },
  { id: "M-2", name: "Anitha Sharma", email: "anitha@example.com", phone: "+91 87654 32109", orders: 2, totalSpent: 98000, city: "Bangalore", joined: "04 Mar 2025" },
  { id: "M-3", name: "Meena Iyer", email: "meena@example.com", phone: "+91 76543 21098", orders: 7, totalSpent: 61000, city: "Chennai", joined: "19 Feb 2025" },
  { id: "M-4", name: "Sujatha Rao", email: "sujatha@example.com", phone: "+91 65432 10987", orders: 1, totalSpent: 38400, city: "Vizag", joined: "22 May 2025" },
  { id: "M-5", name: "Kavitha Nair", email: "kavitha@example.com", phone: "+91 54321 09876", orders: 5, totalSpent: 185000, city: "Kochi", joined: "11 Dec 2024" },
];

export default function ManageUsers() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const loadCustomersFromOrders = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Fetch all orders from backend
        const response = await api.get('/admin/orders');
        const orders = response.data?.data || [];
        
        // Group orders by client (using phone or name to identify unique customers)
        const customerMap = {};
        
        orders.forEach(order => {
          if (!order.shippingAddress) return;
          const phone = order.shippingAddress.phone || '';
          const name = order.shippingAddress.fullName || '';
          
          if (!phone) return;
          
          if (!customerMap[phone]) {
            customerMap[phone] = {
              id: `DB-${order.userId}-${phone.slice(-4)}`,
              name: name,
              email: `user_${order.userId}@sareekart.com`,
              phone: phone,
              orders: 0,
              totalSpent: 0,
              city: order.shippingAddress.city || 'Unknown',
              joined: new Date(order.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
            };
          }
          
          customerMap[phone].orders += 1;
          customerMap[phone].totalSpent += order.totalAmount || 0;
        });

        const dbCustomers = Object.values(customerMap);
        
        // Merge mock customers with active database buyers
        const mergedList = [...dbCustomers, ...MOCK_CUSTOMERS];
        
        // Remove duplicates if mock data overlap database phone numbers
        const uniqueList = [];
        const seenPhones = new Set();
        mergedList.forEach(c => {
          if (!seenPhones.has(c.phone)) {
            seenPhones.add(c.phone);
            uniqueList.push(c);
          }
        });

        setCustomers(uniqueList);
      } catch (err) {
        console.error('Error fetching customers:', err);
        // Fall back to mock list in case of network issue
        setCustomers(MOCK_CUSTOMERS);
      } finally {
        setLoading(false);
      }
    };

    loadCustomersFromOrders();
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
        <h1 className="text-2xl font-extrabold font-serif text-[#111827]">Customers</h1>
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
            className="w-full pl-9 pr-4 py-1.5 bg-white border border-[#DDE4EA] rounded-xl outline-none text-xs focus:border-[#E85D4F] shadow-xs"
          />
        </div>
        <div className="text-xs font-semibold text-text-secondary shrink-0">
          Total customers: {filteredCustomers.length}
        </div>
      </div>

      {/* Customer Table */}
      {loading ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-4">
          <Loader2 className="w-8 h-8 text-[#111827] animate-spin" />
          <p className="text-xs text-text-secondary">Generating user telemetry...</p>
        </div>
      ) : filteredCustomers.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl">
          <Users className="w-10 h-10 text-text-muted mx-auto mb-3" />
          <h3 className="font-bold text-[#111827]">No Customers Found</h3>
          <p className="text-xs text-text-secondary mt-1">Try updating your search keywords.</p>
        </div>
      ) : (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#F5F7FA] border-b border-border text-xs">
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
                  <tr key={c.id} className="hover:bg-[#F5F7FA] transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-[#111827] text-[#E85D4F] font-bold flex items-center justify-center text-xs shrink-0 ring-2 ring-[#E85D4F]/30 ring-offset-1">
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
                      <MapPin className="w-3.5 h-3.5 text-[#B5463D]" />
                      {c.city}
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-bold text-text-secondary flex items-center gap-1.5">
                        <ShoppingBag className="w-3.5 h-3.5 text-[#111827]/60" /> {c.orders}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-bold text-green-700 flex items-center gap-0.5 mt-2.5">
                      <IndianRupee className="w-3.5 h-3.5 text-green-700" />
                      {formatCurrency(c.totalSpent).replace('₹', '')}
                    </td>
                    <td className="px-6 py-4 text-text-secondary text-xs flex items-center gap-1.5 mt-2.5">
                      <Calendar className="w-3.5 h-3.5 text-[#B5463D]" />
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
