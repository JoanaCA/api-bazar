package com.bazar.api_bazar.dto.response;

public record ClienteResponse(
        Long idCliente,
        String nombre,
        String apellido,
        String dni
) {
}
