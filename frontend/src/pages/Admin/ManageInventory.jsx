import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Package, AlertTriangle, Layers, Truck, ArrowUpRight, ArrowDownLeft, RefreshCcw, Plus, Search, Filter, ShieldCheck, Check, X, Building2, Barcode, Clock, CheckCircle2, UploadCloud, FileSpreadsheet, Image as ImageIcon, Download } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';
import SareePhotoDropzone from '../../components/admin/SareePhotoDropzone';
import productService from '../../services/productService';
import categoryService from '../../services/categoryService';

const MOCK_WAREHOUSES = [
  { id: 'WH-01', name: 'Bengaluru Central Fulfillment Hub', capacity: 1000, occupied: 850, status: 'Active' },
  { id: 'WH-02', name: 'Varanasi Artisan Weaving Guild Vault', capacity: 800, occupied: 420, status: 'Active' },
  { id: 'WH-03', name: 'Kanchipuram Heritage Reserve', capacity: 600, occupied: 300, status: 'Active' }
];

const MOCK_INVENTORY = [
  {
    sku: 'SK-KANCHI-GOLD-01',
    name: 'Imperial Gold Kanchipuram Tissue Silk Saree',
    category: 'Kanchipuram Silk',
    warehouse: 'WH-01 (Bengaluru)',
    bin: 'A2-R4-S1',
    onHand: 12,
    reserved: 3,
    available: 9,
    unitPrice: 12500,
    status: 'IN_STOCK'
  },
  {
    sku: 'SK-BANARASI-RED-02',
    name: 'Crimson Red Banarasi Zari Brocade Saree',
    category: 'Banarasi Silk',
    warehouse: 'WH-02 (Varanasi)',
    bin: 'B1-R2-S3',
    onHand: 4,
    reserved: 1,
    available: 3,
    unitPrice: 18900,
    status: 'LOW_STOCK'
  },
  {
    sku: 'SK-PAITHANI-BLUE-03',
    name: 'Peacock Blue Paithani Handwoven Silk Saree',
    category: 'Paithani Weave',
    warehouse: 'WH-01 (Bengaluru)',
    bin: 'A3-R1-S2',
    onHand: 0,
    reserved: 0,
    available: 0,
    unitPrice: 15000,
    status: 'OUT_OF_STOCK'
  }
];

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0,
  }).format(val || 0);

