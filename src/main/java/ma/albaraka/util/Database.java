package ma.albaraka.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        var url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/analyse_transactions");
        var user = System.getenv().getOrDefault("DB_USER", "postgres");
        var password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        return DriverManager.getConnection(url, user, password);
    }
}