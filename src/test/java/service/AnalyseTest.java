package service;

import entity.*;
import util.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnalyseTest {
    private final TransactionService service = new TransactionService();
    private final LocalDateTime date = LocalDateTime.of(2026, 9, 1, 12, 0);

    private Transaction transaction(long id, long compte, LocalDateTime date, String montant,
            TypeTransaction type, String lieu) {
        return new Transaction(id, date, new BigDecimal(montant), type, lieu, compte);
    }

    @Test
    void filtresInclusifsTriGroupesEtCalculs() {
        Transaction a = transaction(1, 1, date, "10.10", TypeTransaction.VERSEMENT, "Maroc");
        Transaction b = transaction(2, 1, date.plusDays(1), "20.20", TypeTransaction.RETRAIT,
                "France");
        Transaction c = transaction(3, 2, date.plusMonths(1), "30.30", TypeTransaction.VERSEMENT,
                "Maroc");
        List<Transaction> liste = List.of(c, b, a);
        assertEquals(List.of(a, b, c), service.trierParDate(liste));
        assertEquals(List.of(a),
                service.filtrer(liste, new BigDecimal("10.10"), new BigDecimal("10.10"),
                        TypeTransaction.VERSEMENT, date, date, "maroc"));
        assertEquals(List.of(a, b), service.filtrer(liste, null, null, null, date, b.date(), ""));
        assertEquals(new BigDecimal("60.60"), service.total(liste));
        assertEquals(new BigDecimal("20.20"), service.moyenne(liste));
        assertEquals(BigDecimal.ZERO, service.moyenne(List.of()));
        assertEquals(2, service.regrouperParType(liste).get(TypeTransaction.VERSEMENT).size());
        assertEquals(2, service.regrouperParMois(liste).get(YearMonth.of(2026, 9)).size());
        assertThrows(IllegalArgumentException.class,
                () -> service.filtrer(liste, BigDecimal.TEN, BigDecimal.ONE, null, null, null,
                        null));
        assertThrows(IllegalArgumentException.class,
                () -> service.filtrer(liste, null, null, null, date.plusDays(1), date, null));
    }

    @Test
    void seuilMontantEtLieu() {
        Transaction normal = transaction(1, 1, date, "10000", TypeTransaction.VERSEMENT, "Maroc");
        Transaction montant = transaction(2, 2, date, "10000.01", TypeTransaction.VERSEMENT,
                "Maroc");
        Transaction lieu = transaction(3, 3, date, "1", TypeTransaction.VERSEMENT, "France");
        assertEquals(List.of(montant, lieu),
                service.suspectes(List.of(normal, montant, lieu), "maroc"));
    }

    @Test
    void frequenceParCompteInclutExactementSoixanteSecondes() {
        Transaction a = transaction(1, 1, date, "1", TypeTransaction.VERSEMENT, "Maroc");
        Transaction b = transaction(2, 1, date.plusSeconds(20), "1", TypeTransaction.VERSEMENT,
                "Maroc");
        Transaction c = transaction(3, 1, date.plusSeconds(40), "1", TypeTransaction.VERSEMENT,
                "Maroc");
        Transaction d = transaction(4, 1, date.plusSeconds(60), "1", TypeTransaction.VERSEMENT,
                "Maroc");
        assertEquals(4, service.suspectes(List.of(d, b, c, a), "Maroc").size());
        Transaction apres = transaction(4, 1, date.plusSeconds(60).plusNanos(1), "1",
                TypeTransaction.VERSEMENT, "Maroc");
        assertTrue(service.suspectes(List.of(a, b, c, apres), "Maroc").isEmpty());
        Transaction autreCompte = transaction(4, 2, d.date(), "1", TypeTransaction.VERSEMENT,
                "Maroc");
        assertTrue(service.suspectes(List.of(a, b, c, autreCompte), "Maroc").isEmpty());
    }

    @Test
    void precisionMonetaireEtDecouvert() {
        Compte courant = new CompteCourant(1L, "C", BigDecimal.ZERO, 1L, BigDecimal.TEN);
        Compte epargne = new CompteEpargne(2L, "E", BigDecimal.ZERO, 1L, BigDecimal.ONE);
        assertDoesNotThrow(() -> CompteService.verifierSolde(courant, new BigDecimal("-10")));
        assertThrows(IllegalArgumentException.class,
                () -> CompteService.verifierSolde(courant, new BigDecimal("-10.01")));
        assertThrows(IllegalArgumentException.class,
                () -> CompteService.verifierSolde(epargne, new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> Validation.positif(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> Validation.montant(new BigDecimal("0.001")));
        assertDoesNotThrow(() -> Validation.montant(new BigDecimal("1.000")));
    }
}
