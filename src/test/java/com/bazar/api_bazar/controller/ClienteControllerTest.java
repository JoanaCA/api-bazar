package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.response.ClienteResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.service.IClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(ClienteController.class)
@DisplayName("ClienteController (Web)")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClienteService clienteService;

    private static final String CLIENTE_VALIDO_JSON = """
            {
              "nombre": "Ana",
              "apellido": "Perez",
              "dni": "30123456"
            }
            """;

    @Test
    @DisplayName("POST /api/v1/clientes válido devuelve 201 y Location")
    void crear_valido_devuelve201YLocation() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class)))
                .thenReturn(new ClienteResponse(1L, "Ana", "Perez", "30123456"));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/clientes/1")))
                .andExpect(jsonPath("$.idCliente").value(1))
                .andExpect(jsonPath("$.dni").value("30123456"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes con DNI inválido devuelve 400")
    void crear_dniInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Perez",
                                  "dni": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dni").exists());

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("POST /api/v1/clientes con DNI duplicado devuelve 409")
    void crear_dniDuplicado_devuelve409() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new ReglaDeNegocioException("Ya existe un cliente con el DNI 30123456"));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes devuelve página de clientes")
    void listar_devuelvePagina() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new ClienteResponse(1L, "Ana", "Perez", "30123456")),
                pageable, 1));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} existente devuelve 200")
    void obtener_existente_devuelve200() throws Exception {
        when(clienteService.obtener(1L))
                .thenReturn(new ClienteResponse(1L, "Ana", "Perez", "30123456"));

        mockMvc.perform(get("/api/v1/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Perez"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} inexistente devuelve 404")
    void obtener_inexistente_devuelve404() throws Exception {
        when(clienteService.obtener(99L))
                .thenThrow(new RecursoNoEncontradoException("cliente", 99L));

        mockMvc.perform(get("/api/v1/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} válido devuelve 200")
    void actualizar_valido_devuelve200() throws Exception {
        when(clienteService.actualizar(eq(1L), any(ClienteRequest.class)))
                .thenReturn(new ClienteResponse(1L, "Ana", "Perez", "30123456"));

        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} inválido devuelve 400")
    void actualizar_invalido_devuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "apellido": "Perez",
                                  "dni": "30123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} con DNI de otro cliente devuelve 409")
    void actualizar_dniDuplicado_devuelve409() throws Exception {
        when(clienteService.actualizar(eq(1L), any(ClienteRequest.class)))
                .thenThrow(new ReglaDeNegocioException("Ya existe otro cliente con el DNI 30123456"));

        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} inexistente devuelve 404")
    void actualizar_inexistente_devuelve404() throws Exception {
        when(clienteService.actualizar(eq(99L), any(ClienteRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("cliente", 99L));

        mockMvc.perform(put("/api/v1/clientes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} devuelve 204")
    void eliminar_existente_devuelve204() throws Exception {
        doNothing().when(clienteService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isNoContent());

        verify(clienteService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} con ventas devuelve 409")
    void eliminar_conVentas_devuelve409() throws Exception {
        doThrow(new ReglaDeNegocioException("No se puede eliminar el cliente 1 porque tiene ventas registradas"))
                .when(clienteService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} inexistente devuelve 404")
    void eliminar_inexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("cliente", 99L))
                .when(clienteService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/clientes/99"))
                .andExpect(status().isNotFound());
    }
}
