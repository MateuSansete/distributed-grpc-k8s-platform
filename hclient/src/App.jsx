import { useState } from "react";
import Header from "./components/Header";
import SearchForm from "./components/SearchForm";
import StatusMessage from "./components/StatusMessage";
import ResultsTable from "./components/ResultsTable";

const GATEWAY = "http://localhost:8080"; // Mude para "" se o Nginx for fazer proxy no k8s

export default function App() {
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState({ type: "", text: "" });
  const [meta, setMeta] = useState("");

  const handleSearch = async (formData) => {
    setIsLoading(true);
    setResults([]);
    setMeta("");
    setStatus({ type: "info", text: "Buscando…" });

    const params = new URLSearchParams(formData);

    try {
      const resp = await fetch(`${GATEWAY}/api/packages/search?${params}`);
      const searchTimeHeader = resp.headers.get("X-Search-Time-Ms");

      if (resp.ok) {
        const data = await resp.json();
        
        if (!data.packages || data.packages.length === 0) {
          setStatus({ type: "warn", text: "Nenhum pacote encontrado para esses critérios." });
        } else {
          setStatus({ type: "", text: "" }); // Limpa o status
          
          const totalPackages = data.packages?.length || 0;

          const combinations =
            data.totalCombinationsEvaluated ??
            data.total_combinations_evaluated ??
            0;

          const gatewayTime =
            data.searchTimeMs ??
            data.search_time_ms ??
            0;

          let metaText =
            `${totalPackages} pacote(s)`;

          if (combinations) {
            metaText += ` · ${combinations} combinações avaliadas`;
          }

          if (gatewayTime) {
            metaText += ` · tempo no gateway: ${gatewayTime} ms`;
          }

          if (searchTimeHeader) {
            metaText += ` (header X-Search-Time-Ms: ${searchTimeHeader} ms)`;
          }

          setMeta(metaText);
          
          setResults(data.packages);
        }
      } else if (resp.status === 400) {
        const body = await resp.json().catch(() => ({}));
        setStatus({ type: "error", text: `Requisição inválida: ${body.message || "verifique os campos."}` });
      } else if (resp.status === 503) {
        const body = await resp.json().catch(() => ({}));
        setStatus({ type: "error", text: `Serviço indisponível: ${body.message || "tente novamente em instantes."}` });
      } else {
        setStatus({ type: "error", text: `Erro inesperado (HTTP ${resp.status}).` });
      }
    } catch (err) {
      console.error(err);
      setStatus({ type: "error", text: "Falha de rede ao contatar o Gateway P." });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="container">
      <Header />
      <SearchForm onSearch={handleSearch} isLoading={isLoading} />
      <StatusMessage status={status} meta={meta} />
      <ResultsTable results={results} />
    </main>
  );
}