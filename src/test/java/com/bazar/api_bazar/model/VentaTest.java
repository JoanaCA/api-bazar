package com.bazar.api_bazar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.detalle;
import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Venta (dominio)")
class VentaTest {

    @Test
    @DisplayName("restituirStock devuelve a cada producto las unidades de su detalle")
    void restituirStock_variosDetalles_devuelveLasUnidadesACadaProducto() {
        Producto taza = producto(1L, "Taza", "100.00", 7);
        Producto plato = producto(2L, "Plato", "50.00", 0);
        Venta venta = venta(1L, cliente(1L, "Ana", "Perez", "30123456"), LocalDate.of(2026, 1, 15),
                detalle(taza, 3), detalle(plato, 2));

        venta.restituirStock();

        assertThat(taza.getCantidadDisponible()).isEqualTo(10);
        assertThat(plato.getCantidadDisponible()).isEqualTo(2);
    }

    @Test
    @DisplayName("restituirStock no falla en una venta sin detalles")
    void restituirStock_sinDetalles_noHaceNada() {
        Venta venta = new Venta();

        assertThatCode(venta::restituirStock).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Una venta nueva arranca con total en cero y sin detalles")
    void ventaNueva_valoresIniciales() {
        Venta venta = new Venta();

        assertThat(venta.getTotal()).isEqualByComparingTo("0");
        assertThat(venta.getDetalles()).isEmpty();
    }

    @Test
    @DisplayName("Dos ventas con el mismo id son iguales; sin id nunca lo son")
    void equals_comparaPorId() {
        Venta a = new Venta();
        a.setCodigoVenta(1L);
        Venta b = new Venta();
        b.setCodigoVenta(1L);
        Venta distinta = new Venta();
        distinta.setCodigoVenta(2L);
        Venta sinId1 = new Venta();
        Venta sinId2 = new Venta();

        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .hasSameHashCodeAs(b)
                .isNotEqualTo(distinta)
                .isNotEqualTo(sinId1)
                .isNotEqualTo("no soy venta");
        assertThat(sinId1).isNotEqualTo(sinId2);
    }
}
