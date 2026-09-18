import { useEffect, useMemo, useState } from 'react';
import {
  Truck,
  MapPin,
  Search,
  Plus,
  Trash2,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Clock,
  ShieldCheck,
  Building2,
  Navigation,
  RefreshCw,
  X,
  Compass
} from 'lucide-react';
import logisticsService from '../../services/logisticsService';
import SEO from '../../components/common/SEO';

export default function ManageLogistics() {
  const [overrides, setOverrides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState(null);

  // Live Diagnostic Lookup
  const [testPincode, setTestPincode] = useState('560001');
  const [testResult, setTestResult] = useState(null);
  const [testing, setTesting] = useState(false);
  const [testError, setTestError] = useState('');

  // Add / Edit Override Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formPincode, setFormPincode] = useState('');
  const [formCity, setFormCity] = useState('');
  const [formState, setFormState] = useState('');
  const [formZone, setFormZone] = useState('TIER_1');
  const [formServiceable, setFormServiceable] = useState(true);
  const [formCodAvailable, setFormCodAvailable] = useState(true);
  const [formCourier, setFormCourier] = useState('Blue Dart Apex Express');
  const [formTransitDays, setFormTransitDays] = useState(2);
  const [formNotes, setFormNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [modalError, setModalError] = useState('');

  const fetchOverrides = async () => {
    try {
      setLoading(true);
      const res = await logisticsService.getAdminOverrides();
      if (res && res.data) {
        setOverrides(res.data);
      }
    } catch (err) {
      console.error('Failed to load overrides:', err);
    } finally {
      setLoading(false);
    }
  };

  const runDiagnostic = async (pin) => {
    const clean = String(pin).replace(/\D/g, '');
    if (clean.length !== 6) {
      setTestError('PIN code must be exactly 6 digits');
      setTestResult(null);
      return;
    }
    setTesting(true);
    setTestError('');
    try {
      const res = await logisticsService.checkPincode(clean);
      if (res && res.data) {
        setTestResult(res.data);
      }
    } catch (err) {
      setTestError(err.message || 'Error looking up PIN code');
      setTestResult(null);
    } finally {
      setTesting(false);
    }
  };

  useEffect(() => {
    fetchOverrides();
    runDiagnostic('560001');
  }, []);

  const handleDeleteOverride = async (pincode) => {
    if (!window.confirm(`Are you sure you want to delete the custom override for PIN ${pincode}?`)) {
      return;
    }
    try {
      await logisticsService.deleteAdminOverride(pincode);
      setFeedbackMessage({ type: 'success', text: `Override for ${pincode} removed successfully.` });
      fetchOverrides();
      if (testPincode === pincode) runDiagnostic(pincode);
    } catch (err) {
      setFeedbackMessage({ type: 'error', text: err.message || 'Failed to delete override.' });
    }
  };

  const handleSaveOverride = async (e) => {
    e.preventDefault();
    setModalError('');
    if (!/^\d{6}$/.test(formPincode)) {
      setModalError('PIN code must be exactly 6 digits.');
      return;
    }
    if (!formCity.trim() || !formState.trim()) {
      setModalError('City and State are required.');
      return;
    }

    setSubmitting(true);
    try {
      await logisticsService.saveAdminOverride({
        pincode: formPincode.trim(),
        city: formCity.trim(),
        state: formState.trim(),
        zone: formZone,
        serviceable: formServiceable,
        codAvailable: formCodAvailable,
        courierPartner: formCourier.trim(),
        transitDays: parseInt(formTransitDays, 10),
        notes: formNotes.trim()
      });

      setFeedbackMessage({ type: 'success', text: `Logistics override for PIN ${formPincode} saved!` });
      setIsModalOpen(false);
      resetForm();
      fetchOverrides();
      if (testPincode === formPincode) runDiagnostic(formPincode);
    } catch (err) {
      setModalError(err.message || 'Failed to save override.');
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormPincode('');
    setFormCity('');
    setFormState('');
    setFormZone('TIER_1');
    setFormServiceable(true);
    setFormCodAvailable(true);
    setFormCourier('Blue Dart Apex Express');
    setFormTransitDays(2);
    setFormNotes('');
    setModalError('');
  };

  const filteredOverrides = useMemo(() => {
    return overrides.filter((o) => {
      const q = searchQuery.toLowerCase();
      return (
        o.pincode.toLowerCase().includes(q) ||
        o.city.toLowerCase().includes(q) ||
        o.state.toLowerCase().includes(q) ||
        o.courierPartner.toLowerCase().includes(q)
      );
    });
  }, [overrides, searchQuery]);

  return (
    <div className="min-h-screen bg-[#FBF9F4] py-8 text-[#17211F]">
      <SEO title="Logistics & Delivery Matrix | SareeKart Artisan Staff" />
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 space-y-8">
        
        {/* Header */}
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between border-b border-[#DDD8CF] pb-6">
          <div>
            <div className="flex items-center gap-2">
              <span className="rounded bg-[#E7F5F3] p-1.5 text-[#1E6A62]">
                <Truck className="h-6 w-6" />
              </span>
              <h1 className="text-2xl sm:text-3xl font-serif font-black tracking-tight text-[#17211F]">
                Logistics & Courier Serviceability Matrix
              </h1>
            </div>
            <p className="mt-1 text-sm font-medium text-[#71817A]">
              Live Indian Postal Circle resolution engine, transit SLAs, COD gating, and corridor overrides.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => { fetchOverrides(); runDiagnostic(testPincode); }}
              className="inline-flex items-center gap-1.5 rounded-[8px] border border-[#DDD8CF] bg-white px-3.5 py-2 text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] shadow-2xs transition-all"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Refresh
            </button>
            <button
              onClick={() => { resetForm(); setIsModalOpen(true); }}
              className="inline-flex items-center gap-1.5 rounded-[8px] bg-[#1E6A62] px-4 py-2 text-xs font-bold text-white hover:bg-[#154e48] shadow-sm transition-all"
            >
              <Plus className="h-4 w-4" />
              Add Pincode Override
            </button>
          </div>
        </div>

        {feedbackMessage && (
          <div className={`p-4 rounded-[8px] text-xs font-bold flex items-center justify-between ${
            feedbackMessage.type === 'success' ? 'bg-[#E7F5F3] text-[#0F766E] border border-[#BFE7E2]' : 'bg-red-50 text-red-700 border border-red-200'
          }`}>
            <span>{feedbackMessage.text}</span>
            <button onClick={() => setFeedbackMessage(null)} className="opacity-70 hover:opacity-100">
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {/* Top Metric Cards */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-5 shadow-2xs">
            <div className="flex items-center justify-between text-[#71817A]">
              <span className="text-xs font-bold uppercase tracking-wider">Active Overrides</span>
              <Navigation className="h-4 w-4 text-[#1E6A62]" />
            </div>
            <p className="mt-2 text-2xl font-black text-[#17211F]">{overrides.length}</p>
            <p className="mt-1 text-[11px] text-[#71817A]">Custom corridor routing rules</p>
          </div>

          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-5 shadow-2xs">
            <div className="flex items-center justify-between text-[#71817A]">
              <span className="text-xs font-bold uppercase tracking-wider">South Hub SLA</span>
              <Clock className="h-4 w-4 text-[#0F766E]" />
            </div>
            <p className="mt-2 text-2xl font-black text-[#0F766E]">24 – 48 Hours</p>
            <p className="mt-1 text-[11px] text-[#71817A]">WH-01 Bengaluru / WH-03 Kanchipuram</p>
          </div>

          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-5 shadow-2xs">
            <div className="flex items-center justify-between text-[#71817A]">
              <span className="text-xs font-bold uppercase tracking-wider">North & West Hub SLA</span>
              <Building2 className="h-4 w-4 text-[#B45309]" />
            </div>
            <p className="mt-2 text-2xl font-black text-[#B45309]">2 – 3 Days</p>
            <p className="mt-1 text-[11px] text-[#71817A]">WH-02 Varanasi Guild / Delhi Gateway</p>
          </div>

          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-5 shadow-2xs">
            <div className="flex items-center justify-between text-[#71817A]">
              <span className="text-xs font-bold uppercase tracking-wider">Coverage Circles</span>
              <Compass className="h-4 w-4 text-[#4338CA]" />
            </div>
            <p className="mt-2 text-2xl font-black text-[#4338CA]">28 States & UTs</p>
            <p className="mt-1 text-[11px] text-[#71817A]">Full Indian Postal Circle Matrix</p>
          </div>
        </div>

        {/* Diagnostic Simulator & Zone Taxonomy */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Simulator Box */}
          <div className="lg:col-span-2 rounded-[10px] border border-[#DDD8CF] bg-white p-6 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-[#F0ECE1] pb-3">
              <div className="flex items-center gap-2">
                <MapPin className="h-5 w-5 text-[#1E6A62]" />
                <h2 className="text-base font-black text-[#17211F]">Live Pincode Simulator & Corridor Inspector</h2>
              </div>
              <span className="text-[11px] font-bold text-[#71817A]">Real-Time Resolution</span>
            </div>

            <form 
              onSubmit={(e) => { e.preventDefault(); runDiagnostic(testPincode); }}
              className="flex gap-2"
            >
              <input
                type="text"
                maxLength={6}
                value={testPincode}
                onChange={(e) => setTestPincode(e.target.value.replace(/\D/g, ''))}
                placeholder="Enter 6-digit PIN (e.g. 560001, 110001, 194101)"
                className="h-11 flex-1 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-4 font-mono text-sm font-bold outline-none focus:border-[#1E6A62]"
              />
              <button
                type="submit"
                disabled={testing}
                className="h-11 rounded-[8px] bg-[#1E6A62] px-6 text-xs font-bold text-white hover:bg-[#154e48] transition-colors disabled:opacity-50"
              >
                {testing ? 'Analyzing...' : 'Test PIN'}
              </button>
            </form>

            {testError && (
              <p className="text-xs font-bold text-red-600 flex items-center gap-1.5">
                <AlertCircle className="h-4 w-4" />
                {testError}
              </p>
            )}

            {testResult && (
              <div className="rounded-[8px] border border-[#E2DDD5] bg-[#FAF8F5] p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-lg font-black text-[#17211F]">{testResult.pincode}</span>
                    <span className="rounded bg-[#E7ECE9] px-2 py-0.5 text-xs font-bold text-[#17211F]">
                      {testResult.city}, {testResult.state}
                    </span>
                  </div>
                  {testResult.customOverrideApplied ? (
                    <span className="rounded bg-amber-100 px-2 py-0.5 text-[10px] font-black uppercase text-amber-800">
                      ★ DB Override Active
                    </span>
                  ) : (
                    <span className="rounded bg-emerald-100 px-2 py-0.5 text-[10px] font-black uppercase text-emerald-800">
                      Standard Matrix Route
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs pt-2 border-t border-[#EAE5DC]">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#71817A]">Logistics Zone</span>
                    <p className="font-black text-[#17211F]">{testResult.zoneDisplayName}</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#71817A]">Assigned Courier</span>
                    <p className="font-black text-[#1E6A62]">{testResult.courierPartner}</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#71817A]">Transit SLA</span>
                    <p className="font-black text-[#17211F]">{testResult.transitDays} Business Days</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#71817A]">Estimated Arrival</span>
                    <p className="font-black text-[#0F766E]">{testResult.estimatedDeliveryDate}</p>
                  </div>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#EAE5DC] text-xs">
                  <div className="flex items-center gap-3">
                    <span className={`inline-flex items-center gap-1 font-bold ${testResult.serviceable ? 'text-emerald-700' : 'text-red-700'}`}>
                      {testResult.serviceable ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
                      {testResult.serviceable ? 'Courier Serviceable' : 'Non-Serviceable'}
                    </span>
                    <span className={`inline-flex items-center gap-1 font-bold ${testResult.codAvailable ? 'text-emerald-700' : 'text-amber-700'}`}>
                      {testResult.codAvailable ? <CheckCircle2 className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
                      {testResult.codAvailable ? 'Cash on Delivery Allowed' : 'Prepaid Only (COD Gated)'}
                    </span>
                  </div>
                  <span className="text-[11px] text-[#71817A] italic">
                    Origin: {testResult.fulfillmentHub}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Quick Zone Taxonomy Panel */}
          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-6 shadow-2xs space-y-3">
            <h3 className="text-sm font-black text-[#17211F] flex items-center gap-1.5">
              <ShieldCheck className="h-4 w-4 text-[#1E6A62]" />
              SareeKart Courier Fleet Matrix
            </h3>
            <div className="space-y-2.5 text-xs text-[#4B5563]">
              <div className="p-2.5 rounded-[6px] bg-[#F7F4EE] border border-[#E7E2D8]">
                <strong className="text-[#17211F]">Blue Dart Apex Air:</strong>
                <p className="text-[11px] text-[#71817A] mt-0.5">Metros & South Capital corridors (1-2 days). High-priority silk delivery.</p>
              </div>
              <div className="p-2.5 rounded-[6px] bg-[#F7F4EE] border border-[#E7E2D8]">
                <strong className="text-[#17211F]">Delhivery Express:</strong>
                <p className="text-[11px] text-[#71817A] mt-0.5">Tier 1 & 2 commercial centers across North, West & Central India (2-3 days).</p>
              </div>
              <div className="p-2.5 rounded-[6px] bg-[#F7F4EE] border border-[#E7E2D8]">
                <strong className="text-[#17211F]">DTDC Air Express:</strong>
                <p className="text-[11px] text-[#71817A] mt-0.5">Hilly, North-East & Remote mountain corridors (4-5 days). COD restricted.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Custom Overrides Table */}
        <div className="rounded-[10px] border border-[#DDD8CF] bg-white shadow-2xs overflow-hidden">
          <div className="p-5 border-b border-[#DDD8CF] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-base font-black text-[#17211F]">Custom Pincode Routing Overrides</h2>
              <p className="text-xs text-[#71817A] mt-0.5">Overrides priority routing rules for specific 6-digit PIN codes.</p>
            </div>

            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-[#71817A]" />
              <input
                type="text"
                placeholder="Search overrides..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="h-9 w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] pl-9 pr-3 text-xs font-semibold outline-none focus:border-[#1E6A62]"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#FAF8F5] border-b border-[#DDD8CF] text-[#71817A] font-bold uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="px-5 py-3">PIN Code</th>
                  <th className="px-5 py-3">Location</th>
                  <th className="px-5 py-3">Zone</th>
                  <th className="px-5 py-3">Serviceable</th>
                  <th className="px-5 py-3">COD Status</th>
                  <th className="px-5 py-3">Assigned Courier</th>
                  <th className="px-5 py-3">SLA (Days)</th>
                  <th className="px-5 py-3">Notes</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F0ECE1]">
                {loading ? (
                  <tr>
                    <td colSpan={9} className="px-5 py-8 text-center text-[#71817A] font-medium">
                      Loading logistics matrix overrides...
                    </td>
                  </tr>
                ) : filteredOverrides.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="px-5 py-8 text-center text-[#71817A] font-medium">
                      {searchQuery ? 'No overrides match your search query.' : 'No custom pincode overrides defined. System is using the standard Indian Postal Circle engine.'}
                    </td>
                  </tr>
                ) : (
                  filteredOverrides.map((ov) => (
                    <tr key={ov.pincode} className="hover:bg-[#FDFCF9] transition-colors">
                      <td className="px-5 py-3.5 font-mono font-black text-[#17211F]">
                        {ov.pincode}
                      </td>
                      <td className="px-5 py-3.5 font-semibold text-[#17211F]">
                        {ov.city}, {ov.state}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="rounded bg-[#E7ECE9] px-2 py-0.5 text-[10px] font-bold text-[#17211F]">
                          {ov.zone}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1 font-bold ${ov.serviceable ? 'text-emerald-700' : 'text-red-600'}`}>
                          {ov.serviceable ? <CheckCircle2 className="h-3.5 w-3.5" /> : <XCircle className="h-3.5 w-3.5" />}
                          {ov.serviceable ? 'Active' : 'Gated'}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1 font-bold ${ov.codAvailable ? 'text-emerald-700' : 'text-amber-700'}`}>
                          {ov.codAvailable ? 'COD Enabled' : 'Prepaid Only'}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 font-semibold text-[#1E6A62]">
                        {ov.courierPartner}
                      </td>
                      <td className="px-5 py-3.5 font-black text-[#17211F]">
                        {ov.transitDays}d
                      </td>
                      <td className="px-5 py-3.5 text-[#71817A] max-w-xs truncate">
                        {ov.notes || '—'}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <button
                          onClick={() => handleDeleteOverride(ov.pincode)}
                          className="text-red-500 hover:text-red-700 p-1 rounded hover:bg-red-50 transition-colors"
                          title="Delete Override"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add / Edit Override Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="w-full max-w-lg rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#F0ECE1] pb-3">
              <h2 className="text-lg font-black text-[#17211F]">Add Pincode Override</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-[#71817A] hover:text-[#17211F]">
                <X className="h-5 w-5" />
              </button>
            </div>

            {modalError && (
              <p className="text-xs font-bold text-red-600 bg-red-50 p-2.5 rounded border border-red-200">
                {modalError}
              </p>
            )}

            <form onSubmit={handleSaveOverride} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">PIN Code *</label>
                  <input
                    type="text"
                    maxLength={6}
                    required
                    value={formPincode}
                    onChange={(e) => setFormPincode(e.target.value.replace(/\D/g, ''))}
                    placeholder="6 digits"
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 font-mono text-xs font-bold outline-none focus:border-[#1E6A62]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">Zone Tier</label>
                  <select
                    value={formZone}
                    onChange={(e) => setFormZone(e.target.value)}
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-white px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    <option value="METRO">METRO (Capital Hub)</option>
                    <option value="TIER_1">TIER_1 (Commercial Hub)</option>
                    <option value="TIER_2">TIER_2 (Regional Hub)</option>
                    <option value="REGIONAL">REGIONAL (District)</option>
                    <option value="REMOTE">REMOTE (Mountain Corridor)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">City *</label>
                  <input
                    type="text"
                    required
                    value={formCity}
                    onChange={(e) => setFormCity(e.target.value)}
                    placeholder="City Name"
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-semibold outline-none focus:border-[#1E6A62]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">State *</label>
                  <input
                    type="text"
                    required
                    value={formState}
                    onChange={(e) => setFormState(e.target.value)}
                    placeholder="State Name"
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-semibold outline-none focus:border-[#1E6A62]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">Assigned Courier</label>
                  <select
                    value={formCourier}
                    onChange={(e) => setFormCourier(e.target.value)}
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-white px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    <option value="Blue Dart Apex Express">Blue Dart Apex Express</option>
                    <option value="Delhivery Express">Delhivery Express</option>
                    <option value="Blue Dart Air Cargo">Blue Dart Air Cargo</option>
                    <option value="DTDC Air Express">DTDC Air Express</option>
                    <option value="SareeKart White-Glove VIP Courier">SareeKart White-Glove VIP Courier</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17211F] mb-1">Transit SLA (Days)</label>
                  <input
                    type="number"
                    min={1}
                    max={10}
                    required
                    value={formTransitDays}
                    onChange={(e) => setFormTransitDays(e.target.value)}
                    className="h-10 w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 font-mono text-xs font-bold outline-none focus:border-[#1E6A62]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-1">
                <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-[#17211F]">
                  <input
                    type="checkbox"
                    checked={formServiceable}
                    onChange={(e) => setFormServiceable(e.target.checked)}
                    className="h-4 w-4 rounded border-[#DDD8CF] text-[#1E6A62] accent-[#1E6A62]"
                  />
                  Delivery Serviceable
                </label>
                <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-[#17211F]">
                  <input
                    type="checkbox"
                    checked={formCodAvailable}
                    onChange={(e) => setFormCodAvailable(e.target.checked)}
                    className="h-4 w-4 rounded border-[#DDD8CF] text-[#1E6A62] accent-[#1E6A62]"
                  />
                  Allow Cash on Delivery (COD)
                </label>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17211F] mb-1">Notes / Staff Rationale</label>
                <textarea
                  rows={2}
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  placeholder="Reason for special routing or gating..."
                  className="w-full rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] p-2.5 text-xs outline-none focus:border-[#1E6A62]"
                />
              </div>

              <div className="flex justify-end gap-2 border-t border-[#F0ECE1] pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="rounded-[6px] border border-[#DDD8CF] bg-white px-4 py-2 text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-[6px] bg-[#1E6A62] px-5 py-2 text-xs font-bold text-white hover:bg-[#154e48] disabled:opacity-50"
                >
                  {submitting ? 'Saving...' : 'Save Override'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
