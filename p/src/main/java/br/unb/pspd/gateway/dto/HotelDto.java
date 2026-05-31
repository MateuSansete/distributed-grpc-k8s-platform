package br.unb.pspd.gateway.dto;

import java.util.List;

import br.unb.pspd.travel.proto.Hotel;

/** Hotel em formato JSON limpo. */
public record HotelDto(
        String hotelId,
        String name,
        String city,
        int stars,
        MoneyDto pricePerNight,
        int availableRooms,
        List<String> amenities) {

    public static HotelDto from(Hotel h) {
        return new HotelDto(
                h.getHotelId(),
                h.getName(),
                h.getCity(),
                h.getStars(),
                MoneyDto.from(h.getPricePerNight()),
                h.getAvailableRooms(),
                List.copyOf(h.getAmenitiesList()));
    }
}
