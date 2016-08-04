package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.validation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileConverter;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.utilities.data.exporters.AbstractMzTabConverter;
import uk.ac.ebi.pride.utilities.data.exporters.MzIdentMLMzTabConverter;
import uk.ac.ebi.pride.utilities.data.exporters.PRIDEMzTabConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Utility.*;
import static uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.validation.Validator.extractZipFiles;

/**
 * Created by tobias on 03/08/2016.
 */
public class Convertor {

    private static final Logger log = LoggerFactory.getLogger(Convertor.class);

    public static void startConversion(CommandLine cmd) throws IOException {
        if (cmd.hasOption(ARG_MZID) || cmd.hasOption(ARG_PRIDEXML)) {
            File inputFile = cmd.hasOption(ARG_MZID)? new File(cmd.getOptionValue(ARG_MZID))
                      : cmd.hasOption(ARG_PRIDEXML) ? new File(cmd.getOptionValue(ARG_PRIDEXML))
                      : null;
            if (inputFile==null || inputFile.isDirectory()) {
                log.error("Unable to convert whole directory.");
            } else {
                File outputFile = null;
                String outputFormat = null;
                if (cmd.hasOption(ARG_OUTPUTFILE)) {
                    outputFile  = new File(cmd.getOptionValue(ARG_OUTPUTFILE));
                    outputFormat = FilenameUtils.getExtension(outputFile.getAbsolutePath());
                } else if (cmd.hasOption(ARG_OUTPUTTFORMAT)) {
                    outputFormat =  cmd.getOptionValue(ARG_OUTPUTTFORMAT);
                    outputFile = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + outputFormat);
                } else {
                    log.error("No output file or output format specified.");
                }
                if (outputFile!=null) {
                    if (outputFormat.equals(ARG_MZTAB)) {
                        convertToMztab(inputFile, outputFile, cmd.hasOption(ARG_MZID)? FileType.MZID : cmd.hasOption(ARG_PRIDEXML) ? FileType.PRIDEXML : null);
                    } else if (cmd.hasOption(ARG_MZID) && outputFormat.equals(ARG_PROBED)) {
                        // convert to probed
                    } else if (cmd.hasOption(ARG_MZID) && outputFormat.equals(ARG_BIGBED)) {
                        // convert to probed
                        // convert to bigbed
                    }
                } else {
                    log.error("No valid output format to convert input file to.");
                }
            }
        } else if (cmd.hasOption(ARG_PROBED)) {
            // convert to bigbed
        }
        exit();
    }

    private static void convertToMztab(File inputFile, File outputMztabFile, FileType inputFormat) throws IOException{
        log.info("About to convert input file: " + inputFile.getAbsolutePath() + " to: " + outputMztabFile.getAbsolutePath());
        log.info("Input file format is: " + inputFormat);
        List<File> filesToConvert = new ArrayList<File>();
        filesToConvert.add(inputFile);
        filesToConvert = extractZipFiles(filesToConvert);
        filesToConvert.stream().forEach(file -> {
            try {
                AbstractMzTabConverter mzTabconverter = null;
                if (inputFormat.equals(FileType.MZID)) {
                    MzIdentMLControllerImpl mzIdentMLController = new MzIdentMLControllerImpl(inputFile);
                    mzTabconverter = new MzIdentMLMzTabConverter(mzIdentMLController);
                } else if (inputFormat.equals(FileType.PRIDEXML)) {
                    PrideXmlControllerImpl prideXmlController = new PrideXmlControllerImpl(inputFile);
                    mzTabconverter = new PRIDEMzTabConverter(prideXmlController);
                }
                if (mzTabconverter != null) {
                    MZTabFile mzTabFile = mzTabconverter.getMZTabFile();
                    MZTabFileConverter checker = new MZTabFileConverter();
                    checker.check(mzTabFile);
                    BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outputMztabFile));
                    mzTabFile.printMZTab(writer);
                    writer.close();
                    log.info("Successfully written to mzTab file: " + outputMztabFile.getAbsolutePath());
                } else {
                    throw new IOException("Unable to parse input file format correctly");
                }
            } catch (IOException ioe) {
                log.error("IOException: ", ioe);
            }
        });
    }

}
