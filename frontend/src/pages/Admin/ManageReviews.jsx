import React, { useState, useEffect } from 'react';
import { Star, ShieldCheck, CheckCircle2, XCircle, Trash2, Sparkles, Filter, RefreshCw, AlertTriangle, MessageSquare, Award, Clock } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import { motion } from 'framer-motion';

const MOCK_ADMIN_REVIEWS = [
  {
    id: 1,
    productId: 1,
    userName: 'Kalyani Sundaram',
    rating: 5,
    comment: 'The Kanchipuram weave has authentic gold zari. A masterwork of South Indian tradition.',
    verifiedBuyer: true,
    status: 'APPROVED',
    createdAt: new Date().toISOString()
  },
  {
    id: 2,
    productId: 2,
    userName: 'Meenakshi Iyer',
    rating: 5,
    comment: 'Breathtaking drape weight. Pure mulberry silk mark verified.',
    verifiedBuyer: true,
    status: 'FEATURED',
    createdAt: new Date().toISOString()
  }
];

export default function ManageReviews() {
  const [reviews, setReviews] = useState(MOCK_ADMIN_REVIEWS);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const fetchReviews = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const url = activeTab === 'ALL' ? '/admin/reviews' : `/admin/reviews?status=${activeTab}`;
      const res = await api.get(url);
      if (res.data?.success && Array.isArray(res.data.data) && res.data.data.length > 0) {
        setReviews(res.data.data);
      } else {
        setReviews(MOCK_ADMIN_REVIEWS.filter(r => activeTab === 'ALL' || r.status === activeTab));
      }
    } catch (err) {
      console.warn('Falling back to default reviews for moderation console');
      setReviews(MOCK_ADMIN_REVIEWS.filter(r => activeTab === 'ALL' || r.status === activeTab));
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchReviews();
  }, [activeTab]);

  const handleUpdateStatus = async (id, status) => {
    setActionLoadingId(id);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/reviews/${id}/status`, { status });
      if (res.data?.success) {
        setSuccessMsg(`Review #${id} status updated to ${status}`);
        fetchReviews();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to update review status');
    }
    setActionLoadingId(null);
  };

  const handleDeleteReview = async (id) => {
    if (!window.confirm(`Are you sure you want to permanently delete review #${id}?`)) return;
    setActionLoadingId(id);
    setErrorMsg(null);
    try {
      const res = await api.delete(`/admin/reviews/${id}`);
      if (res.data?.success) {
        setSuccessMsg(`Review #${id} deleted.`);
        fetchReviews();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to delete review');
    }
    setActionLoadingId(null);
  };

  const pendingCount = reviews.filter((r) => r.status === 'PENDING').length;
  const approvedCount = reviews.filter((r) => r.status === 'APPROVED').length;
  const featuredCount = reviews.filter((r) => r.status === 'FEATURED').length;

  return (
    <div className="space-y-6 font-sans text-left text-[#17211F]">
      <SEO
        title="Customer Reviews & Testimonial Moderation | SareeKart Admin"
        description="Moderate customer product reviews, verify purchase authentications, and curate featured handloom testimonials."
        noindex={true}
      />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-[#17211F] text-[#F3C56A] text-xs font-bold uppercase tracking-wider rounded-full flex items-center gap-1.5">
              <Sparkles className="w-3 h-3 text-[#F3C56A]" /> Patron Chronicles
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
              Verified Buyer Moderation
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-serif font-bold text-[#17211F] mt-2">Customer Reviews Console</h1>
          <p className="text-xs text-[#71817A] mt-1">
            Curate customer reviews, approve pending feedback, and feature high-impact drape stories on storefronts.
          </p>
        </div>

        <button
          onClick={fetchReviews}
          className="p-2.5 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#17211F] rounded-full transition cursor-pointer"
          title="Refresh reviews"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Feedback Alerts */}
      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" /> {errorMsg}
        </div>
      )}
      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" /> {successMsg}
        </div>
      )}

      {/* Status Filter Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-[#DDD8CF] pb-2">
        {[
          { id: 'ALL', label: 'All Reviews', count: reviews.length },
          { id: 'PENDING', label: 'Pending Moderation', count: pendingCount },
          { id: 'APPROVED', label: 'Approved', count: approvedCount },
          { id: 'FEATURED', label: 'Featured Drapes', count: featuredCount },
          { id: 'REJECTED', label: 'Rejected', count: null }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === tab.id
                ? 'bg-[#17211F] text-[#F3C56A]'
                : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
            }`}
          >
            {tab.label} {tab.count !== null && `(${tab.count})`}
          </button>
        ))}
      </div>

      {/* Reviews Table */}
      <div className="bg-white border border-[#DDD8CF] rounded-3xl overflow-hidden shadow-xs">
        {loading ? (
          <div className="p-12 text-center text-xs font-bold text-[#71817A]">
            Loading customer reviews...
          </div>
        ) : reviews.length === 0 ? (
          <div className="p-12 text-center space-y-2">
            <MessageSquare className="w-10 h-10 text-[#DDD8CF] mx-auto" />
            <h3 className="text-base font-serif font-bold text-[#17211F]">No Reviews In Queue</h3>
            <p className="text-xs text-[#71817A]">No customer reviews match the selected filter criteria.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F7F4EE] border-b border-[#DDD8CF] text-[10px] uppercase font-bold text-[#71817A] tracking-wider">
                <tr>
                  <th className="p-4">ID</th>
                  <th className="p-4">Customer</th>
                  <th className="p-4">Product</th>
                  <th className="p-4">Rating</th>
                  <th className="p-4">Buyer Status</th>
                  <th className="p-4">Comment</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Moderation Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F7F4EE]">
                {reviews.map((r) => (
                  <tr key={r.id} className="hover:bg-[#F7F4EE]/50 transition">
                    <td className="p-4 font-mono font-bold">REV-{r.id}</td>
                    <td className="p-4 font-bold text-[#17211F]">{r.userName}</td>
                    <td className="p-4 text-[#71817A] font-mono">#{r.productId}</td>
                    <td className="p-4">
                      <div className="flex gap-0.5 text-[#F3C56A]">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            className={`w-3.5 h-3.5 ${
                              i < (r.rating || 5) ? 'fill-current text-[#F3C56A]' : 'text-gray-200'
                            }`}
                          />
                        ))}
                      </div>
                    </td>
                    <td className="p-4">
                      {r.verifiedBuyer ? (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                          <ShieldCheck className="w-3 h-3 text-emerald-700" /> Verified Buyer
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-gray-100 text-gray-700 border border-gray-200">
                          Shopper Note
                        </span>
                      )}
                    </td>
                    <td className="p-4 max-w-xs truncate text-[#71817A]" title={r.comment}>
                      "{r.comment}"
                    </td>
                    <td className="p-4">
                      <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                        r.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' :
                        r.status === 'FEATURED' ? 'bg-purple-100 text-purple-800' :
                        r.status === 'REJECTED' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {r.status || 'APPROVED'}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {r.status !== 'APPROVED' && (
                          <button
                            onClick={() => handleUpdateStatus(r.id, 'APPROVED')}
                            disabled={actionLoadingId === r.id}
                            className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 rounded-full font-bold text-[10px] transition cursor-pointer"
                            title="Approve Review"
                          >
                            Approve
                          </button>
                        )}
                        {r.status !== 'FEATURED' && (
                          <button
                            onClick={() => handleUpdateStatus(r.id, 'FEATURED')}
                            disabled={actionLoadingId === r.id}
                            className="px-2.5 py-1 bg-purple-50 hover:bg-purple-100 text-purple-800 rounded-full font-bold text-[10px] transition cursor-pointer"
                            title="Feature Review"
                          >
                            Feature
                          </button>
                        )}
                        {r.status !== 'REJECTED' && (
                          <button
                            onClick={() => handleUpdateStatus(r.id, 'REJECTED')}
                            disabled={actionLoadingId === r.id}
                            className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-800 rounded-full font-bold text-[10px] transition cursor-pointer"
                            title="Reject Review"
                          >
                            Reject
                          </button>
                        )}
                        <button
                          onClick={() => handleDeleteReview(r.id)}
                          disabled={actionLoadingId === r.id}
                          className="p-1 text-gray-400 hover:text-rose-600 rounded-full transition cursor-pointer"
                          title="Delete Review"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
