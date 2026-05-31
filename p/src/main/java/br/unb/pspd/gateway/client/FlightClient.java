package br.unb.pspd.gateway.client;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;

import br.unb.pspd.gateway.exception.FlightServiceUnavailableException;
import br.unb.pspd.travel.proto.Flight;
import br.unb.pspd.travel.proto.FlightSearchRequest;
import br.unb.pspd.travel.proto.FlightSearchResponse;
import br.unb.pspd.travel.proto.FlightServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;

/**
 * Client gRPC para o FlightService (módulo A, porta 50051).
 * O stub é injetado pelo net.devh a partir de {@code grpc.client.flight-service} no application.yaml.
 */
@Component
public class FlightClient {

    private static final Logger log = LoggerFactory.getLogger(FlightClient.class);
    private static final long DEADLINE_SECONDS = 5;

    @GrpcClient("flight-service")
    private FlightServiceGrpc.FlightServiceBlockingStub stub;

    /**
     * Busca voos para a rota {@code origin -> destination}. Os filtros casam por
     * código de aeroporto (ex.: BSB, GIG). A {@code departureDate} faz o módulo A
     * retornar voos na data ou depois dela.
     */
    public List<Flight> searchFlights(String origin, String destination, LocalDate departureDate, int passengers) {
        FlightSearchRequest.Builder req = FlightSearchRequest.newBuilder()
                .setOrigin(origin)
                .setDestination(destination)
                .setPassengers(passengers);
        if (departureDate != null) {
            req.setDepartureDate(toTimestamp(departureDate));
        }

        long t0 = System.currentTimeMillis();
        try {
            FlightSearchResponse resp = stub
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .searchFlights(req.build());
            long elapsed = System.currentTimeMillis() - t0;
            log.info("flight.search.time={}ms origin={} destination={} found={}",
                    elapsed, origin, destination, resp.getTotalFound());
            return resp.getFlightsList();
        } catch (StatusRuntimeException e) {
            log.error("Erro gRPC ao chamar FlightService (status={})", e.getStatus(), e);
            throw new FlightServiceUnavailableException(
                    "FlightService (voos) indisponível: " + e.getStatus().getCode(), e);
        }
    }

    private static Timestamp toTimestamp(LocalDate date) {
        Instant instant = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
