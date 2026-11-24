package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDni(String dni);

    boolean existsByDniAndIdClienteNot(String dni, Long idCliente);
}
