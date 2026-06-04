export default function StatusMessage({ status, meta }) {
  return (
    <>
      {status.text && (
        <section className={`status-card ${status.type}`}>
          <div className="status-icon">
            {status.type === "error" && "❌"}
            {status.type === "warn" && "⚠️"}
            {status.type === "info" && "🔍"}
          </div>

          <div>{status.text}</div>
        </section>
      )}

      {meta && (
        <section className="meta-card">
          <div className="meta-icon"></div>
          <div>{meta}</div>
        </section>
      )}
    </>
  );
}