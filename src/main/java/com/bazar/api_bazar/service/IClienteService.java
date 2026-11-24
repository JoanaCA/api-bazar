package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.response.ClienteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IClienteService {

    ClienteResponse crear(ClienteRequest request);

    Page<ClienteResponse> listar(Pageable pageable);

    ClienteResponse obtener(Long idCliente);

    ClienteResponse actualizar(Long idCliente, ClienteRequest request);

    void eliminar(Long idCliente);
}
