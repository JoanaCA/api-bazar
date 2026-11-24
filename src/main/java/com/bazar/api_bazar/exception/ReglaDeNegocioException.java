package com.bazar.api_bazar.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio (por ejemplo, DNI duplicado
 * o eliminar un recurso que está en uso). Se traduce a HTTP 409.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
