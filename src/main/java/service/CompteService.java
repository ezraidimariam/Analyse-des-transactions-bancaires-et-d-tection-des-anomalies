package service;

import dao.ClientDAO;
import dao.CompteDAO;
import entity.Compte;
import entity.CompteCourant;
import entity.CompteEpargne;
import util.DatabaseConnection;
import util.Validation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CompteService {
    private final CompteDAO compteDAO;
    private final ClientDAO clientDAO;

    public CompteService() {
        clientDAO = new ClientDAO();
        compteDAO = new CompteDAO();
    }

    public Compte creerCourant(String numero, BigDecimal solde, long idClient, BigDecimal decouvert)
            throws SQLException {
        numero = validerCreation(numero, solde, idClient);
        Validation.nonNegatif(decouvert);

        CompteCourant compte = new CompteCourant(null, numero, solde, idClient, decouvert);
        return compteDAO.ajouter(compte);
    }

    public Compte creerEpargne(String numero, BigDecimal solde, long idClient, BigDecimal taux)
            throws SQLException {
        numero = validerCreation(numero, solde, idClient);
        validerTaux(taux);

        CompteEpargne compte = new CompteEpargne(null, numero, solde, idClient, taux);
        return compteDAO.ajouter(compte);
    }

    private String validerCreation(String numero, BigDecimal solde, long idClient)
            throws SQLException {
        Validation.id(idClient);
        clientDAO.rechercherParId(idClient)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
        Validation.nonNegatif(solde);

        return Validation.texte(numero, "Numero", 30);
    }

    // Un champ null conserve sa valeur actuelle. La lecture est verrouillee.
    public void modifier(long id, BigDecimal solde, BigDecimal decouvert, BigDecimal taux)
            throws SQLException {
        Validation.id(id);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte ancien = compteDAO.verrouiller(connection, id);
                if (solde == null) {
                    solde = ancien.getSolde();
                }
                Compte nouveau;

                if (ancien instanceof CompteCourant courant) {
                    if (taux != null) {
                        throw new IllegalArgumentException("Un compte courant n'a pas de taux.");
                    }
                    nouveau = preparerCourant(courant, solde, decouvert);
                } else {
                    if (decouvert != null) {
                        throw new IllegalArgumentException(
                                "Un compte epargne n'a pas de decouvert.");
                    }
                    CompteEpargne epargne = (CompteEpargne) ancien;
                    nouveau = preparerEpargne(epargne, solde, taux);
                }
                verifierSolde(nouveau, solde);
                compteDAO.modifier(connection, nouveau);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private CompteCourant preparerCourant(CompteCourant compte, BigDecimal solde,
            BigDecimal decouvert) {
        if (decouvert == null) {
            decouvert = compte.getDecouvertAutorise();
        }
        Validation.nonNegatif(decouvert);
        return new CompteCourant(compte.getId(), compte.getNumero(), solde,
                compte.getIdClient(), decouvert);
    }

    private CompteEpargne preparerEpargne(CompteEpargne compte, BigDecimal solde,
            BigDecimal taux) {
        if (taux == null) {
            taux = compte.getTauxInteret();
        }
        validerTaux(taux);
        return new CompteEpargne(compte.getId(), compte.getNumero(), solde,
                compte.getIdClient(), taux);
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

        return compteDAO.rechercherParId(id);
    }

    public Optional<Compte> rechercherParNumero(String numero) throws SQLException {
        numero = Validation.texte(numero, "Numero", 30);
        return compteDAO.rechercherParNumero(numero);
    }

    public List<Compte> rechercherParClient(long id) throws SQLException {
        clientDAO.rechercherParId(id)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable."));

        return compteDAO.rechercherParClient(id);
    }

    public List<Compte> lister() throws SQLException {
        return compteDAO.findAll();
    }

    public Optional<Compte> maximum() throws SQLException {
        return lister().stream().max(Comparator.comparing(Compte::getSolde));
    }

    public Optional<Compte> minimum() throws SQLException {
        return lister().stream().min(Comparator.comparing(Compte::getSolde));
    }

    public void supprimer(long id) throws SQLException {
        Validation.id(id);
        compteDAO.supprimer(id);
    }
}
