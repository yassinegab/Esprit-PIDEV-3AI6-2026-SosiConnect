package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class MainFX extends Application {
    private static MainFX instance;

    public MainFX() {
        instance = this;
    }

    public static MainFX getInstance() {
        return instance;
    }

    public void openUrl(String url) {
        getHostServices().showDocument(url);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("SOSI Project - User Module");
        stage.setScene(scene);
        stage.setTitle("SOSI+ Healthcare - Admin");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0); // Assure la fermeture de Spring Boot / Tomcat
    }

    public static void main(String[] args) {
        // Load environment variables into system properties for Spring Boot configuration
        io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .systemProperties()
                .load();

        // Démarrer Spring Boot en arrière-plan
        org.springframework.context.ApplicationContext context = org.springframework.boot.SpringApplication.run(org.example.SosiApplication.class, args);
        org.example.SosiApplication.setContext(context);
        // Lancer l'interface JavaFX
        launch(args);
    }
}