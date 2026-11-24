package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Requerimiento 6: totales calculados en la base de datos (no se traen las ventas a memoria). */
    @Query("""
            select new com.bazar.api_bazar.dto.response.VentasDiaResponse(sum(v.total), count(v))
            from Venta v
            where v.fechaVenta = :fecha
            """)
    VentasDiaResponse totalesPorDia(@Param("fecha") LocalDate fecha);

    /** Requerimiento 7. */
    Optional<Venta> findTopByOrderByTotalDesc();

    boolean existsByClienteIdCliente(Long idCliente);

    boolean existsByDetallesProductoCodigoProducto(Long codigoProducto);
}
