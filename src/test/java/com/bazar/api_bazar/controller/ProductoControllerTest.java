package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.service.IProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(ProductoController.class)
@DisplayName("ProductoController (Web)")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProductoService productoService;

    private static final String PRODUCTO_VALIDO_JSON = """
            {
              "nombre": "Taza",
              "marca": "Acme",
              "costo": 100.50,
              "cantidadDisponible": 10
            }
            """;

    @Test
    @DisplayName("POST /api/v1/productos válido devuelve 201, Location y el cuerpo")
    void crear_valido_devuelve201YLocation() throws Exception {
        when(productoService.crear(any(ProductoRequest.class)))
                .thenReturn(new ProductoResponse(1L, "Taza", "Acme", new BigDecimal("100.50"), 10));

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_VALIDO_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/productos/1")))
                .andExpect(jsonPath("$.codigoProducto").value(1))
                .andExpect(jsonPath("$.nombre").value("Taza"))
                .andExpect(jsonPath("$.costo").value(100.50))
                .andExpect(jsonPath("$.cantidadDisponible").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/productos inválido devuelve 400 con errores por campo")
    void crear_invalido_devuelve400ConErrores() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "marca": "Acme",
                                  "costo": -1,
                                  "cantidadDisponible": -5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.costo").exists())
                .andExpect(jsonPath("$.errores.cantidadDisponible").exists());

        verifyNoInteractions(productoService);
    }

    @Test
    @DisplayName("POST /api/v1/productos con JSON mal formado devuelve 400")
    void crear_jsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{mal formado"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productoService);
    }

    @Test
    @DisplayName("GET /api/v1/productos devuelve página con content y page, y pasa el Pageable al service")
    void listar_devuelvePagina() throws Exception {
        Pageable esperado = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("nombre")));
        when(productoService.listar(esperado)).thenReturn(new PageImpl<>(
                List.of(new ProductoResponse(1L, "Taza", "Acme", new BigDecimal("100.50"), 10)),
                esperado, 1));

        mockMvc.perform(get("/api/v1/productos")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "nombre,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Taza"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(5));

        verify(productoService).listar(esperado);
    }

    @Test
    @DisplayName("GET /api/v1/productos/stock-bajo devuelve página de productos")
    void listarStockBajo_devuelvePagina() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(productoService.listarStockBajo(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new ProductoResponse(2L, "Plato", "Acme", new BigDecimal("50.00"), 2)),
                pageable, 1));

        mockMvc.perform(get("/api/v1/productos/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoProducto").value(2))
                .andExpect(jsonPath("$.content[0].cantidadDisponible").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} existente devuelve 200")
    void obtener_existente_devuelve200() throws Exception {
        when(productoService.obtener(1L))
                .thenReturn(new ProductoResponse(1L, "Taza", "Acme", new BigDecimal("100.50"), 10));

        mockMvc.perform(get("/api/v1/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoProducto").value(1))
                .andExpect(jsonPath("$.nombre").value("Taza"));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} inexistente devuelve 404 con ProblemDetail")
    void obtener_inexistente_devuelve404() throws Exception {
        when(productoService.obtener(99L))
                .thenThrow(new RecursoNoEncontradoException("producto", 99L));

        mockMvc.perform(get("/api/v1/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No se encontró producto con id 99"));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} con tipo inválido devuelve 400")
    void obtener_idNoNumerico_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/productos/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productoService);
    }

    @Test
    @DisplayName("PUT /api/v1/productos/{id} válido devuelve 200")
    void actualizar_valido_devuelve200() throws Exception {
        when(productoService.actualizar(eq(1L), any(ProductoRequest.class)))
                .thenReturn(new ProductoResponse(1L, "Taza", "Acme", new BigDecimal("100.50"), 10));

        mockMvc.perform(put("/api/v1/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_VALIDO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoProducto").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/productos/{id} inválido devuelve 400")
    void actualizar_invalido_devuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "marca": "",
                                  "costo": 10,
                                  "cantidadDisponible": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.marca").exists());

        verifyNoInteractions(productoService);
    }

    @Test
    @DisplayName("PUT /api/v1/productos/{id} inexistente devuelve 404")
    void actualizar_inexistente_devuelve404() throws Exception {
        when(productoService.actualizar(eq(99L), any(ProductoRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("producto", 99L));

        mockMvc.perform(put("/api/v1/productos/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_VALIDO_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} devuelve 204")
    void eliminar_existente_devuelve204() throws Exception {
        doNothing().when(productoService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/productos/1"))
                .andExpect(status().isNoContent());

        verify(productoService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} con ventas asociadas devuelve 409")
    void eliminar_enUso_devuelve409() throws Exception {
        doThrow(new ReglaDeNegocioException("No se puede eliminar el producto 1 porque forma parte de ventas registradas"))
                .when(productoService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/productos/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"));
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} inexistente devuelve 404")
    void eliminar_inexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("producto", 99L))
                .when(productoService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/productos/99"))
                .andExpect(status().isNotFound());
    }
}
