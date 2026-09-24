package ma.albaraka.entity;

import java.math.BigDecimal;

public sealed interface Compte permits CompteCourant, CompteEpargne {
    Long id();
    String numero();
    BigDecimal solde();
    Long idClient();
}