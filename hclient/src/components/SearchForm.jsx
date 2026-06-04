import { useState } from "react";
import { AIRPORTS } from "../utils/formatters";

export default function SearchForm({ onSearch, isLoading }) {
  const [formData, setFormData] = useState({
    origin: "BSB",
    destination: "GRU",
    departureDate: "",
    returnDate: "",
    travelers: 2,
    maxResults: 20,
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch(formData);
  };

  return (
    <form className="card" onSubmit={handleSubmit}>
      <div className="grid">
        <label>
          Origem
          <select name="origin" value={formData.origin} onChange={handleChange} required>
            {AIRPORTS.map((a) => (
              <option key={a.code} value={a.code}>{a.code} — {a.label}</option>
            ))}
          </select>
        </label>
        
        <label>
          Destino
          <select name="destination" value={formData.destination} onChange={handleChange} required>
            {AIRPORTS.map((a) => (
              <option key={a.code} value={a.code}>{a.code} — {a.label}</option>
            ))}
          </select>
        </label>
        
        <label>
          Data de ida
          <input type="date" name="departureDate" value={formData.departureDate} onChange={handleChange} required />
        </label>
        
        <label>
          Data de volta
          <input type="date" name="returnDate" value={formData.returnDate} onChange={handleChange} required />
        </label>
        
        <label>
          Viajantes
          <input type="number" name="travelers" min="1" value={formData.travelers} onChange={handleChange} required />
        </label>
        
        <label>
          Máx. resultados
          <input type="number" name="maxResults" min="1" value={formData.maxResults} onChange={handleChange} />
        </label>
      </div>
      <button type="submit" disabled={isLoading}>
        {isLoading ? "Buscando..." : "Buscar pacotes"}
      </button>
    </form>
  );
}