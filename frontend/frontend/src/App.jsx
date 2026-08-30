import { useState, useEffect } from "react";

// ─── MOCK DATA ────────────────────────────────────────────────────────────────
const MOCK_PRODUCTS_ADMIN = [
  { id: 1, name: "Kanchipuram Silk Brocade Mehandi Green Saree", category: "Kanchipuram", fabric: "Silk", price: 42870, stock: 5, status: "Active", sales: 12 },
  { id: 2, name: "Paithani Silk Butta Bottle Green With Akruthi Border", category: "Paithani", fabric: "Silk", price: 152500, stock: 2, status: "Active", sales: 4 },
  { id: 3, name: "Kanchipuram Silk Horizontal Lines Yellow Saree", category: "Kanchipuram", fabric: "Silk", price: 46450, stock: 0, status: "Out of Stock", sales: 18 },
  { id: 4, name: "Classic Kanchipuram Silk Plain Black Saree", category: "Kanchipuram", fabric: "Silk", price: 33680, stock: 8, status: "Active", sales: 9 },
  { id: 5, name: "Uppada Silk Jaal Peacock Blue Saree", category: "Uppada", fabric: "Silk", price: 38400, stock: 3, status: "Active", sales: 7 },
  { id: 6, name: "Banarasi Silk Bridal Red Saree", category: "Banarasi", fabric: "Silk", price: 65000, stock: 1, status: "Low Stock", sales: 22 },
  { id: 7, name: "Tussar Embroidery Pastel Green Saree", category: "Tussar", fabric: "Silk", price: 26510, stock: 11, status: "Active", sales: 5 },
  { id: 8, name: "Pochampally Ikat Cotton Saree", category: "Pochampally", fabric: "Cotton", price: 8500, stock: 20, status: "Active", sales: 31 },
];

const MOCK_ORDERS_ADMIN = [
  { id: "ORD-1001", customer: "Priya Reddy", product: "Kanchipuram Silk Brocade Mehandi Green", amount: 42870, status: "Delivered", date: "12 Jun 2025", city: "Hyderabad" },
  { id: "ORD-1002", customer: "Anitha Sharma", product: "Banarasi Silk Bridal Red Saree", amount: 65000, status: "Processing", date: "14 Jun 2025", city: "Bangalore" },
  { id: "ORD-1003", customer: "Meena Iyer", product: "Pochampally Ikat Cotton Saree", amount: 8500, status: "Shipped", date: "15 Jun 2025", city: "Chennai" },
  { id: "ORD-1004", customer: "Sujatha Rao", product: "Uppada Silk Jaal Peacock Blue", amount: 38400, status: "Pending", date: "15 Jun 2025", city: "Visakhapatnam" },
  { id: "ORD-1005", customer: "Kavitha Nair", product: "Classic Kanchipuram Silk Plain Black", amount: 33680, status: "Delivered", date: "11 Jun 2025", city: "Kochi" },
  { id: "ORD-1006", customer: "Divya Patel", product: "Paithani Silk Butta Bottle Green", amount: 152500, status: "Processing", date: "16 Jun 2025", city: "Mumbai" },
];

const MOCK_CUSTOMERS = [
  { id: 1, name: "Priya Reddy", email: "priya@example.com", phone: "+91 98765 43210", orders: 4, totalSpent: 142800, city: "Hyderabad", joined: "Jan 2025" },
  { id: 2, name: "Anitha Sharma", email: "anitha@example.com", phone: "+91 87654 32109", orders: 2, totalSpent: 98000, city: "Bangalore", joined: "Mar 2025" },
  { id: 3, name: "Meena Iyer", email: "meena@example.com", phone: "+91 76543 21098", orders: 7, totalSpent: 61000, city: "Chennai", joined: "Feb 2025" },
  { id: 4, name: "Sujatha Rao", email: "sujatha@example.com", phone: "+91 65432 10987", orders: 1, totalSpent: 38400, city: "Vizag", joined: "May 2025" },
  { id: 5, name: "Kavitha Nair", email: "kavitha@example.com", phone: "+91 54321 09876", orders: 5, totalSpent: 185000, city: "Kochi", joined: "Dec 2024" },
];

const CHART_DATA = [
  { month: "Jan", revenue: 128000, orders: 18 },
  { month: "Feb", revenue: 195000, orders: 27 },
  { month: "Mar", revenue: 164000, orders: 23 },
  { month: "Apr", revenue: 210000, orders: 31 },
  { month: "May", revenue: 248000, orders: 38 },
  { month: "Jun", revenue: 186000, orders: 28 },
];

const STORE_PRODUCTS = [
  { id: 1, name: "Banarasi Silk Saree", price: 4999, category: "Banarasi", image: "https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250", badge: "Bestseller" },
  { id: 2, name: "Taranga Kanchi Silk Tissue Brocade Gold Saree", price:26133, category: "Kanchipuram", image: "https://kankatala.com/cdn/shop/files/1215863175_2.webp?v=1761908736", badge: "New" },  
  { id: 3, name: "Venkatagiri Cotton Butta Black Saree With Jamdani Pallu", price: 18667, category: "Designer", image: "https://kankatala.com/cdn/shop/files/1216423158_1.webp?v=1780134877", badge: "20% OFF" },
  { id: 4, name: "Pochampally Silk Ikat Purple Saree", price: 34111, category: "Pochampally", image: "https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134", badge: "" },
  { id: 5, name: "Uppada Silk Saree", price: 38400, category: "Uppada", image: "https://kankatala.com/cdn/shop/files/1216039129_1.webp?v=1777711821", badge: "Premium" },
  { id: 6, name: "Tussar Silk Saree", price: 26510, category: "Tussar", image: "https://kankatala.com/cdn/shop/files/1215745847_1.webp?v=1777723358&width=1946", badge: "" },
];

const NAV_SECTIONS = {
  home: null,
  collections: ["Kanchipuram", "Banarasi", "Uppada", "Paithani", "Pochampally", "Gadwal", "Tussar", "Organza"],
  wedding: ["Bridal Sarees", "Silk Blends", "Zari Work", "Temple Border", "Heavy Embroidery", "Gold Weave"],
  "new arrivals": ["Just In", "Pre-order", "Limited Edition", "Festival Collection", "Summer 2026"],
};

const fmt = (n) => "₹" + Number(n).toLocaleString("en-IN");

// ─── ADMIN HELPERS ────────────────────────────────────────────────────────────
function StatusBadge({ status }) {
  const map = {
    "Active": { bg: "#e8f5e9", color: "#2e7d32" },
    "Out of Stock": { bg: "#fce4ec", color: "#c62828" },
    "Low Stock": { bg: "#fff3e0", color: "#e65100" },
    "Delivered": { bg: "#e8f5e9", color: "#2e7d32" },
    "Shipped": { bg: "#e3f2fd", color: "#1565c0" },
    "Processing": { bg: "#fff3e0", color: "#e65100" },
    "Pending": { bg: "#f3e5f5", color: "#6a1b9a" },
  };
  const s = map[status] || { bg: "#f5f5f5", color: "#555" };
  return (
    <span style={{ background: s.bg, color: s.color, padding: "3px 10px", borderRadius: 20, fontSize: 11, fontWeight: 700, letterSpacing: "0.04em", whiteSpace: "nowrap" }}>{status}</span>
  );
}

function StatCard({ icon, label, value, sub, color }) {
  return (
    <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, padding: "20px 24px", display: "flex", alignItems: "flex-start", gap: 16 }}>
      <div style={{ width: 48, height: 48, borderRadius: 8, background: color + "18", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 22, flexShrink: 0 }}>{icon}</div>
      <div>
        <div style={{ fontSize: 12, color: "#888", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase", marginBottom: 4 }}>{label}</div>
        <div style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", lineHeight: 1 }}>{value}</div>
        {sub && <div style={{ fontSize: 12, color: "#aaa", marginTop: 4 }}>{sub}</div>}
      </div>
    </div>
  );
}

function MiniBarChart({ data }) {
  const max = Math.max(...data.map(d => d.revenue));
  return (
    <div style={{ display: "flex", alignItems: "flex-end", gap: 8, height: 120, padding: "0 4px" }}>
      {data.map((d, i) => (
        <div key={i} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
          <div style={{ fontSize: 10, color: "#888" }}>{fmt(d.revenue / 1000)}k</div>
          <div style={{ width: "100%", height: Math.round((d.revenue / max) * 80), background: "linear-gradient(180deg, #c9a227, #8b5e00)", borderRadius: "3px 3px 0 0" }} />
          <div style={{ fontSize: 10, color: "#888" }}>{d.month}</div>
        </div>
      ))}
    </div>
  );
}

