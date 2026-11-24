package com.bazar.api_bazar.dto.response;

import java.math.BigDecimal;

public record ProductoResponse(
        Long codigoProducto,
        String nombre,
        String marca,
        BigDecimal costo,
        Integer cantidadDisponible
) {
}
