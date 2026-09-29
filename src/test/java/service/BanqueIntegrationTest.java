package service;

import dao.*;
import entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class BanqueIntegrationTest {
    private final ClientService clients = new ClientService();
    private final CompteService comptes = new CompteService();
    private final TransactionService transactions = new TransactionService();
    private final TransactionDAO dao = new TransactionDAO();
    private final List<Long> clientsCrees = new ArrayList<>();

    private Client client() throws SQLException {
        String nom = "TEST-" + UUID.randomUUID().toString().substring(0, 12);
        Client client = clients.ajouter(nom, nom + "@test.ma");
        clientsCrees.add(client.id());
        return client;
    }

    private Compte courant(Client client, String solde) throws SQLException {
        return comptes.creerCourant("T-" + UUID.randomUUID().toString().substring(0, 20),
                new BigDecimal(solde), client.id(), BigDecimal.ZERO);
    }

    @AfterEach
    void nettoyerUniquementLesClientsCreesParCeTest() throws SQLException {
        for (Long id : clientsCrees) {
            if (clients.rechercherParId(id).isPresent()) { clients.supprimer(id); }
        }
    }

    private void solde(Compte compte, String attendu) throws SQLException {
        assertEquals(0, comptes.rechercherParId(compte.getId()).orElseThrow().getSolde().compareTo(new BigDecimal(attendu)));
    }

    @Test
    void crudClientsComptesEtTransactions() throws Exception {
        Client client = client();
        clients.modifier(client.id(), "Client modifie", client.email());
        assertEquals("Client modifie", clients.rechercherParId(client.id()).orElseThrow().nom());
        assertTrue(clients.rechercherParNom("modifie").stream().anyMatch(c -> c.id().equals(client.id())));
        assertThrows(SQLException.class, () -> clients.ajouter("Doublon", client.email()));
        Compte courant = courant(client, "100");
        Compte epargne = comptes.creerEpargne("E-" + client.id(), new BigDecimal("200"), client.id(), BigDecimal.ONE);
        assertEquals(2, clients.nombreComptes(client.id()));
        assertEquals(0, clients.soldeTotal(client.id()).compareTo(new BigDecimal("300")));
        comptes.modifierDecouvert(courant.getId(), BigDecimal.TEN);
        comptes.mettreAJourSolde(courant.getId(), new BigDecimal("110"));
        comptes.modifierTaux(epargne.getId(), new BigDecimal("2"));
        assertEquals(0, ((CompteCourant) comptes.rechercherParId(courant.getId()).orElseThrow())
                .getDecouvertAutorise().compareTo(BigDecimal.TEN));
        assertEquals(0, ((CompteEpargne) comptes.rechercherParId(epargne.getId()).orElseThrow())
                .getTauxInteret().compareTo(new BigDecimal("2")));
        assertEquals(courant.getId(), comptes.rechercherParNumero(courant.getNumero()).orElseThrow().getId());
        assertEquals(2, comptes.rechercherParClient(client.id()).size());
        assertTrue(comptes.maximum().isPresent());
        assertTrue(comptes.minimum().isPresent());
        Transaction t = dao.ajouter(new Transaction(null, LocalDateTime.now(), BigDecimal.ONE,
                TypeTransaction.VERSEMENT, "Maroc", courant.getId()));
        dao.modifier(new Transaction(t.id(), t.date(), BigDecimal.TEN, t.type(), "France", t.idCompte()));
        assertEquals("France", dao.rechercherParId(t.id()).orElseThrow().lieu());
        dao.supprimer(t.id());
        assertTrue(dao.rechercherParId(t.id()).isEmpty());
        comptes.supprimer(epargne.getId());
        assertTrue(comptes.rechercherParId(epargne.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> clients.rechercherParId(0));
        assertThrows(IllegalArgumentException.class, () -> clients.ajouter("Test", "email-invalide"));
    }

    @Test
    void versementRetraitVirementHistoriqueEtRollback() throws Exception {
        Client client = client();
        Compte source = courant(client, "100");
        Compte cible = courant(client, "50");
        transactions.versement(source.getId(), new BigDecimal("20"), "Maroc");
        transactions.retrait(source.getId(), BigDecimal.TEN, "Maroc");
        transactions.virement(source.getId(), cible.getId(), new BigDecimal("30"), "Maroc");
        solde(source, "80");
        solde(cible, "80");
        assertEquals(3, transactions.parCompte(source.getId()).size());
        assertEquals(1, transactions.parCompte(cible.getId()).size());
        assertEquals(4, transactions.parClient(client.id()).size());
        assertEquals(0, transactions.totalCompte(source.getId()).compareTo(new BigDecimal("60")));
        assertEquals(0, transactions.totalClient(client.id()).compareTo(new BigDecimal("90")));
        assertThrows(IllegalArgumentException.class, () -> transactions.retrait(source.getId(), new BigDecimal("81"), "Maroc"));
        assertThrows(IllegalArgumentException.class, () -> transactions.virement(source.getId(), source.getId(), BigDecimal.ONE, "Maroc"));
        // L'echec du credit survient apres l'ecriture du debit : tout doit etre annule.
        comptes.mettreAJourSolde(cible.getId(), new BigDecimal("9999999999999.99"));
        assertThrows(IllegalArgumentException.class, () -> transactions.virement(source.getId(), cible.getId(), BigDecimal.ONE, "Maroc"));
        solde(source, "80");
        solde(cible, "9999999999999.99");
        assertEquals(4, transactions.parClient(client.id()).size());
    }

    @Test
    void retraitsConcurrentsRespectentLeSolde() throws Exception {
        Compte compte = courant(client(), "100");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        Callable<Boolean> retrait = () -> {
            depart.await();
            try {
                transactions.retrait(compte.getId(), new BigDecimal("80"), "Maroc");
                return true;
            } catch (IllegalArgumentException exception) { return false; }
        };
        try {
            Future<Boolean> a = pool.submit(retrait);
            Future<Boolean> b = pool.submit(retrait);
            depart.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        solde(compte, "20");
        assertEquals(1, transactions.parCompte(compte.getId()).size());
    }

    @Test
    void rapportsEtInactivite() throws Exception {
        Client client = client();
        Compte inactif = courant(client, "5");
        Compte actif = courant(client, "20");
        Compte ancien = courant(client, "30");
        dao.ajouter(new Transaction(null, LocalDateTime.now().minusDays(40), BigDecimal.ONE,
                TypeTransaction.VERSEMENT, "Maroc", ancien.getId()));
        transactions.versement(actif.getId(), new BigDecimal("10001"), "France");
        RapportService rapports = new RapportService();
        var idsInactifs = rapports.comptesInactifs(30).stream().map(Compte::getId).toList();
        assertTrue(idsInactifs.contains(inactif.getId()));
        assertTrue(idsInactifs.contains(ancien.getId()));
        assertFalse(idsInactifs.contains(actif.getId()));
        assertTrue(rapports.soldesBas(BigDecimal.TEN).stream().anyMatch(c -> c.getId().equals(inactif.getId())));
        var mois = rapports.rapportMensuel(YearMonth.now());
        assertTrue(mois.stream().anyMatch(t -> t.idCompte().equals(actif.getId())));
        assertTrue(rapports.nombreParType(mois).get(TypeTransaction.VERSEMENT) >= 1);
        assertTrue(rapports.volumeTotal(mois).compareTo(new BigDecimal("10001")) >= 0);
        assertTrue(rapports.transactionsSuspectes("Maroc").stream().anyMatch(t -> t.idCompte().equals(actif.getId())));
        // Comparer le top 5 a tous les soldes reels, meme si la base contient deja des clients.
        List<BigDecimal> attendus = new ArrayList<>();
        for (Client c : clients.lister()) { attendus.add(clients.soldeTotal(c.id())); }
        attendus.sort(Comparator.reverseOrder());
        assertEquals(attendus.stream().limit(5).toList(), new ArrayList<>(rapports.top5().values()));
    }

    @Test
    void menuEnregistreUneOperationEtContinueApresErreur() throws Exception {
        Compte compte = courant(client(), "100");
        String saisie = String.join("\n", "3", "1", compte.getId().toString(), "20", "Maroc",
                "3", "2", compte.getId().toString(), "999", "Maroc", "0") + "\n";
        var entree = System.in;
        var sortie = System.out;
        var capture = new java.io.ByteArrayOutputStream();
        try {
            System.setIn(new java.io.ByteArrayInputStream(saisie.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            System.setOut(new java.io.PrintStream(capture, true, java.nio.charset.StandardCharsets.UTF_8));
            new ui.Main().demarrer();
        } finally {
            System.setIn(entree);
            System.setOut(sortie);
        }
        solde(compte, "120");
        String resultat = capture.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(resultat.contains("Solde insuffisant"));
        assertTrue(resultat.contains("Au revoir."));
    }
}

