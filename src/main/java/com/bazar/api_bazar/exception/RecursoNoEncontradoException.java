package com.bazar.api_bazar.exception;

/**
 * Se lanza cuando un recurso solicitado no existe. Se traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String recurso, Object id) {
        super("No se encontró %s con id %s".formatted(recurso, id));
    }
}
