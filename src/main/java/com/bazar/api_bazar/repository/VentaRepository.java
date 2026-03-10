package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByFechaVenta(LocalDate fecha_venta); // rquerimiento 6
    Optional<Venta> findTopByOrderByTotalDesc(); //requerimienot 7

}
