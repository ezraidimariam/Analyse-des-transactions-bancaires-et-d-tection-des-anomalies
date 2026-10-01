package service;

import dao.ClientDAO;
import dao.CompteDAO;
import dao.TransactionDAO;
import entity.Compte;
import entity.Transaction;
import entity.TypeTransaction;
import util.DatabaseConnection;
import util.Validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TransactionService {
    public static final BigDecimal SEUIL_MONTANT = new BigDecimal("10000");
    public static final int MAX_OPERATIONS_PAR_MINUTE = 3;
    private final CompteDAO compteDAO;
    private final TransactionDAO transactionDAO;

    public TransactionService() {
        compteDAO = new CompteDAO();
        transactionDAO = new TransactionDAO();
    }

    public void versement(long id, BigDecimal montant, String lieu) throws SQLException {
        Validation.id(id);
        Validation.positif(montant);
        lieu = Validation.texte(lieu, "Lieu", 120);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte compte = compteDAO.verrouiller(connection, id);
                BigDecimal nouveauSolde = compte.getSolde().add(montant);
                CompteService.verifierSolde(compte, nouveauSolde);
                compteDAO.mettreAJourSolde(connection, id, nouveauSolde);
                Transaction transaction = new Transaction(null, LocalDateTime.now(), montant,
                        TypeTransaction.VERSEMENT, lieu, id);
                transactionDAO.ajouter(connection, transaction);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void retrait(long id, BigDecimal montant, String lieu) throws SQLException {
        Validation.id(id);
        Validation.positif(montant);
        lieu = Validation.texte(lieu, "Lieu", 120);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte compte = compteDAO.verrouiller(connection, id);
                BigDecimal nouveauSolde = compte.getSolde().subtract(montant);
                CompteService.verifierSolde(compte, nouveauSolde);
                compteDAO.mettreAJourSolde(connection, id, nouveauSolde);
                Transaction transaction = new Transaction(null, LocalDateTime.now(), montant,
                        TypeTransaction.RETRAIT, lieu, id);
                transactionDAO.ajouter(connection, transaction);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void virement(long source, long destination, BigDecimal montant, String lieu)
            throws SQLException {
        Validation.id(destination);

        if (source == destination) {
            throw new IllegalArgumentException("Choisissez deux comptes differents.");
        }
        Validation.id(source);
        Validation.positif(montant);
        lieu = Validation.texte(lieu, "Lieu", 120);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte compteSource;
                Compte compteDestination;
                // Meme ordre de verrouillage pour deux virements en sens inverse.

                if (destination < source) {
                    compteDestination = compteDAO.verrouiller(connection, destination);
                    compteSource = compteDAO.verrouiller(connection, source);
                } else {
                    compteSource = compteDAO.verrouiller(connection, source);
                    compteDestination = compteDAO.verrouiller(connection, destination);
                }
                BigDecimal soldeSource = compteSource.getSolde().subtract(montant);
                CompteService.verifierSolde(compteSource, soldeSource);
                BigDecimal soldeDestination = compteDestination.getSolde().add(montant);
                CompteService.verifierSolde(compteDestination, soldeDestination);

                compteDAO.mettreAJourSolde(connection, source, soldeSource);
                compteDAO.mettreAJourSolde(connection, destination, soldeDestination);
                LocalDateTime date = LocalDateTime.now();
                transactionDAO.ajouter(connection,
                        new Transaction(null, date, montant, TypeTransaction.VIREMENT, lieu, source));
                transactionDAO.ajouter(connection,
                        new Transaction(null, date, montant, TypeTransaction.VIREMENT, lieu, destination));
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Transaction> parCompte(long id) throws SQLException {
        compteDAO.rechercherParId(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable."));

        return trierParDate(transactionDAO.rechercherParCompte(id));
    }

    public List<Transaction> parClient(long id) throws SQLException {
        new ClientDAO().rechercherParId(id)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
        List<Long> ids = compteDAO.rechercherParClient(id).stream().map(Compte::getId).toList();

        return trierParDate(lister().stream().filter(transaction -> ids.contains(transaction.idCompte())).toList());
    }

    public List<Transaction> lister() throws SQLException {
        return transactionDAO.findAll();
    }

    public List<Transaction> trierParDate(List<Transaction> liste) {
        return liste.stream().sorted(Comparator.comparing(Transaction::date)).toList();
    }

    public List<Transaction> filtrer(List<Transaction> liste, BigDecimal minimum,
            BigDecimal maximum,
            TypeTransaction type, LocalDateTime debut, LocalDateTime fin, String lieu) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Minimum superieur au maximum.");
        }

        if (minimum != null) {
            liste = filtrerParMontantMinimum(liste, minimum);
        }
        if (maximum != null) {
            liste = filtrerParMontantMaximum(liste, maximum);
        }
        if (type != null) {
            liste = filtrerParType(liste, type);
        }
        liste = filtrerParPeriode(liste, debut, fin);
        if (lieu != null && !lieu.isBlank()) {
            liste = filtrerParLieu(liste, lieu);
        }
        return trierParDate(liste);
    }

    private List<Transaction> filtrerParMontantMinimum(List<Transaction> liste, BigDecimal minimum) {
        Validation.nonNegatif(minimum);
        return liste.stream().filter(transaction -> transaction.montant().compareTo(minimum) >= 0).toList();
    }

    private List<Transaction> filtrerParMontantMaximum(List<Transaction> liste, BigDecimal maximum) {
        Validation.nonNegatif(maximum);
        return liste.stream().filter(transaction -> transaction.montant().compareTo(maximum) <= 0).toList();
    }

    private List<Transaction> filtrerParType(List<Transaction> liste, TypeTransaction type) {
        return liste.stream().filter(transaction -> transaction.type() == type).toList();
    }

    private List<Transaction> filtrerParPeriode(List<Transaction> liste, LocalDateTime debut,
            LocalDateTime fin) {
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new IllegalArgumentException("Periode invalide.");
        }
        if (debut != null) {
            liste = liste.stream().filter(transaction -> !transaction.date().isBefore(debut)).toList();
        }
        if (fin != null) {
            liste = liste.stream().filter(transaction -> !transaction.date().isAfter(fin)).toList();
        }
        return liste;
    }

    private List<Transaction> filtrerParLieu(List<Transaction> liste, String lieu) {
        return liste.stream().filter(transaction -> transaction.lieu().equalsIgnoreCase(lieu.trim())).toList();
    }

    public Map<TypeTransaction, List<Transaction>> regrouperParType(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(Transaction::type));
    }

    public Map<YearMonth, List<Transaction>> regrouperParMois(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.date()),
                TreeMap::new, Collectors.toList()));
    }

    public BigDecimal total(List<Transaction> liste) {
        return liste.stream().map(Transaction::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal moyenne(List<Transaction> liste) {
        if (liste.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = total(liste);
        return total.divide(BigDecimal.valueOf(liste.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalCompte(long id) throws SQLException {
        return total(parCompte(id));
    }

    public BigDecimal totalClient(long id) throws SQLException {
        return total(parClient(id));
    }

    public List<Transaction> suspectes(List<Transaction> liste, String paysHabituel) {
        String pays = Validation.texte(paysHabituel, "Pays habituel", 120);
        Set<Transaction> frequentes = detecterFrequenceExcessive(liste);

        return liste.stream()
                .filter(transaction -> estSuspecte(transaction, pays, frequentes))
                .sorted(Comparator.comparing(Transaction::date)).toList();
    }

    private boolean estSuspecte(Transaction transaction, String pays, Set<Transaction> frequentes) {
        if (transaction.montant().compareTo(SEUIL_MONTANT) > 0) {
            return true;
        }
        if (!transaction.lieu().equalsIgnoreCase(pays)) {
            return true;
        }
        if (frequentes.contains(transaction)) {
            return true;
        }
        return false;
    }

    private Set<Transaction> detecterFrequenceExcessive(List<Transaction> liste) {
        Set<Transaction> frequentes = new HashSet<>();
        for (Transaction transaction : liste) {
            LocalDateTime debut = transaction.date();
            LocalDateTime fin = debut.plusMinutes(1);
            List<Transaction> operations = liste.stream()
                    .filter(operation -> operation.idCompte().equals(transaction.idCompte()))
                    .filter(operation -> !operation.date().isBefore(debut) && !operation.date().isAfter(fin))
                    .toList();

            if (operations.size() > MAX_OPERATIONS_PAR_MINUTE) {
                frequentes.addAll(operations);
            }
        }
        return frequentes;
    }
}
