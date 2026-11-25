package com.bazar.api_bazar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.bazar.api_bazar.support.TestData.detalle;
import static com.bazar.api_bazar.support.TestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DetalleVenta (dominio)")
class DetalleVentaTest {

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({
            "100.50, 3, 301.50",
            "0.10, 3, 0.30",
            "1500.00, 1, 1500.00",
            "19.99, 100, 1999.00"
    })
    @DisplayName("El subtotal es precio unitario por cantidad, sin errores de redondeo")
    void getSubtotal_multiplicaPrecioPorCantidad(String precio, int cantidad, String esperado) {
        DetalleVenta detalle = detalle(producto(1L, "Taza", precio, 100), cantidad);

        assertThat(detalle.getSubtotal()).isEqualByComparingTo(new BigDecimal(esperado));
    }

    @Test
    @DisplayName("El precio del detalle no cambia si luego cambia el costo del producto")
    void precioUnitario_esUnaCopiaHistorica() {
        Producto taza = producto(1L, "Taza", "100.00", 10);
        DetalleVenta detalle = detalle(taza, 2);

        taza.setCosto(new BigDecimal("999.00"));

        assertThat(detalle.getPrecioUnitario()).isEqualByComparingTo("100.00");
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("200.00");
    }
}
