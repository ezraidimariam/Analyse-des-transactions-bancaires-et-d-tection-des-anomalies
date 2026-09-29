package service;

import dao.ClientDAO;
import dao.CompteDAO;
import entity.Client;
import entity.Compte;
import util.Validation;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientService {
    private final ClientDAO clients = new ClientDAO();
    private final CompteDAO comptes = new CompteDAO();

    public Client ajouter(String nom, String email) throws SQLException {
        return clients.ajouter(valider(new Client(null, nom, email)));
    }

    public void modifier(long id, String nom, String email) throws SQLException {
        Validation.id(id);
        clients.modifier(valider(new Client(id, nom, email)));
    }

    private Client valider(Client client) {
        String nom = Validation.texte(client.nom(), "Nom", 120);
        String email = Validation.texte(client.email(), "Email", 180);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email invalide.");
        }
        return new Client(client.id(), nom, email);
    }

    public void supprimer(long id) throws SQLException {
        Validation.id(id);
        clients.supprimer(id);
    }

    public Optional<Client> rechercherParId(long id) throws SQLException {
        Validation.id(id);
        return clients.rechercherParId(id);
    }

    public List<Client> rechercherParNom(String nom) throws SQLException {
        return clients.rechercherParNom(Validation.texte(nom, "Nom", 120));
    }

    public List<Client> lister() throws SQLException { return clients.findAll(); }

    public int nombreComptes(long idClient) throws SQLException {
        verifierClient(idClient);
        return comptes.rechercherParClient(idClient).size();
    }

    public BigDecimal soldeTotal(long idClient) throws SQLException {
        verifierClient(idClient);
        return comptes.rechercherParClient(idClient).stream()
                .map(Compte::getSolde).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void verifierClient(long id) throws SQLException {
        rechercherParId(id).orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
    }
}

