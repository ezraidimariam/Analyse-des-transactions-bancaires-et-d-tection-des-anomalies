package ma.albaraka.entity;

import java.math.BigDecimal;

public record CompteEpargne(Long id, String numero, BigDecimal solde, Long idClient,
                            BigDecimal tauxInteret) implements Compte {
}