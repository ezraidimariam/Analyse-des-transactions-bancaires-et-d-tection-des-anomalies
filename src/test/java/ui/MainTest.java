package ui;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private String executer(String saisie) {
        var entree = System.in;
        var sortie = System.out;
        var capture = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(saisie.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
            new Main().demarrer();
        } finally {
            System.setIn(entree);
            System.setOut(sortie);
        }
        return capture.toString(StandardCharsets.UTF_8);
    }

    @Test
    void navigationEtRetours() {
        String resultat = executer("1\n0\n2\n0\n3\n0\n4\n0\n0\n");
        assertTrue(resultat.contains("=== CLIENTS ==="));
        assertTrue(resultat.contains("=== COMPTES ==="));
        assertTrue(resultat.contains("=== TRANSACTIONS ==="));
        assertTrue(resultat.contains("=== ANALYSES ==="));
        assertTrue(resultat.contains("Au revoir."));
    }

    @Test
    void saisieInvalideEtFinDeFlux() {
        String resultat = executer("invalide\n3\n1\nabc\n0\n");
        assertTrue(resultat.contains("Choix invalide."));
        assertTrue(resultat.contains("Saisie invalide"));
        assertTrue(resultat.contains("Au revoir."));
        assertTrue(executer("1\n").contains("Fin de saisie."));
    }
}

