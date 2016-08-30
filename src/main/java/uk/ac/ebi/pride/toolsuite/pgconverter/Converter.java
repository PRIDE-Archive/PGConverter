package uk.ac.ebi.pride.toolsuite.pgconverter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzTabControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.utilities.data.exporters.AbstractMzTabConverter;
import uk.ac.ebi.pride.utilities.data.exporters.MzIdentMLMzTabConverter;
import uk.ac.ebi.pride.utilities.data.exporters.MzTabBedConverter;
import uk.ac.ebi.pride.utilities.data.exporters.PRIDEMzTabConverter;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.pride.toolsuite.pgconverter.utils.Utility.*;

/**
 * Created by tobias on 03/08/2016.
 */
public class Converter {

    private static final Logger log = LoggerFactory.getLogger(Converter.class);

    public static void startConversion(CommandLine cmd) throws IOException {
        log.info("Starting conversion...");
        File inputFile;
        String inputFileType = null;
        if (cmd.hasOption(ARG_INPUTFILE)) {
            inputFile = new File(cmd.getOptionValue(ARG_INPUTFILE));
        } else {
            inputFile = cmd.hasOption(ARG_MZID)? new File(cmd.getOptionValue(ARG_MZID))
                    : cmd.hasOption(ARG_PRIDEXML) ? new File(cmd.getOptionValue(ARG_PRIDEXML))
                    : cmd.hasOption(ARG_MZTAB) ? new File(cmd.getOptionValue(ARG_MZTAB))
                    : null;
        }
        if (inputFile==null || inputFile.isDirectory()) {
            log.error("Unable to convert whole directory.");
        } else {
            inputFileType = FilenameUtils.getExtension(inputFile.getAbsolutePath()).toLowerCase();
            if (inputFileType.equals("xml")) {
                inputFileType = FileType.PRIDEXML.toString();
            }
        }
        File outputFile = null;
        String outputFormat = null;
        if (cmd.hasOption(ARG_OUTPUTFILE)) {
            outputFile  = new File(cmd.getOptionValue(ARG_OUTPUTFILE));
            outputFormat = FilenameUtils.getExtension(outputFile.getAbsolutePath()).toLowerCase();
        } else if (cmd.hasOption(ARG_OUTPUTTFORMAT)) {
            outputFormat =  cmd.getOptionValue(ARG_OUTPUTTFORMAT).toLowerCase();
            if (inputFile != null) {
                outputFile = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + outputFormat);
            }
        } else {
            log.error("No output file or output format specified.");
        }
        if (inputFile!=null && outputFile!=null) {
            switch (inputFileType != null ? inputFileType : "") {
                case ARG_MZID:
                case ARG_PRIDEXML:
                    if (outputFormat.equals(ARG_MZTAB)) {
                        convertToMztab(inputFile, outputFile, inputFileType);
                    } else {
                        if ((inputFileType != null && inputFileType.equals(ARG_MZID)) && outputFormat.equals(ARG_PROBED)) {
                            File intermediateMztab = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + FileType.MZTAB.toString().toLowerCase());
                            convertToMztab(inputFile, intermediateMztab, inputFileType);
                            startMztabToProbed(intermediateMztab, outputFile, cmd);
                        } else {
                            if ((inputFileType != null && inputFileType.equals(ARG_MZID)) && outputFormat.equals(ARG_BIGBED)) {
                                File intermediateMztab = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + FileType.MZTAB.toString().toLowerCase());
                                convertToMztab(inputFile, intermediateMztab, inputFileType);
                                File intermediateProbed = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + FileType.PROBED.toString().toLowerCase());
                                startMztabToProbed(intermediateMztab, intermediateProbed, cmd);
                                startProbedToBigbed(intermediateProbed, cmd);
                            } else {
                                log.error("Unable to convert input mzid/pride xml file into the target output format.");
                            }
                        }
                    }
                    break;
                case ARG_MZTAB:
                    switch (outputFormat) {
                        case ARG_PROBED:
                            startMztabToProbed(inputFile, outputFile, cmd);
                            break;
                        case ARG_BIGBED:
                            File intermediateProbed = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + FileType.PROBED.toString().toLowerCase());
                            startMztabToProbed(inputFile, intermediateProbed, cmd);
                            startProbedToBigbed(intermediateProbed, cmd);
                            break;
                        default:
                            log.error("Unable to convert input mztab into the target output format.");
                            break;
                    }
                    break;
                case ARG_PROBED:
                    if (outputFormat.equals(ARG_BIGBED)) {
                        startProbedToBigbed(inputFile, cmd);
                    }  else {
                        log.error("Unable to convert input probed into the target output format.");
                    }
                    break;
            }
        } else {
            log.error("No output file or format defined.");
        }
    }

    private static void startMztabToProbed(File inputFile, File outputFile, CommandLine cmd) throws IOException {
        convertMztabToProbed(inputFile, outputFile);
        if (cmd.hasOption(ARG_CHROMSIZES)) {
            log.info("Sorting and filtering proBed file according to chrom sizes file: " + cmd.getOptionValue(ARG_CHROMSIZES));
            try {
                MzTabBedConverter.sortProBed(outputFile, new File(cmd.getOptionValue(ARG_CHROMSIZES)));
            } catch (InterruptedException ie) {
                log.error("Interrupted Exception: ", ie);
                throw new IOException(ie);
            }
        }
    }

    private static void startProbedToBigbed (File inputFile, CommandLine cmd) throws IOException {
        File aSQL = null;
        File chromSizes = null;
        File bigBedConverter = null;
        if (cmd.hasOption(ARG_ASQLFILE)) {
            aSQL = new File(cmd.getOptionValue(ARG_ASQLFILE));
        } else if (cmd.hasOption(ARG_ASQLNAME)) {
            aSQL = new File(FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "." + FileType.ASQL.toString().toLowerCase());
            MzTabBedConverter.createAsql(cmd.getOptionValue(ARG_ASQLNAME), aSQL.getAbsolutePath());
        }
        if (cmd.hasOption(ARG_CHROMSIZES)) {
            chromSizes =  new File(cmd.getOptionValue(ARG_CHROMSIZES));
        }
        if (cmd.hasOption(ARG_BIGBEDCONVERTER)) {
            bigBedConverter =  new File(cmd.getOptionValue(ARG_BIGBEDCONVERTER));
        }
        if (aSQL!=null && chromSizes!=null && bigBedConverter!=null) {
            convertProbedToBigbed(inputFile, aSQL, chromSizes, bigBedConverter);
        }
    }

    private static void convertToMztab(File inputFile, File outputMztabFile, String inputFormat) throws IOException{
        log.info("About to convert input file: " + inputFile.getAbsolutePath() + " to: " + outputMztabFile.getAbsolutePath());
        List<File> filesToConvert = new ArrayList<>();
        filesToConvert.add(inputFile);
        filesToConvert = Validator.extractZipFiles(filesToConvert);
        filesToConvert.forEach(file -> {
            try {
                AbstractMzTabConverter mzTabconverter = null;
                if (inputFormat.equals(FileType.MZID.toString())) {
                    MzIdentMLControllerImpl mzIdentMLController = new MzIdentMLControllerImpl(inputFile);
                    mzTabconverter = new MzIdentMLMzTabConverter(mzIdentMLController);
                } else if (inputFormat.equals(FileType.PRIDEXML.toString())) {
                    PrideXmlControllerImpl prideXmlController = new PrideXmlControllerImpl(inputFile);
                    mzTabconverter = new PRIDEMzTabConverter(prideXmlController);
                }
                if (mzTabconverter != null) {
                    MZTabFile mzTabFile = mzTabconverter.getMZTabFile();
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

    private static void convertMztabToProbed(File inputFile, File outputFile) throws IOException{
        try {
            log.info("Converting to bed: " + inputFile.getAbsolutePath());
            MzTabControllerImpl mzTabController = new MzTabControllerImpl(inputFile);
            MzTabBedConverter mzTabBedConverter = new MzTabBedConverter(mzTabController);
            log.info("New proBed file path: " + outputFile.getAbsolutePath());
            boolean createdDirs = outputFile.getParentFile().mkdirs();
            boolean cratedNewFile = outputFile.createNewFile();
            if (createdDirs && cratedNewFile) {
                log.info("Crated new dir and file");
            } else {
                log.error("Unable to create new dir and/or file");
            }
            mzTabBedConverter.convert(outputFile);
            mzTabController.close();
            log.info("Finished processing " + outputFile.getAbsolutePath());
            File mzTabDirectory = inputFile.getParentFile();
            File[] files = mzTabDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains("pride.mztaberrors.out")) {
                        boolean deleted = file.delete();
                        log.info("Deleted file: " + deleted);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception when converting mztab to probed: ", e);
            throw new IOException(e);
        }

    }

    private static void convertProbedToBigbed(File probed, File aSQL, File chromSizes, File bigBedConverter) throws IOException {
        try {
            File outputBigBed = MzTabBedConverter.convertProBedToBigBed(
                    aSQL,
                    "bed12+13",
                    probed,
                    chromSizes,
                    bigBedConverter
            );
            log.info("Generated output bigBed file:" + outputBigBed);
        } catch (IOException|URISyntaxException|InterruptedException e) {
            log.error("Error when converting to bigBed: ", e);
        }
    }

    private static void createAsql(String name, File aSQL) throws IOException{
    }

}
