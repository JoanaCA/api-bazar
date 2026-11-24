package com.bazar.api_bazar.dto.response;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
