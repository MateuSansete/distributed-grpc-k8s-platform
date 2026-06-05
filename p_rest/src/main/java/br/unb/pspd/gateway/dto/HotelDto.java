package br.unb.pspd.gateway.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

public record HotelDto(
        @JsonAlias("hotel_id") String hotelId,
        String name,
        String city,
        int stars,
        @JsonAlias("price_per_night") MoneyDto pricePerNight,
        @JsonAlias("available_rooms") int availableRooms,
        List<String> amenities) {
}