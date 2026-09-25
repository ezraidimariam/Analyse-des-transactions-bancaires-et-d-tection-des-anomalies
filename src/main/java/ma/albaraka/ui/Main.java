package ma.albaraka.ui;

import ma.albaraka.util.Database;

import java.sql.SQLException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Analyse des transactions");
        System.out.println("Java 25 - application prête.");

        try (var connection = Database.getConnection()) {
            System.out.println("Connexion PostgreSQL réussie.");
            System.out.println("Base de données: " + connection.getMetaData().getDatabaseProductName());
        } catch (SQLException exception) {
            System.err.println("Connexion PostgreSQL impossible.");
            System.err.println("Vérifiez PostgreSQL, la base analyse_transactions et DB_URL/DB_USER/DB_PASSWORD.");
            System.exit(1);
        }
    }
}
