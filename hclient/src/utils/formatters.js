export const AIRPORTS = [
  { code: "BSB", label: "Brasília" },
  { code: "GIG", label: "Rio de Janeiro (Galeão)" },
  { code: "GRU", label: "São Paulo (Guarulhos)" },
  { code: "SDU", label: "Rio de Janeiro (Santos Dumont)" },
  { code: "CNF", label: "Belo Horizonte (Confins)" },
  { code: "REC", label: "Recife" },
  { code: "SSA", label: "Salvador" },
];

export function formatMoney(money) {
  if (!money) return "N/A";

  return new Intl.NumberFormat(
    "pt-BR",
    {
      style: "currency",
      currency: money.currency || "BRL",
    }
  ).format(
    (money.amountCents || 0) / 100
  );
}

export function formatDate(dateString) {
  if (!dateString) return "N/A";
  const d = new Date(dateString);
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(d);
}

export function formatDuration(minutes) {
  if (!minutes) return "N/A";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
}