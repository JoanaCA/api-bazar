package com.bazar.api_bazar.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("RecursoNoEncontradoException -> 404 con mensaje de dominio")
    void manejarNoEncontrado() {
        ProblemDetail problema = handler.manejarNoEncontrado(new RecursoNoEncontradoException("producto", 5L));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getTitle()).isEqualTo("Recurso no encontrado");
        assertThat(problema.getDetail()).isEqualTo("No se encontró producto con id 5");
    }

    @Test
    @DisplayName("StockInsuficienteException -> 409 con mensaje de stock")
    void manejarStockInsuficiente() {
        ProblemDetail problema = handler.manejarStockInsuficiente(new StockInsuficienteException("Taza", 2, 5));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getTitle()).isEqualTo("Stock insuficiente");
        assertThat(problema.getDetail()).contains("Taza", "disponible 2", "solicitado 5");
    }

    @Test
    @DisplayName("ReglaDeNegocioException -> 409")
    void manejarReglaDeNegocio() {
        ProblemDetail problema = handler.manejarReglaDeNegocio(new ReglaDeNegocioException("DNI duplicado"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getTitle()).isEqualTo("Regla de negocio incumplida");
        assertThat(problema.getDetail()).isEqualTo("DNI duplicado");
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException -> 409 sin detalles técnicos")
    void manejarConcurrencia() {
        ProblemDetail problema = handler.manejarConcurrencia(
                new ObjectOptimisticLockingFailureException("Producto", 1L));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getTitle()).isEqualTo("Conflicto de concurrencia");
        assertThat(problema.getDetail()).doesNotContain("Optimistic", "Producto");
    }

    @Test
    @DisplayName("Exception genérica -> 500 sin filtrar el detalle interno")
    void manejarErrorInesperado_noFiltraDetallesInternos() {
        ProblemDetail problema = handler.manejarErrorInesperado(
                new RuntimeException("SELECT * FROM secreto_interno falló en 10.0.0.5"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problema.getTitle()).isEqualTo("Error interno del servidor");
        assertThat(problema.getDetail()).doesNotContain("SELECT", "secreto_interno", "10.0.0.5");
    }
}
