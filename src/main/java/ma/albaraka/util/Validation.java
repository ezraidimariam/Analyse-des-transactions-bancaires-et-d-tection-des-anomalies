package ma.albaraka.util;

import java.math.BigDecimal;

public final class Validation {
    private Validation() {
    }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " est obligatoire.");
        }
        return value.trim();
    }

    public static BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " doit être positif.");
        }
        return value;
    }
}