package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.validation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.repo.assay.instrument.AnalyzerInstrumentComponent;
import uk.ac.ebi.pride.archive.repo.assay.instrument.DetectorInstrumentComponent;
import uk.ac.ebi.pride.archive.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.archive.repo.assay.instrument.SourceInstrumentComponent;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Report;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.AssayFileSummary;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.DataConversionUtil;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.PeakFileSummary;
import uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Utility;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzTabControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.utilities.data.core.*;
import uk.ac.ebi.pride.utilities.mol.MoleculeUtilities;
import uk.ac.ebi.pride.utilities.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import static uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils.Utility.*;

/**
 * Created by tobias on 03/08/2016.
 */
public class Validator {

    private static final Logger log = LoggerFactory.getLogger(Validator.class);

    public static void startValidation(CommandLine cmd) throws IOException{
        if (cmd.hasOption(ARG_MZID)) {
            validateMzdentML(cmd);
        } else if (cmd.hasOption(ARG_PRIDEXML)) {
            validatePrideXML(cmd);
        } else if (cmd.hasOption(ARG_MZTAB)) {
            log.info("Unable to validate mzTab files"); //TODO
        }
        Utility.exit(cmd);
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
        }
        return result;
    }


    public static void validateMzdentML(CommandLine cmd) throws IOException{
        List<File> filesToValidate = new ArrayList<File>();
        List<File> peakFiles = new ArrayList<>();
        File file = new File(cmd.getOptionValue(ARG_MZID));
        if (file.isDirectory()) {
            log.error("Unable to validate against directory of mzid files.");
        } else {
            filesToValidate.add(file);
        }
        filesToValidate = extractZipFiles(filesToValidate);
        if (cmd.hasOption("peak") || cmd.hasOption("peaks")) {
            String[] peakFilesString = cmd.hasOption("peak") ? cmd.getOptionValues("peak")
                                      : cmd.hasOption("peaks") ?  cmd.getOptionValue("peaks").split(";") : new String[0];
            for (String aPeakFilesString : peakFilesString) {
                File peakFile = new File(aPeakFilesString);
                if (peakFile.isDirectory()) {
                    peakFiles.addAll(Arrays.asList(peakFile.listFiles(File::isFile)));
                } else {
                    peakFiles.add(peakFile);
                    log.info("Added peak file: " + peakFile.getPath());
                }
            }
            peakFiles = extractZipFiles(peakFiles);
        } else {
            log.error("Peak file not supplied with mzIdentML file.");
        }
        AssayFileSummary assayFileSummary = new AssayFileSummary();
        Report report = new Report();
        FileType fileType = getFileType(filesToValidate.get(0));
        if (fileType.equals(FileType.MZID)) {
            Object[] validation = validateMzidFile(filesToValidate.get(0), peakFiles);
            report = (Report) validation[0];
            assayFileSummary = (AssayFileSummary) validation[1];
        } else {
            String message = "ERROR: Supplied -mzid file is not a valid mzIdentML file: " + filesToValidate.get(0);
            log.error(message);
            report.setStatus(message);
        }
        File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
        outputReport(assayFileSummary, report, outputFile);
    }

    public static void validatePrideXML(CommandLine cmd) throws IOException{
        List<File> filesToValidate = new ArrayList<File>();
        File file = new File(cmd.getOptionValue(ARG_PRIDEXML));
        if (file.isDirectory()) {
           log.error("Unable to validate against directory");
        } else {
            filesToValidate.add(file);
        }
        filesToValidate = extractZipFiles(filesToValidate);
        File pridexxml = filesToValidate.get(0);
        FileType fileType = getFileType(pridexxml);
        AssayFileSummary assayFileSummary = new AssayFileSummary();
        Report report = new Report();
        if (fileType.equals(FileType.PRIDEXML)) {
            Object[] validation = validatePrideXMLFile(pridexxml);
            report = (Report) validation[0];
            assayFileSummary = (AssayFileSummary) validation[1];
        } else {
            String message = "Supplied -pridexml file is not a valid PRIDE XML file: " + pridexxml.getAbsolutePath();
            log.error(message);
            report.setStatus(message);
        }
        File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
        outputReport(assayFileSummary, report, outputFile);
    }

    private static Object[] validateMzidFile(File file, List<File> dataAccessControllerFiles) {
        Object[] result = new Object[2];
        AssayFileSummary assayFileSummary = new AssayFileSummary();
        Report report = new Report();
        try {
            MzIdentMLControllerImpl mzIdentMLController = new MzIdentMLControllerImpl(file, true);
            mzIdentMLController.addMSController(dataAccessControllerFiles);
            Set<String> uniquePeptides = new HashSet<>();
            Set<CvParam> ptms = new HashSet<>();
            final int NUMBER_OF_CHECKS=100;
            List<Boolean> randomChecks = new ArrayList<>();
            IntStream.range(1,NUMBER_OF_CHECKS).sequential().forEach(i -> randomChecks.add( mzIdentMLController.checkRandomSpectraByDeltaMassThreshold(1, 4.0)));
            boolean passedRandomCheck = true;
            int checkFalseCounts = 0;
            for (Boolean check : randomChecks) {
                if (!check) {
                    checkFalseCounts++;
                }
            }
            assayFileSummary.setDeltaMzErrorRate((double) Math.round((double) checkFalseCounts / NUMBER_OF_CHECKS));
            report.setFileName(file.getAbsolutePath());
            assayFileSummary.setNumberOfIdentifiedSpectra(mzIdentMLController.getNumberOfIdentifiedSpectra());
            assayFileSummary.setNumberOfPeptides(mzIdentMLController.getNumberOfPeptides());
            assayFileSummary.setNumberOfProteins(mzIdentMLController.getNumberOfProteins());
            assayFileSummary.setNumberofMissingSpectra(mzIdentMLController.getNumberOfMissingSpectra());
            assayFileSummary.setNumberOfSpectra(mzIdentMLController.getNumberOfSpectra());
            log.info("Starting to parse over proteins (" + assayFileSummary.getNumberOfProteins() + ") and peptides (" + assayFileSummary.getNumberOfPeptides() + ").");
            if (mzIdentMLController.getNumberOfMissingSpectra()<1) {
                mzIdentMLController.getProteinIds().stream().forEach(proteinId ->
                        mzIdentMLController.getProteinById(proteinId).getPeptides().stream().forEach(peptide-> {
                            uniquePeptides.add(peptide.getSequence());
                            peptide.getModifications().stream().forEach(modification ->
                                    modification.getCvParams().stream().forEach(cvParam -> {
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
                IntStream.range(1, (mzIdentMLController.getNumberOfPeptides()<100 ? mzIdentMLController.getNumberOfPeptides() : 100)).sequential().forEach(i -> {
                    Peptide peptide = mzIdentMLController.getProteinById(mzIdentMLController.getProteinIds().stream().findAny().get()).getPeptides().stream().findAny().get();
                    if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                        if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
                            matches.add(false);
                        }
                    }
                });
                assayFileSummary.addPtms(DataConversionUtil.convertAssayPTMs(ptms));
                assayFileSummary.setSpectrumMatchFragmentIons(matches.size() <= 1);
                assayFileSummary.setNumberOfUniquePeptides(uniquePeptides.size());
                log.info("Finished parsing over proteins and peptides.");
            } else {
                log.error("Missing spectra are present");
            }
            scanForGeneralMetadata(mzIdentMLController, assayFileSummary);
            scanForInstrument(mzIdentMLController, assayFileSummary);
            scanForSoftware(mzIdentMLController, assayFileSummary);
            scanForSearchDetails(mzIdentMLController, assayFileSummary);
            scanMzIdentMLSpecificDetails(mzIdentMLController, dataAccessControllerFiles, assayFileSummary);
            if (StringUtils.isEmpty(report.getStatus())) {
                report.setStatus("OK");
            }
        } catch (Exception e) {
            log.error("Exception when scanning mzid file", e);
            report.setStatus("ERROR\n" + e.getMessage());
        }
        result[0]=report;
        result[1]=assayFileSummary;
        return result;
    }

    private static Object[] validatePrideXMLFile(File file) {
        Object[] result = new Object[2];
        log.info("Validating PRIDE XML now: " + file.getAbsolutePath());
        AssayFileSummary assayFileSummary = new AssayFileSummary();
        Report report = new Report();
        try {
            PrideXmlControllerImpl prideXmlController = new PrideXmlControllerImpl(file);
            Set<String> uniquePeptides = new HashSet<>();
            Set<CvParam> ptms = new HashSet<>();

            report.setFileName(file.getAbsolutePath());
            assayFileSummary.setNumberOfIdentifiedSpectra(prideXmlController.getNumberOfIdentifiedSpectra());
            assayFileSummary.setNumberOfPeptides(prideXmlController.getNumberOfPeptides());
            assayFileSummary.setNumberOfProteins(prideXmlController.getNumberOfProteins());
            assayFileSummary.setNumberofMissingSpectra(prideXmlController.getNumberOfMissingSpectra());
            assayFileSummary.setNumberOfSpectra(prideXmlController.getNumberOfSpectra());

            Double errorPSMCount = 0.0;
            Set<Comparable> allIdentifiedSpectrumIds = new HashSet<>();
            Set<Comparable> existingIdentifiedSpectrumIds = new HashSet<>();
            if (assayFileSummary.getNumberofMissingSpectra()<1) {
                for (Comparable proteinId :  prideXmlController.getProteinIds()) {
                    for (Peptide peptide :  prideXmlController.getProteinById(proteinId).getPeptides()) {
                        uniquePeptides.add(peptide.getSequence());
                        List<Double> ptmMasses = new ArrayList<>();
                        peptide.getModifications().stream().forEach(modification ->
                                modification.getCvParams().stream().forEach(cvParam -> {
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
                                })
                        );
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
                IntStream.range(1, (prideXmlController.getNumberOfPeptides()<100 ? prideXmlController.getNumberOfPeptides() : 100)).sequential().forEach(i -> {
                    Peptide peptide = prideXmlController.getProteinById(prideXmlController.getProteinIds().stream().findAny().get()).getPeptides().stream().findAny().get();
                    if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                        if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
                            matches.add(false);
                        }
                    }
                });
                assayFileSummary.addPtms(DataConversionUtil.convertAssayPTMs(ptms));
                assayFileSummary.setSpectrumMatchFragmentIons(matches.size() <= 1);
                assayFileSummary.setNumberOfUniquePeptides(uniquePeptides.size());
                assayFileSummary.setNumberOfSpectra(prideXmlController.getNumberOfSpectra());
                assayFileSummary.setNumberOfIdentifiedSpectra(existingIdentifiedSpectrumIds.size());
                allIdentifiedSpectrumIds.removeAll(existingIdentifiedSpectrumIds);
                assayFileSummary.setNumberofMissingSpectra(allIdentifiedSpectrumIds.size());
                assayFileSummary.setDeltaMzErrorRate((double) (Math.round(errorPSMCount / (double) assayFileSummary.getNumberOfPeptides())));
            } else {
                log.error("Missing spectra are present");
            }
            scanForGeneralMetadata(prideXmlController, assayFileSummary);
            scanForInstrument(prideXmlController, assayFileSummary);
            scanForSoftware(prideXmlController, assayFileSummary);
            scanForSearchDetails(prideXmlController, assayFileSummary);
            if (StringUtils.isEmpty(report.getStatus())) {
                report.setStatus("OK");
            }
        } catch (Exception e) {
        log.error("Exception when scanning mzid file", e);
        report.setStatus("ERROR\n" + e.getMessage());
        }
        result[0] = report;
        result[1] = assayFileSummary;
        return result;
    }


    public static List<File> extractZipFiles(List<File> files) throws IOException {
        List<File> zippedFiles = findZippedFiles(files);
        if (zippedFiles.size()>0) {
            files.removeAll(zippedFiles);
            files.addAll(unzipFiles(zippedFiles, zippedFiles.get(0).getParentFile().getAbsoluteFile()));
        }
        return files.stream().distinct().collect(Collectors.toList());
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
            }
        });
        return unzippedFiles;
    }

    private static void outputReport(AssayFileSummary assayFileSummary, Report report, File reportFile) {
        log.info(report.toString(assayFileSummary));
        if (reportFile!=null) {
            try {
                log.info("Writing report to: " + reportFile.getAbsolutePath());
                Files.write(reportFile.toPath(), report.toString(assayFileSummary).getBytes());
                ObjectOutputStream oos = null;
                FileOutputStream fout;
                try{
                    String serialFileName = reportFile.getAbsolutePath() + ".ser";
                    log.info("Writing serial summary object to: " + serialFileName);
                    fout = new FileOutputStream(serialFileName);
                    oos = new ObjectOutputStream(fout);
                    oos.writeObject(assayFileSummary);
                } catch (Exception ex) {
                    log.error("Error while writing assayFileSummary object: " + reportFile.getAbsolutePath() + ".ser", ex);
                } finally {
                    if(oos  != null){
                        oos.close();
                    }
                }
            } catch (IOException ioe) {
                log.error("Problem when writing report file: ", ioe);
            }
        }
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

    private static void scanForGeneralMetadata(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
        log.info("Started scanning for general metadata.");
        assayFileSummary.setName(dataAccessController.getName());
        assayFileSummary.setShortLabel(StringUtils.isEmpty(dataAccessController.getExperimentMetaData().getShortLabel()) ? "" : dataAccessController.getExperimentMetaData().getShortLabel() );
        assayFileSummary.addContacts(DataConversionUtil.convertContact(dataAccessController.getExperimentMetaData().getPersons()));
        ParamGroup additional = dataAccessController.getExperimentMetaData().getAdditional();
        assayFileSummary.addCvParams(DataConversionUtil.convertAssayGroupCvParams(additional));
        assayFileSummary.addUserParams(DataConversionUtil.convertAssayGroupUserParams(additional));
        log.info("Finished scanning for general metadata.");
    }

    private static void scanForInstrument(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
        log.info("Started scanning for instruments");
        Set<Instrument> instruments = new HashSet<>();
        //check to see if we have instrument configurations in the result file to scan
        //this isn't always present
        if (dataAccessController.getMzGraphMetaData() != null) {
            Collection<InstrumentConfiguration> instrumentConfigurations = dataAccessController.getMzGraphMetaData().getInstrumentConfigurations();
            for (InstrumentConfiguration instrumentConfiguration : instrumentConfigurations) {
                Instrument instrument = new Instrument();

                //set instrument cv param
                uk.ac.ebi.pride.archive.repo.param.CvParam cvParam = new uk.ac.ebi.pride.archive.repo.param.CvParam();
                cvParam.setCvLabel(Constant.MS);
                cvParam.setName(Constant.MS_INSTRUMENT_MODEL_NAME);
                cvParam.setAccession(Constant.MS_INSTRUMENT_MODEL_AC);
                instrument.setCvParam(cvParam);
                instrument.setValue(instrumentConfiguration.getId());

                //build instrument components
                instrument.setSources(new ArrayList<>());
                instrument.setAnalyzers(new ArrayList<>());
                instrument.setDetectors(new ArrayList<>());
                int orderIndex = 1;
                //source
                for (InstrumentComponent source : instrumentConfiguration.getSource()) {
                    SourceInstrumentComponent sourceInstrumentComponent = new SourceInstrumentComponent();
                    sourceInstrumentComponent.setInstrument(instrument);
                    sourceInstrumentComponent.setOrder(orderIndex++);
                    sourceInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(sourceInstrumentComponent, source.getCvParams()));
                    sourceInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(sourceInstrumentComponent, source.getUserParams()));
                    instrument.getSources().add(sourceInstrumentComponent);
                }
                //analyzer
                for (InstrumentComponent analyzer : instrumentConfiguration.getAnalyzer()) {
                    AnalyzerInstrumentComponent analyzerInstrumentComponent = new AnalyzerInstrumentComponent();
                    analyzerInstrumentComponent.setInstrument(instrument);
                    analyzerInstrumentComponent.setOrder(orderIndex++);
                    analyzerInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(analyzerInstrumentComponent, analyzer.getCvParams()));
                    analyzerInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(analyzerInstrumentComponent, analyzer.getUserParams()));
                    instrument.getAnalyzers().add(analyzerInstrumentComponent);
                }
                //detector
                for (InstrumentComponent detector : instrumentConfiguration.getDetector()) {
                    DetectorInstrumentComponent detectorInstrumentComponent = new DetectorInstrumentComponent();
                    detectorInstrumentComponent.setInstrument(instrument);
                    detectorInstrumentComponent.setOrder(orderIndex++);
                    detectorInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(detectorInstrumentComponent, detector.getCvParams()));
                    detectorInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(detectorInstrumentComponent, detector.getUserParams()));
                    instrument.getDetectors().add(detectorInstrumentComponent);
                }
                //store instrument
                instruments.add(instrument);
            }
        } // else do nothing
        assayFileSummary.addInstruments(instruments);
        log.info("Finished scanning for instruments");
    }

    private static void scanForSoftware(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
        log.info("Started scanning for software");
        ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();
        Set<Software> softwares = new HashSet<>();
        softwares.addAll(experimentMetaData.getSoftwares());
        Set softwareSet = new HashSet<>();
        softwareSet.addAll(DataConversionUtil.convertSoftware(softwares));
        assayFileSummary.addSoftwares(softwareSet);
        log.info("Finished scanning for software");
    }

    private static void scanForSearchDetails(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
        log.info("Started scanning for search details");
        // protein group
        boolean proteinGroupPresent = dataAccessController.hasProteinAmbiguityGroup();
        assayFileSummary.setProteinGroupPresent(proteinGroupPresent);

        Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
        if (proteinIds != null && !proteinIds.isEmpty()) {
            Comparable firstProteinId = proteinIds.iterator().next();

            // protein accession
            String accession = dataAccessController.getProteinAccession(firstProteinId);
            assayFileSummary.setExampleProteinAccession(accession);

            // search database
            SearchDataBase searchDatabase = dataAccessController.getSearchDatabase(firstProteinId);
            if (searchDatabase != null) {
                assayFileSummary.setSearchDatabase(searchDatabase.getName());
            }
        }
        log.info("Finished scanning for search details");
    }

    private static void scanMzIdentMLSpecificDetails(MzIdentMLControllerImpl mzIdentMLController, List<File> peakFiles, AssayFileSummary assayFileSummary) throws IOException {
        log.info("Started scanning for mzid-specific details");
        Set<PeakFileSummary> peakFileSummaries = new HashSet<>();
        List peakFileNames = new ArrayList();

        for (File peakFile : peakFiles) {
            peakFileNames.add(peakFile.getName());
            String extension = FilenameUtils.getExtension(peakFile.getAbsolutePath());
            if (MassSpecFileFormat.MZML.toString().equalsIgnoreCase(extension)) {
                getMzMLSummary(peakFile, assayFileSummary);
                break;
            }
        }
        List<SpectraData> spectraDataFiles = mzIdentMLController.getSpectraDataFiles();
        for (SpectraData spectraDataFile : spectraDataFiles) {
            String location = spectraDataFile.getLocation();
            String realFileName = FileUtil.getRealFileName(location);
            Integer numberOfSpectrabySpectraData = mzIdentMLController.getNumberOfSpectrabySpectraData(spectraDataFile);
            peakFileSummaries.add(new PeakFileSummary(realFileName, !peakFileNames.contains(realFileName), numberOfSpectrabySpectraData));
        }
        assayFileSummary.addPeakFileSummaries(peakFileSummaries);
        log.info("Finished scanning for mzid-specific details");
    }

    private static boolean getMzMLSummary(File mappedFile, AssayFileSummary assayFileSummary) {
        log.info("Getting mzml summary.");
        MzMLControllerImpl mzMLController = null;
        try {
            mzMLController = new MzMLControllerImpl(mappedFile);
            if (mzMLController.hasChromatogram()) {
                assayFileSummary.setChromatogram(true);
                mzMLController.close();
                return true;
            }
        } finally {
            if (mzMLController != null) {
                log.info("Finished getting mzml summary.");
                mzMLController.close();
            }
        }
        return false;
    }



}
