package service;

import dao.ClientDAO;
import dao.CompteDAO;
import entity.*;
import util.DatabaseConnection;
import util.Validation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CompteService {
    private final CompteDAO comptes = new CompteDAO();
    private final ClientDAO clients = new ClientDAO();

    public Compte creerCourant(String numero, BigDecimal solde, long idClient, BigDecimal decouvert) throws SQLException {
        numero = validerCreation(numero, solde, idClient);
        Validation.nonNegatif(decouvert);
        return comptes.ajouter(new CompteCourant(null, numero, solde, idClient, decouvert));
    }

    public Compte creerEpargne(String numero, BigDecimal solde, long idClient, BigDecimal taux) throws SQLException {
        numero = validerCreation(numero, solde, idClient);
        validerTaux(taux);
        return comptes.ajouter(new CompteEpargne(null, numero, solde, idClient, taux));
    }

    private String validerCreation(String numero, BigDecimal solde, long idClient) throws SQLException {
        Validation.id(idClient);
        clients.rechercherParId(idClient).orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
        Validation.nonNegatif(solde);
        return Validation.texte(numero, "Numero", 30);
    }

    // Un champ null conserve sa valeur actuelle. La lecture est verrouillee.
    public void modifier(long id, BigDecimal solde, BigDecimal decouvert, BigDecimal taux) throws SQLException {
        Validation.id(id);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte ancien = comptes.verrouiller(connection, id);
                BigDecimal nouveauSolde = solde == null ? ancien.getSolde() : solde;
                Compte nouveau;
                if (ancien instanceof CompteCourant courant) {
                    if (taux != null) { throw new IllegalArgumentException("Un compte courant n'a pas de taux."); }
                    BigDecimal nouveauDecouvert = decouvert == null ? courant.getDecouvertAutorise() : decouvert;
                    Validation.nonNegatif(nouveauDecouvert);
                    nouveau = new CompteCourant(id, ancien.getNumero(), nouveauSolde, ancien.getIdClient(), nouveauDecouvert);
                } else {
                    if (decouvert != null) { throw new IllegalArgumentException("Un compte epargne n'a pas de decouvert."); }
                    CompteEpargne epargne = (CompteEpargne) ancien;
                    BigDecimal nouveauTaux = taux == null ? epargne.getTauxInteret() : taux;
                    validerTaux(nouveauTaux);
                    nouveau = new CompteEpargne(id, ancien.getNumero(), nouveauSolde, ancien.getIdClient(), nouveauTaux);
                }
                verifierSolde(nouveau, nouveauSolde);
                comptes.modifier(connection, nouveau);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void mettreAJourSolde(long id, BigDecimal solde) throws SQLException {
        Validation.montant(solde);
        modifier(id, solde, null, null);
    }

    public void modifierDecouvert(long id, BigDecimal decouvert) throws SQLException {
        Validation.nonNegatif(decouvert);
        modifier(id, null, decouvert, null);
    }

    public void modifierTaux(long id, BigDecimal taux) throws SQLException {
        validerTaux(taux);
        modifier(id, null, null, taux);
    }

    private void validerTaux(BigDecimal taux) {
        Validation.nonNegatif(taux);
        if (taux.compareTo(new BigDecimal("999.99")) > 0) {
            throw new IllegalArgumentException("Taux maximum : 999.99.");
        }
    }

    public static void verifierSolde(Compte compte, BigDecimal solde) {
        Validation.montant(solde);
        BigDecimal minimum = BigDecimal.ZERO;
        if (compte instanceof CompteCourant courant) {
            minimum = courant.getDecouvertAutorise().negate();
        }
        if (solde.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Solde insuffisant : limite autorisee depassee.");
        }
    }

    public Optional<Compte> rechercherParId(long id) throws SQLException {
        Validation.id(id);
        return comptes.rechercherParId(id);
    }

    public Optional<Compte> rechercherParNumero(String numero) throws SQLException {
        return comptes.rechercherParNumero(Validation.texte(numero, "Numero", 30));
    }

    public List<Compte> rechercherParClient(long id) throws SQLException {
        clients.rechercherParId(id).orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
        return comptes.rechercherParClient(id);
    }

    public List<Compte> lister() throws SQLException { return comptes.findAll(); }
    public Optional<Compte> maximum() throws SQLException {
        return lister().stream().max(Comparator.comparing(Compte::getSolde));
    }
    public Optional<Compte> minimum() throws SQLException {
        return lister().stream().min(Comparator.comparing(Compte::getSolde));
    }
    public void supprimer(long id) throws SQLException {
        Validation.id(id);
        comptes.supprimer(id);
    }
}

