package br.unb.pspd.gateway.dto;

/**
 * Pacote de viagem (voo de ida + hotel) já calculado, em formato JSON.
 * {@code returnFlight} fica nulo nesta entrega (apenas ida).
 */
public record TravelPackageDto(
        String packageId,
        FlightDto outboundFlight,
        FlightDto returnFlight,
        HotelDto hotel,
        int nights,
        MoneyDto totalPrice) {
}
