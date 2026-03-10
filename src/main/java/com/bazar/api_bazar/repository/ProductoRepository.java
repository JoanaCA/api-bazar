package com.bazar.api_bazar.repository;
import com.bazar.api_bazar.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCantidadDisponibleLessThan(Double cantidad_disponible);

}
