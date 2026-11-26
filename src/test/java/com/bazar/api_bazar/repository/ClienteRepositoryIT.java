package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.support.AbstractMySqlTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static com.bazar.api_bazar.support.TestData.cliente;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DisplayName("ClienteRepository (MySQL)")
class ClienteRepositoryIT extends AbstractMySqlTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("existsByDni detecta DNIs existentes y ausentes")
    void existsByDni() {
        clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        entityManager.flush();

        assertThat(clienteRepository.existsByDni("30123456")).isTrue();
        assertThat(clienteRepository.existsByDni("99999999")).isFalse();
    }

    @Test
    @DisplayName("existsByDniAndIdClienteNot permite el propio DNI pero detecta el de otro cliente")
    void existsByDniAndIdClienteNot() {
        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Cliente luis = clienteRepository.save(cliente(null, "Luis", "Gomez", "28999888"));
        entityManager.flush();

        // Su propio DNI no cuenta como duplicado
        assertThat(clienteRepository.existsByDniAndIdClienteNot("30123456", ana.getIdCliente())).isFalse();
        // El DNI de otro cliente sí es duplicado
        assertThat(clienteRepository.existsByDniAndIdClienteNot("28999888", ana.getIdCliente())).isTrue();
        assertThat(clienteRepository.existsByDniAndIdClienteNot("30123456", luis.getIdCliente())).isTrue();
    }
}
