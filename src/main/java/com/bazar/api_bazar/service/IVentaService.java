package com.bazar.api_bazar.service;
import com.bazar.api_bazar.dto.MayorVentaDTO;
import com.bazar.api_bazar.dto.VentasDiaDTO;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;

import java.time.LocalDate;
import java.util.List;

public interface IVentaService {

    void saveVenta(Venta venta);
    List<Venta> getVentas();
    Venta getVenta(Long codigo_venta);
    void deleteVenta(Long codigo_venta);
    void editVenta(Venta venta);
    List<Producto> getProductosByVenta(Long codigo_venta); //Requerimiento 5
    VentasDiaDTO getTotalesPorDia(LocalDate fecha_venta); // requerimiento 6
    MayorVentaDTO getMayorVenta(); // reqr 7
}
