package br.unb.pspd.gateway.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import br.unb.pspd.gateway.client.FlightClient;
import br.unb.pspd.gateway.client.HotelClient;
import br.unb.pspd.gateway.dto.FlightDto;
import br.unb.pspd.gateway.dto.HotelDto;
import br.unb.pspd.gateway.dto.MoneyDto;
import br.unb.pspd.gateway.dto.PackageSearchResponseDto;
import br.unb.pspd.gateway.dto.TravelPackageDto;

/**
 * Orquestra a busca de pacotes: chama as APIs REST de Voos (A) e Hoteis (B) em
 * paralelo, faz o produto cartesiano voo × hotel, calcula o preço total, ordena
 * por preço crescente e aplica o limite de resultados.
 */
@Service
public class PackageSearchService {

    private static final Logger log = LoggerFactory.getLogger(PackageSearchService.class);
    private static final int DEFAULT_MAX_RESULTS = 50;

    private final FlightClient flightClient;
    private final HotelClient hotelClient;
    private final ExecutorService executor;

    public PackageSearchService(FlightClient flightClient,
                                HotelClient hotelClient,
                                @Qualifier("packageSearchExecutor") ExecutorService executor) {
        this.flightClient = flightClient;
        this.hotelClient = hotelClient;
        this.executor = executor;
    }

    public PackageSearchResponseDto search(String origin, String destination,
                                           LocalDate departureDate, LocalDate returnDate,
                                           int travelers, int maxResults) {
        long t0 = System.currentTimeMillis();

        // 1) Chama A e B EM PARALELO via HTTP REST num executor dedicado.
        CompletableFuture<List<FlightDto>> flightsFuture = CompletableFuture.supplyAsync(
                () -> flightClient.searchFlights(origin, destination, departureDate, travelers), executor);
        CompletableFuture<List<HotelDto>> hotelsFuture = CompletableFuture.supplyAsync(
                () -> hotelClient.searchHotels(destination, departureDate, returnDate, travelers), executor);

        // 2) Aguarda ambas; propaga a exceção real (503) desembrulhando o CompletionException.
        List<FlightDto> flights;
        List<HotelDto> hotels;
        try {
            flights = flightsFuture.join();
            hotels = hotelsFuture.join();
        } catch (CompletionException e) {
            throw unwrap(e);
        }

        // 3) Número de diárias.
        long nights = ChronoUnit.DAYS.between(departureDate, returnDate);
        if (nights < 1) {
            nights = 1;
        }

        // 4) Produto cartesiano voo × hotel + cálculo de preço.
        int evaluated = 0;
        List<TravelPackageDto> packages = new ArrayList<>();
        for (FlightDto f : flights) {
            for (HotelDto h : hotels) {
                evaluated++;
                long total = f.price().amountCents()
                        + h.pricePerNight().amountCents() * nights * travelers;
                String currency = f.price().currency();

                packages.add(new TravelPackageDto(
                        "PKG-" + f.flightId() + "-" + h.hotelId(),
                        f,    // outboundFlight
                        null, // returnFlight: apenas ida nesta entrega
                        h,    //hotel
                        (int) nights,
                        new MoneyDto(currency, total)));
            }
        }

        // 5) Ordena por preço total crescente.
        packages.sort(Comparator.comparingLong(p -> p.totalPrice().amountCents()));

        // 6) Aplica max_results (default 50).
        int max = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;
        if (packages.size() > max) {
            packages = new ArrayList<>(packages.subList(0, max));
        }

        // 7) Métricas para o relatório/teste de performance.
        long elapsed = System.currentTimeMillis() - t0;
        log.info("Package search: {} flights x {} hotels = {} combinations, returned {}, took {}ms",
                flights.size(), hotels.size(), evaluated, packages.size(), elapsed);

        return new PackageSearchResponseDto(packages, evaluated, elapsed);
    }

    /** Desembrulha o CompletionException para que o advice mapeie a exceção real (ex.: 503). */
    private static RuntimeException unwrap(CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException re) {
            return re;
        }
        return e;
    }
}
