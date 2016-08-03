package javafx.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.*;
import uk.ac.ebi.pride.utilities.data.core.*;
import uk.ac.ebi.pride.utilities.mol.MoleculeUtilities;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    enum FileType {MZID, MZTAB, PRIDEXML, UNKNOWN};

    public static void main(String[] args) throws Exception {
        log.info("Starting application...");
        if (args.length > 0) {
            log.info("Program arguments: " + Arrays.toString(args));
            CommandLine cmd = MainApp.parseArgs(args);
            if (cmd.hasOption("v")) {
                if (cmd.hasOption("mzid")) {
                    validateMzdentML(cmd);
                } else if (cmd.hasOption("pridexml")) {
                    validatePrideXML(cmd);
                } else if (cmd.hasOption("mztab")) {
                    //validateMzTab(cmd);
                }
                exit();
            } else {
                launch(args);
            }
        }
    }

    private static void validateMzdentML(CommandLine cmd) throws IOException{
        List<File> filesToValidate = new ArrayList<File>();
        List<File> peakFiles = new ArrayList<>();
        File file = new File(cmd.getOptionValue("mzid"));
        if (file.isDirectory()) {
            log.error("Unable to validate against directory of mzid files.");
        } else {
            filesToValidate.add(file);
        }
        filesToValidate = extractZipFiles(filesToValidate);
        if (cmd.hasOption("peak")) {
            File peakFile = new File(cmd.getOptionValue("peak"));
            if (file.isDirectory()) {
                peakFiles.addAll(Arrays.asList(peakFile.listFiles(File::isFile)));
            } else {
                peakFiles.add(peakFile);
            }
            peakFiles = extractZipFiles(peakFiles);
        } else {
            log.error("Peak file not supplied with mzIdentML file.");
        }
        List<Report> reports = new ArrayList<>();
        FileType fileType = getFileType(filesToValidate.get(0));
        if (fileType.equals(FileType.MZID)) {
            reports.add(validateMzidFile(filesToValidate.get(0), peakFiles));
        } else {
            log.error("Supplied -mzid file is not a valid mzIdentML file: " + filesToValidate.get(0));
        }
        outputReports(reports);
    }

    private static void validatePrideXML(CommandLine cmd) throws IOException{
        List<File> filesToValidate = new ArrayList<File>();
        List<File> peakFiles = new ArrayList<>();
        File file = new File(cmd.getOptionValue("pridexml"));
        if (file.isDirectory()) {
            filesToValidate.addAll(Arrays.asList(file.listFiles(File::isFile)));
        } else {
            filesToValidate.add(file);
        }
        filesToValidate = extractZipFiles(filesToValidate);
        List<Report> reports = new ArrayList<>();
        filesToValidate.parallelStream().forEach(file1 -> {
            try {
                FileType fileType = getFileType(file1);
                if (fileType.equals(FileType.PRIDEXML)) {
                    reports.add(validatePrideXMLFile(file1));
                } else {
                    log.error("Supplied -pridexml file is not a valid PRIDE XML file: " + file1.getAbsolutePath());
                }
            } catch (IOException ioe) {
                log.error("IOException: ", ioe);
            }
        });
        outputReports(reports);
    }

    private static void exit() {
        log.debug("Exiting.");
        System.exit(1);
    }

    private static void validateFiles(List<File> inputFiles) {
        List<Report> reports = new ArrayList<>();
        inputFiles.stream().forEach(file -> {
            try {
                log.info("Validating file: " + file.getAbsolutePath());
                FileType fileType = getFileType(file);
                if (fileType.equals(FileType.MZID)) {

                } else if (fileType.equals(FileType.PRIDEXML)) {

                } else if (fileType.equals(FileType.MZTAB)) {

                }
                log.info("Finished validating file: " + file.getAbsolutePath());
            } catch (IOException ioe) {
                log.error("IOException: ", ioe);
                exit();
            }
        });
        outputReports(reports);
    }

    private static void outputReports(List<Report> reports) {
        reports.parallelStream().forEach(report -> log.info(report.toString()));
    }

    private static List<File> extractZipFiles(List<File> files) throws IOException {
        List<File> zippedFiles = findZippedFiles(files);
        if (zippedFiles.size()>0) {
            files.removeAll(zippedFiles);
            files.addAll(unzipFiles(zippedFiles, zippedFiles.get(0).getParentFile().getAbsoluteFile()));
        }
        return files.stream().distinct().collect(Collectors.toList());
    }



    private static Report validateMzidFile(File file, List<File> dataAccessControllerFiles) {
        Report result = new Report();
        MzIdentMLControllerImpl mzIdentMLController = new MzIdentMLControllerImpl(file, true);
        mzIdentMLController.addMSController(dataAccessControllerFiles);
        Set<String> uniquePeptides = new HashSet<>();
        Set<CvParam> ptms = new HashSet<>();
        final int NUMBER_OF_CHECKS=100;
        List<Boolean> randomChecks = new ArrayList<>();
        IntStream.range(1,NUMBER_OF_CHECKS).parallel().forEach(i -> randomChecks.add( mzIdentMLController.checkRandomSpectraByDeltaMassThreshold(1, 4.0)));
        boolean passedRandomCheck = true;
        int checkFalseCounts = 0;
        for (Boolean check : randomChecks) {
            if (!check) {
                checkFalseCounts++;
            }
        }
        result.setDeltaMzPercent(Double.valueOf(Math.round(((double) checkFalseCounts / NUMBER_OF_CHECKS)*100.0)/100.0).intValue());
        result.setFileName(file.getAbsolutePath());
        result.setIdentifiedSpectra(mzIdentMLController.getNumberOfIdentifiedSpectra());
        result.setTotalPeptides(mzIdentMLController.getNumberOfPeptides());
        result.setTotalProteins(mzIdentMLController.getNumberOfProteins());
        result.setMissingIdSpectra(mzIdentMLController.getNumberOfMissingSpectra());
        result.setTotalSpecra(mzIdentMLController.getNumberOfSpectra());

        if (mzIdentMLController.getNumberOfMissingSpectra()<1) {
            mzIdentMLController.getProteinIds().parallelStream().forEach(proteinId ->
                    mzIdentMLController.getProteinById(proteinId).getPeptides().parallelStream().forEach(peptide-> {
                        uniquePeptides.add(peptide.getSequence());
                        peptide.getModifications().parallelStream().forEach(modification ->
                                modification.getCvParams().parallelStream().forEach(cvParam -> {
                                    if (cvParam.getCvLookupID() == null) {
                                        log.error("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                                        throw new NullPointerException("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                                    }
                                    if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                                        ptms.add(cvParam);
                                    }
                                }));
                    }));
            List<Boolean> matches = new ArrayList<>();
            matches.add(true);
            IntStream.range(1, (mzIdentMLController.getNumberOfPeptides()<100 ? mzIdentMLController.getNumberOfPeptides() : 100)).parallel().forEach(i -> {
                Peptide peptide = mzIdentMLController.getProteinById(mzIdentMLController.getProteinIds().stream().findAny().get()).getPeptides().stream().findAny().get();
                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                    if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
                        matches.add(false);
                    }
                }
            });
            result.setUniquePTMs(ptms.size());
            result.setMatchFragIons(matches.size() <= 1);
            result.setUniquePeptides(uniquePeptides.size());
        } else {
            log.error("Missing spectra are present");
        }
        return result;
    }

    private static Report validatePrideXMLFile(File file) {
        log.info("Validating PRIDE XML now: " + file.getAbsolutePath());
        Report result = new Report();
        PrideXmlControllerImpl prideXmlController = new PrideXmlControllerImpl(file);
        Set<String> uniquePeptides = new HashSet<>();
        Set<CvParam> ptms = new HashSet<>();

        result.setFileName(file.getAbsolutePath());
        result.setIdentifiedSpectra(prideXmlController.getNumberOfIdentifiedSpectra());
        result.setTotalPeptides(prideXmlController.getNumberOfPeptides());
        result.setTotalProteins(prideXmlController.getNumberOfProteins());
        result.setTotalSpecra(prideXmlController.getNumberOfSpectra());

        Double errorPSMCount = 0.0;
        Set<Comparable> allIdentifiedSpectrumIds = new HashSet<>();
        Set<Comparable> existingIdentifiedSpectrumIds = new HashSet<>();
        if (result.getMissingIdSpectra()<1) {
            for (Comparable proteinId :  prideXmlController.getProteinIds()) {
                for (Peptide peptide :  prideXmlController.getProteinById(proteinId).getPeptides()) {
                    uniquePeptides.add(peptide.getSequence());
                    List<Double> ptmMasses = new ArrayList<>();
                    peptide.getModifications().parallelStream().forEach(modification ->
                            modification.getCvParams().parallelStream().forEach(cvParam -> {
                                if (cvParam.getCvLookupID() == null) {
                                    log.error("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                                    throw new NullPointerException("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                                }
                                if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                                    ptms.add(cvParam);
                                }
                                List<Double> monoMasses = modification.getMonoisotopicMassDelta();
                                if (monoMasses != null && !monoMasses.isEmpty()) {
                                    ptmMasses.add((monoMasses.get(0)==null ? 0.0 : monoMasses.get(0)));
                                }
                            }));
                    Integer charge = prideXmlController.getPeptidePrecursorCharge(proteinId, peptide.getId());
                    double mz = prideXmlController.getPeptidePrecursorMz(proteinId, peptide.getId());
                    Comparable specId = prideXmlController.getPeptideSpectrumId(proteinId, peptide.getId());
                    if ((charge == null || mz == -1) && specId != null) {
                        charge = prideXmlController.getSpectrumPrecursorCharge(specId);
                        mz = prideXmlController.getSpectrumPrecursorMz(specId);
                        if (charge != null && charge == 0) {
                            charge = null;
                        }
                        if (charge == null) {
                            errorPSMCount++;
                        } else {
                            Double deltaMass = MoleculeUtilities.calculateDeltaMz(peptide.getSequence(), mz, charge, ptmMasses);
                            if (deltaMass == null || (deltaMass >= -4.0) || (deltaMass <= 4.0)) {
                                errorPSMCount++;
                            }
                        }
                    }
                    if (specId != null) {
                        allIdentifiedSpectrumIds.add(specId);
                    }
                    Spectrum spectrum = prideXmlController.getSpectrumById(specId);
                    if (spectrum != null) {
                        existingIdentifiedSpectrumIds.add(spectrum.getId());
                    }

                }
            }
            List<Boolean> matches = new ArrayList<>();
            matches.add(true);
            IntStream.range(1, (prideXmlController.getNumberOfPeptides()<100 ? prideXmlController.getNumberOfPeptides() : 100)).parallel().forEach(i -> {
                Peptide peptide = prideXmlController.getProteinById(prideXmlController.getProteinIds().stream().findAny().get()).getPeptides().stream().findAny().get();
                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                    if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
                        matches.add(false);
                    }
                }
            });
            result.setUniquePTMs(ptms.size());
            result.setMatchFragIons(matches.size() <= 1);
            result.setUniquePeptides(uniquePeptides.size());
            result.setTotalSpecra(allIdentifiedSpectrumIds.size());
            result.setIdentifiedSpectra(allIdentifiedSpectrumIds.size());
            allIdentifiedSpectrumIds.removeAll(existingIdentifiedSpectrumIds);
            result.setMissingIdSpectra(allIdentifiedSpectrumIds.size());
            result.setDeltaMzPercent(Double.valueOf((Math.round( errorPSMCount / (double)result.getTotalPeptides())*100.0)/100.0).intValue());
        } else {
            log.error("Missing spectra are present");
        }
        return result;
    }

    public void start(Stage stage) throws Exception {

        log.info("Starting Hello JavaFX and Maven demonstration application");

        String fxmlFile = "/fxml/hello.fxml";
        log.debug("Loading FXML for main view from: {}", fxmlFile);
        FXMLLoader loader = new FXMLLoader();
        Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));

        log.debug("Showing JFX scene");
        Scene scene = new Scene(rootNode, 400, 200);
        scene.getStylesheets().add("/styles/styles.css");

        stage.setTitle("Hello JavaFX and Maven");
        stage.setScene(scene);
        stage.show();
    }

    private static CommandLine parseArgs(String[] args) throws ParseException{
        Options options = new Options();
        options.addOption("v", false, "start to validate a file");
        options.addOption("mzid", true, "input mzid file");
        options.addOption("peak", true, "input peak file/directory");
        options.addOption("pridexml", true, "input pride xml file/directory");
        options.addOption("mztab", true, "input mztab file/directory");
        CommandLineParser parser = new DefaultParser();
        return parser.parse( options, args);
    }

    private static FileType getFileType(File file) throws IOException {
        FileType result = null;
        log.info("Checking file type for : " + file);
        if (PrideXmlControllerImpl.isValidFormat(file)) {
            result = FileType.PRIDEXML;
        } else if (MzIdentMLControllerImpl.isValidFormat(file)) {
            result = FileType.MZID;
        } else if (MzTabControllerImpl.isValidFormat(file)) {
            result = FileType.MZTAB;
        } else {
            log.error("Unrecognised file type: " + file);
            result = FileType.UNKNOWN;
            //exit();
        }
        return result;
    }

    private static List<File> findZippedFiles(List<File> files) {
        List<File> zippedFiles = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".gz")) {
                zippedFiles.add(file);
            }
        }
        return zippedFiles;
    }

    private static List<File> unzipFiles(List<File> zippedFiles, File outputFolder) throws IOException {
        List<File> unzippedFiles = new ArrayList<>();
        zippedFiles.parallelStream().forEach(inputFile -> {
            try {
                log.info("Unzipping file: " + inputFile.getAbsolutePath());
                FileInputStream fis = null;
                GZIPInputStream gs = null;
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    fis = new FileInputStream(inputFile);
                    gs = new GZIPInputStream(fis);
                    String outputFile = outputFolder + File.separator + inputFile.getName().replace(".gz", "");
                    fos = new FileOutputStream(outputFile);
                    bos = new BufferedOutputStream(fos, 2048);
                    byte data[] = new byte[2048];
                    int count;
                    while ((count = gs.read(data, 0, 2048)) != -1) {
                        bos.write(data, 0, count);
                    }
                    bos.flush();
                    bos.close();
                    unzippedFiles.add(new File(outputFile));
                    log.info("Unzipped file: " + outputFile);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                    if (gs != null) {
                        gs.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                }
            } catch (IOException ioe) {
                log.error("IOException when unzipping files.", ioe);
                exit();
            }
        });
        return unzippedFiles;
    }

    private static boolean matchingFragmentIons(List<FragmentIon> fragmentIons, Spectrum spectrum) {
        double[][] massIntensityMap = spectrum.getMassIntensityMap();


        for (FragmentIon fragmentIon : fragmentIons) {
            double intensity = fragmentIon.getIntensity();
            double mz = fragmentIon.getMz();
            boolean matched = false;

            for (double[] massIntensity : massIntensityMap) {
                if (massIntensity[0] == mz && massIntensity[1] == intensity) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                return false;
            }
        }

        return true;
    }

}
