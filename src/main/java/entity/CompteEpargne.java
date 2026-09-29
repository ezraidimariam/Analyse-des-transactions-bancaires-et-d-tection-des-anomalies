package entity;

import java.math.BigDecimal;

public final class CompteEpargne extends Compte {
    private final BigDecimal tauxInteret;

    public CompteEpargne(Long id, String numero, BigDecimal solde, Long idClient, BigDecimal tauxInteret) {
        super(id, numero, solde, idClient);
        this.tauxInteret = tauxInteret;
    }

    public BigDecimal getTauxInteret() { return tauxInteret; }

    @Override
    public String toString() {
        return "Epargne | " + super.toString() + " | taux=" + tauxInteret;
    }
}

