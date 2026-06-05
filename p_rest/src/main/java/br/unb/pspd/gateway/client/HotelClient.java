package br.unb.pspd.gateway.client;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.unb.pspd.gateway.dto.HotelDto;
import br.unb.pspd.gateway.dto.HotelSearchRestResponse;
import br.unb.pspd.gateway.exception.HotelServiceUnavailableException;

@Component
public class HotelClient {

    private static final Logger log = LoggerFactory.getLogger(HotelClient.class);
    private final WebClient webClient;

    public HotelClient(WebClient.Builder webClientBuilder, 
                       @Value("${rest.client.hotel-service.url}") String hotelUrl) {
        this.webClient = webClientBuilder.baseUrl(hotelUrl).build();
    }

    public List<HotelDto> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, int guests) {
        long t0 = System.currentTimeMillis();
        try {
            HotelSearchRestResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("city", city)
                    .queryParam("guests", guests)
                    .build())
                .retrieve()
                .bodyToMono(HotelSearchRestResponse.class)
                .block();

            long elapsed = System.currentTimeMillis() - t0;
            int found = response != null && response.hotels() != null ? response.hotels().size() : 0;
            log.info("hotel.search.rest.time={}ms city={} found={}", elapsed, city, found);
            return response != null ? response.hotels() : List.of();

        } catch (WebClientResponseException e) {
            log.error("Erro REST ao chamar HotelService (status={})", e.getStatusCode(), e);
            throw new HotelServiceUnavailableException("HotelService (hotéis) indisponível: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Erro de conexão com HotelService REST", e);
            throw new HotelServiceUnavailableException("Falha de conexão com HotelService REST", e);
        }
    }
}