package ui;

import entity.*;
import service.*;
import util.DatabaseConnection;
import util.Formatage;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final ClientService clients = new ClientService();
    private final CompteService comptes = new CompteService();
    private final TransactionService transactions = new TransactionService();
    private final RapportService rapports = new RapportService();

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connexion PostgreSQL reussie.");
        } catch (SQLException exception) {
            System.err.println("Connexion impossible : verifiez PostgreSQL et URL, USER, PASSWORD dans DatabaseConnection.");
            System.exit(1);
        }
        new Main().demarrer();
    }

    public void demarrer() {
        while (true) {
            try {
                System.out.println("\n=== BANQUE AL BARAKA ===");
                System.out.println("1. Gestion des clients\n2. Gestion des comptes\n3. Gestion des transactions\n4. Analyses et rapports\n0. Quitter");
                var choix = texte("Choix : ");
                switch (choix) {
                    case "1" -> menuClients();
                    case "2" -> menuComptes();
                    case "3" -> menuTransactions();
                    case "4" -> menuAnalyses();
                    case "0" -> { System.out.println("Au revoir."); return; }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (NoSuchElementException exception) {
                System.out.println("Fin de saisie.");
                return;
            } catch (NumberFormatException exception) {
                System.out.println("Saisie invalide : entrez un nombre avec un point pour les decimales.");
            } catch (DateTimeParseException exception) {
                System.out.println("Date invalide : respectez le format indique.");
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            } catch (SQLException exception) {
                String message = switch (Objects.toString(exception.getSQLState(), "")) {
                    case "23505" -> "Email ou numero de compte deja utilise.";
                    case "23503" -> "Client ou compte lie introuvable.";
                    case "23514", "23502", "22003" -> "Donnees incompatibles avec les contraintes de la base.";
                    default -> "Erreur SQL : verifiez la connexion et les tables de schema.sql.";
                };
                System.out.println(message);
            }
        }
    }

    private void menuClients() throws SQLException {
        System.out.println("\n=== CLIENTS ===\n1. Ajouter\n2. Modifier\n3. Supprimer\n4. Rechercher\n5. Lister\n6. Bilan\n0. Retour");
        switch (texte("Choix : ")) {
            case "1" -> {
                String nom = texte("Nom : ");
                String email = texte("Email : ");
                System.out.println("Client cree : " + clients.ajouter(nom, email));
            }
            case "2" -> {
                long id = id("ID client : ");
                String nom = texte("Nouveau nom : ");
                String email = texte("Nouvel email : ");
                clients.modifier(id, nom, email);
                System.out.println("Client modifie.");
            }
            case "3" -> {
                long id = id("ID client : ");
                if (texte("Supprimer aussi ses comptes et transactions ? oui/non : ").equalsIgnoreCase("oui")) {
                    clients.supprimer(id);
                    System.out.println("Client supprime.");
                }
            }
            case "4" -> {
                String choix = texte("1. Par ID / 2. Par nom : ");
                if (choix.equals("1")) {
                    System.out.println(clients.rechercherParId(id("ID : "))
                            .orElseThrow(() -> new IllegalArgumentException("Client introuvable.")));
                } else if (choix.equals("2")) { afficher(clients.rechercherParNom(texte("Nom : "))); }
                else { System.out.println("Choix invalide."); }
            }
            case "5" -> afficher(clients.lister());
            case "6" -> {
                long id = id("ID client : ");
                System.out.println("Nombre de comptes : " + clients.nombreComptes(id));
                System.out.println("Solde total : " + Formatage.montant(clients.soldeTotal(id)));
            }
            case "0" -> { }
            default -> System.out.println("Choix invalide.");
        }
    }

    private void menuComptes() throws SQLException {
        System.out.println("\n=== COMPTES ===\n1. Creer courant\n2. Creer epargne\n3. Modifier\n4. Rechercher par client\n5. Rechercher par numero\n6. Solde maximum\n7. Solde minimum\n8. Lister\n9. Supprimer\n0. Retour");
        String choix = texte("Choix : ");
        switch (choix) {
            case "1", "2" -> creerCompte(choix);
            case "3" -> modifierCompte();
            case "4" -> afficher(comptes.rechercherParClient(id("ID client : ")));
            case "5" -> afficherCompte(comptes.rechercherParNumero(texte("Numero : ")));
            case "6" -> afficherCompte(comptes.maximum());
            case "7" -> afficherCompte(comptes.minimum());
            case "8" -> afficher(comptes.lister());
            case "9" -> {
                long id = id("ID compte : ");
                if (texte("Supprimer aussi les transactions ? oui/non : ").equalsIgnoreCase("oui")) {
                    comptes.supprimer(id);
                    System.out.println("Compte supprime.");
                }
            }
            case "0" -> { }
            default -> System.out.println("Choix invalide.");
        }
    }

    private void creerCompte(String type) throws SQLException {
        long client = id("ID client : ");
        String numero = texte("Numero du compte : ");
        BigDecimal solde = montant("Solde initial : ");
        Compte compte;
        if (type.equals("1")) {
            compte = comptes.creerCourant(numero, solde, client, montant("Decouvert autorise : "));
        } else {
            compte = comptes.creerEpargne(numero, solde, client, montant("Taux d'interet : "));
        }
        System.out.println("Compte cree : " + compte);
    }

    private void modifierCompte() throws SQLException {
        long id = id("ID compte : ");
        System.out.println("1. Solde\n2. Decouvert autorise\n3. Taux d'interet");
        switch (texte("Choix : ")) {
            case "1" -> comptes.mettreAJourSolde(id, montant("Nouveau solde : "));
            case "2" -> comptes.modifierDecouvert(id, montant("Nouveau decouvert : "));
            case "3" -> comptes.modifierTaux(id, montant("Nouveau taux : "));
            default -> throw new IllegalArgumentException("Choix invalide.");
        }
        System.out.println("Compte modifie.");
    }

    private void menuTransactions() throws SQLException {
        System.out.println("\n=== TRANSACTIONS ===\n1. Versement\n2. Retrait\n3. Virement\n4. Historique d'un compte\n5. Filtrer\n6. Historique d'un client\n0. Retour");
        String choix = texte("Choix : ");
        switch (choix) {
            case "1", "2", "3" -> operation(choix);
            case "4" -> historique(transactions.parCompte(id("ID compte : ")));
            case "5" -> filtrer();
            case "6" -> historique(transactions.parClient(id("ID client : ")));
            case "0" -> { }
            default -> System.out.println("Choix invalide.");
        }
    }

    private void operation(String choix) throws SQLException {
        long source = id("ID compte source : ");
        long destination = choix.equals("3") ? id("ID compte destination : ") : 0;
        BigDecimal montant = montant("Montant : ");
        String lieu = texte("Lieu (pays, ex. Maroc) : ");
        switch (choix) {
            case "1" -> transactions.versement(source, montant, lieu);
            case "2" -> transactions.retrait(source, montant, lieu);
            case "3" -> transactions.virement(source, destination, montant, lieu);
            default -> throw new IllegalArgumentException("Operation invalide.");
        }
        System.out.println("Operation enregistree.");
    }

    private void filtrer() throws SQLException {
        System.out.println("Laisser vide pour ignorer un filtre.");
        BigDecimal minimum = montantFacultatif("Montant minimum : ");
        BigDecimal maximum = montantFacultatif("Montant maximum : ");
        String choixType = texte("Type : VERSEMENT / RETRAIT / VIREMENT : ");
        TypeTransaction type = switch (choixType.toUpperCase(Locale.ROOT)) {
            case "" -> null;
            case "VERSEMENT" -> TypeTransaction.VERSEMENT;
            case "RETRAIT" -> TypeTransaction.RETRAIT;
            case "VIREMENT" -> TypeTransaction.VIREMENT;
            default -> throw new IllegalArgumentException("Type invalide.");
        };
        LocalDateTime debut = dateFacultative("Debut inclus (AAAA-MM-JJTHH:MM:SS) : ");
        LocalDateTime fin = dateFacultative("Fin incluse (AAAA-MM-JJTHH:MM:SS) : ");
        String lieu = texte("Lieu : ");
        historique(transactions.filtrer(transactions.lister(), minimum, maximum, type, debut, fin, lieu));
    }

    private void menuAnalyses() throws SQLException {
        System.out.println("\n=== ANALYSES ===\n1. Top 5 clients\n2. Transactions par type\n3. Rapport mensuel\n4. Transactions suspectes\n5. Comptes inactifs\n6. Alertes\n7. Regroupement par mois\n0. Retour");
        switch (texte("Choix : ")) {
            case "1" -> {
                Map<Client, BigDecimal> top = rapports.top5();
                if (top.isEmpty()) { System.out.println("Aucun client."); }
                top.forEach((client, solde) -> System.out.println(client.nom() + " | " + Formatage.montant(solde)));
            }
            case "2" -> {
                var liste = transactions.lister();
                afficherNombres(liste);
                transactions.regrouperParType(liste).forEach((type, groupe) -> {
                    System.out.println(type);
                    afficherTransactions(groupe);
                });
            }
            case "3" -> {
                YearMonth mois = YearMonth.parse(texte("Mois (AAAA-MM) : "));
                var liste = rapports.rapportMensuel(mois);
                afficherNombres(liste);
                System.out.println("Volume des mouvements : " + Formatage.montant(rapports.volumeTotal(liste)));
            }
            case "4" -> afficherTransactions(rapports.transactionsSuspectes(texte("Pays habituel : ")));
            case "5" -> afficher(rapports.comptesInactifs(entier("Inactifs depuis combien de jours : ")));
            case "6" -> {
                BigDecimal seuil = montant("Seuil de solde bas : ");
                int jours = entier("Seuil d'inactivite en jours : ");
                System.out.println("ALERTE - Solde bas");
                afficher(rapports.soldesBas(seuil));
                System.out.println("ALERTE - Inactivite");
                afficher(rapports.comptesInactifs(jours));
            }
            case "7" -> transactions.regrouperParMois(transactions.lister()).forEach((mois, liste) -> {
                System.out.println(mois);
                historique(liste);
            });
            case "0" -> { }
            default -> System.out.println("Choix invalide.");
        }
    }

    private void afficherNombres(List<Transaction> liste) {
        var nombres = rapports.nombreParType(liste);
        for (TypeTransaction type : TypeTransaction.values()) {
            System.out.println(type + " : " + nombres.getOrDefault(type, 0L));
        }
    }

    private void historique(List<Transaction> liste) {
        afficherTransactions(liste);
        System.out.println("Total : " + Formatage.montant(transactions.total(liste)));
        System.out.println("Moyenne : " + Formatage.montant(transactions.moyenne(liste)));
    }

    private void afficherTransactions(List<Transaction> liste) {
        if (liste.isEmpty()) { System.out.println("Aucune transaction."); }
        liste.forEach(t -> System.out.println(t.id() + " | " + Formatage.date(t.date()) + " | "
                + t.type() + " | " + Formatage.montant(t.montant()) + " | " + t.lieu() + " | compte=" + t.idCompte()));
    }

    private void afficher(List<?> liste) {
        if (liste.isEmpty()) { System.out.println("Aucun resultat."); }
        liste.forEach(System.out::println);
    }

    private void afficherCompte(Optional<Compte> compte) {
        compte.ifPresentOrElse(System.out::println, () -> System.out.println("Aucun compte trouve."));
    }

    private String texte(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private long id(String message) {
        long id = Long.parseLong(texte(message));
        util.Validation.id(id);
        return id;
    }

    private int entier(String message) { return Integer.parseInt(texte(message)); }
    private BigDecimal montant(String message) { return new BigDecimal(texte(message)); }

    private BigDecimal montantFacultatif(String message) {
        String valeur = texte(message);
        return valeur.isEmpty() ? null : new BigDecimal(valeur);
    }

    private LocalDateTime dateFacultative(String message) {
        String valeur = texte(message);
        return valeur.isEmpty() ? null : LocalDateTime.parse(valeur);
    }
}
