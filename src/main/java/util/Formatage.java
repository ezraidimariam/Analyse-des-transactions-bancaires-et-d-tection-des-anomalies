package util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Formatage {
    public static String montant(BigDecimal valeur) {
        return valeur.setScale(2).toPlainString();
    }

    public static String date(LocalDateTime valeur) {
        return valeur.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}

