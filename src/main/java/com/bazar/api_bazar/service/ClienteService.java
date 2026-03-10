package com.bazar.api_bazar.service;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService implements  IClienteService{

    @Autowired
    private ClienteRepository repository;

    @Override
    public List<Cliente> getClientes() {
        return repository.findAll();
    }

    @Override
    public Cliente getCliente(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void saveCliente(Cliente cliente) {
        repository.save(cliente);
    }

    @Override
    public void deleteCliente(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void editCliente(Cliente cliente) {
        repository.save(cliente);
    }






}
