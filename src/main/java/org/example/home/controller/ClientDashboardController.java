package org.example.home.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.user.model.User;
import org.example.utils.SessionManager;
import org.example.cycle.service.CycleService;
import org.example.cycle.service.CycleAnalysisService;
import org.example.wellbeing.service.WellbeingService;
import org.example.wellbeing.model.UserWellBeingData;
import org.example.wellbeing.service.MealService;
import org.example.wellbeing.model.Meal;
import org.example.servicesociaux.frontoffice.controller.services.RendezVousService;
import org.example.servicesociaux.frontoffice.controller.entities.RendezVous;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ClientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label lblNextPeriod;
    @FXML private Label lblStressScore;
    @FXML private Label lblNextAppointment;
    @FXML private LineChart<String, Number> stressChart;
    @FXML private VBox activityContainer;
    @FXML private VBox cycleStatCard;
    @FXML private Button btnQuickCycle;

    private HomeController homeController;
    private final CycleService cycleService = new CycleService();
    private final CycleAnalysisService cycleAnalysisService = new CycleAnalysisService();
    private final WellbeingService wellbeingService = new WellbeingService();
    private final MealService mealService = new MealService();
    private final RendezVousService rdvService = new RendezVousService();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Bonjour, " + currentUser.getPrenom() + " !");
            
            // Hide cycle related UI for male users
            if (currentUser.getSexe() != null && (currentUser.getSexe().equalsIgnoreCase("Homme") || currentUser.getSexe().equalsIgnoreCase("Male"))) {
                if (cycleStatCard != null) {
                    cycleStatCard.setVisible(false);
                    cycleStatCard.setManaged(false);
                }
                if (btnQuickCycle != null) {
                    btnQuickCycle.setVisible(false);
                    btnQuickCycle.setManaged(false);
                }
            }
            
            loadDashboardData(currentUser);
        }
        
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
    }

    private void loadDashboardData(User user) {
        try {
            int userId = user.getId();
            boolean isFemale = user.getSexe() == null || (!user.getSexe().equalsIgnoreCase("Homme") && !user.getSexe().equalsIgnoreCase("Male"));

            // 1. Cycle Data (Only for females)
            if (isFemale) {
                List<org.example.cycle.model.Cycle> cycles = cycleService.getCyclesByUserId(userId);
                LocalDate nextPeriod = cycleAnalysisService.predictNextPeriod(cycles);
                if (nextPeriod != null) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextPeriod);
                    lblNextPeriod.setText("Dans " + daysLeft + " jours");
                } else {
                    lblNextPeriod.setText("Non défini");
                }
            }

            // 2. Wellbeing Data (Stress)
            UserWellBeingData latestWellbeing = wellbeingService.getLatestByUserId(userId);
            if (latestWellbeing != null) {
                double score = (latestWellbeing.getAnxietyTension() + latestWellbeing.getSleepProblems() + 
                               latestWellbeing.getHeadaches() + latestWellbeing.getRestlessness()) / 20.0 * 100.0;
                lblStressScore.setText((int)score + "%");
            } else {
                lblStressScore.setText("--");
            }

            // 3. Next Appointment
            RendezVous nextRdv = rdvService.getProchainByUserId(userId);
            if (nextRdv != null) {
                lblNextAppointment.setText(new java.text.SimpleDateFormat("dd/MM").format(nextRdv.getDateRendezVous()));
            } else {
                lblNextAppointment.setText("Aucun");
            }

            // 4. Stress Chart
            loadStressChart(userId);
            
            // 5. Recent Activities
            loadRecentActivities(user);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStressChart(int userId) {
        try {
            List<UserWellBeingData> history = wellbeingService.getByUserId(userId);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Niveau de Stress");

            for (int i = Math.min(history.size() - 1, 6); i >= 0; i--) {
                UserWellBeingData data = history.get(i);
                double score = (data.getAnxietyTension() + data.getSleepProblems() + 
                               data.getHeadaches() + data.getRestlessness()) / 20.0 * 100.0;
                String date = data.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM"));
                series.getData().add(new XYChart.Data<>(date, score));
            }

            stressChart.getData().clear();
            stressChart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentActivities(User user) throws java.sql.SQLException {
        activityContainer.getChildren().clear();
        int userId = user.getId();
        boolean isFemale = user.getSexe() == null || (!user.getSexe().equalsIgnoreCase("Homme") && !user.getSexe().equalsIgnoreCase("Male"));
        
        // Fetch Meals
        List<Meal> meals = mealService.getByUserId(userId);
        int count = 0;
        for (Meal meal : meals) {
            if (count++ >= 2) break;
            addActivityItem("🥗", "Repas : " + meal.getDescription(), 
                meal.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        }
        
        // Fetch last cycle (Only for females)
        if (isFemale) {
            List<org.example.cycle.model.Cycle> cycles = cycleService.getCyclesByUserId(userId);
            if (!cycles.isEmpty()) {
                org.example.cycle.model.Cycle last = cycles.get(cycles.size()-1);
                addActivityItem("🩸", "Cycle mis à jour", 
                    last.getDate_debut_m().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM")));
            }
        }
    }

    private void addActivityItem(String icon, String title, String time) {
        HBox item = new HBox();
        item.setSpacing(12);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.getStyleClass().add("activity-item-pro");
        item.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        StackPane iconBg = new StackPane();
        iconBg.setPrefSize(35, 35);
        iconBg.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 10;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");
        iconBg.getChildren().add(iconLbl);

        VBox texts = new VBox();
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("activity-title-pro");
        Label timeLbl = new Label(time);
        timeLbl.getStyleClass().add("activity-time-pro");
        texts.getChildren().addAll(titleLbl, timeLbl);

        item.getChildren().addAll(iconBg, texts);
        activityContainer.getChildren().add(item);
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void goToWellbeing() {
        if (homeController != null) invokePrivateMethod(homeController, "showWellbeing");
    }

    @FXML
    private void goToMedicalServices() {
        if (homeController != null) invokePrivateMethod(homeController, "showServicesSociaux");
    }

    @FXML
    private void goToCycle() {
        if (homeController != null) invokePrivateMethod(homeController, "showCycle");
    }

    @FXML
    private void goToAideDon() {
        if (homeController != null) invokePrivateMethod(homeController, "showAideEtdon");
    }

    private void invokePrivateMethod(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
