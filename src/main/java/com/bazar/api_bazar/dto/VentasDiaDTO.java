package com.bazar.api_bazar.dto;

import java.io.Serializable;

public class VentasDiaDTO implements Serializable {

    private Double montoTotal;
    private Integer cantidadVentas;

    public VentasDiaDTO() {
    }

    public VentasDiaDTO(Double montoTotal, Integer cantidadVentas) {
        this.montoTotal = montoTotal;
        this.cantidadVentas = cantidadVentas;
    }

    public Double getMontoTotal() { return montoTotal; }
    public Integer getCantidadVentas() { return cantidadVentas; }
}
