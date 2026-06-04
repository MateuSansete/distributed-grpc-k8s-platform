import {
  formatDate,
  formatDuration,
  formatMoney,
} from "../utils/formatters";

export default function ResultsTable({ results }) {
  if (!results?.length) return null;

  return (
    <section className="results-grid">
      {results.map((pkg, index) => {
        const flight = pkg.outboundFlight;
        const hotel = pkg.hotel;

        return (
          <article
            key={pkg.packageId}
            className={`travel-card ${
              index === 0 ? "best-offer-card" : ""
            }`}
          >
            {index === 0 && (
              <div className="best-offer">
                ⭐ Melhor Oferta
              </div>
            )}

            <div className="card-top">
              <div>
                <div className="airline-name">
                  {flight?.airline}
                </div>

                <div className="flight-id">
                  {flight?.flightId}
                </div>
              </div>

              <div className="price-tag">
                {formatMoney(pkg.totalPrice)}
              </div>
            </div>

            <div className="route-section">
              <div className="airport-block">
                <div className="airport-code">
                  {flight?.origin}
                </div>

                <div className="airport-time">
                  {formatDate(flight?.departureTime)}
                </div>
              </div>

              <div className="route-line">
                ✈️
              </div>

              <div className="airport-block">
                <div className="airport-code">
                  {flight?.destination}
                </div>

                <div className="airport-time">
                  {formatDate(flight?.arrivalTime)}
                </div>
              </div>
            </div>

            <div className="flight-info">
              <div className="info-chip">
                ⏱ {formatDuration(flight?.durationMinutes)}
              </div>

              <div className="info-chip">
                👥 {flight?.availableSeats} assentos
              </div>

              <div className="info-chip">
                🌙 {pkg.nights} noites
              </div>
            </div>

            <div className="divider"></div>

            <div className="section-label">
              HOTEL
            </div>

            <div className="hotel-title">
              {hotel?.name}
            </div>

            <div className="hotel-city">
              📍 {hotel?.city}
            </div>

            <div className="hotel-stars">
              {"⭐".repeat(hotel?.stars || 0)}
            </div>

            <div className="amenities">
              {hotel?.amenities?.map((item) => (
                <span
                  key={item}
                  className="amenity-pill"
                >
                  {item}
                </span>
              ))}
            </div>
          </article>
        );
      })}
    </section>
  );
}