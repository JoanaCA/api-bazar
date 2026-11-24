package com.bazar.api_bazar.dto.response;

import java.math.BigDecimal;

/**
 * Venta de mayor monto. {@code cantidadProductos} es el total de unidades vendidas.
 */
public record MayorVentaResponse(
        Long codigoVenta,
        BigDecimal total,
        Integer cantidadProductos,
        String nombreCliente,
        String apellidoCliente
) {
}
