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

import br.unb.pspd.gateway.exception.HotelServiceUnavailableException;
import br.unb.pspd.travel.proto.Hotel;
import br.unb.pspd.travel.proto.HotelSearchRequest;
import br.unb.pspd.travel.proto.HotelSearchResponse;
import br.unb.pspd.travel.proto.HotelServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;

/**
 * Client gRPC para o HotelService (módulo B, porta 50052).
 * O stub é injetado pelo net.devh a partir de {@code grpc.client.hotel-service} no application.yaml.
 */
@Component
public class HotelClient {

    private static final Logger log = LoggerFactory.getLogger(HotelClient.class);
    private static final long DEADLINE_SECONDS = 5;

    @GrpcClient("hotel-service")
    private HotelServiceGrpc.HotelServiceBlockingStub stub;

    /**
     * Busca hotéis na {@code city} (código de aeroporto/cidade, ex.: GIG) com vagas
     * para {@code guests} hóspedes. check_in/check_out são enviados, mas o módulo B
     * não filtra por eles.
     */
    public List<Hotel> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, int guests) {
        HotelSearchRequest.Builder req = HotelSearchRequest.newBuilder()
                .setCity(city)
                .setGuests(guests);
        if (checkIn != null) {
            req.setCheckIn(toTimestamp(checkIn));
        }
        if (checkOut != null) {
            req.setCheckOut(toTimestamp(checkOut));
        }

        long t0 = System.currentTimeMillis();
        try {
            HotelSearchResponse resp = stub
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .searchHotels(req.build());
            long elapsed = System.currentTimeMillis() - t0;
            log.info("hotel.search.time={}ms city={} found={}", elapsed, city, resp.getTotalFound());
            return resp.getHotelsList();
        } catch (StatusRuntimeException e) {
            log.error("Erro gRPC ao chamar HotelService (status={})", e.getStatus(), e);
            throw new HotelServiceUnavailableException(
                    "HotelService (hotéis) indisponível: " + e.getStatus().getCode(), e);
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
