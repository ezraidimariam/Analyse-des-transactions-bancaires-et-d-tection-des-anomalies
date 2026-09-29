package entity;

import java.math.BigDecimal;

public final class CompteCourant extends Compte {
    private final BigDecimal decouvertAutorise;

    public CompteCourant(Long id, String numero, BigDecimal solde, Long idClient,
            BigDecimal decouvertAutorise) {
        super(id, numero, solde, idClient);
        this.decouvertAutorise = decouvertAutorise;
    }

    public BigDecimal getDecouvertAutorise() {
        return decouvertAutorise;
    }

    @Override
    public String toString() {
        return "Courant | " + super.toString() + " | decouvert=" + decouvertAutorise;
    }
}
