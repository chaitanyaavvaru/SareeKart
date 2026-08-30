function Home() {
  return (
    <div>
      <section style={{
        background:"#800020",
        color:"white",
        padding:"60px",
        textAlign:"center"
      }}>
        <h1>Welcome to SareeKart</h1>
        <h2>Elegant Sarees For Every Occasion</h2>
        <button>Shop Now</button>
      </section>

      <section style={{padding:"40px"}}>
        <h2>Categories</h2>

        <div style={{
          display:"flex",
          gap:"20px",
          flexWrap:"wrap"
        }}>
          <div>Silk Sarees</div>
          <div>Cotton Sarees</div>
          <div>Banarasi Sarees</div>
          <div>Wedding Sarees</div>
          <div>Designer Sarees</div>
        </div>
      </section>
    </div>
  );
}

export default Home;
