package ma.albaraka.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(Long id, LocalDateTime date, BigDecimal montant,
                          TypeTransaction type, String lieu, Long idCompte) {
}