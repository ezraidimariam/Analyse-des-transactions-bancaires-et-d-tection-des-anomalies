package service;

import dao.ClientDAO;
import dao.CompteDAO;
import dao.TransactionDAO;
import entity.Client;
import entity.Compte;
import entity.Transaction;
import entity.TypeTransaction;
import util.Validation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RapportService {
    private final ClientDAO clientDAO;
    private final CompteDAO compteDAO;
    private final TransactionDAO transactionDAO;
    private final TransactionService transactionService;

    public RapportService() {
        clientDAO = new ClientDAO();
        compteDAO = new CompteDAO();
        transactionDAO = new TransactionDAO();
        transactionService = new TransactionService();
    }

    // LinkedHashMap garde l'ordre du classement.
    public Map<Client, BigDecimal> top5() throws SQLException {
        List<Compte> listeComptes = compteDAO.findAll();
        Map<Client, BigDecimal> soldes = new LinkedHashMap<>();
        for (Client client : clientDAO.findAll()) {
            BigDecimal total = calculerSoldeClient(client, listeComptes);
            soldes.put(client, total);
        }

        List<Map.Entry<Client, BigDecimal>> classement = soldes.entrySet().stream()
                .sorted(Map.Entry.<Client, BigDecimal>comparingByValue().reversed())
                .limit(5).toList();

        Map<Client, BigDecimal> resultat = new LinkedHashMap<>();
        for (Map.Entry<Client, BigDecimal> ligne : classement) {
            resultat.put(ligne.getKey(), ligne.getValue());
        }
        return resultat;
    }

    private BigDecimal calculerSoldeClient(Client client, List<Compte> comptes) {
        BigDecimal total = BigDecimal.ZERO;
        for (Compte compte : comptes) {
            if (compte.getIdClient().equals(client.id())) {
                total = total.add(compte.getSolde());
            }
        }
        return total;
    }

    public Map<TypeTransaction, Long> nombreParType(List<Transaction> liste) {
        return liste.stream()
                .collect(Collectors.groupingBy(Transaction::type, Collectors.counting()));
    }

    public BigDecimal volumeTotal(List<Transaction> liste) {
        return transactionService.total(liste);
    }

    public List<Transaction> rapportMensuel(YearMonth mois) throws SQLException {
        List<Transaction> liste = transactionDAO.findAll();
        return liste.stream()
                .filter(transaction -> YearMonth.from(transaction.date()).equals(mois)).toList();
    }

    public List<Transaction> transactionsSuspectes(String pays) throws SQLException {
        List<Transaction> liste = transactionDAO.findAll();
        return transactionService.suspectes(liste, pays);
    }

    public List<Compte> comptesInactifs(int jours) throws SQLException {
        if (jours <= 0) {
            throw new IllegalArgumentException("Nombre de jours strictement positif requis.");
        }
        LocalDateTime limite = LocalDateTime.now().minusDays(jours);
        List<Transaction> liste = transactionDAO.findAll();

        return compteDAO.findAll().stream()
                .filter(compte -> estInactif(compte, liste, limite)).toList();
    }

    private boolean estInactif(Compte compte, List<Transaction> liste, LocalDateTime limite) {
        for (Transaction transaction : liste) {
            if (transaction.idCompte().equals(compte.getId())) {
                if (!transaction.date().isBefore(limite)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<Compte> soldesBas(BigDecimal seuil) throws SQLException {
        Validation.nonNegatif(seuil);

        List<Compte> liste = compteDAO.findAll();
        return liste.stream().filter(compte -> compte.getSolde().compareTo(seuil) < 0).toList();
    }
}
