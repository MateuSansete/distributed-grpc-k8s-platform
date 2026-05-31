package br.unb.pspd.gateway.dto;

import java.time.Instant;

import com.google.protobuf.Timestamp;

import br.unb.pspd.travel.proto.Flight;

/** Voo em formato JSON limpo (timestamps ISO-8601, preço plano). */
public record FlightDto(
        String flightId,
        String airline,
        String origin,
        String destination,
        String departureTime,
        String arrivalTime,
        int durationMinutes,
        MoneyDto price,
        int availableSeats) {

    public static FlightDto from(Flight f) {
        return new FlightDto(
                f.getFlightId(),
                f.getAirline(),
                f.getOrigin(),
                f.getDestination(),
                toIso(f.getDepartureTime()),
                toIso(f.getArrivalTime()),
                f.getDurationMinutes(),
                MoneyDto.from(f.getPrice()),
                f.getAvailableSeats());
    }

    private static String toIso(Timestamp ts) {
        if (ts == null || (ts.getSeconds() == 0 && ts.getNanos() == 0)) {
            return null;
        }
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toString();
    }
}
