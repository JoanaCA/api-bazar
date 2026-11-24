package com.bazar.api_bazar.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
        String marca,

        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales")
        BigDecimal costo,

        @NotNull(message = "La cantidad disponible es obligatoria")
        @Min(value = 0, message = "La cantidad disponible no puede ser negativa")
        Integer cantidadDisponible
) {
}
