export default function Header() {
  return (
    <header className="hero-header">
      <h1 className="hero-title">
        Buscador de Pacotes — PSPD
      </h1>

      <p className="hero-subtitle">
        Orquestração de Microsserviços via Gateway gRPC: Voos (A) × Hotéis (B)
      </p>

      <div className="hero-features">
        <div className="feature-card">
          <span>🛫</span>
          <strong>Voos</strong>
        </div>

        <div className="feature-card">
          <span>🏨</span>
          <strong>Hotéis</strong>
        </div>

        <div className="feature-card">
          <span>⚡</span>
          <strong>gRPC Gateway</strong>
        </div>

        <div className="feature-card">
          <span>🔍</span>
          <strong>Busca Inteligente</strong>
        </div>
      </div>
    </header>
  );
}