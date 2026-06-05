package br.unb.pspd.gateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record FlightDto(
        @JsonAlias("flight_id") String flightId,
        String airline,
        String origin,
        String destination,
        @JsonAlias("departure_time") String departureTime,
        @JsonAlias("arrival_time") String arrivalTime,
        @JsonAlias("duration_minutes") int durationMinutes,
        MoneyDto price,
        @JsonAlias("available_seats") int availableSeats) {
}
