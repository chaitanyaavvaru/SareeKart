import React, { useState } from 'react';
import { FileSpreadsheet, Download, Upload, CheckCircle2, AlertTriangle, AlertCircle, ArrowRight, Layers, Database, ShieldAlert } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import { motion } from 'framer-motion';

export default function ExcelTransactionCenter() {
  const [activeTab, setActiveTab] = useState('sales');
  const [selectedFile, setSelectedFile] = useState(null);
  const [validationResult, setValidationResult] = useState(null);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [statusMsg, setStatusMsg] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  // Mode Options
  const [completedSalesMode, setCompletedSalesMode] = useState(true);
  const [stockAlreadyDeducted, setStockAlreadyDeducted] = useState(false);
  const [confirmSupplierReceipt, setConfirmSupplierReceipt] = useState(false);

  const handleDownloadTemplate = async () => {
    try {
      const response = await api.get(`/excel/templates/${activeTab}`, {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `SareeKart_${activeTab}_template.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      console.error('Download template failed', err);
      setErrorMsg('Failed to download template. Please try again.');
    }
  };

  const [dragging, setDragging] = useState(false);

  const processFile = async (file) => {
    if (!file) return;

    setSelectedFile(file);
    setValidationResult(null);
    setStatusMsg(null);
    setErrorMsg(null);
    setValidating(true);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', activeTab);

    try {
      const res = await api.post('/excel/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.data?.success) {
        setValidationResult(res.data.data);
      }
    } catch (err) {
      console.error('Validation failed', err);
      setErrorMsg(err.response?.data?.message || err.response?.data?.error || 'Error validating spreadsheet');
    }
    setValidating(false);
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) processFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) processFile(file);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragging(true);
  };

  const handleDragLeave = () => {
    setDragging(false);
  };

  const handleImportSubmit = async () => {
    if (!selectedFile || !validationResult?.valid) return;

    setImporting(true);
    setStatusMsg(null);
    setErrorMsg(null);

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('type', activeTab);
    formData.append('mode', completedSalesMode ? 'COMPLETED_SALES' : 'NEW_ORDERS');
    formData.append('stockAlreadyDeducted', stockAlreadyDeducted.toString());
    formData.append('confirmReceipt', confirmSupplierReceipt.toString());

    try {
      const res = await api.post('/excel/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.data?.success) {
        setStatusMsg(res.data.message || 'Batch validated and queued for Owner Approval.');
        setSelectedFile(null);
        setValidationResult(null);
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to submit batch import');
    }
    setImporting(false);
  };

  return (
    <div className="space-y-6 font-sans text-left text-[#17211F]">
      <SEO
        title="Excel (.xlsx) Transaction Engine | SareeKart Admin"
        description="Dual-mode spreadsheet batch import engine for sales, stock adjustments, customer bills, and supplier purchase records."
        noindex={true}
      />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#17211F] text-[#F3C56A] text-xs font-bold uppercase tracking-wider rounded-full">
            <FileSpreadsheet className="w-3.5 h-3.5" /> Batch Excel Engine
          </div>
          <h1 className="text-2xl sm:text-3xl font-serif font-bold text-[#17211F] mt-2">
            Spreadsheet Transactions
          </h1>
          <p className="text-xs text-[#71817A] mt-1">
            Strict row validation, blank cell rejection, and dual-mode transaction ingestion with atomic rollback.
          </p>
        </div>

        <button
          onClick={handleDownloadTemplate}
          className="flex items-center gap-2 px-4 py-2.5 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#17211F] border border-[#DDD8CF] rounded-full text-xs font-bold uppercase tracking-wider transition cursor-pointer shadow-xs"
        >
          <Download className="w-4 h-4 text-[#1E6A62]" /> Download {activeTab.replace('_', ' ')} Template (.xlsx)
        </button>
      </div>

      {/* Alerts */}
      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" /> {errorMsg}
        </div>
      )}
      {statusMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" /> {statusMsg}
        </div>
      )}

      {/* Category Tabs */}
      <div className="flex gap-2 overflow-x-auto border-b border-[#DDD8CF] pb-2">
        {[
          { id: 'sales', label: 'Sales Records' },
          { id: 'stock', label: 'Stock Adjustments' },
          { id: 'customer_bill', label: 'Customer Bills' },
          { id: 'supplier_bill', label: 'Supplier Bills' }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => {
              setActiveTab(tab.id);
              setSelectedFile(null);
              setValidationResult(null);
            }}
            className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer whitespace-nowrap ${
              activeTab === tab.id
                ? 'bg-[#17211F] text-[#F3C56A]'
                : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Configuration Mode Box */}
      <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 space-y-4">
        <h3 className="text-sm font-bold text-[#17211F] uppercase tracking-wider">Batch Ingestion Mode & Rules</h3>

        {activeTab === 'sales' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <label className="flex items-start gap-3 p-4 bg-[#F7F4EE] border border-[#E8E2D9] rounded-2xl cursor-pointer">
              <input
                type="radio"
                name="salesMode"
                checked={!completedSalesMode}
                onChange={() => setCompletedSalesMode(false)}
                className="mt-0.5"
              />
              <div>
                <strong className="block text-[#17211F]">New Orders Mode</strong>
                <span className="text-[#71817A] leading-relaxed">
                  Creates unfulfilled customer orders and automatically reserves physical warehouse stock upon approval.
                </span>
              </div>
            </label>

            <label className="flex items-start gap-3 p-4 bg-[#F7F4EE] border border-[#E8E2D9] rounded-2xl cursor-pointer">
              <input
                type="radio"
                name="salesMode"
                checked={completedSalesMode}
                onChange={() => setCompletedSalesMode(true)}
                className="mt-0.5"
              />
              <div>
                <strong className="block text-[#17211F]">Completed Sales Mode</strong>
                <span className="text-[#71817A] leading-relaxed">
                  Ingests historical offline boutique or exhibition transactions.
                </span>
                {completedSalesMode && (
                  <div className="mt-2 pt-2 border-t border-[#DDD8CF] flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="deductToggle"
                      checked={stockAlreadyDeducted}
                      onChange={(e) => setStockAlreadyDeducted(e.target.checked)}
                    />
                    <label htmlFor="deductToggle" className="font-bold text-[#1E6A62]">
                      Stock was already deducted physically (do not deduct again)
                    </label>
                  </div>
                )}
              </div>
            </label>
          </div>
        )}

        {activeTab === 'supplier_bill' && (
          <div className="p-4 bg-[#F7F4EE] border border-[#E8E2D9] rounded-2xl text-xs space-y-2">
            <strong className="block text-[#17211F]">Supplier Receipt Protocol</strong>
            <p className="text-[#71817A]">
              Supplier invoices record vendor payouts. Select whether goods are physically confirmed in warehouse.
            </p>
            <div className="flex items-center gap-2 pt-1">
              <input
                type="checkbox"
                id="supplierReceipt"
                checked={confirmSupplierReceipt}
                onChange={(e) => setConfirmSupplierReceipt(e.target.checked)}
              />
              <label htmlFor="supplierReceipt" className="font-bold text-[#1E6A62]">
                Confirmed physical goods receipt included in this approval (increment stock once on approval)
              </label>
            </div>
          </div>
        )}
      </div>

      {/* Upload Drag-and-Drop Area */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        className={`bg-white border-2 border-dashed rounded-3xl p-8 text-center space-y-4 transition cursor-pointer ${
          dragging
            ? 'border-[#1E6A62] bg-[#E3F0ED] scale-[1.01]'
            : 'border-[#DDD8CF] hover:border-[#1E6A62]'
        }`}
      >
        <Upload className={`w-10 h-10 mx-auto transition ${dragging ? 'text-[#1E6A62] animate-bounce' : 'text-[#1E6A62]'}`} />
        <div className="space-y-1">
          <p className="text-sm font-bold text-[#17211F]">
            {dragging ? 'Drop your spreadsheet here' : 'Choose or drag & drop your completed .xlsx / .csv file'}
          </p>
          <p className="text-xs text-[#71817A]">
            Every column in the template is required. Zero (0) is permitted; blanks and whitespace are blocked.
          </p>
        </div>
        <div>
          <input
            type="file"
            id="excelFileInput"
            accept=".xlsx,.csv"
            onChange={handleFileChange}
            className="hidden"
          />
          <label
            htmlFor="excelFileInput"
            className="inline-block px-5 py-2.5 bg-[#17211F] hover:bg-[#1E6A62] text-white text-xs font-bold uppercase tracking-wider rounded-full cursor-pointer transition shadow-xs"
          >
            {validating ? 'Parsing file...' : 'Select File or Drop Here'}
          </label>
        </div>
        {selectedFile && (
          <p className="text-xs font-mono font-bold text-[#1E6A62]">Selected: {selectedFile.name}</p>
        )}
      </div>

      {/* Validation Result Box */}
      {validationResult && (
        <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 space-y-4 shadow-xs">
          <div className="flex justify-between items-center border-b border-[#F7F4EE] pb-3">
            <h3 className="text-base font-bold font-serif text-[#17211F]">Validation Summary</h3>
            <span className={`px-3 py-1 rounded-full text-xs font-bold ${
              validationResult.valid
                ? 'bg-emerald-100 text-emerald-800'
                : 'bg-rose-100 text-rose-800'
            }`}>
              {validationResult.valid ? 'Ready to Ingest' : 'Batch Rejected (Errors Found)'}
            </span>
          </div>

          <p className="text-xs text-[#71817A]">
            Total rows detected: <strong>{validationResult.totalRows}</strong> | Errors: <strong>{validationResult.errors?.length || 0}</strong>
          </p>

          {/* Errors Table */}
          {!validationResult.valid && (
            <div className="border border-rose-200 rounded-2xl overflow-hidden bg-rose-50/50">
              <table className="w-full text-left text-xs">
                <thead className="bg-rose-100 text-rose-900 font-bold uppercase text-[10px]">
                  <tr>
                    <th className="p-3">Sheet</th>
                    <th className="p-3">Row</th>
                    <th className="p-3">Column</th>
                    <th className="p-3">Validation Reason</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-rose-200 text-rose-800">
                  {validationResult.errors.map((err, idx) => (
                    <tr key={idx}>
                      <td className="p-3 font-mono">{err.sheet}</td>
                      <td className="p-3 font-mono font-bold">Row {err.row}</td>
                      <td className="p-3 font-bold">{err.column}</td>
                      <td className="p-3">{err.reason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Action Submit */}
          {validationResult.valid && (
            <div className="pt-2 flex justify-end">
              <button
                onClick={handleImportSubmit}
                disabled={importing}
                className="px-6 py-3 bg-[#17211F] hover:bg-[#1E6A62] text-white font-bold text-xs uppercase tracking-widest rounded-full transition shadow-md cursor-pointer flex items-center gap-2"
              >
                {importing ? 'Queueing Batch...' : 'Submit Batch for Owner Approval'} <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
