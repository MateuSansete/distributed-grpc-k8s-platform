package br.unb.pspd.gateway.client;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.unb.pspd.gateway.dto.FlightDto;
import br.unb.pspd.gateway.dto.FlightSearchRestResponse;
import br.unb.pspd.gateway.exception.FlightServiceUnavailableException;

@Component
public class FlightClient {

    private static final Logger log = LoggerFactory.getLogger(FlightClient.class);
    private final WebClient webClient;

    public FlightClient(WebClient.Builder webClientBuilder, 
                        @Value("${rest.client.flight-service.url}") String flightUrl) {
        this.webClient = webClientBuilder.baseUrl(flightUrl).build();
    }

    public List<FlightDto> searchFlights(String origin, String destination, LocalDate departureDate, int passengers) {
        long t0 = System.currentTimeMillis();
        try {
            FlightSearchRestResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("departure_date", departureDate != null ? departureDate.toString() : null)
                    .queryParam("passengers", passengers)
                    .build())
                .retrieve()
                .bodyToMono(FlightSearchRestResponse.class)
                .block();

            long elapsed = System.currentTimeMillis() - t0;
            int found = response != null && response.flights() != null ? response.flights().size() : 0;
            log.info("flight.search.rest.time={}ms origin={} destination={} found={}", elapsed, origin, destination, found);
            return response != null ? response.flights() : List.of();

        } catch (WebClientResponseException e) {
            log.error("Erro REST ao chamar FlightService (status={})", e.getStatusCode(), e);
            throw new FlightServiceUnavailableException("FlightService (voos) indisponível: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Erro de conexão com FlightService REST", e);
            throw new FlightServiceUnavailableException("Falha de conexão com FlightService REST", e);
        }
    }
}