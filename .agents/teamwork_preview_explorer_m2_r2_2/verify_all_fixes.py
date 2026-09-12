import subprocess
import os

frontend_dir = '/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend'

def check_file(rel_path, new_content):
    proc = subprocess.run(
        ['npx', 'eslint', '--stdin', '--stdin-filename', rel_path],
        cwd=frontend_dir,
        input=new_content,
        text=True,
        capture_output=True
    )
    return proc.returncode, proc.stdout, proc.stderr

results = {}

# 1. ManageInventory.jsx
path1 = 'src/pages/Admin/ManageInventory.jsx'
with open(os.path.join(frontend_dir, path1)) as f:
    c1 = f.read()
c1_fixed = c1.replace(
    "  const [targetWarehouse, setTargetWarehouse] = useState('WH-02 Mumbai West Distribution Hub');\n  const { user } = useSelector((state) => state.auth);",
    "  const [targetWarehouse, setTargetWarehouse] = useState('WH-02 Mumbai West Distribution Hub');\n  const [transferQty, setTransferQty] = useState(5);\n  const [transferReason, setTransferReason] = useState('');\n  const { user } = useSelector((state) => state.auth);"
)
results[path1] = check_file(path1, c1_fixed)

# 2. AiAssistantModal.jsx
path2 = 'src/components/common/AiAssistantModal.jsx'
with open(os.path.join(frontend_dir, path2)) as f:
    c2 = f.read()

# Extract and reposition
old_section = """  useEffect(() => {
    const handleOpenWithPrompt = (e) => {
      setIsOpen(true);
      if (e.detail?.prompt) {
        // Small delay to ensure modal mounts before sending message
        setTimeout(() => {
          handleSendMessage(e.detail.prompt);
        }, 150);
      }
    };
    window.addEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
    return () => window.removeEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
  }, []);

  const handleSendMessage = async (textToSend) => {
    const query = textToSend || inputQuery.trim();
    if (!query || isTyping) return;

    const userMsg = { id: Date.now(), sender: 'user', text: query };
    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputQuery('');
    setIsTyping(true);

    try {
      const response = await sendGeminiMessage(messages, query);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: response.text,
          products: response.products,
          suggestions: response.suggestions,
        },
      ]);
    } catch (err) {
      console.error('Error fetching AI stylist response:', err);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: 'I apologize, I encountered a brief connection issue. Please feel free to rephrase or browse our curated catalog.',
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };"""

new_section = """  const handleSendMessage = async (textToSend) => {
    const query = textToSend || inputQuery.trim();
    if (!query || isTyping) return;

    const userMsg = { id: Date.now(), sender: 'user', text: query };
    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputQuery('');
    setIsTyping(true);

    try {
      const response = await sendGeminiMessage(messages, query);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: response.text,
          products: response.products,
          suggestions: response.suggestions,
        },
      ]);
    } catch (err) {
      console.error('Error fetching AI stylist response:', err);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: 'bot',
          text: 'I apologize, I encountered a brief connection issue. Please feel free to rephrase or browse our curated catalog.',
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleSendMessageRef = useRef(handleSendMessage);
  useEffect(() => {
    handleSendMessageRef.current = handleSendMessage;
  });

  useEffect(() => {
    const handleOpenWithPrompt = (e) => {
      setIsOpen(true);
      if (e.detail?.prompt) {
        // Small delay to ensure modal mounts before sending message
        setTimeout(() => {
          handleSendMessageRef.current?.(e.detail.prompt);
        }, 150);
      }
    };
    window.addEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
    return () => window.removeEventListener('sareekart:open-ai-stylist', handleOpenWithPrompt);
  }, []);"""

c2_fixed = c2.replace(old_section, new_section)
results[path2] = check_file(path2, c2_fixed)

# 3. AnalyticsDashboard.jsx
path3 = 'src/pages/Admin/AnalyticsDashboard.jsx'
with open(os.path.join(frontend_dir, path3)) as f:
    c3 = f.read()

target3 = """  // Calculate donut segments
  let cumulativeAngle = 0;
  const segments = distribution.map((item, idx) => {
    const amount = Number(item.amount) || 0;
    const fraction = totalAmount > 0 ? amount / totalAmount : 0;
    const angle = fraction * 360;
    const startAngle = cumulativeAngle;
    cumulativeAngle += angle;

    return {
      ...item,
      color: colors[idx % colors.length],
      fraction,
      startAngle,
      angle,
    };
  });"""

replacement3 = """  // Calculate donut segments
  let cumulativeAngle = 0;
  const segments = [];
  for (let idx = 0; idx < distribution.length; idx++) {
    const item = distribution[idx];
    const amount = Number(item.amount) || 0;
    const fraction = totalAmount > 0 ? amount / totalAmount : 0;
    const angle = fraction * 360;
    const startAngle = cumulativeAngle;
    cumulativeAngle += angle;

    segments.push({
      ...item,
      color: colors[idx % colors.length],
      fraction,
      startAngle,
      angle,
    });
  }"""
c3_fixed = c3.replace(target3, replacement3)
results[path3] = check_file(path3, c3_fixed)

# 4. TrackOrderPage.jsx
path4 = 'src/pages/Orders/TrackOrderPage.jsx'
with open(os.path.join(frontend_dir, path4)) as f:
    c4 = f.read()

