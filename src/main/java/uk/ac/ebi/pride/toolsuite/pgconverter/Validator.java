package uk.ac.ebi.pride.toolsuite.pgconverter;

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
import uk.ac.ebi.pride.toolsuite.pgconverter.utils.*;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.controller.cache.CacheEntry;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.*;
import uk.ac.ebi.pride.utilities.data.core.*;
import uk.ac.ebi.pride.utilities.data.core.Software;
import uk.ac.ebi.pride.utilities.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import static uk.ac.ebi.pride.toolsuite.pgconverter.utils.Utility.*;

/**
 * This class validates an input file and produces a plain text report file,
 * and potentially a serialized version of AssayFileSummary as well.
 *
 * @author Tobias Ternent
 */
public class Validator {

  private static final Logger log = LoggerFactory.getLogger(Validator.class);


  /**
   * This class parses the command line arguments and beings the file validation.
   *
   * @param cmd command line arguments.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  public static void startValidation(CommandLine cmd) throws IOException{
    if (cmd.hasOption(ARG_MZID)) {
      validateMzdentML(cmd);
      } else if (cmd.hasOption(ARG_PRIDEXML)) {
      validatePrideXML(cmd);
      } else if (cmd.hasOption(ARG_MZTAB)) {
      validateMzTab(cmd);
    } else {
      log.error("Unable to validate unknown input file type");
    }
  }

  /**
   * This method identiies a file's format extension type.
   *
   * @param file the input file.
   * @return the corresponding FileType.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  private static FileType getFileType(File file) throws IOException {
    FileType result;
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

  /**
   * This method validates an an mzIdentML file.
   *
   * @param cmd the command line arguments.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  public static void validateMzdentML(CommandLine cmd) throws IOException{
    File file = new File(cmd.getOptionValue(ARG_MZID));
    List<File> filesToValidate = getFilesToValidate(file);
    List<File> peakFiles = getPeakFiles(cmd);
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    FileType fileType = getFileType(filesToValidate.get(0));
    if (fileType.equals(FileType.MZID)) {
      Object[] validation = validateAssayFile(filesToValidate.get(0), FileType.MZID, peakFiles);
      report = (Report) validation[0];
      assayFileSummary = (AssayFileSummary) validation[1];
    } else {
      String message = "ERROR: Supplied -mzid file is not a valid mzIdentML file: " + filesToValidate.get(0);
      log.error(message);
      report.setStatus(message);
    }
    File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    outputReport(assayFileSummary, report, outputFile, cmd.hasOption(ARG_SKIP_SERIALIZATION));
  }

  /**
   * This method validates a PRIDE XML file.
   *
   * @param cmd the command line arguments.
   * @throws IOException if there are problems reading or writing to the file system.
   */
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
      Object[] validation = validateAssayFile(pridexxml, FileType.PRIDEXML, null);
      report = (Report) validation[0];
      assayFileSummary = (AssayFileSummary) validation[1];
    } else {
      String message = "Supplied -pridexml file is not a valid PRIDE XML file: " + pridexxml.getAbsolutePath();
      log.error(message);
      report.setStatus(message);
    }
    File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    outputReport(assayFileSummary, report, outputFile, cmd.hasOption(ARG_SKIP_SERIALIZATION));
  }


  /**
   * This method validated an mzTab file.
   *
   * @param cmd the command line arguments.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  private static void validateMzTab(CommandLine cmd) throws IOException{
    File file = new File(cmd.getOptionValue(ARG_MZTAB));
    List<File> filesToValidate = getFilesToValidate(file);
    List<File> peakFiles = getPeakFiles(cmd);
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    FileType fileType = getFileType(filesToValidate.get(0));
    if (fileType.equals(FileType.MZTAB)) {
      Object[] validation = validateAssayFile(filesToValidate.get(0), FileType.MZTAB, peakFiles);
      report = (Report) validation[0];
      assayFileSummary = (AssayFileSummary) validation[1];
    } else {
      String message = "ERROR: Supplied -mztab file is not a valid mzTab file: " + filesToValidate.get(0);
      log.error(message);
      report.setStatus(message);
    }
    File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    outputReport(assayFileSummary, report, outputFile, cmd.hasOption(ARG_SKIP_SERIALIZATION));
  }

  /**
   * This method gets all the input file ready for validation, if it is extracted.
   *
   * @param file the input file for validation.
   * @return List of extracted files for validation.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  private static List<File> getFilesToValidate(File file) throws IOException {
    List<File> filesToValidate = new ArrayList<>();
    if (file.isDirectory()) {
      log.error("Unable to validate against directory of mzid files.");
    } else {
      filesToValidate.add(file);
    }
    filesToValidate = extractZipFiles(filesToValidate);
    return filesToValidate;
  }

  /**
   * This method gets a list of providede peak files.
   *
   * @param cmd the command line arguments.
   * @return List of peak files.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  private static List<File> getPeakFiles(CommandLine cmd) throws IOException {
    List<File> peakFiles = new ArrayList<>();
    if (cmd.hasOption(ARG_PEAK) || cmd.hasOption(ARG_PEAKS)) {
      String[] peakFilesString = cmd.hasOption(ARG_PEAK) ? cmd.getOptionValues(ARG_PEAK)
          : cmd.hasOption(ARG_PEAKS) ?  cmd.getOptionValue(ARG_PEAKS).split("##") : new String[0];
      for (String aPeakFilesString : peakFilesString) {
        File peakFile = new File(aPeakFilesString);
        if (peakFile.isDirectory()) {
          File[] listFiles = peakFile.listFiles(File::isFile);
          if (listFiles!=null) {
            peakFiles.addAll(Arrays.asList(listFiles));
          }
        } else {
          peakFiles.add(peakFile);
          log.info("Added peak file: " + peakFile.getPath());
        }
      }
      peakFiles = extractZipFiles(peakFiles);
    } else {
      log.error("Peak file not supplied with mzIdentML file.");
    }
    return peakFiles;
  }

  /**
   * This method extracts an input list of files.
   *
   * @param files a list of input zip files to extract.
   * @return a list of extracted files.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  public static List<File> extractZipFiles(List<File> files) throws IOException {
    List<File> zippedFiles = findZippedFiles(files);
    if (zippedFiles.size()>0) {
      files.removeAll(zippedFiles);
      files.addAll(unzipFiles(zippedFiles, zippedFiles.get(0).getParentFile().getAbsoluteFile()));
    }
    return files.stream().distinct().collect(Collectors.toList());
  }

  /**
   * ~This method identifies any gzipped files.
   * @param files a list if input files.
   * @return a list of files that are gzipped.
   */
  private static List<File> findZippedFiles(List<File> files) {
    return files.stream().filter(file -> file.getName().endsWith(".gz")).collect(Collectors.toList());
  }

  /**
   * This method extracts a list of iniput gzipped files to an output directory.
   * @param zippedFiles a list of input files to extract.
   * @param outputFolder the output directory.
   * @return a list of files that have been extracted.
   * @throws IOException if there are problems reading or writing to the file system.
   */
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

  /**
   * This method writes the report to a specified file, and may also write this as a serialized object.
   *
   * @param assayFileSummary the validation summary of the file.
   * @param report the validation report.
   * @param reportFile the report file to output to.
   * @param skipSerialization true to skip serialized output.
   */
  private static void outputReport(AssayFileSummary assayFileSummary, Report report, File reportFile, boolean skipSerialization) {
    log.info(report.toString(assayFileSummary));
    if (reportFile!=null) {
      try {
        log.info("Writing report to: " + reportFile.getAbsolutePath());
        Files.write(reportFile.toPath(), report.toString(assayFileSummary).getBytes());
        if (!skipSerialization) {
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
        } else {
          log.info("Skipping report serialization.");
        }
      } catch (IOException ioe) {
          log.error("Problem when writing report file: ", ioe);
      }
    }
  }

  /**
   * This method checks to see if the fragment ions match the spectrum.
   *
   * @param fragmentIons the fragment ions.
   * @param spectrum the spectrum.
   * @return true if they match, false otherwise.
   */
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

  /**
   * This method scans for general metadata.
   *
   * @param dataAccessController the input controller to read over.
   * @param assayFileSummary the assay file summary to output results to.
   */
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

  /**
   * This method scans for instruments metadata.
   *
   * @param dataAccessController the input controller to read over.
   * @param assayFileSummary the assay file summary to output results to.
   */
  private static void scanForInstrument(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
    log.info("Started scanning for instruments");
    Set<Instrument> instruments = new HashSet<>();
    //check to see if we have instrument configurations in the result file to scan
    //this isn't always present
    MzGraphMetaData mzGraphMetaData = null;
    try {
      mzGraphMetaData = dataAccessController.getMzGraphMetaData();
    } catch (Exception e) {
      log.error("Exception while getting mzgraph instrument data." + e);
    }
    if (mzGraphMetaData != null) {
      Collection<InstrumentConfiguration> instrumentConfigurations = dataAccessController.getMzGraphMetaData().getInstrumentConfigurations();
      for (InstrumentConfiguration instrumentConfiguration : instrumentConfigurations) {
        Instrument instrument = new Instrument();
        //set instrument cv param
        uk.ac.ebi.pride.archive.repo.param.CvParam cvParam = new uk.ac.ebi.pride.archive.repo.param.CvParam();
        cvParam.setCvLabel(Constant.MS);
        cvParam.setName(Utility.MS_INSTRUMENT_MODEL_NAME);
        cvParam.setAccession(Utility.MS_INSTRUMENT_MODEL_AC);
        instrument.setCvParam(cvParam);
        instrument.setValue(instrumentConfiguration.getId());
        //build instrument components
        instrument.setSources(new ArrayList<>());
        instrument.setAnalyzers(new ArrayList<>());
        instrument.setDetectors(new ArrayList<>());
        int orderIndex = 1;
        //source
        for (InstrumentComponent source : instrumentConfiguration.getSource()) {
          if (source!=null) {
            SourceInstrumentComponent sourceInstrumentComponent = new SourceInstrumentComponent();
            sourceInstrumentComponent.setInstrument(instrument);
            sourceInstrumentComponent.setOrder(orderIndex++);
            sourceInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(sourceInstrumentComponent, source.getCvParams()));
            sourceInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(sourceInstrumentComponent, source.getUserParams()));
            instrument.getSources().add(sourceInstrumentComponent);
          }
        }
        //analyzer
        for (InstrumentComponent  analyzer: instrumentConfiguration.getAnalyzer()) {
          if (analyzer!=null) {
            AnalyzerInstrumentComponent analyzerInstrumentComponent = new AnalyzerInstrumentComponent();
            analyzerInstrumentComponent.setInstrument(instrument);
            analyzerInstrumentComponent.setOrder(orderIndex++);
            analyzerInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(analyzerInstrumentComponent, analyzer.getCvParams()));
            analyzerInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(analyzerInstrumentComponent, analyzer.getUserParams()));
            instrument.getAnalyzers().add(analyzerInstrumentComponent);
          }
        }
        //detector
        for (InstrumentComponent detector : instrumentConfiguration.getDetector()) {
          if (detector!=null) {
            DetectorInstrumentComponent detectorInstrumentComponent = new DetectorInstrumentComponent();
            detectorInstrumentComponent.setInstrument(instrument);
            detectorInstrumentComponent.setOrder(orderIndex++);
            detectorInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(detectorInstrumentComponent, detector.getCvParams()));
            detectorInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(detectorInstrumentComponent, detector.getUserParams()));
            instrument.getDetectors().add(detectorInstrumentComponent);
          }
        }
        //store instrument
        instruments.add(instrument);
      }
    } // else do nothing
    assayFileSummary.addInstruments(instruments);
    log.info("Finished scanning for instruments");
  }

  /**
   * This method scans for software metadata.
   *
   * @param dataAccessController the input controller to read over.
   * @param assayFileSummary the assay file summary to output results to.
   */
  private static void scanForSoftware(DataAccessController dataAccessController, AssayFileSummary assayFileSummary) {
    log.info("Started scanning for software");
    ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();
    Set<Software> softwares = new HashSet<>();
    softwares.addAll(experimentMetaData.getSoftwares());
    Set<uk.ac.ebi.pride.archive.repo.assay.software.Software> softwareSet = new HashSet<>();
    softwareSet.addAll(DataConversionUtil.convertSoftware(softwares));
    assayFileSummary.addSoftwares(softwareSet);
    log.info("Finished scanning for software");
  }

  /**
   * This method scans for search details metadata.
   *
   * @param dataAccessController the input controller to read over.
   * @param assayFileSummary the assay file summary to output results to.
   */
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

  /**
   * This method scans for ReferencedIdentificationController-specific metadata.
   *
   * @param referencedIdentificationController the input controller to read over.
   * @param peakFiles the input related peak files.
   * @param assayFileSummary the assay file summary to output results to.
   * @throws IOException if there are problems reading or writing to the file system.
   */
  private static void scanRefIdControllerpecificDetails(ReferencedIdentificationController referencedIdentificationController, List<File> peakFiles, AssayFileSummary assayFileSummary) throws IOException {
    log.info("Started scanning for mzid-specific details");
    Set<PeakFileSummary> peakFileSummaries = new HashSet<>();
    List<String> peakFileNames = new ArrayList<>();
    for (File peakFile : peakFiles) {
      peakFileNames.add(peakFile.getName());
      String extension = FilenameUtils.getExtension(peakFile.getAbsolutePath());
      if (MassSpecFileFormat.MZML.toString().equalsIgnoreCase(extension)) {
        getMzMLSummary(peakFile, assayFileSummary);
        break;
      }
    }
    List<SpectraData> spectraDataFiles = referencedIdentificationController.getSpectraDataFiles();
    for (SpectraData spectraDataFile : spectraDataFiles) {
      String location = spectraDataFile.getLocation();
      String realFileName = FileUtil.getRealFileName(location);
      Integer numberOfSpectrabySpectraData = referencedIdentificationController.getNumberOfSpectrabySpectraData(spectraDataFile);
      peakFileSummaries.add(new PeakFileSummary(realFileName, !peakFileNames.contains(realFileName), numberOfSpectrabySpectraData));
    }
    assayFileSummary.addPeakFileSummaries(peakFileSummaries);
    log.info("Finished scanning for ReferencedIdentificationController-specific details");
  }


  /**
   * This method checks if a mapped mzML file has chromatograms or not.
   * @param mappedFile the input mzML file.
   * @param assayFileSummary the assay file summary to output the result to.
   * @return true if a mzML has chromatograms, false otherwise.
   */
  private static boolean getMzMLSummary(File mappedFile, AssayFileSummary assayFileSummary) {
    log.info("Getting mzml summary.");
    MzMLControllerImpl mzMLController = null;
    boolean result = false;
    try {
      mzMLController = new MzMLControllerImpl(mappedFile);
      if (mzMLController.hasChromatogram()) {
        assayFileSummary.setChromatogram(true);
        mzMLController.close();
        result = true;
      }
    } finally {
      if (mzMLController != null) {
        log.info("Finished getting mzml summary.");
        mzMLController.close();
      }
    }
    return result;
  }

  /**
   * This method validates an input assay file.
   *
   * @param file the input assay file.
   * @return an array of objects[2]: a Report object and an AssayFileSummary, respectively.
   */
  private static Object[] validateAssayFile(File file, FileType type, List<File> dataAccessControllerFiles) {
    Object[] result = new Object[2];
    log.info("Validating assay file: " + file.getAbsolutePath());
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    try {
      final AssayFileController assayFileController;
      switch(type) {
        case MZID :
          assayFileController = new MzIdentMLControllerImpl(file);
          assayFileController.addMSController(dataAccessControllerFiles);
          break;
        case PRIDEXML :
          assayFileController = new PrideXmlControllerImpl(file);
          break;
        case MZTAB : assayFileController = new MzTabControllerImpl(file);
          assayFileController.addMSController(dataAccessControllerFiles);
          break;
        default : log.error("Unrecognized assay fle type: " + type);
          assayFileController = new MzIdentMLControllerImpl(file);
          break;
      }
      Set<String> uniquePeptides = new HashSet<>();
      Set<CvParam> ptms = new HashSet<>();
      final int NUMBER_OF_CHECKS=1;
      List<Boolean> randomChecks = new ArrayList<>();
      IntStream.range(1,NUMBER_OF_CHECKS).sequential().forEach(i -> randomChecks.add(assayFileController.checkRandomSpectraByDeltaMassThreshold(1, 4.0)));
      int checkFalseCounts = 0;
      for (Boolean check : randomChecks) {
        if (!check) {
          checkFalseCounts++;
        }
      }
      assayFileSummary.setDeltaMzErrorRate((double) Math.round(((double) checkFalseCounts / NUMBER_OF_CHECKS)*100)/100);
      report.setFileName(file.getAbsolutePath());
      assayFileSummary.setNumberOfIdentifiedSpectra(assayFileController.getNumberOfIdentifiedSpectra());
      assayFileSummary.setNumberOfPeptides(assayFileController.getNumberOfPeptides());
      assayFileSummary.setNumberOfProteins(assayFileController.getNumberOfProteins());
      assayFileSummary.setNumberofMissingSpectra(assayFileController.getNumberOfMissingSpectra());
      assayFileSummary.setNumberOfSpectra(assayFileController.getNumberOfSpectra());
      if (assayFileSummary.getNumberofMissingSpectra()<1) {
        double pepCount =0;
        int protCount = 0;
        final int PEP_STEP_SIZE= 100000;
        for (Comparable proteinId : assayFileController.getProteinIds()) {
          protCount++;
          if (log.isDebugEnabled()) {
            log.debug("\nPercent through total proteins, " + protCount + " / " +   assayFileController.getProteinIds().size() + " : " + ((int)((protCount * 100.0f) /  assayFileController.getProteinIds().size())) );
            calcCacheSizses(assayFileController);
          }
          for (Peptide peptide : assayFileController.getProteinById(proteinId).getPeptides()) {
            pepCount++;
            if (log.isDebugEnabled()) {
              if (((int)pepCount) % PEP_STEP_SIZE == 0) {
                log.info("Processed another " + PEP_STEP_SIZE + " peptides. Now at: " + pepCount);
              }
            }
            uniquePeptides.add(peptide.getSequence());
            peptide.getModifications().forEach(modification ->
                modification.getCvParams().forEach(cvParam -> {
                  if (cvParam.getCvLookupID() == null) {
                    log.error("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                    throw new NullPointerException("A PTM CV Param's ontology is not defined properly in file: " + file.getPath());
                  }
                  if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                    ptms.add(cvParam);
                  }
                })
            );
          }
        }
        List<Boolean> matches = new ArrayList<>();
        matches.add(true);
        IntStream.range(1, (assayFileController.getNumberOfPeptides()<100 ? assayFileController.getNumberOfPeptides() : 100)).sequential().forEach(i -> {
          Peptide peptide = assayFileController.getProteinById(assayFileController.getProteinIds().stream().findAny().get()).getPeptides().stream().findAny().get();
          if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
            if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
              matches.add(false);
            }
          }
        });
        assayFileSummary.addPtms(DataConversionUtil.convertAssayPTMs(ptms));
        assayFileSummary.setSpectrumMatchFragmentIons(matches.size() <= 1);
        assayFileSummary.setNumberOfUniquePeptides(uniquePeptides.size());
      } else {
        log.error("Missing spectra are present");
      }
      scanForGeneralMetadata(assayFileController, assayFileSummary);
      scanForInstrument(assayFileController, assayFileSummary);
      scanForSoftware(assayFileController, assayFileSummary);
      scanForSearchDetails(assayFileController, assayFileSummary);
      switch(type) {
        case MZID :
          scanRefIdControllerpecificDetails((ReferencedIdentificationController) assayFileController, dataAccessControllerFiles, assayFileSummary);
          break;
        case MZTAB :
          scanRefIdControllerpecificDetails((ReferencedIdentificationController) assayFileController, dataAccessControllerFiles, assayFileSummary);
          break;
        default : // do nothing
          break;
      }
      if (StringUtils.isEmpty(report.getStatus())) {
        report.setStatus("OK");
      }
    } catch (Exception e) {
      log.error("Exception when scanning assay file", e);
      report.setStatus("ERROR\n" + e.getMessage());
    }
    result[0] = report;
    result[1] = assayFileSummary;
    return result;
  }

  /**
   * This method outputs the cache sizes for debugging purposes.
   *
   * @param cachedDataAccessController the data access controller for the assay file
   */
  private static void calcCacheSizses(CachedDataAccessController cachedDataAccessController) {
    Arrays.stream(CacheEntry.values()).forEach(cacheEntry -> log.debug("Cache entry: " + cacheEntry.name() + " Size: " + (
        (cachedDataAccessController.getCache().get(cacheEntry)==null?
            "null" :
            cachedDataAccessController.getCache().get(cacheEntry) instanceof Map ?
                ((Map)cachedDataAccessController.getCache().get(cacheEntry)).size() :
                cachedDataAccessController.getCache().get(cacheEntry) instanceof Collection ?
                    ((Collection)cachedDataAccessController.getCache().get(cacheEntry)).size() :
                    "null"))));
  }
}
