package dao;

import entity.*;
import util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompteDAO {
    public Compte ajouter(Compte compte) throws SQLException {
        String sql = """
                INSERT INTO compte(numero, solde, id_client, type_compte, decouvert_autorise, taux_interet)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING *
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            remplir(statement, compte);
            try (ResultSet resultat = statement.executeQuery()) {
                resultat.next();
                return lire(resultat);
            }
        }
    }

    public void modifier(Connection connection, Compte compte) throws SQLException {
        String sql = """
                UPDATE compte SET numero=?, solde=?, id_client=?, type_compte=?,
                decouvert_autorise=?, taux_interet=? WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            remplir(statement, compte);
            statement.setLong(7, compte.getId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Compte introuvable.");
            }
        }
    }

    public void modifier(Compte compte) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            modifier(connection, compte);
        }
    }

    public void mettreAJourSolde(Connection connection, long id, BigDecimal solde) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE compte SET solde=? WHERE id=?")) {
            statement.setBigDecimal(1, solde);
            statement.setLong(2, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Compte introuvable.");
            }
        }
    }

    public void supprimer(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM compte WHERE id=?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Compte introuvable.");
            }
        }
    }

    public Optional<Compte> rechercherParId(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM compte WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultat = statement.executeQuery()) {
                return resultat.next() ? Optional.of(lire(resultat)) : Optional.empty();
            }
        }
    }

    // Le verrou reste actif jusqu'au commit ou rollback de la connexion.
    public Compte verrouiller(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM compte WHERE id=? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultat = statement.executeQuery()) {
                if (!resultat.next()) { throw new IllegalArgumentException("Compte introuvable."); }
                return lire(resultat);
            }
        }
    }

    public Optional<Compte> rechercherParNumero(String numero) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM compte WHERE numero=?")) {
            statement.setString(1, numero);
            try (ResultSet resultat = statement.executeQuery()) {
                return resultat.next() ? Optional.of(lire(resultat)) : Optional.empty();
            }
        }
    }

    public List<Compte> rechercherParClient(long idClient) throws SQLException {
        List<Compte> comptes = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM compte WHERE id_client=? ORDER BY id")) {
            statement.setLong(1, idClient);
            try (ResultSet resultat = statement.executeQuery()) {
                while (resultat.next()) { comptes.add(lire(resultat)); }
            }
        }
        return comptes;
    }

    public List<Compte> findAll() throws SQLException {
        List<Compte> comptes = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM compte ORDER BY id");
             ResultSet resultat = statement.executeQuery()) {
            while (resultat.next()) { comptes.add(lire(resultat)); }
        }
        return comptes;
    }

    private void remplir(PreparedStatement statement, Compte compte) throws SQLException {
        statement.setString(1, compte.getNumero());
        statement.setBigDecimal(2, compte.getSolde());
        statement.setLong(3, compte.getIdClient());
        if (compte instanceof CompteCourant courant) {
            statement.setString(4, "COURANT");
            statement.setBigDecimal(5, courant.getDecouvertAutorise());
            statement.setNull(6, Types.NUMERIC);
        } else {
            CompteEpargne epargne = (CompteEpargne) compte;
            statement.setString(4, "EPARGNE");
            statement.setNull(5, Types.NUMERIC);
            statement.setBigDecimal(6, epargne.getTauxInteret());
        }
    }

    private Compte lire(ResultSet resultat) throws SQLException {
        long id = resultat.getLong("id");
        String numero = resultat.getString("numero");
        BigDecimal solde = resultat.getBigDecimal("solde");
        long client = resultat.getLong("id_client");
        if (resultat.getString("type_compte").equals("COURANT")) {
            return new CompteCourant(id, numero, solde, client, resultat.getBigDecimal("decouvert_autorise"));
        }
        return new CompteEpargne(id, numero, solde, client, resultat.getBigDecimal("taux_interet"));
    }
}

