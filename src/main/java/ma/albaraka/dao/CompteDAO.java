package ma.albaraka.dao;

import ma.albaraka.entity.Compte;
import java.util.List;
import java.util.Optional;

public interface CompteDAO {
    Compte save(Compte compte);
    void updateSolde(Long id, java.math.BigDecimal solde);
    List<Compte> findByClient(Long idClient);
    Optional<Compte> findByNumero(String numero);
    Optional<Compte> findMaximum();
    Optional<Compte> findMinimum();
    List<Compte> findInactive(int jours);
}