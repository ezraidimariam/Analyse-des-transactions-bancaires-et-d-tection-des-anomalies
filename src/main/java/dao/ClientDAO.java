package dao;

import entity.Client;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDAO {
    public Client ajouter(Client client) throws SQLException {
        String sql = "INSERT INTO client(nom, email) VALUES (?, ?) RETURNING id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.nom());
            statement.setString(2, client.email());
            try (ResultSet resultat = statement.executeQuery()) {
                resultat.next();
                return new Client(resultat.getLong("id"), client.nom(), client.email());
            }
        }
    }

    public void modifier(Client client) throws SQLException {
        String sql = "UPDATE client SET nom=?, email=? WHERE id=?";
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
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM client WHERE id=?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Client introuvable.");
            }
        }
    }

    public Optional<Client> rechercherParId(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM client WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultat = statement.executeQuery()) {
                return resultat.next() ? Optional.of(lire(resultat)) : Optional.empty();
            }
        }
    }

    public List<Client> rechercherParNom(String nom) throws SQLException {
        List<Client> clients = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM client WHERE LOWER(nom) LIKE LOWER(?) ORDER BY id")) {
            statement.setString(1, "%" + nom + "%");
            try (ResultSet resultat = statement.executeQuery()) {
                while (resultat.next()) { clients.add(lire(resultat)); }
            }
        }
        return clients;
    }

    public List<Client> findAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM client ORDER BY id");
             ResultSet resultat = statement.executeQuery()) {
            while (resultat.next()) { clients.add(lire(resultat)); }
        }
        return clients;
    }

    private Client lire(ResultSet resultat) throws SQLException {
        return new Client(resultat.getLong("id"), resultat.getString("nom"), resultat.getString("email"));
    }
}

