import { useState, useEffect } from "react";
import axios from "axios";
function App() {
  const categories = [
    "Silk Sarees",
    "Cotton Sarees",
    "Banarasi Sarees",
    "Wedding Sarees",
    "Designer Sarees"
  ];

  const products = [
    { name: "Banarasi Silk Saree", price: "₹4,999" },
    { name: "Kanchipuram Silk Saree", price: "₹6,499" },
    { name: "Designer Party Saree", price: "₹2,999" },
    { name: "Cotton Daily Wear Saree", price: "₹1,499" }
  ];

  return (
    <div style={{
      fontFamily: "Arial",
      background: "#f5f5f5",
      minHeight: "100vh"
    }}>
      <nav style={{
        background: "#800020",
        color: "white",
        padding: "20px",
        display: "flex",
        justifyContent: "space-between"
      }}>
        <h2>SareeKart</h2>

        <div>
          Home | Products | Cart | Login | Admin
        </div>
      </nav>

      <section style={{
        background: "#a00030",
        color: "white",
        textAlign: "center",
        padding: "80px 20px"
      }}>
        <h1 style={{ fontSize: "50px" }}>
          Welcome to SareeKart
        </h1>

        <h3>Elegant Sarees For Every Occasion</h3>

        <button style={{
          marginTop: "20px",
          padding: "15px 30px",
          background: "gold",
          border: "none"
        }}>
          Shop Now
        </button>
      </section>

      <section style={{ padding: "40px" }}>
        <h2>Shop By Category</h2>

        <div style={{
          display: "flex",
          gap: "20px",
          flexWrap: "wrap",
          marginTop: "20px"
        }}>
          {categories.map((category, index) => (
            <div
              key={index}
              style={{
                background: "white",
                padding: "30px",
                width: "180px",
                borderRadius: "10px",
                boxShadow: "0px 2px 8px rgba(0,0,0,0.15)"
              }}
            >
              {category}
            </div>
          ))}
        </div>
      </section>

      <section style={{ padding: "40px" }}>
        <h2>Featured Products</h2>

        <div style={{
          display: "flex",
          gap: "20px",
          flexWrap: "wrap",
          marginTop: "20px"
        }}>
          {products.map((product, index) => (
            <div
              key={index}
              style={{
                width: "250px",
                background: "white",
                borderRadius: "10px",
                boxShadow: "0px 2px 8px rgba(0,0,0,0.15)"
              }}
            >
              <div style={{
                height: "220px",
                background: "#ddd",
                display: "flex",
                justifyContent: "center",
                alignItems: "center"
              }}>
                Saree Image
              </div>

              <div style={{ padding: "15px" }}>
                <h3>{product.name}</h3>
                <p>{product.price}</p>

                <button style={{
                  background: "#800020",
                  color: "white",
                  border: "none",
                  padding: "10px 15px"
                }}>
                  Add To Cart
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <footer style={{
        background: "#800020",
        color: "white",
        textAlign: "center",
        padding: "20px"
      }}>
        © 2026 SareeKart. All Rights Reserved.
      </footer>
    </div>
  );
}

export default App;
