package com.bazar.api_bazar.service;
import com.bazar.api_bazar.model.Cliente;
import java.util.List;

public interface IClienteService {

    List<Cliente> getClientes();
    Cliente getCliente(Long id);
    void saveCliente(Cliente cliente);
    void deleteCliente(Long id);
    void editCliente(Cliente cliente);
}
