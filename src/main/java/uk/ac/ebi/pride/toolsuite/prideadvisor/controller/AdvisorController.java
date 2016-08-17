package uk.ac.ebi.pride.toolsuite.prideadvisor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AdvisorController implements ControlledScreen, Initializable {
    private static final Logger log = LoggerFactory.getLogger(AdvisorController.class);

    @FXML private Button setupValidate;
    @FXML private Button setupConvert;

    ScreensController myController;

    @FXML protected void setScreenValidate(ActionEvent event) {
        myController.setScreen(ScreensFramework.VALIDATOR_SCREEN);
    }

    @FXML protected void setScreenConvert(ActionEvent event) {
        //todo
    }


    @Override
    public void setScreenParent(ScreensController screenPage) {
        myController = screenPage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
