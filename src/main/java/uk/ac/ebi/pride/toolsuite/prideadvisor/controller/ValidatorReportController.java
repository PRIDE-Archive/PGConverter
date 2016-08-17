package uk.ac.ebi.pride.toolsuite.prideadvisor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ValidatorReportController implements ControlledScreen, Initializable {
    private static final Logger log = LoggerFactory.getLogger(ValidatorReportController.class);
/*

    @FXML private Button home;
    @FXML private Button back;
   // @FXML private Button save;

    @FXML private Label messageLabel;



    File inputFile;
*/
    ScreensController myController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startValidation((List<File>) myController.getUserData());
    }

    @Override
    public void setScreenParent(ScreensController screenPage) {
        myController = screenPage;
    }

    private void startValidation(List<File> inputFiles) {
        //messageLabel.setText("Set message label text");
    }/*



    @FXML
    private void goToMain(ActionEvent event){
        myController.setScreen(ScreensFramework.MAIN_SCREEN);
    }

    @FXML
    private void goToValidate(ActionEvent event){
        myController.setScreen(ScreensFramework.VALIDATOR_SCREEN);
    }
*/
/*    @FXML
    private void saveReport(ActionEvent event){
        //todo
    }*/


}
