package uk.ac.ebi.pride.toolsuite.prideadvisor;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.validation.Convertor;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.validation.Validator;
import java.util.*;

import static uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Utility.*;

public class MainApp  {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting application...");
        if (args.length > 0) {
            log.info("Program arguments: " + Arrays.toString(args));
            CommandLine cmd = MainApp.parseArgs(args);
            if (cmd.hasOption(ARG_VALIDATION)) {
                Validator.startValidation(cmd);
            } else if (cmd.hasOption(ARG_CONVERSION)) {
                Convertor.startConversion(cmd);
            }  /*else {
                launch(args);
            }*/
        }  /*else {
            launch(args);
        }*/
    }

/*    public void start(Stage stage) throws Exception {

        log.info("Starting PRIDE-advisor JavaFX application");
        ScreensFramework framework = new ScreensFramework();
        framework.start(stage);
        *//*String fxmlFile = "/fxml/advisor.fxml";
        log.debug("Loading FXML for main view from: {}", fxmlFile);
        FXMLLoader loader = new FXMLLoader();
        Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));

        log.debug("Showing JFX scene");
        Scene scene = new Scene(rootNode, 800, 600);
        scene.getStylesheets().add("/styles/styles.css");

        stage.setTitle("PRIDE-advisor");
        stage.setScene(scene);
        stage.show();*//*
    }*/

    private static CommandLine parseArgs(String[] args) throws ParseException{
        Options options = new Options();
        options.addOption(ARG_VALIDATION, false, "start to validate a file");
        options.addOption(ARG_CONVERSION, false, "start to convert a file");
        options.addOption(ARG_MZID, true, "mzid file");
        options.addOption(ARG_PEAK, true, "peak file");
        options.addOption(ARG_PEAKS, true, "peak files");
        options.addOption(ARG_PRIDEXML, true, "pride xml file");
        options.addOption(ARG_MZTAB, true, "mztab file");
        options.addOption(ARG_PROBED, true, "probed file");
        options.addOption(ARG_OUTPUTFILE, true, "exact output file");
        options.addOption(ARG_OUTPUTTFORMAT, true, "exact output file format");
        options.addOption(ARG_INPUTFILE, true, "exact input file");
        options.addOption(ARG_CHROMSIZES, true, "chrom sizes file");
        options.addOption(ARG_REPORTFILE, true, "report file");
        CommandLineParser parser = new DefaultParser();
        return parser.parse( options, args);
    }
}
