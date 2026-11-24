package com.bazar.api_bazar.dto.response;

import java.math.BigDecimal;

/**
 * Reporte de ventas de un día: monto acumulado y cantidad de ventas.
 */
public record VentasDiaResponse(BigDecimal montoTotal, Long cantidadVentas) {

    public VentasDiaResponse {
        // SUM() devuelve null cuando no hay ventas ese día.
        montoTotal = montoTotal == null ? BigDecimal.ZERO : montoTotal;
        cantidadVentas = cantidadVentas == null ? 0L : cantidadVentas;
    }
}
