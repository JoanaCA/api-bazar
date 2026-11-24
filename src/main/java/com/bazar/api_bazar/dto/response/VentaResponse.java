package com.bazar.api_bazar.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VentaResponse(
        Long codigoVenta,
        LocalDate fechaVenta,
        BigDecimal total,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        List<DetalleVentaResponse> detalles
) {
}
