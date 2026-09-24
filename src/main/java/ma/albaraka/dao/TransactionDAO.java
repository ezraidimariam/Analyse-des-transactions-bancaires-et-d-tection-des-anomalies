package ma.albaraka.dao;

import ma.albaraka.entity.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionDAO {
    Transaction save(Transaction transaction);
    List<Transaction> findByCompte(Long idCompte);
    List<Transaction> findByClient(Long idClient);
    List<Transaction> findBetween(LocalDateTime debut, LocalDateTime fin);
    List<Transaction> findAll();
}