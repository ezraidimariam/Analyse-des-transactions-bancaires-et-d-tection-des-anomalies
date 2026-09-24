package ma.albaraka.entity;

import java.math.BigDecimal;

public record CompteCourant(Long id, String numero, BigDecimal solde, Long idClient,
                            BigDecimal decouvertAutorise) implements Compte {
}