import api from '../api/axiosConfig';

/**
 * Downloads official GST Tax Invoice (PDF or Excel) for an order.
 * Works seamlessly for both customer portal (/orders/{id}/invoice)
 * and admin console (/admin/invoices/{id}).
 */
export async function downloadInvoice(orderId, format = 'PDF', isAdmin = false) {
  const endpoint = isAdmin ? `/admin/invoices/${orderId}` : `/orders/${orderId}/invoice`;

  try {
    const response = await api.get(endpoint, {
      params: { format },
      responseType: 'blob',
    });

    const disposition = response.headers['content-disposition'] || '';
    const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^;"]+)"?/i);
    const ext = format.toLowerCase() === 'excel' || format.toLowerCase() === 'xlsx' ? 'xlsx' : 'pdf';
    const fileName = fileNameMatch
      ? decodeURIComponent(fileNameMatch[1] || fileNameMatch[2])
      : `SareeKart_TaxInvoice_${orderId}.${ext}`;

    const blob = new Blob([response.data], {
      type: format.toUpperCase() === 'PDF'
        ? 'application/pdf'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => window.URL.revokeObjectURL(url), 1000);
  } catch (err) {
    console.error('Invoice download failed:', err);
    throw new Error(err.response?.data?.message || err.message || 'Could not download invoice', { cause: err });
  }
}
