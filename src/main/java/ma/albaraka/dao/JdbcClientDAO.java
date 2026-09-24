package ma.albaraka.dao;

import ma.albaraka.entity.Client;
import ma.albaraka.util.Database;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcClientDAO implements ClientDAO {
    private Client map(java.sql.ResultSet result) throws SQLException {
        return new Client(result.getLong("id"), result.getString("nom"), result.getString("email"));
    }

    public Client save(Client client) {
        var sql = "INSERT INTO client(nom,email) VALUES (?,?) RETURNING id";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.nom()); statement.setString(2, client.email());
            try (var result = statement.executeQuery()) { result.next(); return new Client(result.getLong(1), client.nom(), client.email()); }
        } catch (SQLException exception) { throw new IllegalStateException("Impossible d'ajouter le client.", exception); }
    }

    public Client update(Client client) {
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement("UPDATE client SET nom=?, email=? WHERE id=?")) {
            statement.setString(1, client.nom()); statement.setString(2, client.email()); statement.setLong(3, client.id());
            statement.executeUpdate(); return client;
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de modifier le client.", exception); }
    }

    public void delete(Long id) {
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement("DELETE FROM client WHERE id=?")) {
            statement.setLong(1, id); statement.executeUpdate();
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de supprimer le client.", exception); }
    }

    public Optional<Client> findById(Long id) { return find("SELECT * FROM client WHERE id=?", id).stream().findFirst(); }

    public List<Client> findByNom(String nom) { return find("SELECT * FROM client WHERE LOWER(nom) LIKE LOWER(?)", "%" + nom + "%"); }

    public List<Client> findAll() { return find("SELECT * FROM client ORDER BY nom"); }

    private List<Client> find(String sql, Object... parameters) {
        var clients = new ArrayList<Client>();
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (var index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            try (var result = statement.executeQuery()) { while (result.next()) clients.add(map(result)); }
            return clients;
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de rechercher les clients.", exception); }
    }
}