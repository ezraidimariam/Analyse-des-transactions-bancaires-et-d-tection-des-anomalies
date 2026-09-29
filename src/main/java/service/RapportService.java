package service;

import dao.ClientDAO;
import dao.CompteDAO;
import dao.TransactionDAO;
import entity.*;
import util.Validation;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class RapportService {
    private final ClientDAO clients = new ClientDAO();
    private final CompteDAO comptes = new CompteDAO();
    private final TransactionDAO transactions = new TransactionDAO();
    private final TransactionService analyses = new TransactionService();

    // LinkedHashMap garde l'ordre du classement, sans ajouter de classe DTO.
    public Map<Client, BigDecimal> top5() throws SQLException {
        List<Compte> listeComptes = comptes.findAll();
        Map<Client, BigDecimal> soldes = new LinkedHashMap<>();
        for (Client client : clients.findAll()) {
            BigDecimal total = listeComptes.stream().filter(c -> c.getIdClient().equals(client.id()))
                    .map(Compte::getSolde).reduce(BigDecimal.ZERO, BigDecimal::add);
            soldes.put(client, total);
        }
        return soldes.entrySet().stream()
                .sorted(Map.Entry.<Client, BigDecimal>comparingByValue().reversed())
                .limit(5).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (premier, second) -> premier, LinkedHashMap::new));
    }

    public Map<TypeTransaction, Long> nombreParType(List<Transaction> liste) {
        return liste.stream().collect(Collectors.groupingBy(Transaction::type, Collectors.counting()));
    }

    public BigDecimal volumeTotal(List<Transaction> liste) { return analyses.total(liste); }

    public List<Transaction> rapportMensuel(YearMonth mois) throws SQLException {
        return transactions.findAll().stream()
                .filter(t -> YearMonth.from(t.date()).equals(mois)).toList();
    }

    public List<Transaction> transactionsSuspectes(String pays) throws SQLException {
        return analyses.suspectes(transactions.findAll(), pays);
    }

    public List<Compte> comptesInactifs(int jours) throws SQLException {
        if (jours <= 0) { throw new IllegalArgumentException("Nombre de jours strictement positif requis."); }
        LocalDateTime limite = LocalDateTime.now().minusDays(jours);
        List<Transaction> liste = transactions.findAll();
        return comptes.findAll().stream().filter(compte -> {
            Optional<LocalDateTime> derniereDate = liste.stream()
                    .filter(t -> t.idCompte().equals(compte.getId()))
                    .map(Transaction::date).max(Comparator.naturalOrder());
            // Sans transaction, le compte est considere inactif.
            return derniereDate.map(date -> date.isBefore(limite)).orElse(true);
        }).toList();
    }

    public List<Compte> soldesBas(BigDecimal seuil) throws SQLException {
        Validation.nonNegatif(seuil);
        return comptes.findAll().stream().filter(c -> c.getSolde().compareTo(seuil) < 0).toList();
    }
}

