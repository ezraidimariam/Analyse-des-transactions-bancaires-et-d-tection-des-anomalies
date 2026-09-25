# Analyse des transactions

Application Java de gestion et d'analyse de comptes bancaires avec PostgreSQL.

## Fonctionnalités

- Gestion des clients.
- Gestion des comptes courants et comptes d'épargne.
- Mise à jour des soldes.
- Recherche de comptes par client ou numéro.
- Recherche des comptes avec le solde maximum ou minimum.
- Détection des comptes inactifs.
- Gestion des transactions bancaires.

## Prérequis

- Java 25 LTS.
- Maven 3.9 ou supérieur.
- PostgreSQL.

## Configuration de la base de données

Créez une base nommée `analyse_transactions`, puis exécutez le schéma :

```bash
psql -U postgres -d analyse_transactions -f src/main/resources/schema.sql
```

La connexion utilise par défaut :

- URL : `jdbc:postgresql://localhost:5432/analyse_transactions`
- Utilisateur : `postgres`
- Mot de passe : `postgres`

Ces valeurs peuvent être remplacées avec les variables d'environnement suivantes :

```text
DB_URL=jdbc:postgresql://localhost:5432/analyse_transactions
DB_USER=postgres
DB_PASSWORD=postgres
```

## Compilation et tests

```bash
mvn clean test
```

Pour compiler uniquement le projet et les tests :

```bash
mvn clean test-compile
```

## Exécution

Le `pom.xml` configure `ma.albaraka.ui.Main` comme classe principale :

```bash
mvn exec:java
```

La classe `ma.albaraka.ui.Main` doit être présente dans le projet pour exécuter cette commande. Le code métier et l'accès aux données se trouvent sous `src/main/java/ma/albaraka`.

## Structure

```text
src/main/java/ma/albaraka/
├── dao/       Interfaces et implémentations JDBC
├── entity/    Modèle métier des clients, comptes et transactions
└── util/      Connexion à la base et validations
```

## Class diagram

```mermaid
classDiagram
	direction LR

	class Client {
		<<record>>
		+Long id
		+String nom
		+String email
	}

	class Compte {
		<<sealed interface>>
		+Long id()
		+String numero()
		+BigDecimal solde()
		+Long idClient()
	}

	class CompteCourant {
		<<record>>
		+BigDecimal decouvertAutorise
	}

	class CompteEpargne {
		<<record>>
		+BigDecimal tauxInteret
	}

	class Transaction {
		<<record>>
		+Long id
		+LocalDateTime date
		+BigDecimal montant
		+TypeTransaction type
		+String lieu
		+Long idCompte
	}

	class TypeTransaction {
		<<enumeration>>
		VERSEMENT
		RETRAIT
		VIREMENT
	}

	class ClientDAO {
		<<interface>>
		+save(Client) Client
		+update(Client) Client
		+delete(Long) void
		+findById(Long) Optional~Client~
		+findByNom(String) List~Client~
		+findAll() List~Client~
	}

	class CompteDAO {
		<<interface>>
		+save(Compte) Compte
		+updateSolde(Long, BigDecimal) void
		+findByClient(Long) List~Compte~
		+findByNumero(String) Optional~Compte~
		+findMaximum() Optional~Compte~
		+findMinimum() Optional~Compte~
		+findInactive(int) List~Compte~
	}

	class TransactionDAO {
		<<interface>>
		+save(Transaction) Transaction
		+findByCompte(Long) List~Transaction~
		+findByClient(Long) List~Transaction~
		+findBetween(LocalDateTime, LocalDateTime) List~Transaction~
		+findAll() List~Transaction~
	}

	class JdbcClientDAO {
		<<JDBC implementation>>
	}

	class JdbcCompteDAO {
		<<JDBC implementation>>
	}

	class Database {
		<<utility>>
		+getConnection() Connection
	}

	class Validation {
		<<utility>>
		+required(String, String) String
		+positive(BigDecimal, String) BigDecimal
	}

	Compte <|.. CompteCourant
	Compte <|.. CompteEpargne
	ClientDAO <|.. JdbcClientDAO
	CompteDAO <|.. JdbcCompteDAO

	Client "1" --> "0..*" Compte : idClient
	Compte "1" --> "0..*" Transaction : idCompte
	Transaction --> TypeTransaction

	JdbcClientDAO ..> Client
	JdbcCompteDAO ..> Compte
	JdbcClientDAO ..> Database
	JdbcCompteDAO ..> Database
```

## Dépendances principales

- PostgreSQL JDBC `42.7.12`
- Maven Compiler Plugin `3.14.1`
- Maven Surefire Plugin `3.5.3`
