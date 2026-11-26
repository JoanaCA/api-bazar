package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.dto.request.VentaRequest;
import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.MayorVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.StockInsuficienteException;
import com.bazar.api_bazar.service.IVentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VentaController.class)
@DisplayName("VentaController (Web)")
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IVentaService ventaService;

    private static final String VENTA_VALIDA_JSON = """
            {
              "clienteId": 1,
              "fechaVenta": "2026-01-15",
              "items": [
                { "productoId": 10, "cantidad": 3 }
              ]
            }
            """;

    private static VentaResponse ventaEjemplo() {
        return new VentaResponse(
                1L,
                LocalDate.of(2026, 1, 15),
                new BigDecimal("301.50"),
                1L,
                "Ana",
                "Perez",
                List.of(new DetalleVentaResponse(10L, "Taza", 3, new BigDecimal("100.50"), new BigDecimal("301.50"))));
    }

    @Test
    @DisplayName("POST /api/v1/ventas válido devuelve 201 y Location")
    void crear_valido_devuelve201YLocation() throws Exception {
        when(ventaService.crear(any(VentaRequest.class))).thenReturn(ventaEjemplo());

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/ventas/1")))
                .andExpect(jsonPath("$.codigoVenta").value(1))
                .andExpect(jsonPath("$.total").value(301.50))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3));
    }

    @Test
    @DisplayName("POST /api/v1/ventas sin items devuelve 400")
    void crear_sinItems_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": 1,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.items").exists());

        verifyNoInteractions(ventaService);
    }

    @Test
    @DisplayName("POST /api/v1/ventas con stock insuficiente devuelve 409")
    void crear_stockInsuficiente_devuelve409() throws Exception {
        when(ventaService.crear(any(VentaRequest.class)))
                .thenThrow(new StockInsuficienteException("Taza", 2, 5));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stock insuficiente"));
    }

    @Test
    @DisplayName("POST /api/v1/ventas con cliente o producto inexistente devuelve 404")
    void crear_recursoInexistente_devuelve404() throws Exception {
        when(ventaService.crear(any(VentaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("producto", 99L));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/ventas con conflicto optimista devuelve 409")
    void crear_conflictoConcurrencia_devuelve409() throws Exception {
        when(ventaService.crear(any(VentaRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("Producto", 10L));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de concurrencia"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas devuelve página de ventas")
    void listar_devuelvePagina() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(ventaService.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ventaEjemplo()), pageable, 1));

        mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoVenta").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id} existente devuelve 200")
    void obtener_existente_devuelve200() throws Exception {
        when(ventaService.obtener(1L)).thenReturn(ventaEjemplo());

        mockMvc.perform(get("/api/v1/ventas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNombre").value("Ana"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id} inexistente devuelve 404")
    void obtener_inexistente_devuelve404() throws Exception {
        when(ventaService.obtener(99L))
                .thenThrow(new RecursoNoEncontradoException("venta", 99L));

        mockMvc.perform(get("/api/v1/ventas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/ventas/{id} válido devuelve 200")
    void actualizar_valido_devuelve200() throws Exception {
        when(ventaService.actualizar(eq(1L), any(VentaRequest.class))).thenReturn(ventaEjemplo());

        mockMvc.perform(put("/api/v1/ventas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoVenta").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/ventas/{id} inválido devuelve 400")
    void actualizar_invalido_devuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/ventas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": null,
                                  "items": [{ "productoId": 10, "cantidad": 0 }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.clienteId").exists())
                .andExpect(jsonPath("$.errores['items[0].cantidad']").exists());

        verifyNoInteractions(ventaService);
    }

    @Test
    @DisplayName("PUT /api/v1/ventas/{id} con stock insuficiente devuelve 409")
    void actualizar_stockInsuficiente_devuelve409() throws Exception {
        when(ventaService.actualizar(eq(1L), any(VentaRequest.class)))
                .thenThrow(new StockInsuficienteException("Taza", 1, 5));

        mockMvc.perform(put("/api/v1/ventas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/ventas/{id} inexistente devuelve 404")
    void actualizar_inexistente_devuelve404() throws Exception {
        when(ventaService.actualizar(eq(99L), any(VentaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("venta", 99L));

        mockMvc.perform(put("/api/v1/ventas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VENTA_VALIDA_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/ventas/{id} devuelve 204")
    void eliminar_existente_devuelve204() throws Exception {
        doNothing().when(ventaService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/ventas/1"))
                .andExpect(status().isNoContent());

        verify(ventaService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/ventas/{id} inexistente devuelve 404")
    void eliminar_inexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("venta", 99L))
                .when(ventaService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/ventas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id}/productos devuelve la lista de detalles (requerimiento 5)")
    void listarProductos_existente_devuelve200() throws Exception {
        when(ventaService.listarProductos(1L)).thenReturn(ventaEjemplo().detalles());

        mockMvc.perform(get("/api/v1/ventas/1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoNombre").value("Taza"))
                .andExpect(jsonPath("$[0].cantidad").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id}/productos de venta inexistente devuelve 404")
    void listarProductos_inexistente_devuelve404() throws Exception {
        when(ventaService.listarProductos(99L))
                .thenThrow(new RecursoNoEncontradoException("venta", 99L));

        mockMvc.perform(get("/api/v1/ventas/99/productos"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/ventas/reportes/dia/{fecha} devuelve totales (requerimiento 6)")
    void totalesPorDia_fechaValida_devuelve200() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 1, 15);
        when(ventaService.totalesPorDia(fecha))
                .thenReturn(new VentasDiaResponse(new BigDecimal("301.50"), 2L));

        mockMvc.perform(get("/api/v1/ventas/reportes/dia/2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoTotal").value(301.50))
                .andExpect(jsonPath("$.cantidadVentas").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/reportes/dia/{fecha} con fecha inválida devuelve 400")
    void totalesPorDia_fechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/reportes/dia/2026-13-45"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ventaService);
    }

    @Test
    @DisplayName("GET /api/v1/ventas/reportes/mayor devuelve la mayor venta (requerimiento 7)")
    void mayorVenta_conVentas_devuelve200() throws Exception {
        when(ventaService.mayorVenta()).thenReturn(
                new MayorVentaResponse(1L, new BigDecimal("301.50"), 3, "Ana", "Perez"));

        mockMvc.perform(get("/api/v1/ventas/reportes/mayor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoVenta").value(1))
                .andExpect(jsonPath("$.cantidadProductos").value(3))
                .andExpect(jsonPath("$.apellidoCliente").value("Perez"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/reportes/mayor sin ventas devuelve 404")
    void mayorVenta_sinVentas_devuelve404() throws Exception {
        when(ventaService.mayorVenta())
                .thenThrow(new RecursoNoEncontradoException("No hay ventas registradas"));

        mockMvc.perform(get("/api/v1/ventas/reportes/mayor"))
                .andExpect(status().isNotFound());
    }
}
