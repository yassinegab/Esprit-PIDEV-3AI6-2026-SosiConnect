package org.example.user.model;

import java.sql.Timestamp;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String telephone;
    private UserRole role;
    private int age;
    private String sexe;
    private double poids;
    private double taille;
    private String handicap;
    private Timestamp dateCreation;
    private Timestamp derniereMiseAJour;

    public User() {
    }

    public User(int id, String nom, String prenom, String email, String password,
                String telephone, UserRole role, int age, String sexe,
                double poids, double taille, String handicap,
                Timestamp dateCreation, Timestamp derniereMiseAJour) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.telephone = telephone;
        this.role = role;
        this.age = age;
        this.sexe = sexe;
        this.poids = poids;
        this.taille = taille;
        this.handicap = handicap;
        this.dateCreation = dateCreation;
        this.derniereMiseAJour = derniereMiseAJour;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public double getTaille() { return taille; }
    public void setTaille(double taille) { this.taille = taille; }
    public double getPoids() { return poids; }
    public void setPoids(double poids) { this.poids = poids; }
    public boolean isHandicap() { return handicap; }
    public void setHandicap(boolean handicap) { this.handicap = handicap; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public String getUser_role() { return user_role; }
    public void setUser_role(String user_role) { this.user_role = user_role; }
    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }
    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    // Helper for Hospital Module
    public String getNomComplet() {
        return (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
    }

    public UserRole getRole() {
        if (user_role == null) return UserRole.PATIENT;
        try {
            return UserRole.valueOf(user_role.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (user_role.equalsIgnoreCase("CLIENT")) return UserRole.PATIENT;
            return UserRole.PATIENT;
        }
    }

    public void setRole(UserRole role) {
        if (role != null) {
            this.user_role = role.name();
        }
    }
}
