package entity;

import java.math.BigDecimal;

public abstract sealed class Compte permits CompteCourant, CompteEpargne {
    private final Long id;
    private final String numero;
    private final BigDecimal solde;
    private final Long idClient;

    protected Compte(Long id, String numero, BigDecimal solde, Long idClient) {
        this.id = id;
        this.numero = numero;
        this.solde = solde;
        this.idClient = idClient;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public BigDecimal getSolde() { return solde; }
    public Long getIdClient() { return idClient; }

    @Override
    public String toString() {
        return "ID=" + id + " | numero=" + numero + " | solde=" + solde + " | client=" + idClient;
    }
}

