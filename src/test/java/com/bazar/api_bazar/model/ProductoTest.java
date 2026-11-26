package com.bazar.api_bazar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.bazar.api_bazar.support.TestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Producto (dominio)")
class ProductoTest {

    @Test
    @DisplayName("equals compara por id y no considera iguales dos entidades sin id")
    void equals_comparaPorId() {
        Producto a = producto(1L, "Taza", "100.00", 10);
        Producto b = producto(1L, "Otro nombre", "50.00", 2);
        Producto distinto = producto(2L, "Taza", "100.00", 10);
        Producto sinId1 = new Producto();
        Producto sinId2 = new Producto();

        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .hasSameHashCodeAs(b)
                .isNotEqualTo(distinto)
                .isNotEqualTo(sinId1)
                .isNotEqualTo("no soy producto");
        assertThat(sinId1).isNotEqualTo(sinId2);
    }
}
