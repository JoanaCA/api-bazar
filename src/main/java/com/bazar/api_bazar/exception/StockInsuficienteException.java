package com.bazar.api_bazar.exception;

/**
 * Se lanza cuando se intenta vender más unidades de las disponibles. Se traduce a HTTP 409.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String nombreProducto, int disponible, int solicitado) {
        super("Stock insuficiente para '%s': disponible %d, solicitado %d"
                .formatted(nombreProducto, disponible, solicitado));
    }
}
