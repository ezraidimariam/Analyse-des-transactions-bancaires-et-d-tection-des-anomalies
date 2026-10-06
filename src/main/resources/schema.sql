CREATE TABLE IF NOT EXISTS client (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS compte (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    solde NUMERIC(15,2) NOT NULL,
    id_client BIGINT NOT NULL,
    type_compte VARCHAR(10) NOT NULL CHECK (type_compte IN ('COURANT', 'EPARGNE')),
    decouvert_autorise NUMERIC(15,2) CHECK (decouvert_autorise >= 0),
    taux_interet NUMERIC(5,2) CHECK (taux_interet >= 0),

    FOREIGN KEY (id_client) REFERENCES client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_bancaire (
    id BIGSERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL,
    montant NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    type VARCHAR(10) NOT NULL CHECK (type IN ('VERSEMENT', 'RETRAIT', 'VIREMENT')),
    lieu VARCHAR(120) NOT NULL,
    id_compte BIGINT NOT NULL,

    FOREIGN KEY (id_compte) REFERENCES compte(id) ON DELETE CASCADE
);
