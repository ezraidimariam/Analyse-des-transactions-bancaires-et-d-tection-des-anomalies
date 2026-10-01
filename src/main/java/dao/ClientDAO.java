package dao;

import entity.Client;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDAO {

    public Client ajouter(Client client) throws SQLException {
        String sql = """
                INSERT INTO client(nom, email)
                VALUES (?, ?)
                RETURNING id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, client.nom());
            statement.setString(2, client.email());

            try (ResultSet resultat = statement.executeQuery()) {
                if (resultat.next()) {
                    long id = resultat.getLong("id");
                    return new Client(id, client.nom(), client.email());
                }
            }
        }
        throw new SQLException("Impossible de creer le client.");
    }

    public void modifier(Client client) throws SQLException {
        String sql = """
                UPDATE client
                SET nom=?, email=?
                WHERE id=?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, client.nom());
            statement.setString(2, client.email());
            statement.setLong(3, client.id());

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Client introuvable.");
            }
        }
    }

    public void supprimer(long id) throws SQLException {
        String sql = """
                DELETE FROM client
                WHERE id=?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Client introuvable.");
            }
        }
    }

    public Optional<Client> rechercherParId(long id) throws SQLException {
        String sql = """
                SELECT id, nom, email
                FROM client
                WHERE id=?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultat = statement.executeQuery()) {
                if (resultat.next()) {
                    Client client = new Client(resultat.getLong("id"),
                            resultat.getString("nom"), resultat.getString("email"));
                    return Optional.of(client);
                }
                return Optional.empty();
            }
        }
    }

    public List<Client> rechercherParNom(String nom) throws SQLException {
        String sql = """
                SELECT id, nom, email
                FROM client
                WHERE LOWER(nom) LIKE LOWER(?)
                ORDER BY id
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + nom + "%");

            try (ResultSet resultat = statement.executeQuery()) {
                while (resultat.next()) {
                    Client client = new Client(resultat.getLong("id"),
                            resultat.getString("nom"), resultat.getString("email"));
                    clients.add(client);
                }
            }
        }

        return clients;
    }

    public List<Client> findAll() throws SQLException {
        String sql = """
                SELECT id, nom, email
                FROM client
                ORDER BY id
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultat = statement.executeQuery()) {

            while (resultat.next()) {
                Client client = new Client(resultat.getLong("id"),
                        resultat.getString("nom"), resultat.getString("email"));
                clients.add(client);
            }
        }

        return clients;
    }

}
