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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
                Transaction sortie = new Transaction(null, date, montant,
                        TypeTransaction.VIREMENT, lieu, source);
                Transaction entree = new Transaction(null, date, montant,
                        TypeTransaction.VIREMENT, lieu, destination);
                transactionDAO.ajouter(connection, sortie);
                transactionDAO.ajouter(connection, entree);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Transaction> parCompte(long id) throws SQLException {
        if (compteDAO.rechercherParId(id).isEmpty()) {
            throw new IllegalArgumentException("Compte introuvable.");
        }

        return trierParDate(transactionDAO.rechercherParCompte(id));
    }

    public List<Transaction> parClient(long id) throws SQLException {
        ClientDAO clientDAO = new ClientDAO();
        if (clientDAO.rechercherParId(id).isEmpty()) {
            throw new IllegalArgumentException("Client introuvable.");
        }
        List<Long> ids = new ArrayList<>();
        for (Compte compte : compteDAO.rechercherParClient(id)) {
            ids.add(compte.getId());
        }
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : lister()) {
            if (ids.contains(transaction.idCompte())) {
                resultat.add(transaction);
            }
        }
        return trierParDate(resultat);
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
            Validation.nonNegatif(minimum);
            liste = liste.stream().filter(t -> t.montant().compareTo(minimum) >= 0).toList();
        }
        if (maximum != null) {
            Validation.nonNegatif(maximum);
            liste = liste.stream().filter(t -> t.montant().compareTo(maximum) <= 0).toList();
        }
        if (type != null) {
            liste = liste.stream().filter(t -> t.type() == type).toList();
        }
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new IllegalArgumentException("Periode invalide.");
        }
        if (debut != null) {
            liste = liste.stream().filter(transaction -> !transaction.date().isBefore(debut)).toList();
        }
        if (fin != null) {
            liste = liste.stream().filter(transaction -> !transaction.date().isAfter(fin)).toList();
        }
        if (lieu != null && !lieu.isBlank()) {
            liste = liste.stream().filter(t -> t.lieu().equalsIgnoreCase(lieu.trim())).toList();
        }
        return trierParDate(liste);
    }

    public Map<TypeTransaction, List<Transaction>> regrouperParType(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(Transaction::type));
    }

    public Map<YearMonth, List<Transaction>> regrouperParMois(List<Transaction> liste) {
        Map<YearMonth, List<Transaction>> groupes = new TreeMap<>();
        for (Transaction transaction : liste) {
            YearMonth mois = YearMonth.from(transaction.date());
            if (!groupes.containsKey(mois)) {
                groupes.put(mois, new ArrayList<>());
            }
            groupes.get(mois).add(transaction);
        }
        return groupes;
    }

    public BigDecimal total(List<Transaction> liste) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction transaction : liste) {
            total = total.add(transaction.montant());
        }
        return total;
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
        List<Transaction> frequentes = new ArrayList<>();

        for (Transaction transaction : liste) {
            LocalDateTime debut = transaction.date();
            LocalDateTime fin = debut.plusMinutes(1);
            List<Transaction> operations = new ArrayList<>();
            for (Transaction operation : liste) {
                if (!operation.idCompte().equals(transaction.idCompte())) {
                    continue;
                }
                if (operation.date().isBefore(debut) || operation.date().isAfter(fin)) {
                    continue;
                }
                operations.add(operation);
            }

            if (operations.size() > MAX_OPERATIONS_PAR_MINUTE) {
                frequentes.addAll(operations);
            }
        }
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : liste) {
            boolean montantEleve = transaction.montant().compareTo(SEUIL_MONTANT) > 0;
            boolean lieuInhabituel = !transaction.lieu().equalsIgnoreCase(pays);
            boolean tropFrequente = frequentes.contains(transaction);
            if (montantEleve || lieuInhabituel || tropFrequente) {
                resultat.add(transaction);
            }
        }
        return trierParDate(resultat);
    }
}
