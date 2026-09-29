package util;

import java.math.BigDecimal;

public class Validation {
    public static String texte(String valeur, String champ, int longueurMax) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " obligatoire.");
        }
        String resultat = valeur.trim();
        if (resultat.length() > longueurMax) {
            throw new IllegalArgumentException(champ + " trop long (maximum " + longueurMax + ").");
        }
        return resultat;
    }

    public static BigDecimal montant(BigDecimal valeur) {
        if (valeur == null || valeur.stripTrailingZeros().scale() > 2
                || valeur.abs().compareTo(new BigDecimal("9999999999999.99")) > 0) {
            throw new IllegalArgumentException("Montant invalide : maximum 13 chiffres et 2 decimales.");
        }
        return valeur;
    }

    public static BigDecimal positif(BigDecimal valeur) {
        montant(valeur);
        if (valeur.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }
        return valeur;
    }

    public static BigDecimal nonNegatif(BigDecimal valeur) {
        montant(valeur);
        if (valeur.signum() < 0) {
            throw new IllegalArgumentException("La valeur ne peut pas etre negative.");
        }
        return valeur;
    }

    public static void id(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Identifiant invalide.");
        }
    }
}

