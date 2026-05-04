package org.example.user.model;

import java.sql.Timestamp;

public class DossierMedical {

    private int id;
    private int userId;
    private String antecedentsMedicaux;
    private String maladiesChroniques;
    private String allergies;
    private String traitementsEnCours;
    private String diagnostics;
    private String notesMedecin;
    private String objectifSante;
    private String niveauActivite;
    private Timestamp dateCreation;
    private Timestamp derniereMiseAJour;

    public DossierMedical() {
    }

    public DossierMedical(int id, int userId, String antecedentsMedicaux, String maladiesChroniques,
                          String allergies, String traitementsEnCours, String diagnostics,
                          String notesMedecin, String objectifSante, String niveauActivite,
                          Timestamp dateCreation, Timestamp derniereMiseAJour) {
        this.id = id;
        this.userId = userId;
        this.antecedentsMedicaux = antecedentsMedicaux;
        this.maladiesChroniques = maladiesChroniques;
        this.allergies = allergies;
        this.traitementsEnCours = traitementsEnCours;
        this.diagnostics = diagnostics;
        this.notesMedecin = notesMedecin;
        this.objectifSante = objectifSante;
        this.niveauActivite = niveauActivite;
        this.dateCreation = dateCreation;
        this.derniereMiseAJour = derniereMiseAJour;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    public String getAntecedentsMedicaux() {
        return antecedentsMedicaux;
    }

    public void setAntecedentsMedicaux(String antecedentsMedicaux) {
        this.antecedentsMedicaux = antecedentsMedicaux;
    }


    public String getMaladiesChroniques() {
        return maladiesChroniques;
    }

    public void setMaladiesChroniques(String maladiesChroniques) {
        this.maladiesChroniques = maladiesChroniques;
    }


    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }


    public String getTraitementsEnCours() {
        return traitementsEnCours;
    }

    public void setTraitementsEnCours(String traitementsEnCours) {
        this.traitementsEnCours = traitementsEnCours;
    }


    public String getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(String diagnostics) {
        this.diagnostics = diagnostics;
    }


    public String getNotesMedecin() {
        return notesMedecin;
    }

    public void setNotesMedecin(String notesMedecin) {
        this.notesMedecin = notesMedecin;
    }


    public String getObjectifSante() {
        return objectifSante;
    }

    public void setObjectifSante(String objectifSante) {
        this.objectifSante = objectifSante;
    }


    public String getNiveauActivite() {
        return niveauActivite;
    }

    public void setNiveauActivite(String niveauActivite) {
        this.niveauActivite = niveauActivite;
    }


    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }


    public Timestamp getDerniereMiseAJour() {
        return derniereMiseAJour;
    }

    public void setDerniereMiseAJour(Timestamp derniereMiseAJour) {
        this.derniereMiseAJour = derniereMiseAJour;
    }
}