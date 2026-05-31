package br.unb.pspd.gateway.exception;

/**
 * Lançada quando o FlightService (módulo A) está indisponível ou retorna erro gRPC.
 * Mapeada para HTTP 503 pelo {@link br.unb.pspd.gateway.controller.GlobalExceptionHandler}.
 */
public class FlightServiceUnavailableException extends RuntimeException {

    public FlightServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
