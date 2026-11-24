package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.dto.request.VentaRequest;
import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.MayorVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.service.IVentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final IVentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaRequest request) {
        VentaResponse creada = ventaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.codigoVenta()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public Page<VentaResponse> listar(Pageable pageable) {
        return ventaService.listar(pageable);
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.obtener(id);
    }

    @PutMapping("/{id}")
    public VentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody VentaRequest request) {
        return ventaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Requerimiento 5: productos de una venta. */
    @GetMapping("/{id}/productos")
    public List<DetalleVentaResponse> listarProductos(@PathVariable Long id) {
        return ventaService.listarProductos(id);
    }

    /** Requerimiento 6: monto total y cantidad de ventas de un día (fecha ISO: yyyy-MM-dd). */
    @GetMapping("/reportes/dia/{fecha}")
    public VentasDiaResponse totalesPorDia(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ventaService.totalesPorDia(fecha);
    }

    /** Requerimiento 7: venta de mayor monto. */
    @GetMapping("/reportes/mayor")
    public MayorVentaResponse mayorVenta() {
        return ventaService.mayorVenta();
    }
}