export default function ManageInventory() {
  const [inventory, setInventory] = useState(MOCK_INVENTORY);
  const [searchTerm, setSearchTerm] = useState('');
  const [showPOModal, setShowPOModal] = useState(false);
  const [notification, setNotification] = useState(null);

  // PO Form State
  const [supplier, setSupplier] = useState('Varanasi Weaver Cooperative');
  const [poSKU, setPoSKU] = useState('SK-ROYAL-BANARASI-SILK-SA-1');
  const [poQty, setPoQty] = useState(25);

  // Transfer Form State
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [transfers, setTransfers] = useState([]);
  const [transferSku, setTransferSku] = useState('SK-KANCHI-GOLD-01');
  const [sourceWarehouse, setSourceWarehouse] = useState('WH-01 Bengaluru Central Fulfillment Hub');
  const [targetWarehouse, setTargetWarehouse] = useState('WH-02 Mumbai West Distribution Hub');
  const [transferQty, setTransferQty] = useState(5);
  const [transferReason, setTransferReason] = useState('');
  const { user } = useSelector((state) => state.auth);
  const [showOfflineModal, setShowOfflineModal] = useState(false);
  const [offlineTab, setOfflineTab] = useState('photos'); // 'photos' | 'excel'
  const [selectedPhotoSku, setSelectedPhotoSku] = useState('SK-KANCHI-GOLD-01');
  const [selectedPhotoItem, setSelectedPhotoItem] = useState(null);
  const [itemPhotos, setItemPhotos] = useState([]);
  const [loadingPhotos, setLoadingPhotos] = useState(false);

  // Offline Excel State in Inventory
  const [excelFile, setExcelFile] = useState(null);
  const [excelDragging, setExcelDragging] = useState(false);
  const [excelValidating, setExcelValidating] = useState(false);
  const [excelResult, setExcelResult] = useState(null);
  const [excelMsg, setExcelMsg] = useState(null);
  const [excelErr, setExcelErr] = useState(null);

  // Add Saree Modal State for Inventory
  const [showAddSareeModal, setShowAddSareeModal] = useState(false);
  const [categories, setCategories] = useState([]);
  const [sareeModalLoading, setSareeModalLoading] = useState(false);
  const [sareeModalError, setSareeModalError] = useState(null);
  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formPrice, setFormPrice] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formStockQuantity, setFormStockQuantity] = useState('10');
  const [formFabric, setFormFabric] = useState('Silk');
  const [formOccasion, setFormOccasion] = useState('Wedding');
  const [formColor, setFormColor] = useState('');
  const [formImages, setFormImages] = useState([]);

  const handleOpenPhotoUpload = async (item) => {
    const targetSku = item?.sku || (inventory[0]?.sku || '');
    setSelectedPhotoSku(targetSku);
    setSelectedPhotoItem(item || inventory.find(i => i.sku === targetSku) || null);
    setItemPhotos([]);
    setOfflineTab('photos');
    setShowOfflineModal(true);
    setLoadingPhotos(true);
    try {
      const res = await api.get(`/admin/photos/by-sku/${targetSku}`);
      if (res.data?.success && Array.isArray(res.data.images)) {
        setItemPhotos(res.data.images);
      }
    } catch (_) {
      // Fallback
    } finally {
      setLoadingPhotos(false);
    }
  };

  const handleProcessExcel = async (file) => {
    if (!file) return;
    setExcelFile(file);
    setExcelResult(null);
    setExcelMsg(null);
    setExcelErr(null);
    setExcelValidating(true);
    const fd = new FormData();
    fd.append('file', file);
    fd.append('type', 'stock');
    try {
      const res = await api.post('/excel/preview', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.data?.success) {
        setExcelResult(res.data.data);
      }
    } catch (err) {
      setExcelErr(err.response?.data?.message || err.response?.data?.error || 'Validation error');
    } finally {
      setExcelValidating(false);
    }
  };

  const handleImportExcelStock = async () => {
    if (!excelFile || !excelResult?.valid) return;
    setExcelValidating(true);
    setExcelErr(null);
    const fd = new FormData();
    fd.append('file', excelFile);
    fd.append('type', 'stock');
    fd.append('mode', 'COMPLETED_SALES');
    try {
      const res = await api.post('/excel/import', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.data?.success) {
        setExcelMsg(res.data.message || 'Inventory stock batch queued for Owner/Manager approval.');
        setExcelFile(null);
        setExcelResult(null);
        fetchInventory();
      }
    } catch (err) {
      setExcelErr(err.response?.data?.message || err.response?.data?.error || 'Import failed');
    } finally {
      setExcelValidating(false);
    }
  };

  const fetchInventory = async () => {
    try {
      const [invRes, transRes] = await Promise.allSettled([
        api.get('/inventory'),
        api.get('/admin/inventory/transfers')
      ]);

      if (invRes.status === 'fulfilled' && invRes.value.data?.success && Array.isArray(invRes.value.data.data) && invRes.value.data.data.length > 0) {
        setInventory(invRes.value.data.data);
        if (invRes.value.data.data[0]?.sku) {
          setPoSKU(invRes.value.data.data[0].sku);
          setTransferSku(invRes.value.data.data[0].sku);
        }
      }

      if (transRes.status === 'fulfilled' && transRes.value.data?.success && Array.isArray(transRes.value.data.data)) {
        setTransfers(transRes.value.data.data);
      }
    } catch (err) {
      console.log('Falling back to default inventory and transfers');
    }
  };

  useEffect(() => {
    fetchInventory();
    const fetchCategories = async () => {
      try {
        const response = await categoryService.getCategories();
        if (response.success && response.data) {
          setCategories(response.data);
          if (response.data[0]?.id) {
            setFormCategoryId(response.data[0].id);
          }
        }
      } catch (err) {
        console.error('Failed to load categories:', err);
      }
    };
    fetchCategories();
  }, []);

  const handleOpenAddSareeModal = () => {
    setFormName('');
    setFormDescription('');
    setFormPrice('');
    setFormCategoryId(categories[0]?.id || '');
    setFormStockQuantity('10');
    setFormFabric('Silk');
    setFormOccasion('Wedding');
    setFormColor('');
    setFormImages([]);
    setSareeModalError(null);
    setShowAddSareeModal(true);
  };

  const handleAddSareeSubmit = async (e) => {
    e.preventDefault();
    if (!formName.trim() || !formPrice || !formStockQuantity) {
      setSareeModalError('Please fill all required fields.');
      return;
    }

    const parsedCategoryId = formCategoryId ? parseInt(formCategoryId, 10) : (categories[0]?.id || null);

    const payload = {
      name: formName.trim(),
      description: formDescription,
      price: parseFloat(formPrice),
      categoryId: isNaN(parsedCategoryId) ? null : parsedCategoryId,
      stockQuantity: parseInt(formStockQuantity, 10) || 0,
      fabric: formFabric,
      occasion: formOccasion,
      color: formColor,
      images: Array.isArray(formImages) ? formImages : []
    };

    try {
      setSareeModalLoading(true);
      setSareeModalError(null);
      await productService.createProduct(payload);
      setShowAddSareeModal(false);
      setNotification(`Saree "${formName.trim()}" onboarded successfully with auto-allocated warehouse SKU!`);
      fetchInventory();
    } catch (err) {
      console.error('Failed to create saree:', err);
      const errMsg = err.response?.data?.message || err.response?.data?.error || err.message || 'Error occurred while saving saree.';
      setSareeModalError(errMsg);
    } finally {
      setSareeModalLoading(false);
    }
  };

  const filteredInventory = inventory.filter(item =>
    (item.productName || item.name)?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.sku?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.category?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleCreatePO = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/inventory/adjust', {
        sku: poSKU,
        quantityAdjustment: parseInt(poQty, 10),
        reason: `PO restock from ${supplier}`
      });
      setNotification(res.data?.message || 'Stock adjustment submitted. Status: Pending owner approval');
      setShowPOModal(false);
      fetchInventory();
    } catch (err) {
      setNotification(err.response?.data?.message || 'Failed to submit restock request');
    }
  };

  const handleCreateTransfer = async (e) => {
    e.preventDefault();
    if (sourceWarehouse === targetWarehouse) {
      setNotification('Source and target warehouse must be different.');
      return;
    }
    try {
      const res = await api.post('/admin/inventory/transfer', {
        sku: transferSku,
        sourceWarehouse,
        targetWarehouse,
        quantity: parseInt(transferQty, 10),
        reason: transferReason
      });
      setNotification(res.data?.message || 'Transfer request submitted. Status: Pending owner approval');
      setShowTransferModal(false);
      fetchInventory();
    } catch (err) {
      setNotification(err.response?.data?.message || 'Failed to submit transfer request');
    }
  };

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Inventory & Warehouse Management | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold font-serif text-[#111827]">Inventory & Supply Chain Console</h1>
            <span className="px-2.5 py-0.5 bg-[#17211F] text-[#F3C56A] text-[10px] font-bold uppercase tracking-wider rounded-full">
              {user?.role === 'OWNER' ? 'OWNER • Executive Approver' : user?.role === 'MANAGER' ? 'MANAGER • Maker Operations' : 'ADMIN • Supply Chain'}
            </span>
          </div>
          <p className="text-xs text-[#6b5c4d] mt-1">Multi-warehouse stock allocation, SKU tracking, offline asset dropzone, and purchase orders</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={handleOpenAddSareeModal}
            className="h-11 px-5 bg-[#17211F] hover:bg-[#253633] text-[#F3C56A] font-bold text-xs uppercase tracking-wider rounded-full shadow-xs transition-all flex items-center gap-2 cursor-pointer"
            title="Directly add a new saree and allocate inventory"
          >
            <Plus className="w-4 h-4 text-[#F3C56A]" /> Add New Saree
          </button>
          <button
            onClick={() => {
              if (filteredInventory[0]) {
                handleOpenPhotoUpload(filteredInventory[0]);
              } else {
                setOfflineTab('photos');
                setShowOfflineModal(true);
              }
            }}
            className="h-11 px-5 bg-[#1E6A62] hover:bg-[#164e48] text-white font-bold text-xs uppercase tracking-wider rounded-full shadow-xs transition-all flex items-center gap-2 cursor-pointer"
          >
            <UploadCloud className="w-4 h-4 text-[#F3C56A]" /> Offline Drop & Upload
          </button>
          <button
            onClick={() => setShowTransferModal(true)}
            className="h-11 px-5 bg-white border border-[#DDD8CF] hover:bg-[#F7F4EE] text-[#17211F] font-bold text-xs uppercase tracking-wider rounded-full shadow-xs transition-all flex items-center gap-2 cursor-pointer"
          >
            <Truck className="w-4 h-4 text-[#1E6A62]" /> Inter-Warehouse Transfer
          </button>
          <button
            onClick={() => setShowPOModal(true)}
            className="h-11 px-5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center gap-2 cursor-pointer"
          >
            <Plus className="w-4 h-4" /> Issue Purchase Order
          </button>
        </div>
      </div>

      {notification && (
        <div className="p-4 bg-amber-50 border border-amber-200 text-amber-900 text-xs font-bold rounded-2xl flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-600" />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-amber-700 hover:text-amber-900 cursor-pointer">✕</button>
        </div>
      )}

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Total SKUs</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">148 Active SKUs</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Total Valuation</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">₹1.85 Crore</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Low Stock Alerts</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">4 Critical Items</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Truck className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Pending POs</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">3 Active POs</h3>
          </div>
        </div>
      </div>

      {/* Warehouse Capacity Overview */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-6 shadow-xs space-y-4">
        <h2 className="text-sm font-bold font-serif text-[#111827] flex items-center gap-2">
          <Building2 className="w-4 h-4 text-[#E85D4F]" /> Multi-Warehouse Storage Allocation
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {MOCK_WAREHOUSES.map((wh) => (
            <div key={wh.id} className="p-4 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="font-bold text-[#111827]">{wh.name}</span>
                <span className="font-mono text-[10px] font-bold text-[#E85D4F]">{wh.id}</span>
              </div>
              <div className="h-2 bg-white border border-[#DDE4EA] rounded-full overflow-hidden">
                <div
                  className="h-full bg-[#111827] rounded-full"
                  style={{ width: `${(wh.occupied / wh.capacity) * 100}%` }}
                />
              </div>
              <div className="flex justify-between items-center text-[10px] text-[#6b5c4d] font-medium">
                <span>{wh.occupied} / {wh.capacity} Bins Occupied</span>
                <span className="font-bold text-[#111827]">{Math.round((wh.occupied / wh.capacity) * 100)}% Capacity</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Inventory Table & Filters */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-2 bg-white border border-[#DDE4EA] rounded-xl px-3 py-2 w-full sm:w-80">
            <Search className="w-4 h-4 text-[#6b5c4d]" />
            <input
              type="text"
              placeholder="Search SKU, name or barcode..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="bg-transparent border-none outline-none text-xs w-full text-[#111827]"
            />
          </div>
          <span className="text-xs font-bold text-[#6b5c4d]">Showing {filteredInventory.length} SKUs</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">SKU / Barcode</th>
                <th className="px-6 py-3.5">Drape Title</th>
                <th className="px-6 py-3.5">Warehouse Location</th>
                <th className="px-6 py-3.5 text-center">On-Hand</th>
                <th className="px-6 py-3.5 text-center">Reserved</th>
                <th className="px-6 py-3.5 text-center">Available</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-center">Photos & Offline Drop</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {filteredInventory.map((item) => (
                <tr key={item.sku} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-[#111827] flex items-center gap-2">
                    <Barcode className="w-4 h-4 text-[#E85D4F]" /> {item.sku}
                  </td>
                  <td className="px-6 py-4 font-bold text-[#111827]">
                    <div>
                      <span className="block">{item.productName || item.name}</span>
                      <span className="text-[10px] text-[#E85D4F] uppercase tracking-wider block font-sans">{item.category}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 font-medium text-[#6b5c4d]">
                    <div>
                      <span className="block font-bold text-[#111827]">{item.warehouseName || item.warehouse}</span>
                      <span className="text-[10px] text-[#6b5c4d] block font-mono">Bin: {item.binLocation || item.bin}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-center font-bold text-[#111827]">{item.onHand}</td>
                  <td className="px-6 py-4 text-center font-medium text-amber-700">{item.reserved}</td>
                  <td className="px-6 py-4 text-center font-extrabold text-emerald-800">{item.available}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                      item.status === 'IN_STOCK' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                      item.status === 'LOW_STOCK' ? 'bg-amber-50 text-amber-800 border-amber-200' :
                      'bg-red-50 text-red-800 border-red-200'
                    }`}>
                      {item.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <button
                      type="button"
                      onClick={() => handleOpenPhotoUpload(item)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[#F7F4EE] hover:bg-[#1E6A62] text-[#1E6A62] hover:text-white border border-[#DDD8CF] font-bold text-[11px] uppercase tracking-wider transition cursor-pointer shadow-xs"
                      title="Offline Drop & Upload Saree Photos"
                    >
                      <ImageIcon className="w-3.5 h-3.5" />
                      <span>Drop Photos</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Inter-Warehouse Transfer Activity */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <div className="flex items-center gap-2">
            <Truck className="w-4 h-4 text-[#1E6A62]" />
            <h2 className="text-sm font-bold font-serif text-[#111827]">Inter-Warehouse Stock Transfers</h2>
            <span className="px-2 py-0.5 bg-[#17211F] text-[#F3C56A] text-[10px] font-bold rounded-full">Maker-Checker</span>
          </div>
          <span className="text-xs font-bold text-[#6b5c4d]">{transfers.length} Recorded Transfers</span>
        </div>

        {transfers.length === 0 ? (
          <div className="p-8 text-center text-xs text-[#71817A]">
            No inter-warehouse transfers logged yet. Click "Inter-Warehouse Transfer" above to initiate a stock rebalance.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">Transfer ID</th>
                  <th className="px-6 py-3.5">SKU / Item</th>
                  <th className="px-6 py-3.5">Source Hub</th>
                  <th className="px-6 py-3.5">Target Hub</th>
                  <th className="px-6 py-3.5 text-center">Quantity</th>
                  <th className="px-6 py-3.5">Reason</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5">Requested By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {transfers.map((t) => (
                  <tr key={t.id} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-[#111827]">TRF-{t.id}</td>
                    <td className="px-6 py-4 font-bold text-[#111827]">
                      <div>
                        <span>{t.sku}</span>
                        {t.productName && <span className="text-[10px] text-[#71817A] block font-normal">{t.productName}</span>}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-[#71817A]">{t.sourceWarehouse}</td>
                    <td className="px-6 py-4 font-bold text-[#1E6A62]">{t.targetWarehouse}</td>
                    <td className="px-6 py-4 text-center font-bold text-[#111827]">{t.quantity}</td>
                    <td className="px-6 py-4 text-[#71817A] max-w-xs truncate" title={t.reason}>{t.reason || 'Regional stock rebalance'}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        t.status === 'APPROVED' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                        t.status === 'REJECTED' ? 'bg-rose-50 text-rose-800 border-rose-200' :
                        'bg-amber-50 text-amber-800 border-amber-200'
                      }`}>
                        {t.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-[#71817A] text-[11px]">{t.requestedByEmail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Inter-Warehouse Transfer Modal */}
      <AnimatePresence>
        {showTransferModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowTransferModal(false)} className="absolute inset-0 bg-black/50 backdrop-blur-xs" />
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-lg w-full p-6 space-y-6 z-10 text-left shadow-2xl">
              <div className="flex justify-between items-center border-b border-[#DDE4EA] pb-4">
                <div>
                  <h3 className="text-lg font-bold font-serif text-[#111827]">Inter-Warehouse Stock Rebalance</h3>
                  <p className="text-xs text-[#71817A]">Maker-Checker Protocol: Requests will require Owner sign-off in Approval Center</p>
                </div>
                <button onClick={() => setShowTransferModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827]"><X className="w-5 h-5" /></button>
              </div>

              <form onSubmit={handleCreateTransfer} className="space-y-4">
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Select SKU to Transfer *</label>
                  <select
                    value={transferSku}
                    onChange={(e) => setTransferSku(e.target.value)}
                    className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62] bg-white"
                  >
                    {inventory.map((inv) => (
                      <option key={inv.sku} value={inv.sku}>
                        {inv.sku} — {inv.name || 'Silk Drape'} ({inv.available} Available)
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Source Hub *</label>
                    <select
                      value={sourceWarehouse}
                      onChange={(e) => setSourceWarehouse(e.target.value)}
                      className="w-full h-11 px-3 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62] bg-white"
                    >
                      <option value="WH-01 Bengaluru Central Fulfillment Hub">WH-01 Bengaluru Central</option>
                      <option value="WH-02 Mumbai West Distribution Hub">WH-02 Mumbai West</option>
                      <option value="WH-03 Delhi North Regional Vault">WH-03 Delhi North</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Target Hub *</label>
                    <select
                      value={targetWarehouse}
                      onChange={(e) => setTargetWarehouse(e.target.value)}
                      className="w-full h-11 px-3 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62] bg-white"
                    >
                      <option value="WH-02 Mumbai West Distribution Hub">WH-02 Mumbai West</option>
                      <option value="WH-03 Delhi North Regional Vault">WH-03 Delhi North</option>
                      <option value="WH-01 Bengaluru Central Fulfillment Hub">WH-01 Bengaluru Central</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Transfer Units *</label>
                    <input
                      type="number"
                      required
                      min="1"
                      max="100"
                      value={transferQty}
                      onChange={(e) => setTransferQty(e.target.value)}
                      className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#1E6A62]"
                    />
                  </div>

                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Carrier Logistics</label>
                    <select className="w-full h-11 px-3 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62] bg-white">
                      <option>Blue Dart Express Secure</option>
                      <option>Delhivery Surface Heavy</option>
                      <option>DTDC Air Cargo</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Business Justification *</label>
                  <input
                    type="text"
                    required
                    value={transferReason}
                    onChange={(e) => setTransferReason(e.target.value)}
                    placeholder="e.g. Navratri showroom stock rebalance"
                    className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62]"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full h-12 bg-[#17211F] hover:bg-[#1E6A62] text-[#F3C56A] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all mt-4 cursor-pointer"
                >
                  Submit Stock Transfer Request
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Issue Purchase Order Modal */}
      <AnimatePresence>
        {showPOModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowPOModal(false)} className="absolute inset-0 bg-black/50 backdrop-blur-xs" />
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-md w-full p-6 space-y-6 z-10 text-left shadow-2xl">
              <div className="flex justify-between items-center border-b border-[#DDE4EA] pb-4">
                <h3 className="text-lg font-bold font-serif text-[#111827]">Issue Weaver Purchase Order</h3>
                <button onClick={() => setShowPOModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827]"><X className="w-5 h-5" /></button>
              </div>

              <form onSubmit={handleCreatePO} className="space-y-4">
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Supplier / Guild Cooperative *</label>
                  <select value={supplier} onChange={e => setSupplier(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#E85D4F]">
                    <option>Varanasi Weaver Cooperative</option>
                    <option>Kanchipuram Silk Master Guild</option>
                    <option>Pochampally Ikat Weavers</option>
                  </select>
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Target SKU *</label>
                  <input type="text" required value={poSKU} onChange={e => setPoSKU(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-mono outline-none focus:border-[#E85D4F]" />
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Restock Quantity *</label>
                  <input type="number" required min="5" value={poQty} onChange={e => setPoQty(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#E85D4F]" />
                </div>

                <button type="submit" className="w-full h-12 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all mt-4 cursor-pointer">
                  Issue PO to Supplier
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Offline Drop & Upload Modal */}
      <AnimatePresence>
        {showOfflineModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowOfflineModal(false)}
              className="absolute inset-0 bg-black/50 backdrop-blur-xs"
            />
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-2xl w-full p-6 space-y-6 z-10 text-left shadow-2xl max-h-[90vh] overflow-y-auto"
            >
              <div className="flex justify-between items-start border-b border-[#DDE4EA] pb-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-xl font-bold font-serif text-[#111827]">Offline Drop & Upload Console</h3>
                    <span className="px-2.5 py-0.5 bg-[#17211F] text-[#F3C56A] text-[10px] font-bold rounded-full uppercase tracking-wider">
                      {user?.role || 'OWNER'} Privileged
                    </span>
                  </div>
                  <p className="text-xs text-[#71817A] mt-1">
                    Direct local storage: drop high-res saree drape photos or bulk inventory spreadsheets without external cloud latency.
                  </p>
                </div>
                <button onClick={() => setShowOfflineModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827] cursor-pointer">
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Tabs */}
              <div className="flex gap-2 border-b border-[#DDE4EA] pb-2">
                <button
                  type="button"
                  onClick={() => setOfflineTab('photos')}
                  className={`flex items-center gap-2 px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition cursor-pointer ${
                    offlineTab === 'photos'
                      ? 'bg-[#17211F] text-[#F3C56A]'
                      : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
                  }`}
                >
                  <UploadCloud className="w-4 h-4 text-[#F3C56A]" /> Saree Photos Dropzone
                </button>
                <button
                  type="button"
                  onClick={() => setOfflineTab('excel')}
                  className={`flex items-center gap-2 px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition cursor-pointer ${
                    offlineTab === 'excel'
                      ? 'bg-[#17211F] text-[#F3C56A]'
                      : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
                  }`}
                >
                  <FileSpreadsheet className="w-4 h-4 text-[#1E6A62]" /> Stock Spreadsheet (.xlsx / .csv)
                </button>
              </div>

              {/* Tab 1: Photos */}
              {offlineTab === 'photos' && (
                <div className="space-y-4">
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">
                      Target Drape / SKU *
                    </label>
                    <select
                      value={selectedPhotoSku}
                      onChange={(e) => {
                        const targetSku = e.target.value;
                        setSelectedPhotoSku(targetSku);
                        const itm = inventory.find(i => i.sku === targetSku);
                        setSelectedPhotoItem(itm || null);
                        handleOpenPhotoUpload(itm || { sku: targetSku });
                      }}
                      className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#1E6A62] bg-white font-mono"
                    >
                      {inventory.map((inv) => (
                        <option key={inv.sku} value={inv.sku}>
                          {inv.sku} &mdash; {inv.productName || inv.name} ({inv.available || inv.onHand || 0} Units)
                        </option>
                      ))}
                    </select>
                  </div>

                  {selectedPhotoItem && (
                    <div className="p-3 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl flex items-center justify-between text-xs">
                      <div>
                        <span className="font-bold text-[#111827] block">{selectedPhotoItem.productName || selectedPhotoItem.name}</span>
                        <span className="text-[10px] text-[#71817A] font-mono">Location: {selectedPhotoItem.warehouseName || selectedPhotoItem.warehouse} &bull; Bin: {selectedPhotoItem.binLocation || selectedPhotoItem.bin}</span>
                      </div>
                      <span className="font-bold text-[#1E6A62] text-[11px]">
                        Available: {selectedPhotoItem.available || 0}
                      </span>
                    </div>
                  )}

                  <div className="space-y-2">
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block">
                      Drop Photos for this Drape
                    </label>
                    <SareePhotoDropzone
                      sku={selectedPhotoSku}
                      productId={selectedPhotoItem?.productId}
                      value={itemPhotos}
                      onChange={(newUrls) => setItemPhotos(newUrls)}
                      maxPhotos={6}
                    />
                  </div>

                  <p className="text-[11px] text-[#71817A] bg-emerald-50 border border-emerald-200 text-emerald-900 p-3 rounded-xl">
                    &bull; Uploaded photos are stored on the server at <code className="font-mono font-bold">/uploads/saree-photos/</code> and immediately attached to SKU <strong className="font-mono">{selectedPhotoSku}</strong> in the catalog.
                  </p>
                </div>
              )}

              {/* Tab 2: Excel */}
              {offlineTab === 'excel' && (
                <div className="space-y-4">
                  {excelMsg && (
                    <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs font-bold rounded-xl flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" /> {excelMsg}
                    </div>
                  )}
                  {excelErr && (
                    <div className="p-3 bg-rose-50 border border-rose-200 text-rose-900 text-xs font-bold rounded-xl flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" /> {excelErr}
                    </div>
                  )}

                  <div
                    onDrop={(e) => {
                      e.preventDefault();
                      setExcelDragging(false);
                      const file = e.dataTransfer.files?.[0];
                      if (file) handleProcessExcel(file);
                    }}
                    onDragOver={(e) => { e.preventDefault(); setExcelDragging(true); }}
                    onDragLeave={() => setExcelDragging(false)}
                    className={`border-2 border-dashed rounded-2xl p-6 text-center space-y-3 transition cursor-pointer ${
                      excelDragging
                        ? 'border-[#1E6A62] bg-[#E3F0ED] scale-[1.01]'
                        : 'border-[#DDE4EA] bg-[#F5F7FA] hover:border-[#1E6A62]'
                    }`}
                  >
                    <FileSpreadsheet className="w-8 h-8 text-[#1E6A62] mx-auto" />
                    <p className="text-xs font-bold text-[#111827]">
                      {excelDragging ? 'Drop stock spreadsheet here' : 'Drag & drop inventory stock spreadsheet (.xlsx / .csv)'}
                    </p>
                    <p className="text-[10px] text-[#71817A]">
                      Columns: SKU, Warehouse, Bin, Adjustment Quantity, Reason
                    </p>
                    <input
                      type="file"
                      id="inventoryExcelInput"
                      accept=".xlsx,.csv"
                      className="hidden"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) handleProcessExcel(file);
                      }}
                    />
                    <label
                      htmlFor="inventoryExcelInput"
                      className="inline-block px-4 py-2 bg-[#17211F] hover:bg-[#1E6A62] text-white text-[11px] font-bold uppercase tracking-wider rounded-full cursor-pointer transition shadow-xs"
                    >
                      {excelValidating ? 'Parsing Spreadsheet...' : 'Browse or Drop File'}
                    </label>
                    {excelFile && (
                      <p className="text-xs font-mono font-bold text-[#1E6A62]">Loaded: {excelFile.name}</p>
                    )}
                  </div>

                  {excelResult && (
                    <div className="p-4 bg-white border border-[#DDD8CF] rounded-xl space-y-3 text-xs">
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-[#111827]">Validation: {excelResult.totalRows} Rows Evaluated</span>
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          excelResult.valid ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                        }`}>
                          {excelResult.valid ? 'Ready to Apply' : 'Errors Detected'}
                        </span>
                      </div>
                      {excelResult.valid && (
                        <button
                          type="button"
                          onClick={handleImportExcelStock}
                          className="w-full h-11 bg-[#1E6A62] hover:bg-[#164e48] text-white font-bold text-xs uppercase tracking-wider rounded-full shadow-md transition cursor-pointer"
                        >
                          Execute Stock Ingestion
                        </button>
                      )}
                    </div>
                  )}
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Add Saree Product & Inventory Modal */}
      <AnimatePresence>
        {showAddSareeModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowAddSareeModal(false)}
              className="absolute inset-0 bg-black/50 backdrop-blur-xs"
            />
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-xl w-full p-6 space-y-5 z-10 text-left shadow-2xl max-h-[90vh] flex flex-col"
            >
              <div className="flex justify-between items-center border-b border-[#DDE4EA] pb-4 shrink-0">
                <div>
                  <h3 className="text-xl font-bold font-serif text-[#111827]">Add Saree & Allocate Inventory</h3>
                  <p className="text-xs text-[#71817A] mt-0.5">
                    Onboard new drape: auto-assigns warehouse SKU, initial stock, and offline photos.
                  </p>
                </div>
                <button onClick={() => setShowAddSareeModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827] cursor-pointer">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={handleAddSareeSubmit} className="flex-grow overflow-y-auto space-y-4 pr-1">
                {sareeModalError && (
                  <div className="bg-red-50 border border-red-200 text-red-900 text-xs p-3 rounded-xl font-semibold flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                    <span>{sareeModalError}</span>
                  </div>
                )}

                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Saree Title *</label>
                  <input
                    type="text"
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="e.g. Royal Banarasi Zari Silk Saree"
                    required
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-4 py-2.5 text-xs outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Category *</label>
                    <select
                      value={formCategoryId}
                      onChange={(e) => setFormCategoryId(e.target.value)}
                      required
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none"
                    >
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Fabric</label>
                    <select
                      value={formFabric}
                      onChange={(e) => setFormFabric(e.target.value)}
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none"
                    >
                      {["Silk", "Cotton", "Chiffon", "Georgette", "Linen", "Organza"].map(f => (
                        <option key={f} value={f}>{f}</option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Occasion</label>
                    <select
                      value={formOccasion}
                      onChange={(e) => setFormOccasion(e.target.value)}
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none"
                    >
                      {["Wedding", "Festive", "Casual", "Party", "Formal"].map(o => (
                        <option key={o} value={o}>{o}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Unit Price (INR) *</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={formPrice}
                      onChange={(e) => setFormPrice(e.target.value)}
                      placeholder="12500"
                      required
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none font-bold"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Initial Stock *</label>
                    <input
                      type="number"
                      min="1"
                      value={formStockQuantity}
                      onChange={(e) => setFormStockQuantity(e.target.value)}
                      placeholder="10"
                      required
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none font-bold"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Color</label>
                    <input
                      type="text"
                      value={formColor}
                      onChange={(e) => setFormColor(e.target.value)}
                      placeholder="e.g. Royal Blue"
                      className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl px-3 py-2.5 text-xs outline-none"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">
                    Saree Photos Dropzone
                    <span className="ml-1 font-normal text-[#71817A] normal-case">(up to 6 · JPG/PNG/WebP)</span>
                  </label>
                  <SareePhotoDropzone
                    value={formImages}
                    onChange={setFormImages}
                    maxPhotos={6}
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#111827] tracking-wider block">Craftsmanship Notes / Description</label>
                  <textarea
                    rows={3}
                    value={formDescription}
                    onChange={(e) => setFormDescription(e.target.value)}
                    placeholder="Weave details, zari metallic blend, pallu motifs, artisan guild provenance..."
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#1E6A62] rounded-xl p-3 text-xs outline-none resize-none"
                  />
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={sareeModalLoading}
                    className="w-full h-12 bg-[#17211F] hover:bg-[#1E6A62] text-[#F3C56A] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                  >
                    {sareeModalLoading ? 'Creating Saree & Initializing Inventory...' : 'Save Saree & Initialize Inventory'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
