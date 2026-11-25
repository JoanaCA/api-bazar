package com.bazar.api_bazar.mapper;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.response.ClienteResponse;
import com.bazar.api_bazar.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.clienteRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClienteMapper")
class ClienteMapperTest {

    private final ClienteMapper mapper = new ClienteMapperImpl();

    @Test
    @DisplayName("toEntity copia los datos del request y deja sin id")
    void toEntity_copiaCamposYNoAsignaId() {
        Cliente entidad = mapper.toEntity(clienteRequest());

        assertThat(entidad.getNombre()).isEqualTo("Ana");
        assertThat(entidad.getApellido()).isEqualTo("Perez");
        assertThat(entidad.getDni()).isEqualTo("30123456");
        assertThat(entidad.getIdCliente()).isNull();
    }

    @Test
    @DisplayName("toResponse expone el id y los datos del cliente")
    void toResponse_copiaTodosLosCampos() {
        ClienteResponse respuesta = mapper.toResponse(cliente(3L, "Luis", "Gomez", "28999888"));

        assertThat(respuesta).isEqualTo(new ClienteResponse(3L, "Luis", "Gomez", "28999888"));
    }

    @Test
    @DisplayName("actualizar modifica los datos pero nunca el id")
    void actualizar_noPisaElId() {
        Cliente existente = cliente(9L, "Viejo", "Apellido", "11111111");

        mapper.actualizar(new ClienteRequest("Nuevo", "Otro", "22222222"), existente);

        assertThat(existente.getIdCliente()).isEqualTo(9L);
        assertThat(existente.getNombre()).isEqualTo("Nuevo");
        assertThat(existente.getApellido()).isEqualTo("Otro");
        assertThat(existente.getDni()).isEqualTo("22222222");
    }
}
