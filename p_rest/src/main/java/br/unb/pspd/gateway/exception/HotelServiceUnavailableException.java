package br.unb.pspd.gateway.exception;

/**
 * Lançada quando o HotelService (módulo B) está indisponível ou retorna erro gRPC.
 * Mapeada para HTTP 503 pelo {@link br.unb.pspd.gateway.controller.GlobalExceptionHandler}.
 */
public class HotelServiceUnavailableException extends RuntimeException {

    public HotelServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
