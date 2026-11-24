package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.response.ClienteResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.mapper.ClienteMapper;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService implements IClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.existsByDni(request.dni())) {
            throw new ReglaDeNegocioException("Ya existe un cliente con el DNI " + request.dni());
        }
        Cliente cliente = clienteMapper.toEntity(request);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    public Page<ClienteResponse> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(clienteMapper::toResponse);
    }

    @Override
    public ClienteResponse obtener(Long idCliente) {
        return clienteMapper.toResponse(buscar(idCliente));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long idCliente, ClienteRequest request) {
        Cliente cliente = buscar(idCliente);
        if (clienteRepository.existsByDniAndIdClienteNot(request.dni(), idCliente)) {
            throw new ReglaDeNegocioException("Ya existe otro cliente con el DNI " + request.dni());
        }
        clienteMapper.actualizar(request, cliente);
        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional
    public void eliminar(Long idCliente) {
        Cliente cliente = buscar(idCliente);
        if (ventaRepository.existsByClienteIdCliente(idCliente)) {
            throw new ReglaDeNegocioException(
                    "No se puede eliminar el cliente %d porque tiene ventas registradas".formatted(idCliente));
        }
        clienteRepository.delete(cliente);
    }

    private Cliente buscar(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", idCliente));
    }
}
