// Frontend (HClient) do projeto PSPD.
// Consome o gateway gRPC->REST em http://localhost:8080/api/packages/search.

// URL vazia = relativa ao host atual. Em K8s, o nginx faz proxy de /api/ para o gateway.
// Para rodar localmente sem K8s, substitua por "http://localhost:8080".
const GATEWAY = "";

// Aeroportos (códigos IATA usados pelos mocks de A e B) + rótulo amigável.
const AIRPORTS = [
  { code: "BSB", label: "Brasília" },
  { code: "GIG", label: "Rio de Janeiro (Galeão)" },
  { code: "GRU", label: "São Paulo (Guarulhos)" },
  { code: "SDU", label: "Rio de Janeiro (Santos Dumont)" },
  { code: "CNF", label: "Belo Horizonte (Confins)" },
  { code: "REC", label: "Recife" },
  { code: "SSA", label: "Salvador" },
];

const form = document.getElementById("search-form");
const submitBtn = document.getElementById("submit-btn");
const statusEl = document.getElementById("status");
const metaEl = document.getElementById("meta");
const resultsEl = document.getElementById("results");
const tbody = document.querySelector("#results-table tbody");

// Popula os dropdowns de origem/destino.
function fillAirports() {
  const originSel = document.getElementById("origin");
  const destSel = document.getElementById("destination");
  AIRPORTS.forEach((a) => {
    const text = `${a.code} — ${a.label}`;
    originSel.add(new Option(text, a.code));
    destSel.add(new Option(text, a.code));
  });
  originSel.value = "BSB";
  destSel.value = "GIG";
}

// Datas padrão: ida em +10 dias, volta em +13 dias.
function fillDefaultDates() {
  const iso = (d) => d.toISOString().slice(0, 10);
  const dep = new Date();
  dep.setDate(dep.getDate() + 10);
  const ret = new Date();
  ret.setDate(ret.getDate() + 13);
  document.getElementById("departureDate").value = iso(dep);
  document.getElementById("returnDate").value = iso(ret);
}

const brl = (cents) =>
  (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const stars = (n) => "★".repeat(n) + "☆".repeat(Math.max(0, 5 - n));

function fmtTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function showStatus(message, type) {
  statusEl.hidden = false;
  statusEl.className = `status ${type}`;
  statusEl.textContent = message;
}

function clearOutput() {
  statusEl.hidden = true;
  metaEl.hidden = true;
  resultsEl.hidden = true;
  tbody.innerHTML = "";
}

function renderResults(data, searchTimeHeader) {
  const pkgs = data.packages || [];
  if (pkgs.length === 0) {
    showStatus("Nenhum pacote encontrado para esses critérios.", "warn");
    return;
  }

  pkgs.forEach((p, i) => {
    const f = p.outboundFlight;
    const h = p.hotel;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${i + 1}</td>
      <td><strong>${f.airline}</strong><br><span class="muted">${f.flightId}</span></td>
      <td>${f.origin} → ${f.destination}<br><span class="muted">${fmtTime(f.departureTime)}</span></td>
      <td>${h.name}<br><span class="muted">${stars(h.stars)} · ${h.city}</span></td>
      <td class="num">${p.nights}</td>
      <td class="num"><strong>${brl(p.totalPrice.amountCents)}</strong></td>`;
    tbody.appendChild(tr);
  });

  metaEl.hidden = false;
  metaEl.textContent =
    `${pkgs.length} pacote(s) · ${data.totalCombinationsEvaluated} combinações avaliadas · ` +
    `tempo no gateway: ${data.searchTimeMs} ms` +
    (searchTimeHeader ? ` (header X-Search-Time-Ms: ${searchTimeHeader} ms)` : "");
  resultsEl.hidden = false;
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  clearOutput();
  submitBtn.disabled = true;
  showStatus("Buscando…", "info");

  const params = new URLSearchParams({
    origin: form.origin.value,
    destination: form.destination.value,
    departureDate: form.departureDate.value,
    returnDate: form.returnDate.value,
    travelers: form.travelers.value,
  });
  if (form.maxResults.value) params.set("maxResults", form.maxResults.value);

  try {
    const resp = await fetch(`${GATEWAY}/api/packages/search?${params}`);
    const searchTimeHeader = resp.headers.get("X-Search-Time-Ms");

    if (resp.ok) {
      const data = await resp.json();
      statusEl.hidden = true;
      renderResults(data, searchTimeHeader);
    } else if (resp.status === 400) {
      const body = await resp.json().catch(() => ({}));
      showStatus(`Requisição inválida: ${body.message || "verifique os campos."}`, "error");
    } else if (resp.status === 503) {
      const body = await resp.json().catch(() => ({}));
      showStatus(`Serviço indisponível: ${body.message || "tente novamente em instantes."}`, "error");
    } else {
      showStatus(`Erro inesperado (HTTP ${resp.status}).`, "error");
    }
  } catch (err) {
    showStatus(
      "Não foi possível conectar ao gateway (localhost:8080). Ele está rodando?",
      "error"
    );
  } finally {
    submitBtn.disabled = false;
  }
});

fillAirports();
fillDefaultDates();
