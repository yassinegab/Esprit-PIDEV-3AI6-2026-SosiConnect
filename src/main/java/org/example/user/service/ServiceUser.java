package org.example.user.service;

import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean emailExistsForAnotherUser(String email, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ? AND id <> ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public void add(User user) throws SQLException {
        String sql = "INSERT INTO user(nom, prenom, email, password, telephone, role, age, sexe, poids, taille, handicap) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, hashedPassword);
            ps.setString(5, user.getTelephone());
            ps.setString(6, user.getRole().name());
            ps.setInt(7, user.getAge());
            ps.setString(8, user.getSexe());
            ps.setDouble(9, user.getPoids());
            ps.setDouble(10, user.getTaille());
            ps.setString(11, user.getHandicap());

            ps.executeUpdate();
        }
    }

    public void updateAdminUser(User user) throws SQLException {
        String sql = "UPDATE user SET nom = ?, prenom = ?, email = ?, telephone = ?, role = ?, age = ?, sexe = ?, poids = ?, taille = ?, handicap = ? WHERE id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getTelephone());
            ps.setString(5, user.getRole().name());
            ps.setInt(6, user.getAge());
            ps.setString(7, user.getSexe());
            ps.setDouble(8, user.getPoids());
            ps.setDouble(9, user.getTaille());
            ps.setString(10, user.getHandicap());
            ps.setInt(11, user.getId());

            ps.executeUpdate();
        }
    }

    public void updatePatientProfile(User user) throws SQLException {
        String sql = "UPDATE user SET nom = ?, prenom = ?, email = ?, telephone = ?, age = ?, sexe = ?, poids = ?, taille = ?, handicap = ? WHERE id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getTelephone());
            ps.setInt(5, user.getAge());
            ps.setString(6, user.getSexe());
            ps.setDouble(7, user.getPoids());
            ps.setDouble(8, user.getTaille());
            ps.setString(9, user.getHandicap());
            ps.setInt(10, user.getId());

            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public User login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                boolean valid;
                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                    valid = BCrypt.checkpw(password, storedPassword);
                } else {
                    valid = password.equals(storedPassword);
                    if (valid) {
                        migratePlainPasswordToHashed(rs.getInt("id"), password);
                    }
                }

                if (valid) {
                    return mapResultSetToUser(rs);
                }
            }
        }

        return null;
    }

    private void migratePlainPasswordToHashed(int userId, String plainPassword) throws SQLException {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id DESC";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }

        return list;
    }

    public List<User> searchUsers(String keyword, String roleFilter) throws SQLException {
        List<User> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM user WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (nom LIKE ? OR prenom LIKE ? OR email LIKE ? OR CONCAT(nom, ' ', prenom) LIKE ?) ");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (roleFilter != null && !roleFilter.equalsIgnoreCase("TOUS")) {
            sql.append("AND role = ? ");
            params.add(roleFilter.toUpperCase());
        }

        sql.append("ORDER BY id DESC");

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }

        return list;
    }

    public List<User> getPatients() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE role = 'PATIENT' ORDER BY nom, prenom";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }

        return list;
    }

    public List<User> searchPatientsByName(String keyword) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE role = 'PATIENT' AND (nom LIKE ? OR prenom LIKE ? OR CONCAT(nom, ' ', prenom) LIKE ?) ORDER BY nom, prenom";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }

        return list;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }

        return null;
    }

    public User findOrCreateGoogleUser(String email, String firstName, String lastName) throws SQLException {
        User existing = findByEmail(email);

        if (existing != null) {
            return existing;
        }

        User user = new User();
        user.setNom(lastName == null || lastName.isBlank() ? "Google" : lastName.trim());
        user.setPrenom(firstName == null || firstName.isBlank() ? "Utilisateur" : firstName.trim());
        user.setEmail(email);
        user.setPassword(java.util.UUID.randomUUID().toString() + "Aa1");
        user.setTelephone("00000000");
        user.setRole(UserRole.PATIENT);
        user.setAge(18);
        user.setSexe("Autre");
        user.setPoids(0);
        user.setTaille(0);
        user.setHandicap("");

        add(user);

        return findByEmail(email);
    }

    public int countAllUsers() throws SQLException {
        return countByQuery("SELECT COUNT(*) FROM user");
    }

    public int countByRole(UserRole role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countAgeBetween(int min, int max) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE age BETWEEN ? AND ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, min);
            ps.setInt(2, max);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countAgeGreaterThan(int min) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE age > ?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, min);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private int countByQuery(String sql) throws SQLException {
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("telephone"),
                UserRole.valueOf(rs.getString("role")),
                rs.getInt("age"),
                rs.getString("sexe"),
                rs.getDouble("poids"),
                rs.getDouble("taille"),
                rs.getString("handicap"),
                rs.getTimestamp("dateCreation"),
                rs.getTimestamp("derniereMiseAJour")
        );
    }
}