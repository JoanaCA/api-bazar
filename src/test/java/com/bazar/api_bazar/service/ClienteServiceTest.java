package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.response.ClienteResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.mapper.ClienteMapperImpl;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.clienteRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private VentaRepository ventaRepository;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(clienteRepository, ventaRepository, new ClienteMapperImpl());
    }

    @Test
    @DisplayName("crear guarda el cliente y devuelve el id asignado")
    void crear_guardaYDevuelveElId() {
        when(clienteRepository.existsByDni("30123456")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> {
            Cliente guardado = invocacion.getArgument(0);
            guardado.setIdCliente(1L);
            return guardado;
        });

        ClienteResponse respuesta = service.crear(clienteRequest());

        assertThat(respuesta).isEqualTo(new ClienteResponse(1L, "Ana", "Perez", "30123456"));
    }

    @Test
    @DisplayName("crear con un DNI ya registrado lanza ReglaDeNegocioException y no guarda")
    void crear_conDniDuplicado_lanzaReglaDeNegocio() {
        when(clienteRepository.existsByDni("30123456")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(clienteRequest()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("30123456");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtener devuelve el cliente")
    void obtener_devuelveElCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ana", "Perez", "30123456")));

        assertThat(service.obtener(1L).apellido()).isEqualTo("Perez");
    }

    @Test
    @DisplayName("obtener un cliente inexistente lanza RecursoNoEncontradoException")
    void obtener_inexistente_lanzaNoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    @DisplayName("listar devuelve una página de clientes mapeados")
    void listar_devuelveLaPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.findAll(pageable)).thenReturn(
                new PageImpl<>(List.of(cliente(1L, "Ana", "Perez", "30123456")), pageable, 1));

        Page<ClienteResponse> pagina = service.listar(pageable);

        assertThat(pagina.getContent()).singleElement()
                .satisfies(c -> assertThat(c.nombre()).isEqualTo("Ana"));
    }

    @Test
    @DisplayName("actualizar modifica los datos del cliente existente")
    void actualizar_modificaLosDatos() {
        Cliente existente = cliente(1L, "Viejo", "Apellido", "11111111");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByDniAndIdClienteNot("22222222", 1L)).thenReturn(false);

        ClienteResponse respuesta = service.actualizar(1L, new ClienteRequest("Nuevo", "Otro", "22222222"));

        assertThat(respuesta).isEqualTo(new ClienteResponse(1L, "Nuevo", "Otro", "22222222"));
    }

    @Test
    @DisplayName("actualizar con el DNI de otro cliente lanza ReglaDeNegocioException")
    void actualizar_conDniDeOtroCliente_lanzaReglaDeNegocio() {
        Cliente existente = cliente(1L, "Ana", "Perez", "30123456");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByDniAndIdClienteNot("22222222", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new ClienteRequest("Ana", "Perez", "22222222")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("22222222");

        assertThat(existente.getDni()).isEqualTo("30123456");
    }

    @Test
    @DisplayName("actualizar con su propio DNI está permitido")
    void actualizar_conSuPropioDni_estaPermitido() {
        Cliente existente = cliente(1L, "Ana", "Perez", "30123456");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByDniAndIdClienteNot("30123456", 1L)).thenReturn(false);

        ClienteResponse respuesta = service.actualizar(1L, new ClienteRequest("Anita", "Perez", "30123456"));

        assertThat(respuesta.nombre()).isEqualTo("Anita");
    }

    @Test
    @DisplayName("actualizar un cliente inexistente lanza RecursoNoEncontradoException sin validar el DNI")
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(9L, clienteRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(clienteRepository, never()).existsByDniAndIdClienteNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("eliminar un cliente sin ventas lo borra")
    void eliminar_sinVentas_loBorra() {
        Cliente existente = cliente(1L, "Ana", "Perez", "30123456");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepository.existsByClienteIdCliente(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(clienteRepository).delete(existente);
    }

    @Test
    @DisplayName("eliminar un cliente con ventas lanza ReglaDeNegocioException y no lo borra")
    void eliminar_conVentas_lanzaReglaDeNegocio() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ana", "Perez", "30123456")));
        when(ventaRepository.existsByClienteIdCliente(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ventas registradas");

        verify(clienteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminar un cliente inexistente lanza RecursoNoEncontradoException")
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(9L)).isInstanceOf(RecursoNoEncontradoException.class);

        verify(clienteRepository, never()).delete(any());
    }
}
