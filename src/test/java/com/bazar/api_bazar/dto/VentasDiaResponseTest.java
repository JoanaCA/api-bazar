package com.bazar.api_bazar.dto;

import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VentasDiaResponse")
class VentasDiaResponseTest {

    @Test
    @DisplayName("Cuando SUM() devuelve null (día sin ventas) el monto y la cantidad quedan en cero")
    void sinVentas_devuelveCeros() {
        VentasDiaResponse respuesta = new VentasDiaResponse(null, null);

        assertThat(respuesta.montoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(respuesta.cantidadVentas()).isZero();
    }

    @Test
    @DisplayName("Con datos conserva los valores")
    void conVentas_conservaLosValores() {
        VentasDiaResponse respuesta = new VentasDiaResponse(new BigDecimal("301.50"), 2L);

        assertThat(respuesta.montoTotal()).isEqualByComparingTo("301.50");
        assertThat(respuesta.cantidadVentas()).isEqualTo(2L);
    }
}
