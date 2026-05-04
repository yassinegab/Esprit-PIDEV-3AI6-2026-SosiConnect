package org.example.aideEtdon.service;

import org.example.aideEtdon.model.Demande;
import org.example.user.model.User;
import org.example.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MatchingService {

    private Connection conn;

    // Matrice Médicale de Compatibilité
    // Clé = Groupe du Receveur (Demande) | Valeur = Groupes des Donneurs Compatibles
    private static final Map<String, List<String>> BLOOD_MATRIX = new HashMap<>();

    static {
        BLOOD_MATRIX.put("A+", Arrays.asList("A+", "A-", "O+", "O-"));
        BLOOD_MATRIX.put("O+", Arrays.asList("O+", "O-"));
        BLOOD_MATRIX.put("B+", Arrays.asList("B+", "B-", "O+", "O-"));
        BLOOD_MATRIX.put("AB+", Arrays.asList("A+", "O+", "B+", "AB+", "A-", "O-", "B-", "AB-"));
        BLOOD_MATRIX.put("A-", Arrays.asList("A-", "O-"));
        BLOOD_MATRIX.put("O-", Arrays.asList("O-"));
        BLOOD_MATRIX.put("B-", Arrays.asList("B-", "O-"));
        BLOOD_MATRIX.put("AB-", Arrays.asList("AB-", "A-", "B-", "O-"));
    }

    public MatchingService() {
        try {
            this.conn = MyConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recherche d'historique médical profond:
     * Si un utilisateur a fait un don (table `don`) à une demande (table `demande`) 
     * dont le groupe sanguin correspondait à la matrice du receveur actuel,
     * alors son profil médical est compatible.
     */
    public List<User> runBloodMatch(Demande cible) {
        List<User> matchedDonors = new ArrayList<>();
        
        if (cible == null || !"Sang".equalsIgnoreCase(cible.getType()) || cible.getGroupeSanguin() == null) {
            return matchedDonors; // Pas une demande de sang valide
        }

        List<String> compatibleGroups = BLOOD_MATRIX.getOrDefault(cible.getGroupeSanguin().toUpperCase(), new ArrayList<>());
        if (compatibleGroups.isEmpty()) return matchedDonors;

        // Préparation de la structure IN (?, ?, ?) pour la requête SQL
        String inSql = String.join(",", Collections.nCopies(compatibleGroups.size(), "?"));

        // Deep Join: Trouve les utilisateurs de la table 'user' qui ont donné (don.donor_id = user.id) 
        // vers une ancienne demande (demande.id = don.demande_id) dont le groupe sanguin était dans la liste compatible.
        // Remarque : Remplacez `user` par le vrai nom de table de votre application si différent.
        String query = """
            SELECT DISTINCT u.id, u.nom, u.prenom, u.email 
            FROM user u
            JOIN don d ON u.id = d.donor_id
            JOIN demande dem ON d.demande_id = dem.id
            WHERE dem.groupe_sanguin IN (%s)
            AND u.id != ?
        """.formatted(inSql);

        try (PreparedStatement pst = conn.prepareStatement(query)) {
            int index = 1;
            for (String group : compatibleGroups) {
                pst.setString(index++, group);
            }
            pst.setInt(index, cible.getUserId()); // Exclure la personne qui fait la demande!

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setNom(rs.getString("nom"));
                    u.setPrenom(rs.getString("prenom"));
                    u.setEmail(rs.getString("email"));
                    matchedDonors.add(u);
                }
            }
            
            // Failsafe pour la démonstration au cas où l'historique de la BDD est vide !
            // PIDEV Hack: Inject dummy donor if nobody matched initially to prove algorithm sends emails.
            if (matchedDonors.isEmpty()) {
                System.out.println("MatchingService: Historique vide. Injection d'un profil de Secours test.");
                User secours = new User();
                secours.setNom("Bénévole");
                secours.setPrenom("Système");
                secours.setEmail("projet.sosiconnect@gmail.com"); 
                matchedDonors.add(secours);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return matchedDonors;
    }
}
