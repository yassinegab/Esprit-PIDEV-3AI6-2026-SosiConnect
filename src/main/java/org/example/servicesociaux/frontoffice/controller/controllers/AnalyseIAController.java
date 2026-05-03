package org.example.servicesociaux.frontoffice.controller.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.user.model.DossierMedical;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyseIAController {

    /* ── FXML ── */
    @FXML private Label   titreLabel;
    @FXML private Label   statusLabel;
    @FXML private Label   errorLabel;
    @FXML private Label   engineLabel;
    @FXML private VBox    recapPanel;
    @FXML private TabPane tabPane;

    @FXML private Tab tabGroq;
    @FXML private Tab tabHF;
    @FXML private Tab tabVision;
    @FXML private Tab tabGemini;
    @FXML private Tab tabVoix;

    @FXML private VBox groqPanel;
    @FXML private VBox hfPanel;
    @FXML private VBox visionPanel;
    @FXML private VBox geminiPanel;
    @FXML private VBox voixPanel;

    @FXML private Button btnGroq;
    @FXML private Button btnHF;
    @FXML private Button btnVision;
    @FXML private Button btnGemini;
    @FXML private Button btnVoix;
    @FXML private Label  imageChoisieLabel;

    /* ════════════════════════════════════════════════════════
       ✅ FIX PRINCIPAL : Lecture du fichier .env
       Java NE lit PAS automatiquement les fichiers .env.
       Ce bloc charge le .env manuellement depuis le projet.
    ════════════════════════════════════════════════════════ */
    private static final Map<String, String> DOT_ENV = chargerDotEnv();

    private static Map<String, String> chargerDotEnv() {
        Map<String, String> env = new HashMap<>();

        // Liste des emplacements possibles pour le .env
        List<String> candidats = List.of(
                ".env",
                System.getProperty("user.dir") + File.separator + ".env",
                System.getProperty("user.dir") + File.separator + ".." + File.separator + ".env",
                "src" + File.separator + "main" + File.separator + "resources" + File.separator + ".env"
        );

        File envFile = null;
        for (String chemin : candidats) {
            File f = new File(chemin);
            if (f.exists() && f.isFile()) {
                envFile = f;
                System.out.println("[ENV] Fichier .env trouve : " + f.getAbsolutePath());
                break;
            }
        }

        if (envFile == null) {
            System.err.println("[ENV] ⚠ Aucun .env trouve. Chemins testes :");
            for (String c : candidats) System.err.println("       " + new File(c).getAbsolutePath());
            System.err.println("[ENV] Fallback sur System.getenv()");
            return env;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key   = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim()
                            .replaceAll("^\"|\"$", "")
                            .replaceAll("^'|'$", "");
                    env.put(key, value);
                    System.out.println("[ENV] ✓ " + key + " = "
                            + value.substring(0, Math.min(10, value.length())) + "...");
                }
            }
            System.out.println("[ENV] Total : " + env.size() + " cle(s) chargee(s)");
        } catch (Exception e) {
            System.err.println("[ENV] Erreur lecture : " + e.getMessage());
        }
        return env;
    }

    /** Lit une clé API : .env en priorité, puis System.getenv() */
    private static String getKey(String name) {
        String val = DOT_ENV.get(name);
        if (val != null && !val.isBlank()) return val;
        val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            System.out.println("[ENV] " + name + " lue via System.getenv()");
            return val;
        }
        System.err.println("[ENV] ⚠ Cle '" + name + "' INTROUVABLE dans .env et System.getenv() !");
        return "";
    }

    /* ── URLs ── */
    private static final String GROQ_URL         = "https://api.groq.com/openai/v1/chat/completions";
    private static final String HF_URL_BASE      = "https://api-inference.huggingface.co/models/";
    private static final String GEMINI_URL        = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final String GROQ_VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    /* ── Modèles HF avec fallbacks ── */
    private static final String HF_MODEL_1 = "facebook/bart-large-mnli";
    private static final String HF_MODEL_2 = "cross-encoder/nli-deberta-v3-base";
    private static final String HF_MODEL_3 = "typeform/distilbert-base-uncased-mnli";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    /* ── State ── */
    private DossierMedical dossier;
    private String         mode;
    private File           imageMedicaleChoisie;
    private Process        ttsProcess;

    /* ── Traductions HF EN→FR ── */
    private static final Map<String, String> FR = Map.ofEntries(
            Map.entry("critical condition",              "Etat critique"),
            Map.entry("severe condition",                "Etat grave"),
            Map.entry("moderate condition",              "Etat modere"),
            Map.entry("mild condition",                  "Etat leger"),
            Map.entry("stable condition",                "Etat stable"),
            Map.entry("high cardiovascular risk",        "Risque cardio eleve"),
            Map.entry("moderate cardiovascular risk",    "Risque cardio modere"),
            Map.entry("low cardiovascular risk",         "Risque cardio faible"),
            Map.entry("severe metabolic disorder",       "Trouble metabolique severe"),
            Map.entry("diabetes or obesity",             "Diabete / obesite"),
            Map.entry("metabolic syndrome",              "Syndrome metabolique"),
            Map.entry("healthy metabolism",              "Metabolisme equilibre"),
            Map.entry("severe respiratory failure",      "Insuffisance respiratoire"),
            Map.entry("breathing difficulty",            "Difficulte respiratoire"),
            Map.entry("normal breathing",                "Respiration normale"),
            Map.entry("immediate hospitalization needed","Hospitalisation urgente"),
            Map.entry("urgent medical consultation",     "Consultation urgente"),
            Map.entry("routine follow-up",               "Suivi de routine"),
            Map.entry("immunodeficiency",                "Immunodeficience"),
            Map.entry("moderate immune deficit",         "Deficit immunitaire modere"),
            Map.entry("healthy immune system",           "Immunite normale")
    );

    /* ── Dimensions HF ── */
    private static final LinkedHashMap<String, String[]> HF_DIMS = new LinkedHashMap<>() {{
        put("gravite",      new String[]{"critical condition","severe condition","moderate condition","mild condition","stable condition"});
        put("cardio",       new String[]{"high cardiovascular risk","moderate cardiovascular risk","low cardiovascular risk"});
        put("metabolique",  new String[]{"severe metabolic disorder","diabetes or obesity","metabolic syndrome","healthy metabolism"});
        put("respiratoire", new String[]{"severe respiratory failure","breathing difficulty","normal breathing"});
        put("urgence",      new String[]{"immediate hospitalization needed","urgent medical consultation","routine follow-up"});
        put("immunite",     new String[]{"immunodeficiency","moderate immune deficit","healthy immune system"});
    }};

    /* ════════════════════════════════════════════════════════
       INIT
    ════════════════════════════════════════════════════════ */
    @FXML
    public void initialize() {
        System.out.println("[INIT] AnalyseIAController");
        verifierCles();
        viderPanels();
    }

    private void verifierCles() {
        for (String nom : List.of("GROQ_KEY","HF_KEY","GEMINI_KEY")) {
            String val = getKey(nom);
            System.out.println("[API-CHECK] " + nom + " : "
                    + (val.isBlank() ? "MANQUANTE ⚠" : "OK (" + val.substring(0,Math.min(10,val.length())) + "...)"));
        }
    }

    public void setDossier(DossierMedical d, String mode) {
        this.dossier = d;
        this.mode    = mode;
        System.out.println("[INIT] Dossier #" + d.getId() + " mode=" + mode);
        titreLabel.setText("Analyse IA — Dossier #" + d.getId());
        remplirRecap(d);
        switch (mode) {
            case "GROQ"   -> lancerGroq();
            case "HF"     -> lancerHF();
            case "GEMINI" -> lancerGemini();
            case "VOIX"   -> lancerVoix();
        }
    }

    private void viderPanels() {
        for (VBox p : List.of(groqPanel,hfPanel,visionPanel,geminiPanel,voixPanel))
            if (p != null) { p.getChildren().clear(); afficherPlaceholder(p,"Cliquez sur le bouton."); }
    }

    /* ── Recap ── */
    private void remplirRecap(DossierMedical d) {
        recapPanel.getChildren().clear();
        row(recapPanel,"Dossier",    "#"+d.getId());
        row(recapPanel,"Cree le",    d.getDateCreationFormatee());
        row(recapPanel,"Activite",   nvl(d.getNiveauActivite(),"—"));
        row(recapPanel,"Maladies",   nvl(d.getMaladiesChroniques(),"Aucune"));
        row(recapPanel,"Traitements",nvl(d.getTraitementsEnCours(),"Aucun"));
        row(recapPanel,"Allergies",  nvl(d.getAllergies(),"Aucune"));
        row(recapPanel,"Diagnostics",nvl(d.getDiagnostics(),"—"));
        row(recapPanel,"Objectif",   nvl(d.getObjectifSante(),"—"));
    }
    private void row(VBox parent, String label, String valeur) {
        HBox r = new HBox(8); r.setAlignment(Pos.CENTER_LEFT);
        r.setStyle("-fx-padding:4 0;-fx-border-color:transparent transparent #f0e6ff transparent;");
        Label l = new Label(label+":"); l.setStyle("-fx-font-size:10;-fx-text-fill:#9c27b0;-fx-min-width:85;-fx-font-weight:bold;");
        String v = valeur != null && valeur.length()>40 ? valeur.substring(0,40)+"…" : nvl(valeur,"—");
        Label val = new Label(v); val.setStyle("-fx-font-size:11;-fx-text-fill:#333;"); val.setWrapText(true);
        r.getChildren().addAll(l,val); parent.getChildren().add(r);
    }

    /* ════════════════════════════════════════════════════════
       GROQ
    ════════════════════════════════════════════════════════ */
    @FXML public void lancerGroq() {
        String key = getKey("GROQ_KEY");
        if (key.isBlank()) { showError("GROQ_KEY manquante dans .env !"); return; }
        tabPane.getSelectionModel().select(tabGroq);
        engineLabel.setText("Groq LLaMA 3.3-70B");
        setAllBtns(true); setStatus("Groq en cours...");
        groqPanel.getChildren().clear();
        afficherChargement(groqPanel,"Interrogation Groq LLaMA 3.3-70B...");
        System.out.println("[GROQ] Cle : "+key.substring(0,Math.min(12,key.length()))+"...");

        CompletableFuture.supplyAsync(() -> { try { return appelGroq(key); } catch(Exception e){throw new RuntimeException(e);} })
                .thenAccept(json -> Platform.runLater(() -> {
                    setStatus("Groq termine ✓");
                    afficherResultatsGroq(json);
                    setAllBtns(false);
                }))
                .exceptionally(ex -> { Platform.runLater(() -> {
                    System.err.println("[GROQ ERR] "+rootMsg(ex));
                    groqPanel.getChildren().clear();
                    afficherErreurAPI(groqPanel,"Groq LLaMA",rootMsg(ex));
                    showError("Groq: "+rootMsg(ex)); setAllBtns(false);
                }); return null; });
    }

    private JsonNode appelGroq(String key) throws Exception {
        Map<String,Object> sys = new LinkedHashMap<>(); sys.put("role","system");
        sys.put("content","Tu es un medecin expert. Reponds UNIQUEMENT en JSON francais valide, sans texte avant/apres.");
        Map<String,Object> usr = new LinkedHashMap<>(); usr.put("role","user"); usr.put("content",promptGroq());
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("model","llama-3.3-70b-versatile");
        body.put("messages",List.of(sys,usr));
        body.put("temperature",0.15); body.put("max_tokens",5000);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GROQ_URL))
                .header("Authorization","Bearer "+key).header("Content-Type","application/json")
                .timeout(java.time.Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> resp = http.send(req,HttpResponse.BodyHandlers.ofString());
        System.out.println("[GROQ] HTTP "+resp.statusCode());
        if(resp.statusCode()==401) throw new RuntimeException("401 — Cle GROQ invalide. Verifiez GROQ_KEY dans .env");
        if(resp.statusCode()==429) throw new RuntimeException("429 — Rate limit Groq, attendez.");
        if(resp.statusCode()!=200) throw new RuntimeException("HTTP "+resp.statusCode()+" : "+resp.body().substring(0,Math.min(200,resp.body().length())));

        String c = MAPPER.readTree(resp.body()).at("/choices/0/message/content").asText();
        c = c.replaceAll("(?s)```json|```","").trim();
        int s=c.indexOf('{'), e=c.lastIndexOf('}');
        if(s>=0&&e>s) c=c.substring(s,e+1);
        System.out.println("[GROQ] Parse OK");
        return MAPPER.readTree(c);
    }

    private String promptGroq() {
        return "ANALYSE MEDICALE\n"
                +"- Antecedents: "+nvl(dossier.getAntecedentsMedicaux(),"Aucun")+"\n"
                +"- Maladies: "+nvl(dossier.getMaladiesChroniques(),"Aucune")+"\n"
                +"- Allergies: "+nvl(dossier.getAllergies(),"Aucune")+"\n"
                +"- Traitements: "+nvl(dossier.getTraitementsEnCours(),"Aucun")+"\n"
                +"- Diagnostics: "+nvl(dossier.getDiagnostics(),"Aucun")+"\n"
                +"- Objectif: "+nvl(dossier.getObjectifSante(),"Non defini")+"\n"
                +"- Activite: "+nvl(dossier.getNiveauActivite(),"Non defini")+"\n\n"
                +"Reponds avec ce JSON exact:\n"
                +"{\n"
                +"  \"gravite\":\"faible|modere|eleve\",\n"
                +"  \"score_sante\":<0-100>,\n"
                +"  \"urgence\":<true|false>,\n"
                +"  \"resume_cas\":\"...\",\n"
                +"  \"recommandations\":[\"...\"],\n"
                +"  \"examens_suggeres\":[\"...\"],\n"
                +"  \"facteurs_risque\":[\"...\"],\n"
                +"  \"complications_potentielles\":[\"...\"],\n"
                +"  \"plan_suivi\":{\"immediat\":\"...\",\"court_terme\":\"...\",\"moyen_terme\":\"...\",\"long_terme\":\"...\"},\n"
                +"  \"score_details\":{\"cardiovasculaire\":<0-100>,\"metabolique\":<0-100>,\"respiratoire\":<0-100>,\"renal\":<0-100>,\"hepatique\":<0-100>,\"neurologique\":<0-100>},\n"
                +"  \"medecins_experts\":[{\"nom\":\"...\",\"specialite\":\"...\",\"institution\":\"...\",\"pays\":\"...\",\"email\":\"...\"}],\n"
                +"  \"hopitaux_experts\":[{\"nom\":\"...\",\"ville\":\"...\",\"pays\":\"...\",\"specialites\":[\"...\"],\"reputation\":\"...\",\"site_web\":\"...\"}]\n"
                +"}";
    }

    /* ════════════════════════════════════════════════════════
       HUGGINGFACE — CORRIGE
    ════════════════════════════════════════════════════════ */
    @FXML public void lancerHF() {
        String key = getKey("HF_KEY");
        if(key.isBlank()){ showError("HF_KEY manquante dans .env !"); return; }
        tabPane.getSelectionModel().select(tabHF);
        engineLabel.setText("HuggingFace BART Zero-Shot");
        setAllBtns(true); hfPanel.getChildren().clear();
        System.out.println("[HF] Cle : "+key.substring(0,Math.min(8,key.length()))+"...");

        // Barre de progression
        VBox progBox = new VBox(12); progBox.setStyle("-fx-padding:20;");
        Label titP = new Label("Classification medicale en cours...");
        titP.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#6a1b9a;");
        ProgressBar pb = new ProgressBar(0); pb.setMaxWidth(Double.MAX_VALUE); pb.setStyle("-fx-accent:#9c27b0;");
        Label lblE = new Label("Preparation..."); lblE.setStyle("-fx-font-size:11;-fx-text-fill:#666;");
        Label lblN = new Label("0 / 6"); lblN.setStyle("-fx-font-size:10;-fx-text-fill:#999;");
        progBox.getChildren().addAll(titP,pb,lblE,lblN);
        hfPanel.getChildren().add(progBox);
        setStatus("HuggingFace en cours...");

        String texte = textePatient();
        System.out.println("[HF] Texte : "+texte.substring(0,Math.min(80,texte.length())));
        Map<String,JsonNode> resultats = new LinkedHashMap<>();
        AtomicInteger etape = new AtomicInteger(0);

        CompletableFuture.supplyAsync(() -> {
            for(Map.Entry<String,String[]> entry : HF_DIMS.entrySet()) {
                String dim = entry.getKey();
                String[] labels = entry.getValue();
                int num = etape.incrementAndGet();
                Platform.runLater(() -> {
                    pb.setProgress((double)(num-1)/HF_DIMS.size());
                    lblE.setText("Analyse : "+dim+"...");
                    lblN.setText((num-1)+" / "+HF_DIMS.size());
                });
                System.out.println("[HF] "+num+"/6 : "+dim);
                JsonNode res = hfAvecFallback(texte,labels,dim,key);
                if(res!=null){ resultats.put(dim,res); System.out.println("[HF] ✓ "+dim); }
                else { System.err.println("[HF] ✗ "+dim+" → estimation locale"); resultats.put(dim+"_local",estimerLocal(dim)); }
                try{ Thread.sleep(1500); } catch(InterruptedException ignored){}
            }
            return resultats;
        }).thenAccept(res -> Platform.runLater(() -> {
            pb.setProgress(1.0); lblN.setText("6 / 6 ✓");
            setStatus("HuggingFace termine ✓");
            System.out.println("[HF] Fin. Cles : "+res.keySet());
            hfPanel.getChildren().clear();
            afficherResultatsHF(res); setAllBtns(false);
        })).exceptionally(ex -> { Platform.runLater(() -> {
            System.err.println("[HF FATAL] "+rootMsg(ex));
            hfPanel.getChildren().clear(); afficherHFEstimation(); setAllBtns(false);
        }); return null; });
    }

    private JsonNode hfAvecFallback(String texte, String[] labels, String dim, String key) {
        for(String model : new String[]{HF_MODEL_1,HF_MODEL_2,HF_MODEL_3}) {
            System.out.println("[HF] Essai : "+model+" / "+dim);
            try {
                JsonNode res = appelHF(texte,Arrays.asList(labels),model,key);
                if(res!=null&&res.has("labels")&&res.get("labels").size()>0){
                    System.out.println("[HF] ✓ "+model); return res;
                }
            } catch(Exception e) {
                String msg = e.getMessage()!=null?e.getMessage():"";
                System.err.println("[HF] ✗ "+model+" : "+msg);
                if(msg.contains("loading")||msg.contains("503")) {
                    System.out.println("[HF] Cold start, attente 22s...");
                    try{ Thread.sleep(22_000); } catch(InterruptedException ignored){}
                    try {
                        JsonNode retry = appelHF(texte,Arrays.asList(labels),model,key);
                        if(retry!=null&&retry.has("labels")){ System.out.println("[HF] ✓ retry"); return retry; }
                    } catch(Exception e2){ System.err.println("[HF] retry echec : "+e2.getMessage()); }
                } else if(msg.contains("429")) {
                    System.out.println("[HF] Rate limit, attente 15s...");
                    try{ Thread.sleep(15_000); } catch(InterruptedException ignored){}
                }
            }
        }
        return null;
    }

    private JsonNode appelHF(String texte, List<String> labels, String model, String key) throws Exception {
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("candidate_labels",labels); params.put("multi_label",false);
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("inputs",texte); body.put("parameters",params);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(HF_URL_BASE+model))
                .header("Authorization","Bearer "+key).header("Content-Type","application/json")
                .timeout(java.time.Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();

        HttpResponse<String> resp = http.send(req,HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        System.out.println("[HF] "+model+" → HTTP "+code);
        if(code==503){ System.out.println("[HF] 503: "+resp.body().substring(0,Math.min(100,resp.body().length()))); throw new RuntimeException("loading"); }
        if(code==429) throw new RuntimeException("429 rate limit");
        if(code==401) throw new RuntimeException("401 — HF_KEY invalide. Verifiez dans .env");
        if(code!=200) throw new RuntimeException("HTTP "+code+" : "+resp.body().substring(0,Math.min(150,resp.body().length())));
        System.out.println("[HF] OK (60c): "+resp.body().substring(0,Math.min(60,resp.body().length())));
        return MAPPER.readTree(resp.body());
    }

    private JsonNode estimerLocal(String dim) {
        System.out.println("[LOCAL] Estimation : "+dim);
        String mal = nvl(dossier.getMaladiesChroniques(),"").toLowerCase();
        String diag = nvl(dossier.getDiagnostics(),"").toLowerCase();
        boolean cardio = mal.contains("hypertension")||mal.contains("cardiaque")||diag.contains("hypertension");
        boolean diab   = mal.contains("diabet")||mal.contains("obesit")||diag.contains("diabet");
        boolean asthme = mal.contains("asthme")||mal.contains("respirat");
        boolean immu   = mal.contains("immun")||nvl(dossier.getTraitementsEnCours(),"").toLowerCase().contains("immunosuppresseur");

        String[] lbls; double[] scrs;
        switch(dim) {
            case "gravite" -> { lbls=new String[]{"critical condition","severe condition","moderate condition","mild condition","stable condition"};
                scrs=(cardio&&diab)?new double[]{.05,.25,.45,.15,.10}:(cardio||diab)?new double[]{.02,.10,.50,.25,.13}:new double[]{.01,.04,.20,.35,.40}; }
            case "cardio" -> { lbls=new String[]{"high cardiovascular risk","moderate cardiovascular risk","low cardiovascular risk"};
                scrs=cardio?new double[]{.65,.25,.10}:new double[]{.10,.30,.60}; }
            case "metabolique" -> { lbls=new String[]{"severe metabolic disorder","diabetes or obesity","metabolic syndrome","healthy metabolism"};
                scrs=diab?new double[]{.10,.60,.20,.10}:new double[]{.05,.10,.15,.70}; }
            case "respiratoire" -> { lbls=new String[]{"severe respiratory failure","breathing difficulty","normal breathing"};
                scrs=asthme?new double[]{.15,.55,.30}:new double[]{.05,.15,.80}; }
            case "urgence" -> { lbls=new String[]{"immediate hospitalization needed","urgent medical consultation","routine follow-up"};
                scrs=(cardio&&diab)?new double[]{.20,.50,.30}:(cardio||diab)?new double[]{.05,.40,.55}:new double[]{.02,.13,.85}; }
            default -> { lbls=new String[]{"immunodeficiency","moderate immune deficit","healthy immune system"};
                scrs=immu?new double[]{.50,.30,.20}:new double[]{.05,.15,.80}; }
        }
        ArrayNode la=MAPPER.createArrayNode(), sc=MAPPER.createArrayNode();
        for(String l:lbls) la.add(l); for(double s:scrs) sc.add(s);
        var obj=MAPPER.createObjectNode(); obj.set("labels",la); obj.set("scores",sc);
        obj.put("sequence",textePatient()); obj.put("_local",true);
        return obj;
    }

    private String textePatient() {
        return "Patient: antecedents="+nvl(dossier.getAntecedentsMedicaux(),"aucun")
                +". Maladies="+nvl(dossier.getMaladiesChroniques(),"aucune")
                +". Allergies="+nvl(dossier.getAllergies(),"aucune")
                +". Traitements="+nvl(dossier.getTraitementsEnCours(),"aucun")
                +". Diagnostics="+nvl(dossier.getDiagnostics(),"aucun")
                +". Objectif="+nvl(dossier.getObjectifSante(),"non defini")
                +". Activite="+nvl(dossier.getNiveauActivite(),"inconnu")+".";
    }

    /* ════════════════════════════════════════════════════════
       GROQ VISION
    ════════════════════════════════════════════════════════ */
    @FXML public void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png","*.bmp"));
        File f = fc.showOpenDialog((Stage)visionPanel.getScene().getWindow());
        if(f!=null){ imageMedicaleChoisie=f; System.out.println("[VISION] Image: "+f.getName());
            imageChoisieLabel.setText(f.getName()+" ("+f.length()/1024+" KB)");
            imageChoisieLabel.setStyle("-fx-text-fill:#2e7d32;-fx-font-size:11;-fx-font-weight:bold;"); }
    }

    @FXML public void lancerVision() {
        if(imageMedicaleChoisie==null){ showError("Importez une image d'abord."); return; }
        String key = getKey("GROQ_KEY");
        if(key.isBlank()){ showError("GROQ_KEY manquante !"); return; }
        tabPane.getSelectionModel().select(tabVision);
        engineLabel.setText("Groq Vision — Llama 4 Scout");
        setAllBtns(true); setStatus("Analyse image..."); visionPanel.getChildren().clear();
        afficherChargement(visionPanel,"Analyse image medicale...");
        final File img=imageMedicaleChoisie;
        CompletableFuture.supplyAsync(() -> { try{ return visionGroq(img,key); } catch(Exception e){throw new RuntimeException(e);} })
                .thenAccept(json -> Platform.runLater(() -> { setStatus("Vision ✓"); afficherVision(json,img); setAllBtns(false); }))
                .exceptionally(ex -> { Platform.runLater(() -> {
                    System.err.println("[VISION ERR] "+rootMsg(ex));
                    visionPanel.getChildren().clear(); afficherErreurAPI(visionPanel,"Groq Vision",rootMsg(ex));
                    showError("Vision: "+rootMsg(ex)); setAllBtns(false);
                }); return null; });
    }

    private JsonNode visionGroq(File img, String key) throws Exception {
        byte[] bytes = Files.readAllBytes(img.toPath());
        if(bytes.length>4_000_000) throw new RuntimeException("Image trop grande (max 4MB).");
        String b64=Base64.getEncoder().encodeToString(bytes);
        String mime=img.getName().toLowerCase().endsWith(".png")?"image/png":"image/jpeg";
        Map<String,Object> iurl=Map.of("url","data:"+mime+";base64,"+b64);
        Map<String,Object> ic=new LinkedHashMap<>(); ic.put("type","image_url"); ic.put("image_url",iurl);
        Map<String,Object> tc=new LinkedHashMap<>(); tc.put("type","text");
        tc.put("text","Analyse cette image medicale. Reponds UNIQUEMENT en JSON valide:\n"
                +"{\"type_image\":\"radiographie|irm|ecg|scanner|photo_clinique|autre\","
                +"\"region_anatomique\":\"...\",\"qualite_image\":\"bonne|moyenne|mauvaise\","
                +"\"anomalies_detectees\":[{\"description\":\"...\",\"localisation\":\"...\",\"severite\":\"legere|moderee|severe\"}],"
                +"\"elements_normaux\":[\"...\"],\"gravite_visuelle\":\"normale|legere|moderee|severe|critique\","
                +"\"recommandation_urgence\":false,\"description_detaillee\":\"...\","
                +"\"diagnostic_differentiel\":[\"...\"],\"examens_complementaires\":[\"...\"],"
                +"\"confiance_analyse\":85,\"conclusion\":\"...\"}");
        Map<String,Object> umsg=new LinkedHashMap<>(); umsg.put("role","user"); umsg.put("content",List.of(ic,tc));
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("model",GROQ_VISION_MODEL); body.put("max_tokens",2000); body.put("temperature",0.1);
        body.put("messages",List.of(umsg));
        HttpRequest req=HttpRequest.newBuilder().uri(URI.create(GROQ_URL))
                .header("Authorization","Bearer "+key).header("Content-Type","application/json")
                .timeout(java.time.Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> resp=http.send(req,HttpResponse.BodyHandlers.ofString());
        System.out.println("[VISION] HTTP "+resp.statusCode());
        if(resp.statusCode()==401) throw new RuntimeException("401 — Cle GROQ invalide");
        if(resp.statusCode()!=200) throw new RuntimeException("HTTP "+resp.statusCode());
        String c=MAPPER.readTree(resp.body()).at("/choices/0/message/content").asText();
        c=c.replaceAll("(?s)```json|```","").trim();
        int s=c.indexOf('{'),e=c.lastIndexOf('}'); if(s>=0&&e>s) c=c.substring(s,e+1);
        return MAPPER.readTree(c);
    }

    /* ════════════════════════════════════════════════════════
       GEMINI
    ════════════════════════════════════════════════════════ */
    @FXML public void lancerGemini() {
        String key=getKey("GEMINI_KEY");
        if(key.isBlank()){ showError("GEMINI_KEY manquante !"); return; }
        tabPane.getSelectionModel().select(tabGemini);
        engineLabel.setText("Google Gemini 1.5 Flash");
        setAllBtns(true); setStatus("Gemini en cours..."); geminiPanel.getChildren().clear();
        afficherChargement(geminiPanel,"Generation plan 30 jours...");
        System.out.println("[GEMINI] Cle : "+key.substring(0,Math.min(12,key.length()))+"...");
        CompletableFuture.supplyAsync(() -> { try{ return appelGemini(key); } catch(Exception e){throw new RuntimeException(e);} })
                .thenAccept(json -> Platform.runLater(() -> { setStatus("Gemini ✓"); afficherGemini(json); setAllBtns(false); }))
                .exceptionally(ex -> { Platform.runLater(() -> {
                    System.err.println("[GEMINI ERR] "+rootMsg(ex));
                    geminiPanel.getChildren().clear(); afficherPlanGeminiDemo();
                    showError("Gemini: "+rootMsg(ex)); setAllBtns(false);
                }); return null; });
    }

    private JsonNode appelGemini(String key) throws Exception {
        Map<String,Object> part=Map.of("text",promptGemini());
        Map<String,Object> content=new LinkedHashMap<>(); content.put("role","user"); content.put("parts",List.of(part));
        Map<String,Object> cfg=new LinkedHashMap<>();
        cfg.put("temperature",0.2); cfg.put("maxOutputTokens",4000); cfg.put("topP",0.8); cfg.put("topK",40);
        List<Map<String,Object>> safety=List.of(
                Map.of("category","HARM_CATEGORY_DANGEROUS_CONTENT","threshold","BLOCK_NONE"),
                Map.of("category","HARM_CATEGORY_HARASSMENT","threshold","BLOCK_NONE"),
                Map.of("category","HARM_CATEGORY_HATE_SPEECH","threshold","BLOCK_NONE"),
                Map.of("category","HARM_CATEGORY_SEXUALLY_EXPLICIT","threshold","BLOCK_NONE"));
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("contents",List.of(content)); body.put("generationConfig",cfg); body.put("safetySettings",safety);
        HttpRequest req=HttpRequest.newBuilder().uri(URI.create(GEMINI_URL+"?key="+key))
                .header("Content-Type","application/json").timeout(java.time.Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> resp=http.send(req,HttpResponse.BodyHandlers.ofString());
        System.out.println("[GEMINI] HTTP "+resp.statusCode());
        if(resp.statusCode()==400) throw new RuntimeException("400 — Cle Gemini invalide ou quota depasse");
        if(resp.statusCode()!=200) throw new RuntimeException("HTTP "+resp.statusCode());
        String text=MAPPER.readTree(resp.body()).at("/candidates/0/content/parts/0/text").asText();
        if(text.isBlank()) throw new RuntimeException("Reponse Gemini vide");
        text=text.replaceAll("(?s)```json|```","").trim();
        int s=text.indexOf('{'),e=text.lastIndexOf('}'); if(s>=0&&e>s) text=text.substring(s,e+1);
        return MAPPER.readTree(text);
    }

    private String promptGemini() {
        return "Tu es un nutritionniste expert.\nPatient:\n"
                +"- Maladies: "+nvl(dossier.getMaladiesChroniques(),"Aucune")+"\n"
                +"- Allergies: "+nvl(dossier.getAllergies(),"Aucune")+"\n"
                +"- Traitements: "+nvl(dossier.getTraitementsEnCours(),"Aucun")+"\n"
                +"- Activite: "+nvl(dossier.getNiveauActivite(),"Sedentaire")+"\n"
                +"- Objectif: "+nvl(dossier.getObjectifSante(),"Ameliorer la sante")+"\n\n"
                +"Genere plan 30 jours JSON valide UNIQUEMENT:\n"
                +"{\"resume_plan\":\"...\",\"calories_journalieres_recommandees\":2000,"
                +"\"plan_nutrition\":{\"aliments_recommandes\":[\"...\"],\"aliments_interdits\":[\"...\"],\"hydratation_litres_jour\":2.5},"
                +"\"plan_exercice\":{\"seances_par_semaine\":3,\"types_exercice\":[\"...\"],"
                +"\"programme\":[{\"jour\":\"Lundi\",\"exercice\":\"...\",\"duree_min\":45,\"calories_brulees\":300}]},"
                +"\"conseils_sommeil\":[\"...\"],\"gestion_stress\":[\"...\"],"
                +"\"supplements_vitamines\":[\"...\"],\"avertissements_medicaux\":[\"...\"]}";
    }

    /* ════════════════════════════════════════════════════════
       TTS VOCAL — CORRIGE POUR WINDOWS
    ════════════════════════════════════════════════════════ */
    @FXML public void lancerVoix() {
        tabPane.getSelectionModel().select(tabVoix);
        setAllBtns(true); voixPanel.getChildren().clear();
        String texte = texteVocal();
        Platform.runLater(() -> { setStatus("Interface vocale prete"); afficherVoix(texte); setAllBtns(false); });
    }

    private void afficherVoix(String texte) {
        voixPanel.getChildren().clear();
        String os = System.getProperty("os.name","").toLowerCase();
        String moteur = os.contains("win")?"Windows SAPI (VBScript)":os.contains("mac")?"macOS say":"Linux espeak";

        VBox hCard = card("#fce4ec","#c2185b");
        lbl(hCard,"Resume Vocal — Dossier #"+dossier.getId(),13,"#880e4f",true);
        lbl(hCard,"Moteur TTS : "+moteur,10,"#888",false);
        voixPanel.getChildren().add(hCard);

        VBox tCard = card("#f3e5f5","#6a1b9a");
        lbl(tCard,"Contenu du resume",12,"#4a148c",true);
        for(String p : texte.split("\\. ")) {
            if(p.isBlank()) continue;
            Label l=new Label("▶  "+p.trim()+"."); l.setStyle("-fx-font-size:12;-fx-text-fill:#333;-fx-padding:3 0;"); l.setWrapText(true);
            tCard.getChildren().add(l);
        }
        voixPanel.getChildren().add(tCard);

        VBox cCard = card("#e8eaf6","#283593");
        lbl(cCard,"Controles",12,"#1a237e",true);
        Label indic = new Label("Pret — cliquez Lire pour demarrer");
        indic.setStyle("-fx-font-size:11;-fx-text-fill:#555;-fx-padding:6 0;");
        Button btnLire=new Button("▶  Lire a voix haute"); btnLire.setStyle(btnSty("#1a237e","white"));
        Button btnStop=new Button("■  Arreter");           btnStop.setStyle(btnSty("#c62828","white")); btnStop.setDisable(true);
        Button btnCopy=new Button("⎘  Copier");            btnCopy.setStyle(btnSty("#2e7d32","white"));
        HBox controls=new HBox(12); controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(btnLire,btnStop,btnCopy);
        cCard.getChildren().addAll(controls,indic);
        voixPanel.getChildren().add(cCard);

        // ✅ FIX : lecture asynchrone + feedback correct
        btnLire.setOnAction(e -> {
            btnLire.setDisable(true); btnStop.setDisable(false);
            indic.setText("Lecture en cours...");
            indic.setStyle("-fx-font-size:11;-fx-text-fill:#1a237e;-fx-font-weight:bold;");
            System.out.println("[TTS] Demarrage...");
            CompletableFuture.runAsync(() -> {
                boolean ok = ttsAvecAttente(texte);
                System.out.println("[TTS] Fin. succes="+ok);
                Platform.runLater(() -> {
                    indic.setText(ok ? "Lecture terminee ✓" : "TTS non disponible — lisez le texte ci-dessus");
                    indic.setStyle("-fx-font-size:11;-fx-text-fill:"+(ok?"#2e7d32":"#c62828")+";");
                    btnLire.setDisable(false); btnStop.setDisable(true);
                });
            });
        });

        btnStop.setOnAction(e -> {
            if(ttsProcess!=null&&ttsProcess.isAlive()){ ttsProcess.destroyForcibly(); System.out.println("[TTS] Arrete"); }
            indic.setText("Lecture arretee.");
            indic.setStyle("-fx-font-size:11;-fx-text-fill:#c62828;");
            btnLire.setDisable(false); btnStop.setDisable(true);
        });

        btnCopy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc=new javafx.scene.input.ClipboardContent();
            cc.putString(texte); javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            btnCopy.setText("Copie !"); System.out.println("[TTS] Copie");
            PauseTransition pt=new PauseTransition(Duration.seconds(2));
            pt.setOnFinished(ev -> btnCopy.setText("⎘  Copier")); pt.play();
        });

        animFade(voixPanel);
    }

    /**
     * ✅ FIX TTS WINDOWS :
     * Fichier .vbs avec SAPI.SpVoice + waitFor() bloque jusqu'à la fin.
     * Avant : on utilisait powershell qui échoue souvent silencieusement.
     */
    private boolean ttsAvecAttente(String texte) {
        String os = System.getProperty("os.name","").toLowerCase();
        System.out.println("[TTS] OS : "+os);
        try {
            ProcessBuilder pb;
            if(os.contains("mac")) {
                System.out.println("[TTS] Utilisation 'say' macOS");
                pb = new ProcessBuilder("say","-v","Thomas","-r","150",texte);
            } else if(os.contains("linux")) {
                System.out.println("[TTS] Utilisation espeak Linux");
                pb = new ProcessBuilder("espeak","-v","fr","-s","140","-a","200",texte);
            } else {
                // ✅ Windows : VBScript + SAPI.SpVoice (natif, fiable)
                String nettoye = texte.replace("\""," ").replace("'"," ")
                        .replace("\n"," ").replace("\r"," ").replace("\\","");
                File vbs = File.createTempFile("tts_med_",".vbs");
                vbs.deleteOnExit();
                System.out.println("[TTS] VBS : "+vbs.getAbsolutePath());
                try(FileWriter fw=new FileWriter(vbs)) {
                    fw.write("On Error Resume Next\r\n");
                    fw.write("Dim sapi\r\n");
                    fw.write("Set sapi = CreateObject(\"SAPI.SpVoice\")\r\n");
                    fw.write("If Err.Number <> 0 Then\r\n");
                    fw.write("  WScript.Echo \"ERREUR: SAPI non disponible\"\r\n");
                    fw.write("  WScript.Quit 1\r\n");
                    fw.write("End If\r\n");
                    fw.write("sapi.Rate = -1\r\n");
                    fw.write("sapi.Volume = 100\r\n");
                    fw.write("sapi.Speak \""+nettoye+"\"\r\n");
                    fw.write("Set sapi = Nothing\r\n");
                    fw.write("WScript.Quit 0\r\n");
                }
                pb = new ProcessBuilder("cscript","//NoLogo",vbs.getAbsolutePath());
            }
            pb.redirectErrorStream(true);
            ttsProcess = pb.start();
            // Lire sortie pour debug
            try(BufferedReader br=new BufferedReader(new InputStreamReader(ttsProcess.getInputStream()))) {
                String l; while((l=br.readLine())!=null) System.out.println("[TTS-OUT] "+l);
            }
            int code = ttsProcess.waitFor();
            System.out.println("[TTS] Exit code : "+code);
            return code==0;
        } catch(Exception e) {
            System.err.println("[TTS] Exception : "+e.getClass().getSimpleName()+" — "+e.getMessage());
            return false;
        }
    }

    private String texteVocal() {
        StringBuilder sb = new StringBuilder();
        sb.append("Resume medical dossier numero ").append(dossier.getId()).append(". ");
        String m=nvl(dossier.getMaladiesChroniques(),null); if(m!=null) sb.append("Maladies: ").append(m).append(". ");
        String t=nvl(dossier.getTraitementsEnCours(),null); if(t!=null) sb.append("Traitements: ").append(t).append(". ");
        String a=nvl(dossier.getAllergies(),null);           if(a!=null) sb.append("Allergies: ").append(a).append(". ");
        String o=nvl(dossier.getObjectifSante(),null);      if(o!=null) sb.append("Objectif: ").append(o).append(". ");
        sb.append("Fin du resume."); return sb.toString();
    }

    /* ════════════════════════════════════════════════════════
       AFFICHAGE RÉSULTATS
    ════════════════════════════════════════════════════════ */
    private void afficherResultatsGroq(JsonNode j) {
        groqPanel.getChildren().clear();
        afficherScore(groqPanel,j.path("score_sante").asInt(70),j.path("gravite").asText("modere"),j.path("urgence").asBoolean(false));
        cardSection(groqPanel,"Analyse clinique",j.path("resume_cas").asText(""),"#4a148c","#f3e5f5");
        afficherScoresSys(groqPanel,j.path("score_details"));
        liste(groqPanel,"Recommandations",        j.path("recommandations"),           "#1565c0","#e3f2fd","○");
        liste(groqPanel,"Examens suggeres",        j.path("examens_suggeres"),          "#2e7d32","#e8f5e9",">");
        liste(groqPanel,"Facteurs de risque",      j.path("facteurs_risque"),           "#e65100","#fff3e0","!");
        liste(groqPanel,"Complications",           j.path("complications_potentielles"),"#c62828","#ffebee","!");
        afficherPlan(groqPanel,j.path("plan_suivi"));
        afficherMedecins(groqPanel,j.path("medecins_experts"));
        afficherHopitaux(groqPanel,j.path("hopitaux_experts"));
        animFade(groqPanel);
    }

    private void afficherResultatsHF(Map<String,JsonNode> results) {
        hfPanel.getChildren().clear();
        long nbAPI=results.keySet().stream().filter(k->!k.endsWith("_local")).count();
        long nbLoc=results.keySet().stream().filter(k->k.endsWith("_local")).count();
        VBox h=card("#e8eaf6","#283593");
        lbl(h,"Classification HuggingFace — Zero-Shot BART",13,"#1a237e",true);
        lbl(h,nbAPI+" via API + "+nbLoc+" estimation(s) locale(s)",11,"#5c6bc0",false);
        hfPanel.getChildren().add(h);

        VBox tb=card("#fafafa","#9e9e9e"); lbl(tb,"Texte analyse",11,"#555",true);
        Label tl=new Label(textePatient()); tl.setStyle("-fx-font-size:10;-fx-text-fill:#666;-fx-font-style:italic;"); tl.setWrapText(true);
        tb.getChildren().add(tl); hfPanel.getChildren().add(tb);

        if(nbLoc>0){ VBox av=card("#fff8e1","#f9a825"); lbl(av,"⚠ "+nbLoc+" estimation(s) locale(s) (API indisponible)",11,"#e65100",true); hfPanel.getChildren().add(av); }

        Map<String,String[]> cfg=new LinkedHashMap<>();
        cfg.put("gravite",     new String[]{"Gravite Globale",        "#c62828","#ffebee"});
        cfg.put("cardio",      new String[]{"Risque Cardiovasculaire","#d32f2f","#fce4ec"});
        cfg.put("metabolique", new String[]{"Etat Metabolique",       "#f57f17","#fff8e1"});
        cfg.put("respiratoire",new String[]{"Fonction Respiratoire",  "#1565c0","#e3f2fd"});
        cfg.put("urgence",     new String[]{"Urgence Medicale",       "#e65100","#fff3e0"});
        cfg.put("immunite",    new String[]{"Etat Immunitaire",       "#2e7d32","#e8f5e9"});

        for(Map.Entry<String,String[]> dim : cfg.entrySet()) {
            String k=dim.getKey(); String[] c=dim.getValue();
            boolean local=!results.containsKey(k)&&results.containsKey(k+"_local");
            JsonNode data=results.getOrDefault(k,results.get(k+"_local"));
            if(data!=null) afficherDimHF(hfPanel,c[0],data,c[1],c[2],local);
            else { VBox e=card("#f5f5f5","#9e9e9e"); lbl(e,"✗ "+c[0]+" — Non disponible",11,"#757575",false); hfPanel.getChildren().add(e); }
        }
        VBox sy=card("#e8f5e9","#2e7d32");
        lbl(sy,"Synthese",12,"#1b5e20",true);
        lbl(sy,"Classification terminee. Consultez un medecin pour confirmer.",11,"#444",false);
        hfPanel.getChildren().add(sy);
        animFade(hfPanel);
    }

    private void afficherDimHF(VBox parent, String titre, JsonNode data,
                               String color, String bg, boolean isLocal) {
        VBox box=card(bg,color);
        HBox tr=new HBox(8); tr.setAlignment(Pos.CENTER_LEFT);
        Label tl=new Label(titre); tl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:"+color+";"); tr.getChildren().add(tl);
        if(isLocal){ Label b=new Label("Estimation"); b.setStyle("-fx-background-color:#fff3e0;-fx-text-fill:#e65100;-fx-padding:2 8;-fx-background-radius:10;-fx-font-size:9;"); tr.getChildren().add(b); }
        box.getChildren().add(tr);
        JsonNode labels=data.path("labels"), scores=data.path("scores");
        if(!labels.isArray()||labels.size()==0){ lbl(box,"Donnees non disponibles",10,"#999",false); parent.getChildren().add(box); return; }
        for(int i=0;i<labels.size();i++) {
            String lEn=labels.get(i).asText(), lFr=FR.getOrDefault(lEn.toLowerCase(),lEn);
            double scr=(i<scores.size())?scores.get(i).asDouble():0.0;
            int pct=(int)(scr*100); boolean top=(i==0); String c=top?color:"#9e9e9e";
            HBox row=new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:"+(top?color+"15":"white")+";-fx-background-radius:8;-fx-padding:8 10;"+(top?"-fx-border-color:"+color+"44;-fx-border-radius:8;":""));
            Label rk=new Label(String.valueOf(i+1)); rk.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:"+c+";-fx-min-width:18;");
            Label lb=new Label(lFr); lb.setStyle("-fx-font-size:11;-fx-text-fill:#333;"+(top?"-fx-font-weight:bold;":"")); lb.setMinWidth(185); lb.setWrapText(true);
            HBox bg2=new HBox(); bg2.setStyle("-fx-background-color:#e0e0e0;-fx-background-radius:6;"); bg2.setPrefHeight(10); bg2.setPrefWidth(150);
            HBox fill=new HBox(); fill.setPrefHeight(10); fill.setPrefWidth(0); fill.setStyle("-fx-background-color:"+c+";-fx-background-radius:6;");
            bg2.getChildren().add(fill);
            new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(fill.prefWidthProperty(),0)),
                    new KeyFrame(Duration.millis(700+i*60),new KeyValue(fill.prefWidthProperty(),150.0*scr))).play();
            Label pl=new Label(pct+"%"); pl.setStyle("-fx-font-size:12;"+(top?"-fx-font-weight:bold;":"")+"fx-text-fill:"+c+";-fx-min-width:38;");
            row.getChildren().addAll(rk,lb,bg2,pl); box.getChildren().add(row);
        }
        parent.getChildren().add(box);
    }

    private void afficherHFEstimation() {
        hfPanel.getChildren().clear();
        VBox al=card("#fff8e1","#f9a825"); lbl(al,"⚠ API HuggingFace indisponible — Estimation complete",12,"#e65100",true); hfPanel.getChildren().add(al);
        Map<String,JsonNode> est=new LinkedHashMap<>();
        for(String k:HF_DIMS.keySet()) est.put(k+"_local",estimerLocal(k));
        afficherResultatsHF(est);
    }

    private void afficherVision(JsonNode j, File img) {
        visionPanel.getChildren().clear();
        VBox h=card("#fff3e0","#e65100"); lbl(h,"Analyse Image — Groq Vision",13,"#bf360c",true); lbl(h,"Fichier: "+img.getName()+" ("+img.length()/1024+" KB)",10,"#666",false); visionPanel.getChildren().add(h);
        String grav=j.path("gravite_visuelle").asText("normale");
        String gc=switch(grav.toLowerCase()){ case "severe","critique"->"#c62828"; case "moderee"->"#e65100"; case "legere"->"#f57f17"; default->"#2e7d32"; };
        VBox gc2=card(gc+"11",gc); lbl(gc2,"Gravite: "+grav.toUpperCase(),13,gc,true);
        if(j.path("recommandation_urgence").asBoolean(false)) lbl(gc2,"CONSULTATION URGENTE RECOMMANDEE",12,"#c62828",true);
        visionPanel.getChildren().add(gc2);
        cardSection(visionPanel,"Description",j.path("description_detaillee").asText(""),"#4a148c","#f3e5f5");
        liste(visionPanel,"Anomalies",anomaliesToArr(j.path("anomalies_detectees")),"#c62828","#ffebee","!");
        liste(visionPanel,"Diagnostic differentiel",j.path("diagnostic_differentiel"),"#1565c0","#e3f2fd",">");
        liste(visionPanel,"Examens complementaires",j.path("examens_complementaires"),"#6a1b9a","#f3e5f5",">");
        cardSection(visionPanel,"Conclusion",j.path("conclusion").asText(""),"#2e7d32","#e8f5e9");
        animFade(visionPanel);
    }

    private JsonNode anomaliesToArr(JsonNode an) {
        ArrayNode arr=MAPPER.createArrayNode();
        if(an!=null&&an.isArray()) for(JsonNode a:an) arr.add(a.path("description").asText("")+" — "+a.path("localisation").asText(""));
        return arr;
    }

    private void afficherGemini(JsonNode j) {
        geminiPanel.getChildren().clear();
        VBox h=card("#e8f5e9","#2e7d32"); lbl(h,"Plan 30 Jours — Google Gemini 1.5 Flash",13,"#1b5e20",true);
        String res=j.path("resume_plan").asText(""); if(!res.isBlank()) lbl(h,res,11,"#333",false);
        int cal=j.path("calories_journalieres_recommandees").asInt(0); if(cal>0) lbl(h,cal+" kcal/jour",12,"#2e7d32",true);
        geminiPanel.getChildren().add(h);
        JsonNode nut=j.path("plan_nutrition");
        if(!nut.isMissingNode()){ VBox nc=card("#fff8e1","#f9a825"); lbl(nc,"Plan Nutrition",12,"#e65100",true);
            listeInline(nc,"Recommandes",nut.path("aliments_recommandes"),"#2e7d32"); listeInline(nc,"Interdits",nut.path("aliments_interdits"),"#c62828");
            lbl(nc,"Hydratation: "+nut.path("hydratation_litres_jour").asDouble(2)+" L/jour",11,"#1565c0",true); geminiPanel.getChildren().add(nc); }
        JsonNode ex=j.path("plan_exercice");
        if(!ex.isMissingNode()){ VBox ec=card("#e3f2fd","#1565c0"); lbl(ec,"Plan Exercice",12,"#0d47a1",true);
            lbl(ec,ex.path("seances_par_semaine").asInt(3)+" seances/semaine",11,"#333",false);
            listeInline(ec,"Exercices",ex.path("types_exercice"),"#2e7d32");
            JsonNode prog=ex.path("programme");
            if(prog.isArray()&&prog.size()>0){ lbl(ec,"Programme hebdomadaire:",11,"#1565c0",true);
                for(JsonNode jj:prog){ HBox r=new HBox(10); r.setStyle("-fx-background-color:white;-fx-background-radius:6;-fx-padding:6 10;");
                    Label jo=new Label(jj.path("jour").asText("")); jo.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#1565c0;-fx-min-width:65;");
                    Label exx=new Label(jj.path("exercice").asText("")); exx.setStyle("-fx-font-size:10;-fx-text-fill:#333;"); HBox.setHgrow(exx,Priority.ALWAYS);
                    Label du=new Label(jj.path("duree_min").asInt(0)+" min"); du.setStyle("-fx-font-size:10;-fx-text-fill:#888;");
                    r.getChildren().addAll(jo,exx,du); ec.getChildren().add(r); } }
            geminiPanel.getChildren().add(ec); }
        liste(geminiPanel,"Conseils Sommeil",       j.path("conseils_sommeil"),       "#37474f","#eceff1","•");
        liste(geminiPanel,"Gestion du Stress",      j.path("gestion_stress"),         "#7b1fa2","#f3e5f5","•");
        liste(geminiPanel,"Supplements et Vitamines",j.path("supplements_vitamines"),  "#1565c0","#e3f2fd",">");
        liste(geminiPanel,"Avertissements Medicaux",j.path("avertissements_medicaux"),"#c62828","#ffebee","!");
        animFade(geminiPanel);
    }

    private void afficherPlanGeminiDemo() {
        geminiPanel.getChildren().clear();
        VBox al=card("#fff8e1","#f9a825"); lbl(al,"Google Gemini indisponible — Plan general",12,"#e65100",true); geminiPanel.getChildren().add(al);
        String[][] c={{"Nutrition","Privilegiez legumes, fruits, proteines maigres.","#2e7d32","#e8f5e9"},
                {"Exercice","30 min marche rapide 5x/semaine.","#1565c0","#e3f2fd"},
                {"Sommeil","7-8h par nuit.","#37474f","#eceff1"},{"Stress","Meditation 10min/jour.","#7b1fa2","#f3e5f5"}};
        for(String[] cs:c){ VBox b=card(cs[3],cs[2]); lbl(b,cs[0],12,cs[2],true); lbl(b,cs[1],11,"#333",false); geminiPanel.getChildren().add(b); }
    }

    /* ── Widgets ── */
    private void afficherScore(VBox p, int score, String grav, boolean urg) {
        String c=score>=75?"#2e7d32":score>=50?"#f57f17":"#c62828";
        VBox box=card("#fff8e1","#f9a825"); HBox r=new HBox(24); r.setAlignment(Pos.CENTER_LEFT);
        VBox sb=new VBox(4); sb.setAlignment(Pos.CENTER);
        Label sl=new Label(score+"/100"); sl.setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:"+c+";");
        Label st=new Label("Score de Sante"); st.setStyle("-fx-font-size:10;-fx-text-fill:#888;"); sb.getChildren().addAll(st,sl);
        VBox inf=new VBox(6); inf.setAlignment(Pos.CENTER_LEFT);
        Label gl=new Label("Gravite: "+grav.toUpperCase()); gl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:"+c+";"); inf.getChildren().add(gl);
        if(urg){ Label ul=new Label("URGENCE — Consultation immediate"); ul.setStyle("-fx-font-size:11;-fx-text-fill:#c62828;-fx-font-weight:bold;-fx-background-color:#ffebee;-fx-padding:5 12;-fx-background-radius:8;"); inf.getChildren().add(ul); }
        r.getChildren().addAll(sb,inf); box.getChildren().add(r); p.getChildren().add(box);
    }

    private void afficherScoresSys(VBox parent, JsonNode scores) {
        if(scores.isMissingNode()) return;
        VBox box=card("#f3e5f5","#7b1fa2"); lbl(box,"Scores par Systemes",12,"#4a148c",true);
        HBox grid=new HBox(10); grid.setAlignment(Pos.CENTER_LEFT);
        for(String[] s:new String[][]{{"cardiovasculaire","Cardio"},{"metabolique","Metabolo"},{"respiratoire","Respi"},{"renal","Renal"},{"hepatique","Hepato"},{"neurologique","Neuro"}})
            grid.getChildren().add(scoreCell(s[1],scores.path(s[0]).asInt(75)));
        box.getChildren().add(grid); parent.getChildren().add(box);
    }

    private VBox scoreCell(String label, int val) {
        VBox c=new VBox(5); c.setAlignment(Pos.CENTER); c.setStyle("-fx-background-color:white;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10;"); c.setPrefWidth(100);
        String col=val>=75?"#2e7d32":val>=50?"#f57f17":"#c62828";
        Label nm=new Label(label); nm.setStyle("-fx-font-size:10;-fx-text-fill:#666;");
        Label vl=new Label(val+"/100"); vl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:"+col+";");
        HBox bg=new HBox(); bg.setStyle("-fx-background-color:#eee;-fx-background-radius:5;"); bg.setPrefHeight(8); bg.setPrefWidth(80);
        HBox fill=new HBox(); fill.setPrefHeight(8); fill.setPrefWidth(0); fill.setStyle("-fx-background-color:"+col+";-fx-background-radius:5;");
        bg.getChildren().add(fill); c.getChildren().addAll(nm,vl,bg);
        new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(fill.prefWidthProperty(),0)),
                new KeyFrame(Duration.millis(900),new KeyValue(fill.prefWidthProperty(),80.0*val/100))).play();
        return c;
    }

    private void afficherPlan(VBox parent, JsonNode plan) {
        if(plan.isMissingNode()) return;
        VBox box=card("#e3f2fd","#1565c0"); lbl(box,"Plan de Suivi Medical",12,"#0d47a1",true);
        for(String[] p:new String[][]{{"immediat","Immediat","#c62828"},{"court_terme","Court terme","#e65100"},{"moyen_terme","Moyen terme","#1565c0"},{"long_terme","Long terme","#2e7d32"}}) {
            String v=plan.path(p[0]).asText(""); if(v.isBlank()) continue;
            HBox r=new HBox(12); r.setAlignment(Pos.TOP_LEFT); r.setStyle("-fx-background-color:white;-fx-background-radius:7;-fx-padding:9;");
            Label lb=new Label(p[1]); lb.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:"+p[2]+";-fx-min-width:90;");
            Label vl=new Label(v); vl.setStyle("-fx-font-size:11;-fx-text-fill:#333;"); vl.setWrapText(true); HBox.setHgrow(vl,Priority.ALWAYS);
            r.getChildren().addAll(lb,vl); box.getChildren().add(r);
        }
        parent.getChildren().add(box);
    }

    private void afficherMedecins(VBox parent, JsonNode med) {
        if(med==null||med.isMissingNode()||!med.isArray()||med.isEmpty()) return;
        VBox box=card("#e8eaf6","#283593"); lbl(box,"Medecins Experts Recommandes",12,"#1a237e",true);
        for(JsonNode m:med){ VBox c=new VBox(4); c.setStyle("-fx-background-color:white;-fx-border-color:#c5cae9;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12;");
            lbl(c,m.path("nom").asText("Medecin"),12,"#1a237e",true);
            lbl(c,m.path("specialite").asText("")+" | "+m.path("pays").asText(""),10,"#666",false);
            lbl(c,m.path("institution").asText(""),11,"#333",false);
            String em=m.path("email").asText(""); if(!em.isBlank()) lbl(c,"✉ "+em,10,"#1565c0",false);
            box.getChildren().add(c); }
        parent.getChildren().add(box);
    }

    private void afficherHopitaux(VBox parent, JsonNode hop) {
        if(hop==null||hop.isMissingNode()||!hop.isArray()||hop.isEmpty()) return;
        VBox box=card("#f9fbe7","#558b2f"); lbl(box,"Hopitaux d'Excellence Recommandes",12,"#33691e",true);
        for(JsonNode h:hop){ VBox c=new VBox(4); c.setStyle("-fx-background-color:white;-fx-border-color:#c5e1a5;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12;");
            lbl(c,h.path("nom").asText("Hopital"),12,"#33691e",true);
            lbl(c,h.path("ville").asText("")+", "+h.path("pays").asText(""),10,"#666",false);
            lbl(c,h.path("reputation").asText(""),11,"#558b2f",true);
            String w=h.path("site_web").asText(""); if(!w.isBlank()) lbl(c,"🌐 "+w,10,"#1565c0",false);
            box.getChildren().add(c); }
        parent.getChildren().add(box);
    }

    private void liste(VBox parent, String titre, JsonNode items, String color, String bg, String bullet) {
        if(items==null||items.isMissingNode()||!items.isArray()||items.isEmpty()) return;
        VBox box=card(bg,color); lbl(box,titre,12,color,true);
        for(JsonNode item:items){ Label l=new Label(bullet+"  "+item.asText()); l.setStyle("-fx-font-size:11;-fx-text-fill:#333;"); l.setWrapText(true); box.getChildren().add(l); }
        parent.getChildren().add(box);
    }

    private void listeInline(VBox parent, String titre, JsonNode items, String color) {
        if(items==null||items.isMissingNode()||!items.isArray()||items.isEmpty()) return;
        StringBuilder sb=new StringBuilder(titre+": ");
        for(int i=0;i<items.size();i++){ if(i>0) sb.append(" • "); sb.append(items.get(i).asText()); }
        Label l=new Label(sb.toString()); l.setStyle("-fx-font-size:11;-fx-text-fill:"+color+";"); l.setWrapText(true); parent.getChildren().add(l);
    }

    private void cardSection(VBox parent, String titre, String contenu, String color, String bg) {
        if(contenu==null||contenu.isBlank()) return;
        VBox box=card(bg,color); lbl(box,titre,12,color,true);
        Label c=new Label(contenu); c.setStyle("-fx-font-size:11;-fx-text-fill:#333;"); c.setWrapText(true); box.getChildren().add(c);
        parent.getChildren().add(box);
    }

    private void afficherErreurAPI(VBox parent, String api, String err) {
        VBox box=card("#ffebee","#c62828");
        lbl(box,"⚠ Erreur — "+api,13,"#c62828",true);
        lbl(box,err,11,"#555",false);
        lbl(box,"Verifiez votre fichier .env et vos cles API.",10,"#888",false);
        parent.getChildren().add(box);
    }

    private void afficherChargement(VBox parent, String msg) {
        VBox box=new VBox(16); box.setAlignment(Pos.CENTER); box.setStyle("-fx-padding:70 0;");
        ProgressIndicator pi=new ProgressIndicator(); pi.setPrefSize(48,48); pi.setStyle("-fx-accent:#9c27b0;");
        Label l=new Label(msg); l.setStyle("-fx-font-size:12;-fx-text-fill:#9c27b0;"); l.setWrapText(true); l.setAlignment(Pos.CENTER);
        box.getChildren().addAll(pi,l); parent.getChildren().add(box);
    }

    private void afficherPlaceholder(VBox parent, String msg) {
        VBox box=new VBox(10); box.setAlignment(Pos.CENTER); box.setStyle("-fx-padding:50 0;");
        Label l=new Label(msg); l.setStyle("-fx-font-size:12;-fx-text-fill:#bbb;"); l.setWrapText(true); l.setAlignment(Pos.CENTER);
        box.getChildren().add(l); parent.getChildren().add(box);
    }

    private VBox card(String bg, String border) {
        VBox v=new VBox(8);
        v.setStyle("-fx-background-color:"+bg+";-fx-border-color:"+border+"22;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");
        return v;
    }

    private void lbl(VBox parent, String text, int size, String color, boolean bold) {
        Label l=new Label(text);
        l.setStyle("-fx-font-size:"+size+";-fx-text-fill:"+color+";"+(bold?"-fx-font-weight:bold;":""));
        l.setWrapText(true); parent.getChildren().add(l);
    }

    private String btnSty(String bg, String fg) {
        return "-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-background-radius:8;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:9 18;-fx-cursor:hand;-fx-border-color:transparent;";
    }

    private void animFade(VBox parent) {
        int d=0;
        for(var child:parent.getChildren()){ child.setOpacity(0);
            FadeTransition ft=new FadeTransition(Duration.millis(280),child); ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(d)); ft.play(); d+=70; }
    }

    /* ── Navigation ── */
    @FXML public void retour() {
        if(ttsProcess!=null&&ttsProcess.isAlive()) ttsProcess.destroyForcibly();
        ((Stage)groqPanel.getScene().getWindow()).close();
    }

    private void setAllBtns(boolean d) {
        for(Button b:List.of(btnGroq,btnHF,btnVision,btnGemini,btnVoix)) if(b!=null) b.setDisable(d);
    }

    private void setStatus(String msg) { if(statusLabel!=null) statusLabel.setText(msg); System.out.println("[STATUS] "+msg); }

    private void showError(String msg) {
        if(errorLabel==null) return;
        errorLabel.setStyle("-fx-text-fill:#c62828;"); errorLabel.setText(msg);
        System.err.println("[ERROR-UI] "+msg);
        PauseTransition p=new PauseTransition(Duration.seconds(8)); p.setOnFinished(e->errorLabel.setText("")); p.play();
    }

    private String rootMsg(Throwable ex) {
        Throwable c=ex; while(c.getCause()!=null) c=c.getCause(); return c.getMessage()!=null?c.getMessage():ex.getClass().getSimpleName();
    }

    private String nvl(String s, String def) { return(s!=null&&!s.isBlank())?s:def; }
}