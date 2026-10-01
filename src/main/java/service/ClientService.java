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
    private final ClientDAO clientDAO;
    private final CompteDAO compteDAO;

    public ClientService() {
        clientDAO = new ClientDAO();
        compteDAO = new CompteDAO();
    }

    public Client ajouter(String nom, String email) throws SQLException {
        Client client = new Client(null, nom, email);
        client = valider(client);
        return clientDAO.ajouter(client);
    }

    public void modifier(long id, String nom, String email) throws SQLException {
        Validation.id(id);
        Client client = new Client(id, nom, email);
        client = valider(client);
        clientDAO.modifier(client);
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
        clientDAO.supprimer(id);
    }

    public Optional<Client> rechercherParId(long id) throws SQLException {
        Validation.id(id);

        return clientDAO.rechercherParId(id);
    }

    public List<Client> rechercherParNom(String nom) throws SQLException {
        nom = Validation.texte(nom, "Nom", 120);
        return clientDAO.rechercherParNom(nom);
    }

    public List<Client> lister() throws SQLException {
        return clientDAO.findAll();
    }

    public int nombreComptes(long idClient) throws SQLException {
        verifierClient(idClient);

        List<Compte> listeComptes = compteDAO.rechercherParClient(idClient);
        return listeComptes.size();
    }

    public BigDecimal soldeTotal(long idClient) throws SQLException {
        verifierClient(idClient);

        List<Compte> listeComptes = compteDAO.rechercherParClient(idClient);
        BigDecimal total = BigDecimal.ZERO;
        for (Compte compte : listeComptes) {
            total = total.add(compte.getSolde());
        }
        return total;
    }

    private void verifierClient(long id) throws SQLException {
        rechercherParId(id).orElseThrow(() -> new IllegalArgumentException("Client introuvable."));
    }
}
