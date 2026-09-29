package service;

import dao.ClientDAO;
import dao.CompteDAO;
import dao.TransactionDAO;
import entity.*;
import util.DatabaseConnection;
import util.Validation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionService {
    public static final BigDecimal SEUIL_MONTANT = new BigDecimal("10000");
    public static final int MAX_OPERATIONS_PAR_MINUTE = 3;
    private final CompteDAO comptes = new CompteDAO();
    private final TransactionDAO transactions = new TransactionDAO();

    public void versement(long id, BigDecimal montant, String lieu) throws SQLException {
        enregistrer(id, null, montant, lieu, TypeTransaction.VERSEMENT);
    }

    public void retrait(long id, BigDecimal montant, String lieu) throws SQLException {
        enregistrer(id, null, montant, lieu, TypeTransaction.RETRAIT);
    }

    public void virement(long source, long destination, BigDecimal montant, String lieu) throws SQLException {
        Validation.id(destination);
        if (source == destination) {
            throw new IllegalArgumentException("Choisissez deux comptes differents.");
        }
        enregistrer(source, destination, montant, lieu, TypeTransaction.VIREMENT);
    }

    private void enregistrer(long id, Long destination, BigDecimal montant, String lieu, TypeTransaction type)
            throws SQLException {
        Validation.id(id);
        Validation.positif(montant);
        lieu = Validation.texte(lieu, "Lieu", 120);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Compte source;
                Compte cible = null;
                // Meme ordre de verrouillage pour deux virements en sens inverse.
                if (destination != null && destination < id) {
                    cible = comptes.verrouiller(connection, destination);
                    source = comptes.verrouiller(connection, id);
                } else {
                    source = comptes.verrouiller(connection, id);
                    if (destination != null) { cible = comptes.verrouiller(connection, destination); }
                }
                BigDecimal solde = type == TypeTransaction.VERSEMENT
                        ? source.getSolde().add(montant) : source.getSolde().subtract(montant);
                CompteService.verifierSolde(source, solde);
                comptes.mettreAJourSolde(connection, id, solde);
                LocalDateTime date = LocalDateTime.now();
                transactions.ajouter(connection, new Transaction(null, date, montant, type, lieu, id));
                if (cible != null) {
                    BigDecimal soldeCible = cible.getSolde().add(montant);
                    CompteService.verifierSolde(cible, soldeCible);
                    comptes.mettreAJourSolde(connection, destination, soldeCible);
                    transactions.ajouter(connection, new Transaction(null, date, montant, type, lieu, destination));
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Transaction> parCompte(long id) throws SQLException {
        comptes.rechercherParId(id).orElseThrow(() -> new IllegalArgumentException("Compte introuvable."));
        return trierParDate(transactions.rechercherParCompte(id));
    }

    public List<Transaction> parClient(long id) throws SQLException {
        new ClientDAO().rechercherParId(id).orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
        List<Long> ids = comptes.rechercherParClient(id).stream().map(Compte::getId).toList();
        return trierParDate(lister().stream().filter(t -> ids.contains(t.idCompte())).toList());
    }

    public List<Transaction> lister() throws SQLException { return transactions.findAll(); }

    public List<Transaction> trierParDate(List<Transaction> liste) {
        return liste.stream().sorted(Comparator.comparing(Transaction::date)).toList();
    }

    public List<Transaction> filtrer(List<Transaction> liste, BigDecimal minimum, BigDecimal maximum,
                                    TypeTransaction type, LocalDateTime debut, LocalDateTime fin, String lieu) {
        if (minimum != null) { Validation.nonNegatif(minimum); }
        if (maximum != null) { Validation.nonNegatif(maximum); }
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Minimum superieur au maximum.");
        }
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new IllegalArgumentException("Periode invalide.");
        }
        return liste.stream()
                .filter(t -> minimum == null || t.montant().compareTo(minimum) >= 0)
                .filter(t -> maximum == null || t.montant().compareTo(maximum) <= 0)
                .filter(t -> type == null || t.type() == type)
                .filter(t -> debut == null || !t.date().isBefore(debut))
                .filter(t -> fin == null || !t.date().isAfter(fin))
                .filter(t -> lieu == null || lieu.isBlank() || t.lieu().equalsIgnoreCase(lieu.trim()))
                .sorted(Comparator.comparing(Transaction::date)).toList();
    }

    public Map<TypeTransaction, List<Transaction>> regrouperParType(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(Transaction::type));
    }

    public Map<YearMonth, List<Transaction>> regrouperParMois(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.date()), TreeMap::new, Collectors.toList()));
    }

    public BigDecimal total(List<Transaction> liste) {
        return liste.stream().map(Transaction::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal moyenne(List<Transaction> liste) {
        return liste.isEmpty() ? BigDecimal.ZERO : total(liste).divide(BigDecimal.valueOf(liste.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalCompte(long id) throws SQLException { return total(parCompte(id)); }
    public BigDecimal totalClient(long id) throws SQLException { return total(parClient(id)); }

    public List<Transaction> suspectes(List<Transaction> liste, String paysHabituel) {
        String pays = Validation.texte(paysHabituel, "Pays habituel", 120);
        Set<Transaction> frequentes = new HashSet<>();
        Map<Long, List<Transaction>> groupes = liste.stream().collect(Collectors.groupingBy(Transaction::idCompte));
        for (List<Transaction> groupe : groupes.values()) {
            List<Transaction> triees = trierParDate(groupe);
            for (int debut = 0; debut < triees.size(); debut++) {
                LocalDateTime limite = triees.get(debut).date().plusMinutes(1);
                int fin = debut;
                // Une fenetre de 60 secondes inclut exactement sa borne de fin.
                while (fin < triees.size() && !triees.get(fin).date().isAfter(limite)) { fin++; }
                if (fin - debut > MAX_OPERATIONS_PAR_MINUTE) {
                    frequentes.addAll(triees.subList(debut, fin));
                }
            }
        }
        return liste.stream().filter(t -> t.montant().compareTo(SEUIL_MONTANT) > 0
                        || !t.lieu().equalsIgnoreCase(pays) || frequentes.contains(t))
                .sorted(Comparator.comparing(Transaction::date)).toList();
    }
}