// ─── ADMIN PAGES ──────────────────────────────────────────────────────────────
function Dashboard() {
  return (
    <div>
      <div style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Dashboard</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>Welcome back, Admin · Tuesday, 16 June 2026</p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 28 }}>
        <StatCard icon="💰" label="Total Revenue" value="₹11,31,550" sub="+18% this month" color="#c9a227" />
        <StatCard icon="📦" label="Total Orders" value="165" sub="28 this month" color="#1565c0" />
        <StatCard icon="🪡" label="Products" value="8" sub="1 out of stock" color="#2e7d32" />
        <StatCard icon="👥" label="Customers" value="5" sub="2 new this month" color="#6a1b9a" />
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 28 }}>
        <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, padding: 24 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
            <div>
              <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00" }}>Revenue Overview</div>
              <div style={{ fontSize: 12, color: "#aaa" }}>Last 6 months</div>
            </div>
            <span style={{ fontSize: 11, color: "#c9a227", fontWeight: 700 }}>↑ 18% vs last period</span>
          </div>
          <MiniBarChart data={CHART_DATA} />
        </div>
        <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, padding: 24 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00", marginBottom: 16 }}>Top Selling Products</div>
          {[...MOCK_PRODUCTS_ADMIN].sort((a, b) => b.sales - a.sales).slice(0, 5).map((p, i) => (
            <div key={p.id} style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}>
              <div style={{ width: 28, height: 28, borderRadius: "50%", background: i === 0 ? "#c9a227" : i === 1 ? "#aaa" : i === 2 ? "#cd7f32" : "#f0ebe2", color: i < 3 ? "#fff" : "#888", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 800, flexShrink: 0 }}>{i + 1}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12, fontWeight: 600, color: "#1a0a00", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.name}</div>
                <div style={{ fontSize: 11, color: "#aaa" }}>{p.sales} sold · {fmt(p.price)}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, padding: 24 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00" }}>Recent Orders</div>
          <span style={{ fontSize: 12, color: "#c9a227", cursor: "pointer", fontWeight: 600 }}>View all →</span>
        </div>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: "1px solid #f0ebe2" }}>
              {["Order ID", "Customer", "Product", "Amount", "Status", "Date"].map(h => (
                <th key={h} style={{ textAlign: "left", padding: "8px 12px", color: "#888", fontWeight: 600, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {MOCK_ORDERS_ADMIN.slice(0, 5).map(o => (
              <tr key={o.id} style={{ borderBottom: "1px solid #faf9f7" }}>
                <td style={{ padding: "10px 12px", color: "#c9a227", fontWeight: 700 }}>{o.id}</td>
                <td style={{ padding: "10px 12px", fontWeight: 600 }}>{o.customer}</td>
                <td style={{ padding: "10px 12px", color: "#555", maxWidth: 200, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{o.product}</td>
                <td style={{ padding: "10px 12px", fontWeight: 700 }}>{fmt(o.amount)}</td>
                <td style={{ padding: "10px 12px" }}><StatusBadge status={o.status} /></td>
                <td style={{ padding: "10px 12px", color: "#888" }}>{o.date}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Products({ onAdd }) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("All");
  const [products, setProducts] = useState(MOCK_PRODUCTS_ADMIN);
  const statuses = ["All", "Active", "Low Stock", "Out of Stock"];
  const filtered = products.filter(p => (filter === "All" || p.status === filter) && p.name.toLowerCase().includes(search.toLowerCase()));
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Products</h1>
          <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>{products.length} products total</p>
        </div>
        <button onClick={onAdd} style={{ background: "#1a0a00", color: "#d4a855", border: "none", padding: "10px 22px", borderRadius: 4, cursor: "pointer", fontWeight: 700, fontSize: 13 }}>+ Add Product</button>
      </div>
      <div style={{ display: "flex", gap: 12, marginBottom: 20, alignItems: "center" }}>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search products..." style={{ padding: "9px 16px", border: "1.5px solid #e0d8cc", borderRadius: 4, fontSize: 13, width: 280, fontFamily: "inherit", outline: "none", background: "#faf9f7" }} />
        <div style={{ display: "flex", gap: 6 }}>
          {statuses.map(s => (
            <button key={s} onClick={() => setFilter(s)} style={{ padding: "8px 16px", borderRadius: 4, border: "1.5px solid", borderColor: filter === s ? "#1a0a00" : "#e0d8cc", background: filter === s ? "#1a0a00" : "#fff", color: filter === s ? "#d4a855" : "#555", fontSize: 12, fontWeight: 600, cursor: "pointer" }}>{s}</button>
          ))}
        </div>
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "#faf9f7", borderBottom: "1px solid #f0ebe2" }}>
              {["Product", "Category", "Price", "Stock", "Sales", "Status", "Actions"].map(h => (
                <th key={h} style={{ textAlign: "left", padding: "12px 16px", color: "#888", fontWeight: 700, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map((p, i) => (
              <tr key={p.id} style={{ borderBottom: "1px solid #faf9f7", background: i % 2 === 0 ? "#fff" : "#fdfcfb" }}>
                <td style={{ padding: "12px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div style={{ width: 40, height: 50, borderRadius: 3, background: "linear-gradient(160deg, #8b5e00, #1a0a00)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, flexShrink: 0 }}>🥻</div>
                    <div>
                      <div style={{ fontWeight: 600, color: "#1a1a1a", maxWidth: 260, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.name}</div>
                      <div style={{ fontSize: 11, color: "#aaa" }}>{p.fabric}</div>
                    </div>
                  </div>
                </td>
                <td style={{ padding: "12px 16px", color: "#555" }}>{p.category}</td>
                <td style={{ padding: "12px 16px", fontWeight: 700, color: "#1a0a00" }}>{fmt(p.price)}</td>
                <td style={{ padding: "12px 16px" }}><span style={{ color: p.stock === 0 ? "#c62828" : p.stock <= 2 ? "#e65100" : "#2e7d32", fontWeight: 700 }}>{p.stock}</span></td>
                <td style={{ padding: "12px 16px", color: "#555" }}>{p.sales}</td>
                <td style={{ padding: "12px 16px" }}><StatusBadge status={p.status} /></td>
                <td style={{ padding: "12px 16px" }}>
                  <div style={{ display: "flex", gap: 6 }}>
                    <button style={{ background: "#f0f7ff", color: "#1565c0", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>Edit</button>
                    <button onClick={() => setProducts(ps => ps.filter(x => x.id !== p.id))} style={{ background: "#fce4ec", color: "#c62828", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filtered.length === 0 && <div style={{ textAlign: "center", padding: 40, color: "#aaa" }}>No products found</div>}
      </div>
    </div>
  );
}

const inputStyle = { width: "100%", padding: "9px 12px", border: "1.5px solid #e0d8cc", borderRadius: 4, fontSize: 13, fontFamily: "inherit", outline: "none", background: "#faf9f7", boxSizing: "border-box" };

const Field = ({ label, name, type = "text", options, form, set }) => (
  <div style={{ marginBottom: 16 }}>
    <label style={{ fontSize: 12, fontWeight: 700, color: "#555", textTransform: "uppercase", letterSpacing: "0.06em", display: "block", marginBottom: 6 }}>{label}</label>
    {options ? (
      <select value={form[name]} onChange={e => set(name, e.target.value)} style={inputStyle}>
        {options.map(o => <option key={o}>{o}</option>)}
      </select>
    ) : type === "textarea" ? (
      <textarea value={form[name]} onChange={e => set(name, e.target.value)} rows={3} style={{ ...inputStyle, resize: "vertical" }} />
    ) : (
      <input type={type} value={form[name]} onChange={e => set(name, e.target.value)} style={inputStyle} />
    )}
    </div>
  );

function AddProductModal({ onClose }) {
  const [form, setForm] = useState({ name: "", category: "Kanchipuram", fabric: "Silk", price: "", stock: "", occasion: "Wedding", description: "" });
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", zIndex: 2000, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div style={{ background: "#fff", borderRadius: 8, width: 560, maxHeight: "90vh", overflow: "auto", boxShadow: "0 24px 64px rgba(0,0,0,0.2)" }}>
        <div style={{ padding: "20px 28px", borderBottom: "1px solid #f0ebe2", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div style={{ fontSize: 18, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif" }}>Add New Product</div>
          <button onClick={onClose} style={{ background: "none", border: "none", fontSize: 20, cursor: "pointer", color: "#888" }}>✕</button>
        </div>
        <div style={{ padding: "24px 28px" }}>
          <Field label="Product Name" name="name" form={form} set={set} />
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            <Field label="Category / Weave" name="category" options={["Kanchipuram", "Banarasi", "Uppada", "Paithani", "Pochampally", "Gadwal", "Tussar", "Organza"]} form={form} set={set} />
            <Field label="Fabric" name="fabric" options={["Silk", "Cotton", "Organza", "Linen", "Tussar"]} form={form} set={set} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            <Field label="Price (₹)" name="price" type="number" form={form} set={set} />
            <Field label="Stock Quantity" name="stock" type="number" form={form} set={set} />
          </div>
          <Field label="Occasion" name="occasion" options={["Wedding", "Festive", "Casual", "Formal", "Party"]} form={form} set={set} />
          <Field label="Description" name="description" type="textarea" form={form} set={set} />
          <div style={{ marginBottom: 16 }}>
            <label style={{ fontSize: 12, fontWeight: 700, color: "#555", textTransform: "uppercase", letterSpacing: "0.06em", display: "block", marginBottom: 6 }}>Product Image</label>
            <div style={{ border: "2px dashed #e0d8cc", borderRadius: 6, padding: "24px", textAlign: "center", color: "#aaa", cursor: "pointer", background: "#faf9f7" }}>
              <div style={{ fontSize: 28 }}>📸</div>
              <div style={{ fontSize: 13, marginTop: 8 }}>Click to upload or drag & drop</div>
              <div style={{ fontSize: 11, marginTop: 4 }}>PNG, JPG up to 5MB</div>
            </div>
          </div>
        </div>
        <div style={{ padding: "16px 28px", borderTop: "1px solid #f0ebe2", display: "flex", gap: 12, justifyContent: "flex-end" }}>
          <button onClick={onClose} style={{ padding: "10px 24px", border: "1.5px solid #e0d8cc", borderRadius: 4, background: "#fff", cursor: "pointer", fontSize: 13, fontWeight: 600 }}>Cancel</button>
          <button onClick={onClose} style={{ padding: "10px 24px", background: "#1a0a00", color: "#d4a855", border: "none", borderRadius: 4, cursor: "pointer", fontSize: 13, fontWeight: 700 }}>Save Product</button>
        </div>
      </div>
    </div>
  );
}

function Orders() {
  const [filter, setFilter] = useState("All");
  const statuses = ["All", "Pending", "Processing", "Shipped", "Delivered"];
  const filtered = MOCK_ORDERS_ADMIN.filter(o => filter === "All" || o.status === filter);
  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Orders</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>{MOCK_ORDERS_ADMIN.length} total orders</p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 24 }}>
        {[
          { label: "Pending", count: MOCK_ORDERS_ADMIN.filter(o => o.status === "Pending").length, color: "#6a1b9a", bg: "#f3e5f5" },
          { label: "Processing", count: MOCK_ORDERS_ADMIN.filter(o => o.status === "Processing").length, color: "#e65100", bg: "#fff3e0" },
          { label: "Shipped", count: MOCK_ORDERS_ADMIN.filter(o => o.status === "Shipped").length, color: "#1565c0", bg: "#e3f2fd" },
          { label: "Delivered", count: MOCK_ORDERS_ADMIN.filter(o => o.status === "Delivered").length, color: "#2e7d32", bg: "#e8f5e9" },
        ].map(s => (
          <div key={s.label} style={{ background: s.bg, borderRadius: 6, padding: "16px 20px" }}>
            <div style={{ fontSize: 28, fontWeight: 800, color: s.color }}>{s.count}</div>
            <div style={{ fontSize: 12, color: s.color, fontWeight: 600 }}>{s.label}</div>
          </div>
        ))}
      </div>
      <div style={{ display: "flex", gap: 6, marginBottom: 16 }}>
        {statuses.map(s => (
          <button key={s} onClick={() => setFilter(s)} style={{ padding: "8px 16px", borderRadius: 4, border: "1.5px solid", borderColor: filter === s ? "#1a0a00" : "#e0d8cc", background: filter === s ? "#1a0a00" : "#fff", color: filter === s ? "#d4a855" : "#555", fontSize: 12, fontWeight: 600, cursor: "pointer" }}>{s}</button>
        ))}
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "#faf9f7", borderBottom: "1px solid #f0ebe2" }}>
              {["Order ID", "Customer", "City", "Product", "Amount", "Status", "Date", "Action"].map(h => (
                <th key={h} style={{ textAlign: "left", padding: "12px 16px", color: "#888", fontWeight: 700, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map((o, i) => (
              <tr key={o.id} style={{ borderBottom: "1px solid #faf9f7", background: i % 2 === 0 ? "#fff" : "#fdfcfb" }}>
                <td style={{ padding: "12px 16px", color: "#c9a227", fontWeight: 700 }}>{o.id}</td>
                <td style={{ padding: "12px 16px", fontWeight: 600 }}>{o.customer}</td>
                <td style={{ padding: "12px 16px", color: "#888" }}>{o.city}</td>
                <td style={{ padding: "12px 16px", color: "#555", maxWidth: 180, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{o.product}</td>
                <td style={{ padding: "12px 16px", fontWeight: 700 }}>{fmt(o.amount)}</td>
                <td style={{ padding: "12px 16px" }}><StatusBadge status={o.status} /></td>
                <td style={{ padding: "12px 16px", color: "#888" }}>{o.date}</td>
                <td style={{ padding: "12px 16px" }}><button style={{ background: "#f0f7ff", color: "#1565c0", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>View</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Customers() {
  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Customers</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>{MOCK_CUSTOMERS.length} registered customers</p>
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "#faf9f7", borderBottom: "1px solid #f0ebe2" }}>
              {["Customer", "Contact", "City", "Orders", "Total Spent", "Joined", "Actions"].map(h => (
                <th key={h} style={{ textAlign: "left", padding: "12px 16px", color: "#888", fontWeight: 700, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {MOCK_CUSTOMERS.map((c, i) => (
              <tr key={c.id} style={{ borderBottom: "1px solid #faf9f7", background: i % 2 === 0 ? "#fff" : "#fdfcfb" }}>
                <td style={{ padding: "12px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ width: 36, height: 36, borderRadius: "50%", background: "linear-gradient(135deg, #1a0a00, #6b2f00)", color: "#d4a855", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, fontSize: 14, flexShrink: 0 }}>{c.name[0]}</div>
                    <div style={{ fontWeight: 600 }}>{c.name}</div>
                  </div>
                </td>
                <td style={{ padding: "12px 16px" }}>
                  <div style={{ fontSize: 12, color: "#555" }}>{c.email}</div>
                  <div style={{ fontSize: 11, color: "#aaa" }}>{c.phone}</div>
                </td>
                <td style={{ padding: "12px 16px", color: "#555" }}>{c.city}</td>
                <td style={{ padding: "12px 16px", fontWeight: 700, color: "#1a0a00" }}>{c.orders}</td>
                <td style={{ padding: "12px 16px", fontWeight: 700, color: "#2e7d32" }}>{fmt(c.totalSpent)}</td>
                <td style={{ padding: "12px 16px", color: "#888" }}>{c.joined}</td>
                <td style={{ padding: "12px 16px" }}><button style={{ background: "#f0f7ff", color: "#1565c0", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>View</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Inventory() {
  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Inventory</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>Track stock levels across all products</p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 16, marginBottom: 24 }}>
        <div style={{ background: "#fce4ec", border: "1px solid #f8bbd0", borderRadius: 6, padding: "16px 20px" }}>
          <div style={{ fontSize: 11, color: "#c62828", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>⚠️ Out of Stock</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: "#c62828" }}>{MOCK_PRODUCTS_ADMIN.filter(p => p.stock === 0).length}</div>
          <div style={{ fontSize: 12, color: "#c62828" }}>products need restocking</div>
        </div>
        <div style={{ background: "#fff3e0", border: "1px solid #ffe0b2", borderRadius: 6, padding: "16px 20px" }}>
          <div style={{ fontSize: 11, color: "#e65100", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>⚡ Low Stock</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: "#e65100" }}>{MOCK_PRODUCTS_ADMIN.filter(p => p.stock > 0 && p.stock <= 3).length}</div>
          <div style={{ fontSize: 12, color: "#e65100" }}>products running low</div>
        </div>
        <div style={{ background: "#e8f5e9", border: "1px solid #c8e6c9", borderRadius: 6, padding: "16px 20px" }}>
          <div style={{ fontSize: 11, color: "#2e7d32", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>✅ In Stock</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: "#2e7d32" }}>{MOCK_PRODUCTS_ADMIN.filter(p => p.stock > 3).length}</div>
          <div style={{ fontSize: 12, color: "#2e7d32" }}>products well stocked</div>
        </div>
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "#faf9f7", borderBottom: "1px solid #f0ebe2" }}>
              {["Product", "Category", "Price", "Stock", "Stock Level", "Status", "Update"].map(h => (
                <th key={h} style={{ textAlign: "left", padding: "12px 16px", color: "#888", fontWeight: 700, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {MOCK_PRODUCTS_ADMIN.map((p, i) => {
              const pct = Math.min(100, Math.round((p.stock / 20) * 100));
              const barColor = p.stock === 0 ? "#ef5350" : p.stock <= 3 ? "#ffa726" : "#66bb6a";
              return (
                <tr key={p.id} style={{ borderBottom: "1px solid #faf9f7", background: i % 2 === 0 ? "#fff" : "#fdfcfb" }}>
                  <td style={{ padding: "12px 16px", fontWeight: 600, maxWidth: 220, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.name}</td>
                  <td style={{ padding: "12px 16px", color: "#555" }}>{p.category}</td>
                  <td style={{ padding: "12px 16px", fontWeight: 700 }}>{fmt(p.price)}</td>
                  <td style={{ padding: "12px 16px", fontWeight: 800, color: barColor }}>{p.stock}</td>
                  <td style={{ padding: "12px 16px", minWidth: 120 }}>
                    <div style={{ background: "#f0ebe2", borderRadius: 4, height: 8, overflow: "hidden" }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: barColor, borderRadius: 4 }} />
                    </div>
                  </td>
                  <td style={{ padding: "12px 16px" }}><StatusBadge status={p.status} /></td>
                  <td style={{ padding: "12px 16px" }}><button style={{ background: "#f0f7ff", color: "#1565c0", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>Update Stock</button></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AdminSettings() {
  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Settings</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>Manage your store configuration</p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
        {[
          { title: "🏪 Store Info", fields: ["Store Name", "Contact Email", "Phone Number", "Address"] },
          { title: "🚚 Shipping", fields: ["Free Shipping Threshold", "Default Delivery Days", "Shipping Zones", "COD Available"] },
          { title: "💳 Payments", fields: ["UPI ID", "Razorpay Key", "PhonePe Number", "COD Limit"] },
          { title: "🔔 Notifications", fields: ["Order Alerts Email", "Low Stock Alert", "WhatsApp Notify", "SMS Alerts"] },
        ].map(section => (
          <div key={section.title} style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, padding: 24 }}>
            <div style={{ fontSize: 15, fontWeight: 800, color: "#1a0a00", marginBottom: 20 }}>{section.title}</div>
            {section.fields.map(field => (
              <div key={field} style={{ marginBottom: 14 }}>
                <label style={{ fontSize: 11, fontWeight: 700, color: "#888", textTransform: "uppercase", letterSpacing: "0.06em", display: "block", marginBottom: 5 }}>{field}</label>
                <input placeholder={`Enter ${field.toLowerCase()}`} style={{ width: "100%", padding: "8px 12px", border: "1.5px solid #e0d8cc", borderRadius: 4, fontSize: 13, fontFamily: "inherit", outline: "none", background: "#faf9f7", boxSizing: "border-box" }} />
              </div>
            ))}
            <button style={{ background: "#1a0a00", color: "#d4a855", border: "none", padding: "9px 20px", borderRadius: 4, cursor: "pointer", fontSize: 12, fontWeight: 700, marginTop: 4 }}>Save Changes</button>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── REFUND ADMIN ───────────────────────────────────────────────────────
function RefundAdmin() {
  const [refunds, setRefunds] = useState([]);
  const [filter, setFilter] = useState("All");
  const [loading, setLoading] = useState(false);

  const statuses = ["All", "PENDING", "SUCCESS", "FAILED"];
  const filtered = filter === "All" ? refunds : refunds.filter(r => r.status === filter);

  const statusColor = {
    "PENDING": { bg: "#fff3e0", color: "#e65100" },
    "SUCCESS": { bg: "#e8f5e9", color: "#2e7d32" },
    "FAILED": { bg: "#fce4ec", color: "#c62828" },
  };

  const loadRefunds = async () => {
    setLoading(true);
    try {
      const res = await fetch("/api/refunds");
      if (res.ok) {
        const data = await res.json();
        setRefunds(data);
      }
    } catch {
      // Use mock data for demo
      setRefunds([
        { id: 1, orderId: "ORD-1002", orderNumber: "ORD-1002", amount: 65000, status: "PENDING", reason: "Customer request", reasonCode: "CUSTOMER_REQUEST", initiatedBy: "admin", createdAt: "15 Jun 2025" },
        { id: 2, orderId: "ORD-1006", orderNumber: "ORD-1006", amount: 152500, status: "SUCCESS", reason: "Out of stock", reasonCode: "OUT_OF_STOCK", initiatedBy: "admin", createdAt: "16 Jun 2025" },
        { id: 3, orderId: "ORD-1004", orderNumber: "ORD-1004", amount: 38400, status: "FAILED", reason: "Gateway error", reasonCode: "OTHER", initiatedBy: "admin", createdAt: "15 Jun 2025" },
        { id: 4, orderId: "ORD-1001", orderNumber: "ORD-1001", amount: 42870, status: "SUCCESS", reason: "Duplicate payment", reasonCode: "DUPLICATE_PAYMENT", initiatedBy: "admin", createdAt: "14 Jun 2025" },
        { id: 5, orderId: "ORD-1003", orderNumber: "ORD-1003", amount: 8500, status: "PENDING", reason: "Customer request", reasonCode: "CUSTOMER_REQUEST", initiatedBy: "admin", createdAt: "16 Jun 2025" },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadRefunds();
  }, []);

  const handleApprove = async (id) => {
    try {
      const res = await fetch(`/api/refunds/${id}/approve`, { method: "POST" });
      if (res.ok) {
        setRefunds(refunds.map(r => r.id === id ? { ...r, status: "SUCCESS" } : r));
      }
    } catch {
      setRefunds(refunds.map(r => r.id === id ? { ...r, status: "SUCCESS" } : r));
    }
  };

  const handleReject = async (id) => {
    try {
      await fetch(`/api/refunds/${id}/reject`, { method: "POST" });
      setRefunds(refunds.map(r => r.id === id ? { ...r, status: "FAILED" } : r));
    } catch {
      setRefunds(refunds.map(r => r.id === id ? { ...r, status: "FAILED" } : r));
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00", fontFamily: "Georgia, serif", margin: 0 }}>Refund Management</h1>
        <p style={{ color: "#888", fontSize: 13, marginTop: 4 }}>Track and manage refund requests</p>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 24 }}>
        {[
          { label: "Total Refunds", count: refunds.length, color: "#1a0a00", bg: "#f7f5f2", icon: "💰" },
          { label: "Pending", count: refunds.filter(r => r.status === "PENDING").length, color: "#e65100", bg: "#fff3e0", icon: "⏳" },
          { label: "Approved", count: refunds.filter(r => r.status === "SUCCESS").length, color: "#2e7d32", bg: "#e8f5e9", icon: "✅" },
          { label: "Failed", count: refunds.filter(r => r.status === "FAILED").length, color: "#c62828", bg: "#fce4ec", icon: "❌" },
        ].map(s => (
          <div key={s.label} style={{ background: s.bg, borderRadius: 6, padding: "16px 20px", border: `1px solid ${s.color}22` }}>
            <div style={{ fontSize: 28 }}>{s.icon}</div>
            <div style={{ fontSize: 24, fontWeight: 800, color: s.color, marginTop: 4 }}>{s.count}</div>
            <div style={{ fontSize: 12, color: "#888", fontWeight: 600 }}>{s.label}</div>
          </div>
        ))}
      </div>
      <div style={{ background: "#fff", border: "1px solid #f0ebe2", borderRadius: 6, overflow: "hidden" }}>
        <div style={{ padding: "12px 16px", borderBottom: "1px solid #f0ebe2", display: "flex", gap: 8, alignItems: "center" }}>
          {statuses.map(s => (
            <button key={s} onClick={() => setFilter(s)} style={{ padding: "6px 14px", borderRadius: 4, border: "1.5px solid", borderColor: filter === s ? "#800020" : "#e0d8cc", background: filter === s ? "#800020" : "#fff", color: filter === s ? "#fff" : "#555", fontSize: 12, fontWeight: 700, cursor: "pointer", textTransform: "capitalize" }}>{s}</button>
          ))}
          <button onClick={loadRefunds} style={{ marginLeft: "auto", background: "#faf9f7", border: "1.5px solid #e0d8cc", padding: "6px 14px", borderRadius: 4, fontSize: 12, cursor: "pointer", fontWeight: 600 }}>🔄 Refresh</button>
        </div>
        {loading ? <div style={{ textAlign: "center", padding: 40, color: "#aaa" }}>Loading refunds...</div> : (
          filtered.length === 0 ? <div style={{ textAlign: "center", padding: 40, color: "#aaa" }}>No refunds found</div> :
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
            <thead>
              <tr style={{ background: "#faf9f7", borderBottom: "1px solid #f0ebe2" }}>
                {["Order", "Amount", "Reason", "Status", "Initiated", "Actions"].map(h => (
                  <th key={h} style={{ textAlign: "left", padding: "12px 16px", color: "#888", fontWeight: 700, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.06em" }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map(r => (
                <tr key={r.id} style={{ borderBottom: "1px solid #faf9f7" }}>
                  <td style={{ padding: "12px 16px", fontWeight: 600 }}>{r.orderNumber}</td>
                  <td style={{ padding: "12px 16px", fontWeight: 700 }}>₹{r.amount?.toLocaleString("en-IN")}</td>
                  <td style={{ padding: "12px 16px", color: "#555", fontSize: 12 }}>{r.reason}</td>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ background: statusColor[r.status]?.bg || "#f5f5f5", color: statusColor[r.status]?.color || "#555", padding: "3px 10px", borderRadius: 20, fontSize: 11, fontWeight: 700 }}>{r.status}</span>
                  </td>
                  <td style={{ padding: "12px 16px", color: "#888", fontSize: 12 }}>{r.createdAt}</td>
                  <td style={{ padding: "12px 16px" }}>
                    {r.status === "PENDING" && (
                      <div style={{ display: "flex", gap: 8 }}>
                        <button onClick={() => handleApprove(r.id)} style={{ background: "#e8f5e9", color: "#2e7d32", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 700 }}>Approve</button>
                        <button onClick={() => handleReject(r.id)} style={{ background: "#fce4ec", color: "#c62828", border: "none", padding: "5px 12px", borderRadius: 4, fontSize: 11, cursor: "pointer", fontWeight: 700 }}>Reject</button>
                      </div>
                    )}
                    {r.status === "FAILED" && <span style={{ fontSize: 11, color: "#c62828" }}>Review</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

const ADMIN_NAV = [
  { id: "dashboard", icon: "📊", label: "Dashboard" },
  { id: "products", icon: "🥻", label: "Products" },
  { id: "orders", icon: "📦", label: "Orders", badge: 2 },
  { id: "customers", icon: "👥", label: "Customers" },
  { id: "inventory", icon: "🏪", label: "Inventory", alert: true },
  { id: "refunds", icon: "💰", label: "Refunds", badge: 1 },
  { id: "settings", icon: "⚙️", label: "Settings" },
];

function AdminPanel({ onGoToStore }) {
  const [page, setPage] = useState("dashboard");
  const [showAddProduct, setShowAddProduct] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const W = sidebarCollapsed ? 64 : 220;
  const renderPage = () => {
    switch (page) {
      case "dashboard": return <Dashboard />;
      case "products": return <Products onAdd={() => setShowAddProduct(true)} />;
      case "orders": return <Orders />;
      case "customers": return <Customers />;
      case "inventory": return <Inventory />;
      case "refunds": return <RefundAdmin />;
      case "settings": return <AdminSettings />;
      default: return <Dashboard />;
    }
  };
  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "#f7f5f2", fontFamily: "'Segoe UI', Arial, sans-serif" }}>
      <aside style={{ width: W, minHeight: "100vh", background: "#1a0a00", display: "flex", flexDirection: "column", transition: "width 0.25s", flexShrink: 0, position: "sticky", top: 0, height: "100vh", overflowY: "auto" }}>
        <div style={{ padding: sidebarCollapsed ? "20px 0" : "20px", borderBottom: "1px solid rgba(212,168,85,0.15)", display: "flex", alignItems: "center", justifyContent: sidebarCollapsed ? "center" : "space-between" }}>
          {!sidebarCollapsed && <div><div style={{ fontSize: 18, fontWeight: 800, color: "#d4a855", fontFamily: "Georgia, serif" }}>SareeKart</div><div style={{ fontSize: 10, color: "rgba(212,168,85,0.5)", letterSpacing: "0.15em", textTransform: "uppercase" }}>Admin Panel</div></div>}
          {sidebarCollapsed && <span style={{ fontSize: 20 }}>🪡</span>}
          {!sidebarCollapsed && <button onClick={() => setSidebarCollapsed(true)} style={{ background: "none", border: "none", color: "rgba(212,168,85,0.5)", cursor: "pointer", fontSize: 16 }}>◀</button>}
        </div>
        {sidebarCollapsed && <button onClick={() => setSidebarCollapsed(false)} style={{ background: "none", border: "none", color: "rgba(212,168,85,0.5)", cursor: "pointer", fontSize: 16, padding: "10px 0", textAlign: "center" }}>▶</button>}
        <nav style={{ flex: 1, padding: "12px 0" }}>
          {ADMIN_NAV.map(item => {
            const active = page === item.id;
            return (
              <button key={item.id} onClick={() => setPage(item.id)} title={sidebarCollapsed ? item.label : ""} style={{ width: "100%", display: "flex", alignItems: "center", gap: 12, padding: sidebarCollapsed ? "13px 0" : "13px 20px", justifyContent: sidebarCollapsed ? "center" : "flex-start", background: active ? "rgba(212,168,85,0.15)" : "none", border: "none", borderLeft: active ? "3px solid #d4a855" : "3px solid transparent", cursor: "pointer", color: active ? "#d4a855" : "rgba(255,255,255,0.6)", fontSize: 13, fontWeight: active ? 700 : 500 }}>
                <span style={{ fontSize: 17 }}>{item.icon}</span>
                {!sidebarCollapsed && <span>{item.label}</span>}
                {item.badge && !sidebarCollapsed && <span style={{ marginLeft: "auto", background: "#c9a227", color: "#1a0a00", borderRadius: 10, fontSize: 10, fontWeight: 800, padding: "2px 7px" }}>{item.badge}</span>}
                {item.alert && !sidebarCollapsed && <span style={{ marginLeft: "auto", background: "#ef5350", color: "#fff", borderRadius: 10, fontSize: 10, fontWeight: 800, padding: "2px 7px" }}>!</span>}
              </button>
            );
          })}
        </nav>
        <div style={{ padding: sidebarCollapsed ? "16px 0" : "16px 20px", borderTop: "1px solid rgba(212,168,85,0.15)", display: "flex", alignItems: "center", gap: 10, justifyContent: sidebarCollapsed ? "center" : "flex-start" }}>
          <div style={{ width: 34, height: 34, borderRadius: "50%", background: "linear-gradient(135deg, #c9a227, #8b5e00)", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, color: "#1a0a00", fontSize: 14, flexShrink: 0 }}>A</div>
          {!sidebarCollapsed && <div><div style={{ fontSize: 12, fontWeight: 700, color: "#d4a855" }}>Admin</div><div style={{ fontSize: 10, color: "rgba(212,168,85,0.5)" }}>admin@sareekart.in</div></div>}
        </div>
      </aside>
      <main style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <div style={{ background: "#fff", borderBottom: "1px solid #f0ebe2", padding: "14px 28px", display: "flex", alignItems: "center", justifyContent: "space-between", position: "sticky", top: 0, zIndex: 50 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            {ADMIN_NAV.find(n => n.id === page)?.icon}
            <span style={{ fontSize: 14, fontWeight: 700, color: "#1a0a00", textTransform: "capitalize" }}>{ADMIN_NAV.find(n => n.id === page)?.label}</span>
          </div>
          <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
            <button style={{ background: "#faf9f7", border: "1.5px solid #e0d8cc", padding: "7px 16px", borderRadius: 4, fontSize: 12, cursor: "pointer", fontWeight: 600 }}>🔔 Alerts</button>
            <button onClick={onGoToStore} style={{ background: "#800020", color: "white", border: "none", padding: "7px 16px", borderRadius: 4, fontSize: 12, cursor: "pointer", fontWeight: 600 }}>🌐 View Store</button>
          </div>
        </div>
        <div style={{ flex: 1, padding: "28px 32px", overflowY: "auto" }}>{renderPage()}</div>
      </main>
      {showAddProduct && <AddProductModal onClose={() => setShowAddProduct(false)} />}
    </div>
  );
}

// ─── AUTH MODAL ───────────────────────────────────────────────────────────────
function AuthModal({ onClose, onLogin }) {
  const [tab, setTab] = useState("login");
  const [loginForm, setLoginForm] = useState({ email: "", password: "" });
  const [signupForm, setSignupForm] = useState({ name: "", email: "", phone: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleLogin = () => {
    if (!loginForm.email || !loginForm.password) { setError("Please fill all fields"); return; }
    setError("");
    onLogin({ name: loginForm.email.split("@")[0], email: loginForm.email });
    onClose();
  };

  const handleSignup = () => {
    if (!signupForm.name || !signupForm.email || !signupForm.password) { setError("Please fill all required fields"); return; }
    if (signupForm.password !== signupForm.confirm) { setError("Passwords do not match"); return; }
    setError("");
    setSuccess("Account created! Please login.");
    setTimeout(() => { setTab("login"); setSuccess(""); }, 1500);
  };

  const inputStyle = { width: "100%", padding: "11px 14px", border: "1.5px solid #e0d8cc", borderRadius: 6, fontSize: 14, fontFamily: "inherit", outline: "none", background: "#faf9f7", boxSizing: "border-box", marginBottom: 14 };

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", zIndex: 3000, display: "flex", alignItems: "center", justifyContent: "center" }} onClick={onClose}>
      <div style={{ background: "#fff", borderRadius: 12, width: 420, boxShadow: "0 32px 80px rgba(0,0,0,0.25)", overflow: "hidden" }} onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div style={{ background: "#800020", padding: "28px 32px 24px", position: "relative" }}>
          <div style={{ fontSize: 24, fontWeight: 800, color: "#fff", fontFamily: "Georgia, serif", letterSpacing: 1 }}>SareeKart</div>
          <div style={{ fontSize: 13, color: "rgba(255,255,255,0.7)", marginTop: 4 }}>Your luxury saree destination</div>
          <button onClick={onClose} style={{ position: "absolute", top: 16, right: 16, background: "rgba(255,255,255,0.15)", border: "none", color: "#fff", width: 32, height: 32, borderRadius: "50%", cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center" }}>✕</button>
        </div>

        {/* Tabs */}
        <div style={{ display: "flex", borderBottom: "1px solid #f0ebe2" }}>
          {["login", "signup"].map(t => (
            <button key={t} onClick={() => { setTab(t); setError(""); }} style={{ flex: 1, padding: "14px", border: "none", background: tab === t ? "#fff" : "#faf9f7", color: tab === t ? "#800020" : "#888", fontWeight: tab === t ? 800 : 500, fontSize: 14, cursor: "pointer", borderBottom: tab === t ? "2px solid #800020" : "none", textTransform: "capitalize" }}>
              {t === "login" ? "Sign In" : "Create Account"}
            </button>
          ))}
        </div>

        <div style={{ padding: "24px 32px 28px" }}>
          {error && <div style={{ background: "#fce4ec", color: "#c62828", padding: "10px 14px", borderRadius: 6, fontSize: 13, marginBottom: 16, fontWeight: 600 }}>{error}</div>}
          {success && <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "10px 14px", borderRadius: 6, fontSize: 13, marginBottom: 16, fontWeight: 600 }}>{success}</div>}

          {tab === "login" ? (
            <>
              <input placeholder="Email address" value={loginForm.email} onChange={e => setLoginForm(f => ({ ...f, email: e.target.value }))} style={inputStyle} />
              <input type="password" placeholder="Password" value={loginForm.password} onChange={e => setLoginForm(f => ({ ...f, password: e.target.value }))} style={inputStyle} />
              <div style={{ textAlign: "right", marginBottom: 20 }}>
                <span style={{ fontSize: 12, color: "#800020", cursor: "pointer", fontWeight: 600 }}>Forgot password?</span>
              </div>
              <button onClick={handleLogin} style={{ width: "100%", background: "#800020", color: "#fff", border: "none", padding: "13px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer", letterSpacing: "0.04em" }}>Sign In</button>
              <div style={{ textAlign: "center", marginTop: 16, fontSize: 13, color: "#888" }}>
                Don't have an account? <span onClick={() => setTab("signup")} style={{ color: "#800020", fontWeight: 700, cursor: "pointer" }}>Sign up</span>
              </div>
            </>
          ) : (
            <>
              <input placeholder="Full name *" value={signupForm.name} onChange={e => setSignupForm(f => ({ ...f, name: e.target.value }))} style={inputStyle} />
              <input placeholder="Email address *" value={signupForm.email} onChange={e => setSignupForm(f => ({ ...f, email: e.target.value }))} style={inputStyle} />
              <input placeholder="Phone number" value={signupForm.phone} onChange={e => setSignupForm(f => ({ ...f, phone: e.target.value }))} style={inputStyle} />
              <input type="password" placeholder="Password *" value={signupForm.password} onChange={e => setSignupForm(f => ({ ...f, password: e.target.value }))} style={inputStyle} />
              <input type="password" placeholder="Confirm password *" value={signupForm.confirm} onChange={e => setSignupForm(f => ({ ...f, confirm: e.target.value }))} style={inputStyle} />
              <button onClick={handleSignup} style={{ width: "100%", background: "#800020", color: "#fff", border: "none", padding: "13px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer" }}>Create Account</button>
              <div style={{ textAlign: "center", marginTop: 16, fontSize: 13, color: "#888" }}>
                Already have an account? <span onClick={() => setTab("login")} style={{ color: "#800020", fontWeight: 700, cursor: "pointer" }}>Sign in</span>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── CART MODAL ───────────────────────────────────────────────────────────────
function CartModal({ cartItems, onClose, onUpdateQty, onRemove }) {
  const [step, setStep] = useState("cart"); // cart | address | payment | success | refund
  const [orderId] = useState(() => Math.floor(Math.random() * 9000) + 1000);
  const [address, setAddress] = useState({ name: "", phone: "", pincode: "", city: "", state: "", address: "" });
  const [payMethod, setPayMethod] = useState("upi");
  const [refundReason, setRefundReason] = useState("");
  const [refundStatus, setRefundStatus] = useState(null); // null | "requesting" | "success" | "error"

  const total = cartItems.reduce((s, i) => s + i.price * i.qty, 0);
  const shipping = total > 5000 ? 0 : 99;
  const grand = total + shipping;

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", zIndex: 3000, display: "flex", alignItems: "flex-start", justifyContent: "flex-end" }} onClick={onClose}>
      <div style={{ background: "#fff", width: 480, maxWidth: "100vw", height: "100vh", display: "flex", flexDirection: "column", boxShadow: "-8px 0 40px rgba(0,0,0,0.2)", overflowY: "auto" }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{ background: "#800020", padding: "20px 24px", display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 800, color: "#fff" }}>
              {step === "cart" ? "🛒 Your Cart" : step === "address" ? "📍 Delivery Address" : step === "payment" ? "💳 Payment" : step === "success" ? "✅ Order Placed!" : step === "refund" ? "🔄 Request Refund" : "✅ Order Placed!"}
            </div>
            <div style={{ fontSize: 12, color: "rgba(255,255,255,0.7)", marginTop: 2 }}>
              {step === "cart" ? `${cartItems.length} item${cartItems.length !== 1 ? "s" : ""}` : step === "address" ? "Where should we deliver?" : step === "payment" ? "Choose payment method" : step === "success" ? "Your order is confirmed!" : step === "refund" ? "We're sorry to see you go" : ""}
            </div>
          </div>
          <button onClick={onClose} style={{ background: "rgba(255,255,255,0.15)", border: "none", color: "#fff", width: 34, height: 34, borderRadius: "50%", cursor: "pointer", fontSize: 16 }}>✕</button>
        </div>

        {/* Steps indicator */}
        {step !== "success" && step !== "refund" && (
          <div style={{ display: "flex", padding: "12px 24px", gap: 0, background: "#faf9f7", borderBottom: "1px solid #f0ebe2", flexShrink: 0 }}>
            {["cart", "address", "payment"].map((s, i) => (
              <div key={s} style={{ display: "flex", alignItems: "center", flex: 1 }}>
                <div style={{ width: 28, height: 28, borderRadius: "50%", background: step === s || (s === "cart" && step !== "cart") || (s === "address" && step === "payment") ? "#800020" : "#e0d8cc", color: step === s || (s === "cart" && step !== "cart") || (s === "address" && step === "payment") ? "#fff" : "#888", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 800, flexShrink: 0 }}>{i + 1}</div>
                <div style={{ fontSize: 11, color: step === s ? "#800020" : "#888", fontWeight: step === s ? 700 : 400, marginLeft: 6, flex: 1, textTransform: "capitalize" }}>{s}</div>
                {i < 2 && <div style={{ width: 20, height: 2, background: "#e0d8cc", marginRight: 6 }} />}
              </div>
            ))}
          </div>
        )}

        {/* Content */}
        <div style={{ flex: 1, overflowY: "auto", padding: "20px 24px" }}>

          {step === "cart" && (
            cartItems.length === 0 ? (
              <div style={{ textAlign: "center", padding: "60px 20px" }}>
                <div style={{ fontSize: 64, marginBottom: 16 }}>🛒</div>
                <div style={{ fontSize: 18, fontWeight: 700, color: "#1a0a00", marginBottom: 8 }}>Your cart is empty</div>
                <div style={{ fontSize: 14, color: "#888" }}>Add some beautiful sarees to get started!</div>
                <button onClick={onClose} style={{ marginTop: 24, background: "#800020", color: "#fff", border: "none", padding: "12px 28px", borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: "pointer" }}>Continue Shopping</button>
              </div>
            ) : (
              <>
                {cartItems.map(item => (
                  <div key={item.id} style={{ display: "flex", gap: 14, marginBottom: 18, background: "#faf9f7", borderRadius: 8, padding: 14, border: "1px solid #f0ebe2" }}>
                    <img src={item.image} alt={item.name} style={{ width: 72, height: 90, objectFit: "cover", borderRadius: 6, flexShrink: 0 }} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 14, fontWeight: 700, color: "#1a0a00", marginBottom: 4, lineHeight: 1.4 }}>{item.name}</div>
                      <div style={{ fontSize: 13, color: "#888", marginBottom: 10 }}>{item.category}</div>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                          <button onClick={() => onUpdateQty(item.id, item.qty - 1)} style={{ width: 28, height: 28, borderRadius: "50%", border: "1px solid #e0d8cc", background: "#fff", cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center", color: "#800020", fontWeight: 700 }}>−</button>
                          <span style={{ fontSize: 15, fontWeight: 700, minWidth: 20, textAlign: "center" }}>{item.qty}</span>
                          <button onClick={() => onUpdateQty(item.id, item.qty + 1)} style={{ width: 28, height: 28, borderRadius: "50%", border: "1px solid #e0d8cc", background: "#fff", cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center", color: "#800020", fontWeight: 700 }}>+</button>
                        </div>
                        <div style={{ fontSize: 16, fontWeight: 800, color: "#800020" }}>{fmt(item.price * item.qty)}</div>
                      </div>
                    </div>
                    <button onClick={() => onRemove(item.id)} style={{ background: "none", border: "none", color: "#ccc", cursor: "pointer", fontSize: 18, alignSelf: "flex-start", padding: 2 }}>✕</button>
                  </div>
                ))}
                {/* Order summary */}
                <div style={{ background: "#faf9f7", borderRadius: 8, padding: 16, border: "1px solid #f0ebe2", marginTop: 8 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00", marginBottom: 12 }}>Order Summary</div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: "#555", marginBottom: 8 }}>
                    <span>Subtotal ({cartItems.reduce((s, i) => s + i.qty, 0)} items)</span>
                    <span>{fmt(total)}</span>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: "#555", marginBottom: 8 }}>
                    <span>Shipping</span>
                    <span style={{ color: shipping === 0 ? "#2e7d32" : "#555" }}>{shipping === 0 ? "FREE" : fmt(shipping)}</span>
                  </div>
                  {shipping === 0 && <div style={{ fontSize: 11, color: "#2e7d32", marginBottom: 8 }}>🎉 You qualify for free shipping!</div>}
                  <div style={{ borderTop: "1px solid #e0d8cc", paddingTop: 10, display: "flex", justifyContent: "space-between", fontWeight: 800, fontSize: 16, color: "#1a0a00" }}>
                    <span>Total</span>
                    <span style={{ color: "#800020" }}>{fmt(grand)}</span>
                  </div>
                </div>
              </>
            )
          )}

          {step === "address" && (
            <div>
              {[
                { placeholder: "Full Name *", key: "name" },
                { placeholder: "Phone Number *", key: "phone" },
                { placeholder: "Address (House/Flat/Street) *", key: "address" },
                { placeholder: "Pincode *", key: "pincode" },
                { placeholder: "City *", key: "city" },
                { placeholder: "State *", key: "state" },
              ].map(f => (
                <input key={f.key} placeholder={f.placeholder} value={address[f.key]} onChange={e => setAddress(a => ({ ...a, [f.key]: e.target.value }))} style={{ width: "100%", padding: "12px 14px", border: "1.5px solid #e0d8cc", borderRadius: 6, fontSize: 14, fontFamily: "inherit", outline: "none", background: "#faf9f7", boxSizing: "border-box", marginBottom: 12 }} />
              ))}
            </div>
          )}

          {step === "payment" && (
            <div>
              <div style={{ background: "#faf9f7", borderRadius: 8, padding: 16, border: "1px solid #f0ebe2", marginBottom: 20 }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00", marginBottom: 8 }}>Order Total</div>
                <div style={{ fontSize: 28, fontWeight: 800, color: "#800020" }}>{fmt(grand)}</div>
                <div style={{ fontSize: 12, color: "#888", marginTop: 4 }}>{cartItems.reduce((s, i) => s + i.qty, 0)} items · {shipping === 0 ? "Free shipping" : `Shipping: ${fmt(shipping)}`}</div>
              </div>
              {[
                { id: "upi", icon: "📱", label: "UPI / PhonePe / GPay", sub: "Instant payment via UPI" },
                { id: "card", icon: "💳", label: "Credit / Debit Card", sub: "Visa, Mastercard, RuPay" },
                { id: "netbanking", icon: "🏦", label: "Net Banking", sub: "All major banks supported" },
                { id: "cod", icon: "💵", label: "Cash on Delivery", sub: "Pay when you receive" },
              ].map(m => (
                <div key={m.id} onClick={() => setPayMethod(m.id)} style={{ display: "flex", alignItems: "center", gap: 14, padding: "14px 16px", borderRadius: 8, border: `2px solid ${payMethod === m.id ? "#800020" : "#e0d8cc"}`, background: payMethod === m.id ? "#fff5f5" : "#faf9f7", marginBottom: 12, cursor: "pointer" }}>
                  <div style={{ fontSize: 24 }}>{m.icon}</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 14, fontWeight: 700, color: "#1a0a00" }}>{m.label}</div>
                    <div style={{ fontSize: 12, color: "#888" }}>{m.sub}</div>
                  </div>
                  <div style={{ width: 20, height: 20, borderRadius: "50%", border: `2px solid ${payMethod === m.id ? "#800020" : "#ccc"}`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {payMethod === m.id && <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#800020" }} />}
                  </div>
                </div>
              ))}
            </div>
          )}

          {step === "success" && (
            <div style={{ textAlign: "center", padding: "40px 20px" }}>
              <div style={{ fontSize: 72, marginBottom: 20 }}>🎉</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: "#2e7d32", marginBottom: 12 }}>Order Placed Successfully!</div>
              <div style={{ fontSize: 14, color: "#555", marginBottom: 24, lineHeight: 1.7 }}>
                Your order has been confirmed.<br />
                You will receive a confirmation SMS & email shortly.<br />
                Estimated delivery: <strong>3–5 business days</strong>
              </div>
              <div style={{ background: "#e8f5e9", borderRadius: 8, padding: 16, marginBottom: 28 }}>
                <div style={{ fontSize: 12, color: "#2e7d32", fontWeight: 600 }}>ORDER ID</div>
                <div style={{ fontSize: 20, fontWeight: 800, color: "#1a0a00", letterSpacing: 1 }}>ORD-{orderId || "—"}</div>
              </div>
              <button onClick={onClose} style={{ background: "#800020", color: "#fff", border: "none", padding: "13px 36px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer" }}>Continue Shopping</button>
            </div>
          )}

          {step === "refund" && (
            <div style={{ padding: "0 20px" }}>
              <div style={{ textAlign: "center", marginBottom: 28 }}>
                <div style={{ fontSize: 48, marginBottom: 12 }}>😔</div>
                <div style={{ fontSize: 20, fontWeight: 800, color: "#1a0a00", marginBottom: 4 }}>We're sorry to see you go</div>
                <div style={{ fontSize: 13, color: "#888" }}>Help us understand why and we'll make it right</div>
              </div>
              {refundStatus === "success" ? (
                <div style={{ textAlign: "center", padding: "40px 20px" }}>
                  <div style={{ fontSize: 64, marginBottom: 16 }}>✅</div>
                  <div style={{ fontSize: 20, fontWeight: 800, color: "#2e7d32", marginBottom: 8 }}>Refund Request Submitted</div>
                  <div style={{ fontSize: 14, color: "#555", marginBottom: 16, lineHeight: 1.6 }}>
                    Your refund request has been received. Our team will review it within 24 hours.
                    <br />You'll receive an email update at your registered address.
                  </div>
                  <div style={{ background: "#e8f5e9", borderRadius: 8, padding: 16, marginBottom: 24 }}>
                    <div style={{ fontSize: 12, color: "#2e7d32", fontWeight: 600 }}>Refund Amount</div>
                    <div style={{ fontSize: 24, fontWeight: 800, color: "#1a0a00" }}>{fmt(grand)}</div>
                  </div>
                  <button onClick={onClose} style={{ background: "#800020", color: "#fff", border: "none", padding: "13px 36px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer" }}>Done</button>
                </div>
              ) : refundStatus === "error" ? (
                <div style={{ textAlign: "center", padding: "40px 20px" }}>
                  <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: "#c62828", marginBottom: 8 }}>Something went wrong</div>
                  <div style={{ fontSize: 14, color: "#555", marginBottom: 24 }}>Please try again or contact support.</div>
                  <button onClick={() => setRefundStatus(null)} style={{ background: "#faf9f7", color: "#800020", border: "1.5px solid #800020", padding: "10px 24px", borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: "pointer", marginRight: 12 }}>Try Again</button>
                  <button onClick={onClose} style={{ background: "#1a0a00", color: "#d4a855", border: "none", padding: "10px 24px", borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: "pointer" }}>Go Back</button>
                </div>
              ) : (
                <>
                  <div style={{ background: "#faf9f7", borderRadius: 8, padding: 16, border: "1px solid #f0ebe2", marginBottom: 20 }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00", marginBottom: 4 }}>Order ID</div>
                    <div style={{ fontSize: 16, fontWeight: 800, color: "#800020" }}>ORD-{orderId || "—"}</div>
                  </div>
                  <div style={{ marginBottom: 20 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#555", textTransform: "uppercase", letterSpacing: "0.06em", display: "block", marginBottom: 6 }}>Reason for refund</label>
                    <select value={refundReason} onChange={e => setRefundReason(e.target.value)} style={{ width: "100%", padding: "12px 14px", border: "1.5px solid #e0d8cc", borderRadius: 6, fontSize: 14, fontFamily: "inherit", outline: "none", background: "#faf9f7", boxSizing: "border-box", marginBottom: 8 }}>
                      <option value="">Select a reason...</option>
                      <option value="Customer request">Customer request</option>
                      <option value="Out of stock">Out of stock</option>
                      <option value="Duplicate payment">Duplicate payment</option>
                      <option value="Order cancelled">Order cancelled</option>
                      <option value="Product not as described">Product not as described</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                  <div style={{ background: "#faf9f7", borderRadius: 8, padding: 16, border: "1px solid #f0ebe2", marginBottom: 20 }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: "#1a0a00", marginBottom: 4 }}>Refund Amount</div>
                    <div style={{ fontSize: 24, fontWeight: 800, color: "#800020" }}>{fmt(grand)}</div>
                  </div>
                  <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
                    <button onClick={() => { if (!refundReason) { setRefundStatus("error"); return; } setRefundStatus("requesting"); setTimeout(() => setRefundStatus("success"), 1500); }} disabled={!refundReason || refundStatus === "requesting" || refundStatus === "error"} style={{ flex: 1, background: !refundReason || refundStatus === "requesting" || refundStatus === "error" ? "#ccc" : "#800020", color: "#fff", border: "none", padding: "14px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer", opacity: !refundReason || refundStatus === "requesting" || refundStatus === "error" ? 0.6 : 1 }}>
                      {refundStatus === "requesting" ? "Submitting..." : "Submit Refund Request"}
                    </button>
                    <button onClick={() => setStep("success")} style={{ background: "#faf9f7", color: "#800020", border: "1.5px solid #800020", padding: "14px 20px", borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: "pointer" }}>Keep Order</button>
                  </div>
                  {refundStatus === "error" && <div style={{ background: "#fce4ec", borderRadius: 6, padding: 10, textAlign: "center", fontSize: 12, color: "#c62828", fontWeight: 600, marginBottom: 12 }}>Please select a reason before submitting.</div>}
                </>
              )}
            </div>
          )}
        </div>

        {/* Footer CTA */}
        {step !== "success" && step !== "refund" && cartItems.length > 0 && (
          <div style={{ padding: "16px 24px", borderTop: "1px solid #f0ebe2", background: "#fff", flexShrink: 0 }}>
            {step !== "cart" && (
              <button onClick={() => setStep(step === "payment" ? "address" : "cart")} style={{ width: "100%", background: "#faf9f7", color: "#800020", border: "1.5px solid #800020", padding: "12px", borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: "pointer", marginBottom: 8 }}>
                ← Back
              </button>
            )}
            <button onClick={() => {
              if (step === "cart") setStep("address");
              else if (step === "address") setStep("payment");
              else if (step === "payment") setStep("success");
            }} style={{ width: "100%", background: "#800020", color: "#fff", border: "none", padding: "14px", borderRadius: 6, fontSize: 15, fontWeight: 700, cursor: "pointer" }}>
              {step === "cart" ? `Proceed to Checkout · ${fmt(grand)}` : step === "address" ? "Continue to Payment →" : `Pay ${fmt(grand)}`}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── NAV DROPDOWN ─────────────────────────────────────────────────────────────
function NavDropdown({ items, onClose }) {
  return (
    <div style={{ position: "absolute", top: "100%", left: 0, background: "#fff", border: "1px solid #f0ebe2", borderRadius: 8, minWidth: 220, boxShadow: "0 8px 32px rgba(0,0,0,0.12)", zIndex: 200, overflow: "hidden", marginTop: 8 }}>
      {items.map(item => (
        <div key={item} onClick={onClose} style={{ padding: "11px 18px", fontSize: 13, color: "#333", cursor: "pointer", borderBottom: "1px solid #faf9f7", fontWeight: 500 }}
          onMouseEnter={e => e.currentTarget.style.background = "#faf9f7"}
          onMouseLeave={e => e.currentTarget.style.background = "#fff"}>
          {item}
        </div>
      ))}
    </div>
  );
}

// ─── STOREFRONT ───────────────────────────────────────────────────────────────
function Storefront({ onGoToAdmin }) {
  const [cartItems, setCartItems] = useState([]);
  const [showCart, setShowCart] = useState(false);
  const [showAuth, setShowAuth] = useState(false);
  const [user, setUser] = useState(null);
  const [activeNav, setActiveNav] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");

  const cartCount = cartItems.reduce((s, i) => s + i.qty, 0);

  const addToCart = (product) => {
    setCartItems(prev => {
      const existing = prev.find(i => i.id === product.id);
      if (existing) return prev.map(i => i.id === product.id ? { ...i, qty: i.qty + 1 } : i);
      return [...prev, { ...product, qty: 1 }];
    });
  };

  const updateQty = (id, qty) => {
    if (qty <= 0) setCartItems(prev => prev.filter(i => i.id !== id));
    else setCartItems(prev => prev.map(i => i.id === id ? { ...i, qty } : i));
  };

  const removeItem = (id) => setCartItems(prev => prev.filter(i => i.id !== id));

  const filteredProducts = STORE_PRODUCTS.filter(p => {
    const matchCat = activeCategory === "All" || p.category === activeCategory;
    const matchSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchCat && matchSearch;
  });

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e) => { if (!e.target.closest("[data-nav]")) setActiveNav(null); };
    document.addEventListener("click", handler);
    return () => document.removeEventListener("click", handler);
  }, []);

  const navLinks = ["home", "collections", "wedding", "new arrivals"];

  return (
    <div style={{ fontFamily: "'Segoe UI', sans-serif", background: "#f8f5f2", minHeight: "100vh" }}>

      {/* ── NAVBAR ── */}
      <nav style={{ background: "#800020", color: "white", padding: "0 48px", display: "flex", justifyContent: "space-between", alignItems: "center", position: "sticky", top: 0, zIndex: 1000, height: 68 }} data-nav>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: 2, fontFamily: "Georgia, serif", cursor: "pointer" }}>SareeKart</div>

        {/* Nav links with dropdowns */}
        <div style={{ display: "flex", gap: 4, alignItems: "center", height: "100%" }}>
          {navLinks.map(link => (
            <div key={link} style={{ position: "relative", height: "100%", display: "flex", alignItems: "center" }} data-nav>
              <button
                onClick={() => setActiveNav(activeNav === link ? null : link)}
                style={{ background: "none", border: "none", color: "rgba(255,255,255,0.9)", cursor: "pointer", fontSize: 13, fontWeight: 600, padding: "0 14px", height: "100%", textTransform: "capitalize", letterSpacing: "0.04em", display: "flex", alignItems: "center", gap: 4, borderBottom: activeNav === link ? "3px solid rgba(255,255,255,0.6)" : "3px solid transparent" }}
              >
                {link}
                {NAV_SECTIONS[link] && <span style={{ fontSize: 9 }}>▼</span>}
              </button>
              {activeNav === link && NAV_SECTIONS[link] && (
                <NavDropdown items={NAV_SECTIONS[link]} onClose={() => setActiveNav(null)} />
              )}
            </div>
          ))}
        </div>

        {/* Right actions */}
        <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
          <button onClick={() => setShowAuth(true)} style={{ background: "rgba(255,255,255,0.12)", color: "white", border: "1px solid rgba(255,255,255,0.3)", padding: "7px 16px", borderRadius: 6, cursor: "pointer", fontSize: 12, fontWeight: 600, display: "flex", alignItems: "center", gap: 6 }}>
            👤 {user ? user.name.charAt(0).toUpperCase() + user.name.slice(1) : "Account"}
          </button>
          <button onClick={() => setShowCart(true)} style={{ background: "rgba(255,255,255,0.12)", color: "white", border: "1px solid rgba(255,255,255,0.3)", padding: "7px 16px", borderRadius: 6, cursor: "pointer", fontSize: 12, fontWeight: 600, position: "relative", display: "flex", alignItems: "center", gap: 6 }}>
            🛒 Cart
            {cartCount > 0 && (
              <span style={{ background: "#f4c430", color: "#800020", borderRadius: 20, fontSize: 11, fontWeight: 800, padding: "1px 7px", minWidth: 18, textAlign: "center" }}>{cartCount}</span>
            )}
          </button>
          <button onClick={onGoToAdmin} style={{ background: "rgba(255,255,255,0.1)", color: "rgba(255,255,255,0.7)", border: "1px solid rgba(255,255,255,0.2)", padding: "7px 14px", borderRadius: 6, cursor: "pointer", fontSize: 11, fontWeight: 700 }}>⚙️ Admin</button>
        </div>
      </nav>

      {/* ── SEARCH BAR ── */}
      <div style={{ background: "#fff", padding: "16px 48px", boxShadow: "0 2px 8px rgba(0,0,0,0.06)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div style={{ position: "relative", width: "55%", maxWidth: 600 }}>
          <span style={{ position: "absolute", left: 16, top: "50%", transform: "translateY(-50%)", fontSize: 16, color: "#aaa", pointerEvents: "none" }}>🔍</span>
          <input
            placeholder="Search for sarees, fabrics, occasions..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            style={{ width: "100%", padding: "12px 16px 12px 44px", borderRadius: 30, border: "1.5px solid #e0d8cc", outline: "none", fontSize: 14, fontFamily: "inherit", background: "#faf9f7", boxSizing: "border-box" }}
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery("")} style={{ position: "absolute", right: 14, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "#aaa", cursor: "pointer", fontSize: 16 }}>✕</button>
          )}
        </div>
      </div>

      {/* ── OFFER BANNER ── */}
      <div style={{ background: "linear-gradient(135deg, #fff3cd, #ffe082)", color: "#795548", padding: "12px", textAlign: "center", fontWeight: 700, fontSize: 14, letterSpacing: "0.02em" }}>
        🎉 Flat 20% OFF on Wedding Sarees &nbsp;|&nbsp; Free Shipping across India on orders above ₹5,000
      </div>

      {/* ── HERO ── */}
      <div style={{ position: "relative", height: 520, overflow: "hidden" }}>
        <img src="https://kankatala.com/cdn/shop/files/1216039129_1.webp?v=1777711821" alt="Hero" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(135deg, rgba(128,0,32,0.75) 0%, rgba(26,10,0,0.5) 100%)", display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", color: "white", textAlign: "center" }}>
          <div style={{ fontSize: 13, letterSpacing: "0.25em", textTransform: "uppercase", color: "rgba(255,215,0,0.9)", marginBottom: 16, fontWeight: 600 }}>Est. 2020 · Handwoven Luxury</div>
          <h1 style={{ fontSize: 64, margin: 0, fontFamily: "Georgia, serif", letterSpacing: "0.04em", textShadow: "0 2px 20px rgba(0,0,0,0.3)" }}>SareeKart</h1>
          <p style={{ fontSize: 18, fontWeight: 300, letterSpacing: "0.08em", marginTop: 12, color: "rgba(255,255,255,0.85)" }}>Luxury Sarees For Every Celebration</p>
          <div style={{ display: "flex", gap: 14, marginTop: 32 }}>
            <button style={{ padding: "14px 36px", background: "#f4c430", color: "#800020", border: "none", fontWeight: 800, cursor: "pointer", borderRadius: 4, fontSize: 14, letterSpacing: "0.06em" }}>Shop Collection</button>
            <button style={{ padding: "14px 36px", background: "transparent", color: "#fff", border: "2px solid rgba(255,255,255,0.6)", fontWeight: 700, cursor: "pointer", borderRadius: 4, fontSize: 14 }}>View Bridal</button>
          </div>
        </div>
      </div>

      <div style={{ maxWidth: 1200, margin: "0 auto", padding: "48px 40px" }}>

        {/* ── CATEGORIES ── */}
        <div style={{ marginBottom: 48 }}>
          <h2 style={{ color: "#800020", fontFamily: "Georgia, serif", fontSize: 22, marginBottom: 20 }}>Shop By Category</h2>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            {["All", "Kanchipuram", "Banarasi", "Uppada", "Pochampally", "Designer", "Tussar"].map(cat => (
              <button key={cat} onClick={() => setActiveCategory(cat)} style={{ padding: "10px 22px", borderRadius: 24, border: `2px solid ${activeCategory === cat ? "#800020" : "#e0d8cc"}`, background: activeCategory === cat ? "#800020" : "#fff", color: activeCategory === cat ? "#fff" : "#555", fontSize: 13, fontWeight: 600, cursor: "pointer", transition: "all 0.15s" }}>
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* ── PRODUCTS ── */}
        <div style={{ marginBottom: 60 }}>
          <h2 style={{ textAlign: "center", color: "#800020", fontFamily: "Georgia, serif", fontSize: 26, marginBottom: 36 }}>Our Premium Collection</h2>
          {filteredProducts.length === 0 ? (
            <div style={{ textAlign: "center", padding: "60px 20px", color: "#888" }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>🔍</div>
              <div style={{ fontSize: 16, fontWeight: 600 }}>No sarees found for "{searchQuery}"</div>
            </div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 28 }}>
              {filteredProducts.map(p => (
                <div key={p.id} style={{ background: "white", borderRadius: 12, overflow: "hidden", boxShadow: "0 4px 16px rgba(0,0,0,0.08)", transition: "transform 0.25s, box-shadow 0.25s", cursor: "pointer" }}
                  onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-6px)"; e.currentTarget.style.boxShadow = "0 12px 32px rgba(128,0,32,0.15)"; }}
                  onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 4px 16px rgba(0,0,0,0.08)"; }}>
                  <div style={{ position: "relative" }}>
                    <img src={p.image} alt={p.name} style={{ width: "100%", height: 320, objectFit: "cover" }} />
                    {p.badge && (
                      <span style={{ position: "absolute", top: 12, left: 12, background: p.badge === "20% OFF" ? "#2e7d32" : p.badge === "New" ? "#1565c0" : "#800020", color: "#fff", padding: "4px 10px", borderRadius: 4, fontSize: 11, fontWeight: 800 }}>{p.badge}</span>
                    )}
                    <button onClick={() => addToCart(p)} style={{ position: "absolute", bottom: 12, right: 12, background: "rgba(128,0,32,0.9)", color: "#fff", border: "none", padding: "8px 16px", borderRadius: 20, fontSize: 12, fontWeight: 700, cursor: "pointer", backdropFilter: "blur(4px)" }}>
                      + Add to Cart
                    </button>
                  </div>
                  <div style={{ padding: "18px 20px 20px" }}>
                    <div style={{ fontSize: 11, color: "#aaa", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 6 }}>{p.category}</div>
                    <div style={{ fontSize: 15, fontWeight: 700, color: "#1a0a00", marginBottom: 10, lineHeight: 1.4 }}>{p.name}</div>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                      <span style={{ fontSize: 20, fontWeight: 800, color: "#800020" }}>{fmt(p.price)}</span>
                      <button onClick={() => addToCart(p)} style={{ background: "#800020", color: "white", border: "none", padding: "8px 18px", cursor: "pointer", borderRadius: 6, fontSize: 12, fontWeight: 700 }}>Add to Cart</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ── FOOTER ── */}
        <footer style={{ background: "#800020", color: "white", textAlign: "center", padding: "40px 32px", borderRadius: 12 }}>
          <h2 style={{ fontFamily: "Georgia, serif", fontSize: 28, margin: "0 0 8px" }}>SareeKart</h2>
          <p style={{ color: "rgba(255,255,255,0.7)", fontSize: 14, marginBottom: 24 }}>Traditional Elegance · Modern Shopping</p>
          <div style={{ display: "flex", justifyContent: "center", gap: 32, marginBottom: 24, flexWrap: "wrap" }}>
            {["About Us", "Contact", "Size Guide", "Returns", "Privacy Policy", "Terms"].map(link => (
              <span key={link} style={{ fontSize: 13, color: "rgba(255,255,255,0.6)", cursor: "pointer" }}
                onMouseEnter={e => e.currentTarget.style.color = "#fff"}
                onMouseLeave={e => e.currentTarget.style.color = "rgba(255,255,255,0.6)"}>{link}</span>
            ))}
          </div>
          <p style={{ fontSize: 12, color: "rgba(255,255,255,0.4)", margin: 0 }}>© 2026 SareeKart. All Rights Reserved.</p>
        </footer>
      </div>

      {/* ── MODALS ── */}
      {showAuth && <AuthModal onClose={() => setShowAuth(false)} onLogin={setUser} />}
      {showCart && <CartModal cartItems={cartItems} onClose={() => setShowCart(false)} onUpdateQty={updateQty} onRemove={removeItem} />}
    </div>
  );
}

// ─── ROOT ─────────────────────────────────────────────────────────────────────
export default function App() {
  const [view, setView] = useState("store");
  return view === "admin"
    ? <AdminPanel onGoToStore={() => setView("store")} />
    : <Storefront onGoToAdmin={() => setView("admin")} />;
}