target4 = """  // Auto-search if URL parameters are provided
  useEffect(() => {
    const urlOrder = searchParams.get('orderId') || params.id;
    const urlAwb = searchParams.get('awb');
    const urlContact = searchParams.get('contact');

    if (urlAwb) {
      setActiveTab('awb');
      setAwbInput(urlAwb);
      fetchTrackingData({ trackingNumber: urlAwb });
    } else if (urlOrder) {
      setActiveTab('orderId');
      setOrderInput(urlOrder);
      if (urlContact) setContactInput(urlContact);
      fetchTrackingData({ orderId: urlOrder.replace(/\D/g, ''), contact: urlContact });
    }
  }, [searchParams, params]);

  const fetchTrackingData = async (paramsObj) => {
    setLoading(true);
    setError(null);
    try {
      const queryParams = new URLSearchParams();
      if (paramsObj.orderId) queryParams.set('orderId', paramsObj.orderId);
      if (paramsObj.trackingNumber) queryParams.set('trackingNumber', paramsObj.trackingNumber);
      if (paramsObj.contact) queryParams.set('contact', paramsObj.contact);

      const res = await api.get(`/orders/track?${queryParams.toString()}`);
      if (res.data?.success && res.data?.data) {
        setOrderData(res.data.data);
      } else {
        throw new Error(res.data?.message || 'No tracking information found.');
      }
    } catch (err) {
      console.error('Tracking fetch error:', err);
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Unable to locate order with provided details. Please check your Order ID or Courier AWB.';
      setError(msg);
      setOrderData(null);
    } finally {
      setLoading(false);
    }
  };"""

replacement4 = """  const fetchTrackingData = async (paramsObj) => {
    setLoading(true);
    setError(null);
    try {
      const queryParams = new URLSearchParams();
      if (paramsObj.orderId) queryParams.set('orderId', paramsObj.orderId);
      if (paramsObj.trackingNumber) queryParams.set('trackingNumber', paramsObj.trackingNumber);
      if (paramsObj.contact) queryParams.set('contact', paramsObj.contact);

      const res = await api.get(`/orders/track?${queryParams.toString()}`);
      if (res.data?.success && res.data?.data) {
        setOrderData(res.data.data);
      } else {
        throw new Error(res.data?.message || 'No tracking information found.');
      }
    } catch (err) {
      console.error('Tracking fetch error:', err);
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Unable to locate order with provided details. Please check your Order ID or Courier AWB.';
      setError(msg);
      setOrderData(null);
    } finally {
      setLoading(false);
    }
  };

  // Auto-search if URL parameters are provided
  useEffect(() => {
    const urlOrder = searchParams.get('orderId') || params.id;
    const urlAwb = searchParams.get('awb');
    const urlContact = searchParams.get('contact');

    if (urlAwb) {
      setActiveTab('awb');
      setAwbInput(urlAwb);
      fetchTrackingData({ trackingNumber: urlAwb });
    } else if (urlOrder) {
      setActiveTab('orderId');
      setOrderInput(urlOrder);
      if (urlContact) setContactInput(urlContact);
      fetchTrackingData({ orderId: urlOrder.replace(/\D/g, ''), contact: urlContact });
    }
  }, [searchParams, params]);"""

c4_fixed = c4.replace(target4, replacement4)
results[path4] = check_file(path4, c4_fixed)

# 5. ProductDetailPage.jsx
path5 = 'src/pages/ProductDetails/ProductDetailPage.jsx'
with open(os.path.join(frontend_dir, path5)) as f:
    c5 = f.read()

target5 = """    const firstDigit = cleanPin[0];
    let days = 3;
    let locationLabel = 'Standard Delivery';
    if (['5', '6'].includes(firstDigit)) {"""

replacement5 = """    const firstDigit = cleanPin[0];
    let days;
    let locationLabel;
    if (['5', '6'].includes(firstDigit)) {"""

c5_fixed = c5.replace(target5, replacement5)
results[path5] = check_file(path5, c5_fixed)

# 6. invoiceService.js
path6 = 'src/services/invoiceService.js'
with open(os.path.join(frontend_dir, path6)) as f:
    c6 = f.read()

c6_fixed = c6.replace(r'[^;\"]+', r'[^;"]+')
c6_fixed = c6_fixed.replace(
    "throw new Error(err.response?.data?.message || err.message || 'Could not download invoice');",
    "throw new Error(err.response?.data?.message || err.message || 'Could not download invoice', { cause: err });"
)
results[path6] = check_file(path6, c6_fixed)

# 7. cross-browser-booking.spec.js
path7 = 'tests/cross-browser-booking.spec.js'
with open(os.path.join(frontend_dir, path7)) as f:
    c7 = f.read()

c7_fixed = c7.replace('let bookedOrderId = null;', 'let bookedOrderId;')
results[path7] = check_file(path7, c7_fixed)

for path, (code, stdout, stderr) in results.items():
    print(f"=== {path} ===")
    print(f"Exit code: {code}")
    if "error" in stdout:
        print("STDOUT ERRORS:")
        for line in stdout.splitlines():
            if "error" in line:
                print(" ", line)
    else:
        print("NO ERRORS")
