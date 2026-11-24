package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Page<Producto> findByCantidadDisponibleLessThan(Integer cantidadDisponible, Pageable pageable);
}
