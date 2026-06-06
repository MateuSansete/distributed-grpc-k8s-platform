package br.unb.pspd.gateway.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.unb.pspd.gateway.exception.FlightServiceUnavailableException;
import br.unb.pspd.gateway.exception.HotelServiceUnavailableException;
import jakarta.validation.ConstraintViolationException;

/** Centraliza o mapeamento de exceções para respostas HTTP. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 503: serviços gRPC indisponíveis ----

    @ExceptionHandler(FlightServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleFlightUnavailable(FlightServiceUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(HotelServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleHotelUnavailable(HotelServiceUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    // ---- 400: entrada inválida ----

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        log.warn("HTTP {} -> {}", status.value(), message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
