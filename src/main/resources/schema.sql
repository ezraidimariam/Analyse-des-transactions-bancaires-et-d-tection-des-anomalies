CREATE TABLE IF NOT EXISTS client (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS compte (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    solde NUMERIC(15, 2) NOT NULL DEFAULT 0,
    id_client BIGINT NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    type_compte VARCHAR(20) NOT NULL CHECK (type_compte IN ('COURANT', 'EPARGNE')),
    decouvert_autorise NUMERIC(15, 2),
    taux_interet NUMERIC(5, 2)
);

CREATE TABLE IF NOT EXISTS transaction_bancaire (
    id BIGSERIAL PRIMARY KEY,
    date_transaction TIMESTAMP NOT NULL,
    montant NUMERIC(15, 2) NOT NULL CHECK (montant > 0),
    type_transaction VARCHAR(20) NOT NULL CHECK (type_transaction IN ('VERSEMENT', 'RETRAIT', 'VIREMENT')),
    lieu VARCHAR(120) NOT NULL,
    id_compte BIGINT NOT NULL REFERENCES compte(id) ON DELETE CASCADE
);