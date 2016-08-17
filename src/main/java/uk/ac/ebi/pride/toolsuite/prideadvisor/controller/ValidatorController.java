package uk.ac.ebi.pride.toolsuite.prideadvisor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Utility;

import java.io.File;
import java.net.URL;
import java.util.*;

public class ValidatorController implements ControlledScreen, Initializable {
    private static final Logger log = LoggerFactory.getLogger(ValidatorController.class);

    @FXML private Button fileChooser;
    @FXML private Button peakChooser;
    @FXML private Button validate;
    @FXML private Label inputFileLabel;
    @FXML private Label peakFilesLabel;
    @FXML private Button home;


    ScreensController myController;
    File inputFile;
    List<File> peakFiles;


    @FXML protected void locateFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chose a file");
        Node node = (Node) event.getSource();
        inputFile = chooser.showOpenDialog(node.getScene().getWindow());
        if (inputFile!=null) {
            inputFileLabel.setText(inputFile.getAbsolutePath());
            peakFiles = null;
        }
        if (FilenameUtils.getExtension(inputFile.getAbsolutePath()).toLowerCase().equals(Utility.FileType.MZID.toString())) {
            peakChooser.setVisible(true);
        } else {
            peakChooser.setVisible(false);
            validate.setDisable(false);
        }
    }

    @FXML protected void locatePeakFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MGF files (*.mgf)", "*.mgf");
        chooser.getExtensionFilters().add(extFilter);
        chooser.setTitle("Choose a peak file");
        if (inputFile!=null) {
            chooser.setInitialDirectory(inputFile.getParentFile());
        }
        Node node = (Node) event.getSource();
        peakFiles = chooser.showOpenMultipleDialog(node.getScene().getWindow());
        if (peakFiles!=null) {
            peakFilesLabel.setText(StringUtils.join(peakFiles, ", "));
            validate.setDisable(false);
        }
    }


    @Override
    public void setScreenParent(ScreensController screenPage) {
        myController = screenPage;
    }

    @FXML
    private void goToMain(ActionEvent event){
        myController.setScreen(ScreensFramework.MAIN_SCREEN);
    }

    @FXML
    private void validateFile(ActionEvent event){
        List<File> filesToPass = new ArrayList<>();
        filesToPass.add(inputFile);
        filesToPass.addAll(peakFiles);
        myController.setUserData(filesToPass);
        myController.setScreen(ScreensFramework.VALIDATOR_REPPORT_SCREEN);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        validate.setDisable(true);
        peakChooser.setVisible(false);
    }
}
