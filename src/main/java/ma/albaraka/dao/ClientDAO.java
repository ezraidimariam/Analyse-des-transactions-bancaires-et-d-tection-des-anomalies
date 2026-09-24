package ma.albaraka.dao;

import ma.albaraka.entity.Client;
import java.util.List;
import java.util.Optional;

public interface ClientDAO {
    Client save(Client client);
    Client update(Client client);
    void delete(Long id);
    Optional<Client> findById(Long id);
    List<Client> findByNom(String nom);
    List<Client> findAll();
}