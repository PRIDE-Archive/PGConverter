package uk.ac.ebi.pride.toolsuite.prideadvisor.controller;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by tobias on 08/08/2016.
 */
public class ScreensFramework extends Application {

    public static final String MAIN_SCREEN = "main";
    public static final String MAIN_SCREEN_FXML = "/fxml/advisor.fxml";
    public static final String VALIDATOR_SCREEN = "validator";
    public static final String VALIDATE_FXML = "/fxml/validate.fxml";
    public static final String VALIDATOR_REPPORT_SCREEN = "report";
    public static final String VALIDATE_REPORT_FXML = "/fxml/report.fxml";


    @Override
    public void start(Stage primaryStage) throws Exception {
        ScreensController mainContainer = new ScreensController();
        mainContainer.loadScreen(ScreensFramework.MAIN_SCREEN, ScreensFramework.MAIN_SCREEN_FXML);
        mainContainer.loadScreen(ScreensFramework.VALIDATOR_SCREEN, ScreensFramework.VALIDATE_FXML);
        mainContainer.loadScreen(ScreensFramework.VALIDATOR_REPPORT_SCREEN, ScreensFramework.VALIDATE_REPORT_FXML);
        mainContainer.setScreen(ScreensFramework.MAIN_SCREEN);
        Group root = new Group();
        root.getChildren().addAll(mainContainer);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
