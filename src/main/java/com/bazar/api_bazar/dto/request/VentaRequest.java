package com.bazar.api_bazar.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos para registrar o reemplazar una venta. El total NO se recibe: lo calcula el backend.
 */
public record VentaRequest(
        @NotNull(message = "El id del cliente es obligatorio")
        Long clienteId,

        /** Opcional: si no se envía se usa la fecha actual. */
        LocalDate fechaVenta,

        @NotEmpty(message = "La venta debe incluir al menos un producto")
        List<@Valid @NotNull(message = "Los items no pueden ser nulos") ItemVentaRequest> items
) {
}
