package ma.albaraka.dao;

import ma.albaraka.entity.*;
import ma.albaraka.util.Database;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCompteDAO implements CompteDAO {
    public Compte save(Compte compte) {
        var sql = "INSERT INTO compte(numero,solde,id_client,type_compte,decouvert_autorise,taux_interet) VALUES (?,?,?,?,?,?) RETURNING id";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, compte.numero()); statement.setBigDecimal(2, compte.solde()); statement.setLong(3, compte.idClient());
            statement.setString(4, compte instanceof CompteCourant ? "COURANT" : "EPARGNE");
            statement.setBigDecimal(5, compte instanceof CompteCourant courant ? courant.decouvertAutorise() : null);
            statement.setBigDecimal(6, compte instanceof CompteEpargne epargne ? epargne.tauxInteret() : null);
            try (var result = statement.executeQuery()) { result.next(); return withId(compte, result.getLong(1)); }
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de créer le compte.", exception); }
    }

    private Compte withId(Compte compte, Long id) {
        return compte instanceof CompteCourant c ? new CompteCourant(id, c.numero(), c.solde(), c.idClient(), c.decouvertAutorise()) :
                new CompteEpargne(id, compte.numero(), compte.solde(), compte.idClient(), ((CompteEpargne) compte).tauxInteret());
    }

    public void updateSolde(Long id, BigDecimal solde) {
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement("UPDATE compte SET solde=? WHERE id=?")) {
            statement.setBigDecimal(1, solde); statement.setLong(2, id); statement.executeUpdate();
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de modifier le solde.", exception); }
    }

    public List<Compte> findByClient(Long idClient) { return find("SELECT * FROM compte WHERE id_client=?", idClient); }
    public Optional<Compte> findByNumero(String numero) { return find("SELECT * FROM compte WHERE numero=?", numero).stream().findFirst(); }
    public Optional<Compte> findMaximum() { return find("SELECT * FROM compte ORDER BY solde DESC LIMIT 1").stream().findFirst(); }
    public Optional<Compte> findMinimum() { return find("SELECT * FROM compte ORDER BY solde LIMIT 1").stream().findFirst(); }

    public List<Compte> findInactive(int jours) {
        return find("SELECT c.* FROM compte c LEFT JOIN transaction_bancaire t ON c.id=t.id_compte " +
                "GROUP BY c.id HAVING COALESCE(MAX(t.date_transaction), TIMESTAMP '1900-01-01') < CURRENT_TIMESTAMP - (? * INTERVAL '1 day')", jours);
    }

    private List<Compte> find(String sql, Object... parameters) {
        var comptes = new ArrayList<Compte>();
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (var index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            try (var result = statement.executeQuery()) { while (result.next()) comptes.add(map(result)); }
            return comptes;
        } catch (SQLException exception) { throw new IllegalStateException("Impossible de rechercher les comptes.", exception); }
    }

    private Compte map(java.sql.ResultSet result) throws SQLException {
        var type = result.getString("type_compte");
        return "COURANT".equals(type) ? new CompteCourant(result.getLong("id"), result.getString("numero"), result.getBigDecimal("solde"), result.getLong("id_client"), result.getBigDecimal("decouvert_autorise")) :
                new CompteEpargne(result.getLong("id"), result.getString("numero"), result.getBigDecimal("solde"), result.getLong("id_client"), result.getBigDecimal("taux_interet"));
    }
}