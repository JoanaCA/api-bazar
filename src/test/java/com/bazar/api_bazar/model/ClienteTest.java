package com.bazar.api_bazar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.bazar.api_bazar.support.TestData.cliente;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cliente (dominio)")
class ClienteTest {

    @Test
    @DisplayName("equals compara por id y no considera iguales dos entidades sin id")
    void equals_comparaPorId() {
        Cliente a = cliente(1L, "Ana", "Perez", "30123456");
        Cliente b = cliente(1L, "Otra", "Persona", "99999999");
        Cliente distinto = cliente(2L, "Ana", "Perez", "30123456");
        Cliente sinId1 = new Cliente();
        Cliente sinId2 = new Cliente();

        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .hasSameHashCodeAs(b)
                .isNotEqualTo(distinto)
                .isNotEqualTo(sinId1)
                .isNotEqualTo("no soy cliente");
        assertThat(sinId1).isNotEqualTo(sinId2);
    }
}
