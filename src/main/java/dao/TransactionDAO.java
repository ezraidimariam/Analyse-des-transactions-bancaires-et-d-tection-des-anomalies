package dao;

import entity.Transaction;
import entity.TypeTransaction;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionDAO {
    public Transaction ajouter(Transaction transaction) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return ajouter(connection, transaction);
        }
    }

    // Cette surcharge partage la transaction SQL avec la mise a jour des soldes.
    public Transaction ajouter(Connection connection, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transaction_bancaire(date, montant, type, lieu, id_compte) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            remplir(statement, transaction);
            try (ResultSet resultat = statement.executeQuery()) {
                resultat.next();
                return new Transaction(resultat.getLong("id"), transaction.date(), transaction.montant(),
                        transaction.type(), transaction.lieu(), transaction.idCompte());
            }
        }
    }

    public void modifier(Transaction transaction) throws SQLException {
        String sql = "UPDATE transaction_bancaire SET date=?, montant=?, type=?, lieu=?, id_compte=? WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            remplir(statement, transaction);
            statement.setLong(6, transaction.id());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Transaction introuvable.");
            }
        }
    }

    public void supprimer(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transaction_bancaire WHERE id=?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Transaction introuvable.");
            }
        }
    }

    public Optional<Transaction> rechercherParId(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM transaction_bancaire WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultat = statement.executeQuery()) {
                return resultat.next() ? Optional.of(lire(resultat)) : Optional.empty();
            }
        }
    }

    public List<Transaction> rechercherParCompte(long idCompte) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM transaction_bancaire WHERE id_compte=? ORDER BY date, id")) {
            statement.setLong(1, idCompte);
            try (ResultSet resultat = statement.executeQuery()) {
                while (resultat.next()) { transactions.add(lire(resultat)); }
            }
        }
        return transactions;
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM transaction_bancaire ORDER BY date, id");
             ResultSet resultat = statement.executeQuery()) {
            while (resultat.next()) { transactions.add(lire(resultat)); }
        }
        return transactions;
    }

    private void remplir(PreparedStatement statement, Transaction transaction) throws SQLException {
        statement.setTimestamp(1, Timestamp.valueOf(transaction.date()));
        statement.setBigDecimal(2, transaction.montant());
        statement.setString(3, transaction.type().name());
        statement.setString(4, transaction.lieu());
        statement.setLong(5, transaction.idCompte());
    }

    private Transaction lire(ResultSet resultat) throws SQLException {
        return new Transaction(resultat.getLong("id"), resultat.getTimestamp("date").toLocalDateTime(),
                resultat.getBigDecimal("montant"), TypeTransaction.valueOf(resultat.getString("type")),
                resultat.getString("lieu"), resultat.getLong("id_compte"));
    }
}

