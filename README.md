# Al Baraka — Analyse des transactions bancaires

Application console réalisée pour le brief 3, sprint 1 de YouCode.
Elle centralise les clients, comptes et transactions de la Banque Al Baraka et
permet de repérer les anomalies et de produire des rapports.

## Technologies et organisation

Java 17, JDBC, PostgreSQL, Maven, Git. Aucun framework applicatif.

```text
src/main/java/
  entity/   Client, Compte, CompteCourant, CompteEpargne, Transaction, TypeTransaction
  dao/      ClientDAO, CompteDAO, TransactionDAO
  service/  ClientService, CompteService, TransactionService, RapportService
  util/     DatabaseConnection, Validation, Formatage
  ui/       Main
src/main/resources/
  creation-base.sql
  schema.sql
src/test/java/
  service/  AnalyseTest, BanqueIntegrationTest
  ui/       MainTest
diagramme-classes.puml
pom.xml
```

Flux : **UI → Service → DAO → PostgreSQL**.
Les DAO utilisent PreparedStatement et try-with-resources.
Les services appliquent les règles métier. Main gère la saisie et les messages.

## Fonctionnalités

- Clients : création, modification, suppression, recherche par ID/nom, liste et bilan.
- Comptes courants/épargne : création, modification du solde/découvert/taux,
  recherche par client/numéro, minimum/maximum, liste et suppression.
- Versement, retrait, virement avec mise à jour des soldes et historique.
- Historique par compte/client, total, moyenne et tri par date.
- Filtres combinables par montant, type, période et lieu.
- Regroupements par type et mois, top 5 clients, rapport mensuel.
- Détection des montants élevés, lieux inhabituels et fréquences excessives.
- Comptes inactifs, alertes de solde bas et d'inactivité.

## PostgreSQL

Prérequis : PostgreSQL démarré, JDK 17 ou supérieur, Maven.

Dans `src/main/java/util/DatabaseConnection.java`, configurer simplement :

- URL : `jdbc:postgresql://localhost:5432/albaraka`
- USER : `postgres`
- PASSWORD : votre mot de passe local.

Le mot de passe est déjà configuré dans le fichier local de cette machine.
La version enregistrée dans Git laisse ce champ vide.
Après modification de ces constantes, recompiler le JAR.

Créer la base **une seule fois** :

```shell
psql -U postgres -d postgres -f src/main/resources/creation-base.sql
psql -U postgres -d albaraka -f src/main/resources/schema.sql
```

Avec pgAdmin : exécuter `CREATE DATABASE albaraka;` dans la base postgres,
ouvrir ensuite le Query Tool de la base albaraka et y exécuter `schema.sql`.
Sur cette machine, la base et les trois tables sont déjà créées.

Le schéma correspond à une nouvelle base. `IF NOT EXISTS` conserve les tables
existantes ; il ne transforme pas un ancien schéma différent.

## Compiler et exécuter

```shell
mvn clean package
java -jar target/albaraka-1.0.0-jar-with-dependencies.jar
```

Ce JAR contient le pilote PostgreSQL. PostgreSQL doit rester démarré.
Dans IntelliJ IDEA : ouvrir `pom.xml` comme projet Maven, choisir un JDK 17 ou
supérieur et lancer `ui.Main`.

Si Maven n'est pas dans PATH sur cette machine :

```powershell
& "$env:USERPROFILE/.maven/maven-3.9.15/bin/mvn.cmd" clean package
```

## Menu et exemple

```text
=== BANQUE AL BARAKA ===
1. Gestion des clients
2. Gestion des comptes
3. Gestion des transactions
4. Analyses et rapports
0. Quitter
```

1. Clients → Ajouter : saisir un nom et un email, puis noter l'ID affiché.
2. Comptes → Créer courant : saisir l'ID client, un numéro unique, un solde et un découvert.
3. Comptes → Créer épargne : créer le deuxième compte.
4. Transactions → Virement : saisir les IDs source/destination, le montant et le pays.
5. Transactions → Historique d'un compte : consulter chaque compte.
6. Analyses → Rapport mensuel : saisir par exemple `2026-09`.

Les montants utilisent un point et au maximum deux décimales.
Les filtres de dates attendent `AAAA-MM-JJTHH:MM:SS`, avec deux bornes incluses.
Un filtre vide est ignoré. Chaque sous-menu revient au menu principal après l'action.
Une saisie incorrecte affiche un message et laisse continuer.

## Règles simples de l'exercice

- Un compte épargne ne devient pas négatif ; un courant respecte son découvert.
- Les opérations partagent une connexion SQL avec commit/rollback. Les comptes
  sont verrouillés pendant l'opération pour éviter les pertes de mise à jour.
- Un virement enregistre deux mouvements positifs de type VIREMENT. Le modèle du
  brief n'a ni sens débit/crédit ni identifiant reliant les deux mouvements.
  Les rapports comptent les mouvements : un virement de 100 contribue à 200 de volume.
- Modifier manuellement un solde est une opération de gestion distincte d'un versement.
- Le CRUD de TransactionDAO manipule les lignes ; les opérations bancaires passent
  par TransactionService afin de mettre à jour le solde et l'historique ensemble.
- Montant suspect : strictement supérieur à 10000.
- Lieu inhabituel : différent du pays saisi pour l'analyse, sans distinction de casse.
- Fréquence excessive : plus de 3 mouvements du même compte dans une fenêtre
  de 60 secondes, borne finale incluse. Les constantes sont dans TransactionService.
- Un compte sans transaction est considéré inactif, faute de date d'ouverture dans le brief.
- Supprimer un client ou un compte supprime aussi les lignes liées, après confirmation.
- Le taux d'intérêt est enregistré et modifiable ; aucune capitalisation automatique.
- Les bonus CSV/JSON et logs fichier ne sont pas ajoutés.

## Tests

```shell
mvn test
```

Cette commande exécute les six tests sans base et ignore les cinq tests PostgreSQL.
Pour exécuter les **11 tests**, avec les tables créées et la connexion configurée :

```powershell
$env:RUN_DB_TESTS = "true"
mvn test
Remove-Item Env:RUN_DB_TESTS
```

Les tests PostgreSQL créent des clients temporaires et suppriment uniquement ceux
qu'ils ont créés, ainsi que leurs comptes et transactions. Ils ne vident pas les tables.
Ils vérifient CRUD, transfert, rollback, retraits concurrents, rapports et menu.
Les tests sans base vérifient les filtres, calculs, anomalies, bornes temporelles et saisies.

## Pour le débriefing Java 17

| Élément | Exemple dans le code |
|---|---|
| record | Client, Transaction |
| sealed class | Compte, avec deux sous-classes final |
| switch expression | type du filtre et message SQL dans Main |
| var | choix du menu et listes des rapports dans Main |
| Optional | rechercherParId et maximum/minimum |
| filter, map, sorted, reduce | TransactionService |
| Collectors.groupingBy | regroupements par type/mois et fréquence |
| lambda | filtres, tris et affichages |

Le diagramme PlantUML est dans [diagramme-classes.puml](diagramme-classes.puml).
Il représente les 17 types du code applicatif, leurs attributs métier et leurs
dépendances ; les classes de test ne font pas partie du diagramme.

