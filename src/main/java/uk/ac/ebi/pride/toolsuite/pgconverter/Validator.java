package uk.ac.ebi.pride.toolsuite.pgconverter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import uk.ac.ebi.pride.archive.repo.assay.instrument.AnalyzerInstrumentComponent;
import uk.ac.ebi.pride.archive.repo.assay.instrument.DetectorInstrumentComponent;
import uk.ac.ebi.pride.archive.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.archive.repo.assay.instrument.SourceInstrumentComponent;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.tools.ErrorHandlerIface;
import uk.ac.ebi.pride.tools.GenericSchemaValidator;
import uk.ac.ebi.pride.tools.ValidationErrorHandler;
import uk.ac.ebi.pride.tools.cl.PrideXmlClValidator;
import uk.ac.ebi.pride.tools.cl.XMLValidationErrorHandler;
import uk.ac.ebi.pride.toolsuite.pgconverter.utils.*;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.*;
import uk.ac.ebi.pride.utilities.data.core.*;
import uk.ac.ebi.pride.utilities.data.core.Software;
import uk.ac.ebi.pride.utilities.util.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
  private static final String PRIDE_XML_SCHEMA = "http://ftp.pride.ebi.ac.uk/pride/resources/schema/pride/pride.xsd";
  private static final String MZID_SCHEMA = "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/psi-pi/mzIdentML1.1.0.xsd";
  public static final String SCHEMA_OK_MESSAGE = "XML schema validation OK on: ";
  private static final String LINE_CONTENT = " Line content: ";
  private static final String FIELD_UNSIGNED_INTEGER = "field must not be empty and must be an unsigned integer containing at least one digit.";

  /**
   * This class parses the command line arguments and beings the file validation.
   *
   * @param cmd command line arguments.
   */
  public static void startValidation(CommandLine cmd) {
    if (cmd.hasOption(ARG_MZID)) {
      validateMzIdentML(cmd);
    } else if (cmd.hasOption(ARG_PRIDEXML)) {
      validatePrideXML(cmd);
    } else if (cmd.hasOption(ARG_MZTAB)) {
      validateMzTab(cmd);
    } else if (cmd.hasOption(ARG_PROBED)) {
      validateProBed(cmd);
    } else {
      log.error("Unable to validate unknown input file type");
    }
  }

  /**
   * This method identiies a file's format extension type.
   *
   * @param file the input file.
   * @return the corresponding FileType.
   */
  private static FileType getFileType(File file) {
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
   */
  private static void validateMzIdentML(CommandLine cmd) {
    File file = new File(cmd.getOptionValue(ARG_MZID));
    List<File> filesToValidate = getFilesToValidate(file);
    File mzid = filesToValidate.get(0);
    List<File> peakFiles = getPeakFiles(cmd);
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    FileType fileType = getFileType(filesToValidate.get(0));
    File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    if (fileType.equals(FileType.MZID)) {
      boolean valid = true; // assume true if not validating schema
      SchemaCheckResult schemaResult;
      List<String> schemaErrors = null;
      if (cmd.hasOption(ARG_SCHEMA_VALIDATION) || cmd.hasOption(ARG_SCHEMA_ONLY_VALIDATION)) {
        schemaResult = validateMzidSchema(MZID_SCHEMA, mzid);
        valid = schemaResult.isValidAgainstSchema();
        schemaErrors = schemaResult.getErrorMessages();
      }
      if (valid) {
        if (cmd.hasOption(ARG_SCHEMA_ONLY_VALIDATION)) {
          report.setStatusOK();
        } else {
          ValidationResult validationResult = validateAssayFile(mzid, FileType.MZID, peakFiles);
          report = validationResult.getReport();
          assayFileSummary = validationResult.getAssayFileSummary();
        }
      } else {
        String message = "ERROR: Supplied -mzid file failed XML schema validation: " + filesToValidate.get(0) +
            (schemaErrors==null ? "" : String.join(",", schemaErrors));
        log.error(message);
        report.setStatus(message);
      }
    } else {
      String message = "ERROR: Supplied -mzid file is not a valid mzIdentML file: " + filesToValidate.get(0);
      log.error(message);
      report.setStatus(message);
    }
    outputReport(assayFileSummary, report, outputFile, cmd.hasOption(ARG_SKIP_SERIALIZATION));
  }

  /**
   * This method validates a PRIDE XML file.
   *
   * @param cmd the command line arguments.
   */
  private static void validatePrideXML(CommandLine cmd) {
    List<File> filesToValidate = new ArrayList<>();
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
    File outputFile  = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    if (fileType.equals(FileType.PRIDEXML)) {
      boolean valid = true; // assume true if not validating schema
      List<String> schemaErrors = null;
      if (cmd.hasOption(ARG_SCHEMA_VALIDATION) || cmd.hasOption(ARG_SCHEMA_ONLY_VALIDATION)) {
        SchemaCheckResult schemaCheckResult = validatePridexmlSchema(PRIDE_XML_SCHEMA, pridexxml);
        valid = schemaCheckResult .isValidAgainstSchema();
        schemaErrors = schemaCheckResult .getErrorMessages();
        log.debug("Schema errors: " + String.join(",", schemaErrors));
      }
      if (valid ) {
        if(cmd.hasOption(ARG_SCHEMA_ONLY_VALIDATION)) {
          report.setStatusOK();
        } else {
          ValidationResult validationResult= validateAssayFile(pridexxml, FileType.PRIDEXML, null);
          report = validationResult.getReport();
          assayFileSummary = validationResult.getAssayFileSummary();
        }
      } else {
        String message = "ERROR: Supplied -pridexml file failed XML schema validation: " + filesToValidate.get(0) + String.join(",", schemaErrors);
        log.error(message);
        report.setStatus(message);
      }
    } else {
      String message = "Supplied -pridexml file is not a valid PRIDE XML file: " + pridexxml.getAbsolutePath();
      log.error(message);
      report.setStatus(message);
    }
    outputReport(assayFileSummary, report, outputFile, cmd.hasOption(ARG_SKIP_SERIALIZATION));
  }


  /**
   * This method validates an mzTab file.
   *
   * @param cmd the command line arguments.
   */
  private static void validateMzTab(CommandLine cmd) {
    File file = new File(cmd.getOptionValue(ARG_MZTAB));
    List<File> filesToValidate = getFilesToValidate(file);
    List<File> peakFiles = getPeakFiles(cmd);
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    FileType fileType = getFileType(filesToValidate.get(0));
    if (fileType.equals(FileType.MZTAB)) {
      ValidationResult validationResult = validateAssayFile(filesToValidate.get(0), FileType.MZTAB, peakFiles);
      report = validationResult.getReport();
      assayFileSummary = validationResult.getAssayFileSummary();
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
   */
  private static List<File> getFilesToValidate(File file) {
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
   */
  private static List<File> getPeakFiles(CommandLine cmd) {
    List<File> peakFiles = new ArrayList<>();
    if (cmd.hasOption(ARG_PEAK) || cmd.hasOption(ARG_PEAKS)) {
      String[] peakFilesString = cmd.hasOption(ARG_PEAK) ? cmd.getOptionValues(ARG_PEAK)
          : cmd.hasOption(ARG_PEAKS) ?  cmd.getOptionValue(ARG_PEAKS).split(STRING_SEPARATOR) : new String[0];
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
   */
  public static List<File> extractZipFiles(List<File> files) {
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
   */
  private static List<File> unzipFiles(List<File> zippedFiles, File outputFolder) {
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
    String title = dataAccessController.getExperimentMetaData().getName();
    assayFileSummary.setName(StringUtils.isEmpty(title) || title.contains("no assay title provided") ?
        dataAccessController.getName() :
        title);
    assayFileSummary.setShortLabel(StringUtils.isEmpty(dataAccessController.getExperimentMetaData().getShortLabel()) ?
        "" :
        dataAccessController.getExperimentMetaData().getShortLabel());
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
    // check to see if we have instrument configurations in the result file to scan, this isn't always present
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
        // set instrument cv param
        uk.ac.ebi.pride.archive.repo.param.CvParam cvParam = new uk.ac.ebi.pride.archive.repo.param.CvParam();
        cvParam.setCvLabel(Constant.MS);
        cvParam.setName(Utility.MS_INSTRUMENT_MODEL_NAME);
        cvParam.setAccession(Utility.MS_INSTRUMENT_MODEL_AC);
        instrument.setCvParam(cvParam);
        instrument.setValue(instrumentConfiguration.getId());
        // build instrument components
        instrument.setSources(new ArrayList<>());
        instrument.setAnalyzers(new ArrayList<>());
        instrument.setDetectors(new ArrayList<>());
        int orderIndex = 1;
        // source
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
        // analyzer
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
        // detector
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
        instruments.add(instrument); //store instrument
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
    Set<Software> softwares = new HashSet<>(experimentMetaData.getSoftwares());
    Set<uk.ac.ebi.pride.archive.repo.assay.software.Software> softwareSet = new HashSet<>(DataConversionUtil.convertSoftware(softwares));
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
   */
  private static void scanRefIdControllerpecificDetails(ReferencedIdentificationController referencedIdentificationController, List<File> peakFiles, AssayFileSummary assayFileSummary) {
    log.info("Started scanning for mzid- or mztab-specific details");
    Set<PeakFileSummary> peakFileSummaries = new HashSet<>();
    List<String> peakFileNames = new ArrayList<>();
    for (File peakFile : peakFiles) {
      peakFileNames.add(peakFile.getName());
      String extension = FilenameUtils.getExtension(peakFile.getAbsolutePath());
      if (MassSpecFileFormat.MZML.toString().equalsIgnoreCase(extension)) {
        log.info("MzML summary: " + getMzMLSummary(peakFile, assayFileSummary));
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
   * @param assayFile the input assay file.
   * @return an array of objects[2]: a Report object and an AssayFileSummary, respectively.
   */
  private static ValidationResult validateAssayFile(File assayFile, FileType type, List<File> dataAccessControllerFiles) {
    File tempAssayFile = createNewTempFile(assayFile);
    List<File> tempDataAccessControllerFiles = new ArrayList<>();
    boolean badtempDataAccessControllerFiles = createTempDataAccessControllerFiles(dataAccessControllerFiles, tempDataAccessControllerFiles);
    log.info("Validating assay file: " + assayFile.getAbsolutePath());
    log.info("From temp file: " + tempAssayFile.getAbsolutePath());
    AssayFileSummary assayFileSummary = new AssayFileSummary();
    Report report = new Report();
    try {
      final ResultFileController assayFileController;
      switch(type) {
        case MZID :
          assayFileController = new MzIdentMLControllerImpl(tempAssayFile);
          assayFileController.addMSController(badtempDataAccessControllerFiles ? dataAccessControllerFiles : tempDataAccessControllerFiles);
          break;
        case PRIDEXML :
          assayFileController = new PrideXmlControllerImpl(tempAssayFile);
          break;
        case MZTAB : assayFileController = new MzTabControllerImpl(tempAssayFile);
          assayFileController.addMSController(badtempDataAccessControllerFiles ? dataAccessControllerFiles : tempDataAccessControllerFiles);
          break;
        default : log.error("Unrecognized assay fle type: " + type);
          assayFileController = new MzIdentMLControllerImpl(tempAssayFile);
          break;
      }
      checkSampleDeltaMzErrorRate(assayFileSummary, assayFileController);
      report.setFileName(assayFile.getAbsolutePath());
      assayFileSummary.setNumberOfIdentifiedSpectra(assayFileController.getNumberOfIdentifiedSpectra());
      assayFileSummary.setNumberOfPeptides(assayFileController.getNumberOfPeptides());
      assayFileSummary.setNumberOfProteins(assayFileController.getNumberOfProteins());
      assayFileSummary.setNumberofMissingSpectra(assayFileController.getNumberOfMissingSpectra());
      assayFileSummary.setNumberOfSpectra(assayFileController.getNumberOfSpectra());
      if (assayFileSummary.getNumberofMissingSpectra()<1) {
        validateProteinsAndPeptides(assayFile, assayFileSummary, assayFileController);
      } else {
        String message = "Missing spectra are present";
        log.error(message);
        report.setStatusError(message);
      }
      scanExtraMetadataDetails(type, dataAccessControllerFiles, assayFileSummary, assayFileController);
      if (StringUtils.isEmpty(report.getStatus())) {
        report.setStatusOK();
      }
    } catch (NullPointerException e) {
      log.error("Null pointer Exception when scanning assay file", e);
      report.setStatusError(e.getMessage());
    } finally {
      deleteAllTempFiles(tempAssayFile, tempDataAccessControllerFiles);
    }
    return new ValidationResult(assayFileSummary, report);
  }

  /**
   * Creates temp data access controller files.
   * @param dataAccessControllerFiles the input data access controller files
   * @param tempDataAccessControllerFiles the temp data acceess controller files that get created
   * @return true if all the temp files were created OK, false otherwise
   */
  private static boolean createTempDataAccessControllerFiles(List<File> dataAccessControllerFiles, List<File> tempDataAccessControllerFiles) {
    boolean badtempDataAccessControllerFiles = true;
    if (CollectionUtils.isNotEmpty(dataAccessControllerFiles)) {
      for (File dataAccessControllerFile : dataAccessControllerFiles) {
        File tempDataAccessControllerFile = createNewTempFile(dataAccessControllerFile);
        if (tempDataAccessControllerFile!=null && 0<tempDataAccessControllerFile.length()) {
          tempDataAccessControllerFiles.add(tempDataAccessControllerFile);
        }
      }
      badtempDataAccessControllerFiles = CollectionUtils.isEmpty(tempDataAccessControllerFiles) ||
          tempDataAccessControllerFiles.size()!=dataAccessControllerFiles.size();
    }
    return badtempDataAccessControllerFiles;
  }

  /**
   * Deletes all the temporary files (assay file, data access controller files).
   * @param tempAssayFile the temp assay file to be deleted
   * @param tempDataAccessControllerFiles the temp data access controller files to be deleted
   */
  private static void deleteAllTempFiles(File tempAssayFile, List<File> tempDataAccessControllerFiles) {
    deleteTempFile(tempAssayFile);
    if (CollectionUtils.isNotEmpty(tempDataAccessControllerFiles)) {
      for (File dataAccessControllerFile : tempDataAccessControllerFiles) {
        if (dataAccessControllerFile != null) {
          deleteTempFile(dataAccessControllerFile);
        }
      }
    }
  }

  /**
   * Checks a sampling of the delta m/z error rates.
   * @param assayFileSummary the assay file summary
   * @param assayFileController the assay file controller
   */
  private static void checkSampleDeltaMzErrorRate(AssayFileSummary assayFileSummary, ResultFileController assayFileController) {
    final int NUMBER_OF_CHECKS = 10;
    List<Boolean> randomChecks = new ArrayList<>();
    IntStream.range(1, NUMBER_OF_CHECKS).sequential().forEach(i -> randomChecks.add(assayFileController.checkRandomSpectraByDeltaMassThreshold(NUMBER_OF_CHECKS, 4.0)));
    int checkFalseCounts = 0;
    for (Boolean check : randomChecks) {
      if (!check) {
        checkFalseCounts++;
      }
    }
    assayFileSummary.setDeltaMzErrorRate(new BigDecimal(((double) checkFalseCounts / (NUMBER_OF_CHECKS*NUMBER_OF_CHECKS))).setScale(2, RoundingMode.HALF_UP).doubleValue());
  }

  /**
   * Scans for extra metadata details.
   * @param type the filetype
   * @param dataAccessControllerFiles the data access controller files
   * @param assayFileSummary the assay file summary
   * @param assayFileController the assay file controller
   */
  private static void scanExtraMetadataDetails(FileType type, List<File> dataAccessControllerFiles, AssayFileSummary assayFileSummary, ResultFileController assayFileController) {
    scanForGeneralMetadata(assayFileController, assayFileSummary);
    scanForInstrument(assayFileController, assayFileSummary);
    scanForSoftware(assayFileController, assayFileSummary);
    scanForSearchDetails(assayFileController, assayFileSummary);
    switch(type) {
      case MZID :
      case MZTAB :
        scanRefIdControllerpecificDetails((ReferencedIdentificationController) assayFileController, dataAccessControllerFiles, assayFileSummary);
        break;
      default : // do nothing
        break;
    }
  }

  /**
   * Validates across proteins and peptides for a given assay file
   * @param assayFile the assay file (e.g. .mzid file)
   * @param assayFileSummary the assay file summary
   * @param assayFileController the assay file controller (e.g. for mzIdentML etc).
   */
  private static void validateProteinsAndPeptides(File assayFile, AssayFileSummary assayFileSummary, ResultFileController assayFileController) throws NullPointerException {
    Set<String> uniquePeptides = new HashSet<>();
    Set<CvParam> ptms = new HashSet<>();
    for (Comparable proteinId : assayFileController.getProteinIds()) {
      for (Peptide peptide : assayFileController.getProteinById(proteinId).getPeptides()) {
        uniquePeptides.add(peptide.getSequence());
        for (Modification modification : peptide.getModifications()) {
          for (CvParam cvParam : modification.getCvParams()) {
            if (StringUtils.isEmpty(cvParam.getCvLookupID())|| StringUtils.isEmpty(cvParam.getAccession()) || StringUtils.isEmpty(cvParam.getName())) {
              String message = "A PTM CV Param's ontology, accession, or name is not defined properly: " + cvParam.toString() + " in file: " +  assayFile.getPath();
              log.error(message);
              throw new NullPointerException(message);
            }
            if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
              ptms.add(cvParam);
            }
          }
        }
      }
    }
    List<Boolean> matches = new ArrayList<>();
    matches.add(true);
    IntStream.range(
            1,
            (assayFileController.getNumberOfPeptides() < 100
                ? assayFileController.getNumberOfPeptides()
                : 100))
        .sequential()
        .forEach(
            i -> {
              Protein protein =
                  assayFileController.getProteinById(
                      assayFileController.getProteinIds().stream().findAny().orElse(null));
              Peptide peptide = null;
              if (protein != null) {
                peptide = protein.getPeptides().stream().findAny().orElse(null);
              } else {
                log.error("Unable to read a random protein.");
              }
              if (peptide != null) {
                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                  if (peptide.getSpectrum() != null) {
                    if (!matchingFragmentIons(peptide.getFragmentation(), peptide.getSpectrum())) {
                      matches.add(false);
                    }
                  }
                } else {
                  log.error("Unable to read peptide form protein: " + protein.toString());
                }
              }
            });
    assayFileSummary.addPtms(DataConversionUtil.convertAssayPTMs(ptms));
    assayFileSummary.setSpectrumMatchFragmentIons(matches.size() <= 1);
    assayFileSummary.setNumberOfUniquePeptides(uniquePeptides.size());
  }

  /**
   * Deletes a temporary file
   * @param tempFile the temp file to be deleted.
   */
  private static void deleteTempFile(File tempFile) {
    log.info("Deleting temp file " + tempFile.getName() + ": " + tempFile.delete());
  }

  /**
   * This method validates an input mzIdentML file according to the supplied schema, and writes the output to a file.
   *
   * @param schemaLocation the location of the schema
   * @param mzIdentML the input mzIdentML file.
   * @param outputFile the output log file with an OK message if there were no errors
   */
  private static void validateMzidSchema(String schemaLocation, File mzIdentML, File outputFile) {
    log.info("Validating mzIdentML XML schema for: " + mzIdentML.getPath() + " using schema: " + schemaLocation);
    ErrorHandlerIface handler = new ValidationErrorHandler();
    try (BufferedReader br = new BufferedReader(new FileReader(mzIdentML))) {
      GenericSchemaValidator genericValidator = new GenericSchemaValidator();
      genericValidator.setSchema(new URI(schemaLocation));
      genericValidator.setErrorHandler(handler);
      genericValidator.validate(br);
      log.info(SCHEMA_OK_MESSAGE + mzIdentML.getName());
      if (outputFile!=null) {
        Files.write(outputFile.toPath(), SCHEMA_OK_MESSAGE.getBytes());
      }
    } catch (IOException | SAXException e) {
      log.error("File Not Found or SAX Exception: ", e);
    } catch (URISyntaxException usi) {
      log.error("URI syntax exxception: ", usi);
    }
  }

  /**
   * This method validates an input mzIdentML file according to the supplied schema, and returns the outcome.
   *
   * @param schemaLocation the location of the schema
   * @param mzIdentML the input mzIdentML file.
   * @return a SchemaCheckResult - if the mzIdentML passed validation, validAgainstSchema will be true. False otherwise, and contains a list of the error messages.
   */
  private static SchemaCheckResult validateMzidSchema(String schemaLocation, File mzIdentML) {
    log.info("Validating mzIdentML XML schema for: " + mzIdentML.getPath() + " using schema: " + schemaLocation);
    SchemaCheckResult result = new SchemaCheckResult(false, new ArrayList<>());
    ValidationErrorHandler handler = new ValidationErrorHandler();
    try (BufferedReader br = new BufferedReader(new FileReader(mzIdentML))) {
      GenericSchemaValidator genericValidator = new GenericSchemaValidator();
      genericValidator.setSchema(new URI(schemaLocation));
      genericValidator.setErrorHandler(handler);
      genericValidator.validate(br);
      log.info(SCHEMA_OK_MESSAGE + mzIdentML.getName());
      List<String> errorMessages = handler.getErrorMessages();
      result.setValidAgainstSchema(errorMessages.size()<1);
      result.setErrorMessages(errorMessages);
    } catch (IOException | SAXException e) {
      log.error("Problem reading or parsing the file: ", e);
    } catch (URISyntaxException usi) {
      log.error("Unable to parse URI syntax: ", usi);
    }
    return result;
  }

  /**
   *
   * This method validates an input PRIDE XML file according to the supplied schema, and writes the output to a file.
   *
   * @param schemaLocation the location of the schema
   * @param pridexml the input PRIDE XML file.
   * @param outputFile the output log file with an OK message if there were no errors
   */
  private static void validatePridexmlSchema(String schemaLocation, File pridexml, File outputFile) {
    log.info("Validating PRIDE XML schema for: " + pridexml.getPath() + " using schema: " + schemaLocation);
    try {
      PrideXmlClValidator validator = new PrideXmlClValidator();
      validator.setSchema(new URL(schemaLocation));
      BufferedReader br = new BufferedReader(new FileReader(pridexml));
      XMLValidationErrorHandler xveh = validator.validate(br);
      final String ERROR_MESSAGES = xveh.getErrorsFormattedAsPlainText();
      if (StringUtils.isEmpty(ERROR_MESSAGES)) {
        log.info(SCHEMA_OK_MESSAGE + pridexml.getName());
        if (outputFile!=null) {
          Files.write(outputFile.toPath(), SCHEMA_OK_MESSAGE.getBytes());
        }
      } else {
        log.error(ERROR_MESSAGES);
        if (outputFile!=null) {
          Files.write(outputFile.toPath(), ERROR_MESSAGES.getBytes());
        }
      }
    } catch (IOException | SAXException e) {
      log.error("File Not Found or SAX Exception: ", e);
    } catch (Exception e) {
      log.error("Exception while validating PRIDE XML schema:", e);
    }
  }

  /**
   * This method validates an input mzIdentML file according to the supplied schema, and returns the outcome.
   *
   * @param schemaLocation the location of the schema
   * @param pridexml the input PRIDE XML file.
   * @return a list of two elements: the first element is a boolean (true or false) if the file passed validation. If false, the 2nd element in the list of the error messages.
   */
  private static SchemaCheckResult validatePridexmlSchema(String schemaLocation, File pridexml) {
    log.info("Validating PRIDE XML schema for: " + pridexml.getPath() + " using schema: " + schemaLocation);
    SchemaCheckResult result = new SchemaCheckResult(false, new ArrayList<>());
    try {
      PrideXmlClValidator validator = new PrideXmlClValidator();
      validator.setSchema(new URL(schemaLocation));
      BufferedReader br = new BufferedReader(new FileReader(pridexml));
      XMLValidationErrorHandler xveh = validator.validate(br);
      final String ERROR_MESSAGES = xveh.getErrorsFormattedAsPlainText();
      result.setValidAgainstSchema(StringUtils.isEmpty(ERROR_MESSAGES));
      if (StringUtils.isEmpty(ERROR_MESSAGES)) {
        log.info(SCHEMA_OK_MESSAGE + pridexml.getName());
      } else {
        log.error(ERROR_MESSAGES);
        result.setErrorMessages(xveh.getErrorsAsList());
      }
    } catch (Exception e) {
      log.error("Exception while validating PRIDE XML schema:", e);
    }
    return result;
  }

  /**
   * This method validates and input proBed file, checks its columns according to the BED column format, and potentially saves the output to a report file.
   * @param proBed the input proBed file.
   * @param columnFormat the BED column format, e.g the default BED12+13.
   * @param reportFile the file to save the output to.
   */
  private static void validateProBed(File proBed, String columnFormat, File reportFile, File asqlFile) {
    log.info("Validation proBed file: " + proBed.getPath() + " using column format: " + columnFormat);
    Report report = new Report();
    report.setFileName(proBed.getPath());
    Set<String> errorMessages = new HashSet<>();
    int defaultBedColumnCount = Integer.parseInt(columnFormat.substring(columnFormat.indexOf("D")+1, columnFormat.indexOf('+')));
    int proBedOptionalColumnsCount = Integer.parseInt(columnFormat.substring(columnFormat.indexOf("+")+1));
    List<AsqlTriple> asqlTriples = (asqlFile!=null ? extractDatatypesAsql(asqlFile) : null);
    try (Stream<String> stream = Files.lines(proBed.toPath())) {
      Set<String> uniqueNames = ConcurrentHashMap.newKeySet();
      stream.parallel().forEach(s -> validateProbeLine(errorMessages, defaultBedColumnCount, proBedOptionalColumnsCount, asqlTriples, uniqueNames, s));
      if (errorMessages.size()>0) {
        StringBuffer errorsReported = new StringBuffer();
        errorMessages.parallelStream().limit(100).forEach(s -> errorsReported.append(s).append("\n"));
        report.setStatus("ERROR: " + errorMessages.size() + " problems encountered. See below for (up to) the first 100 reported errors : \n" + errorsReported);
      } else {
        report.setStatusOK();
      }
      log.info(report.toString());
      if (reportFile!=null) {
        writeProbedReport(report, reportFile);
      }
    } catch (IOException e) {
      final String PROBED_IO_MESSAGE = "Error while reading proBed file.";
      log.error(PROBED_IO_MESSAGE + e);
      if (reportFile!=null) {
        report.setStatus(PROBED_IO_MESSAGE);
        writeProbedReport(report, reportFile);
      }
    }
  }

  /**
   * This method validates a line of a proBed file.
   * @param errorMessages a set of erro messages to record.
   * @param defaultBedColumnCount the default BED column count.
   * @param proBedOptionalColumnsCount the number of proBed extra columns.
   * @param asqlTriples  the ASQL triples constructed from the .AS file.
   * @param uniqueNames a running set of the unique names for the proBed file.
   * @param proBedLine the proBed line to validate.
   */
  private static void validateProbeLine(Set<String> errorMessages, int defaultBedColumnCount, int proBedOptionalColumnsCount, List<AsqlTriple> asqlTriples, Set<String> uniqueNames, String proBedLine) {
    if (org.apache.commons.lang3.StringUtils.isEmpty(proBedLine)) {
      logProbedError("Empty blank line encountered", errorMessages);
    } else {
      if (proBedLine.charAt(0)=='#') {
        log.info("Comment: " + proBedLine);
      } else {
        String[] fields = proBedLine.split("\\t");
        if (fields.length!=(defaultBedColumnCount+proBedOptionalColumnsCount)) {
          final int TOTAL_COLUMNS = defaultBedColumnCount+proBedOptionalColumnsCount;
          logProbedError("Incorrect number of columns found. Expected " + TOTAL_COLUMNS + " instead have : " + fields.length + "." + LINE_CONTENT + proBedLine, errorMessages);
        } else {
          if (isInvalidAsqlTriple(asqlTriples.get(0), fields[0])) {
            logProbedError("1st column 'chrom' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(1), fields[1])) {
            logProbedError("2nd column 'chromStart' " + FIELD_UNSIGNED_INTEGER + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(2), fields[2])) {
            logProbedError("3rd column 'chromEnd' " + FIELD_UNSIGNED_INTEGER + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            int chromStart = Integer.parseInt(fields[1]);
            int chromEnd = Integer.parseInt(fields[2]);
            if (chromEnd<chromStart) {
              logProbedError("2nd and 3rd columns 'chromStart' and 'chromEnd' fields must be in ascending order." + LINE_CONTENT + proBedLine, errorMessages);
            }
          }
          String name = fields[3];
          if (isInvalidAsqlTriple(asqlTriples.get(3), fields[3])) {
            logProbedError("4th column 'name' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            if (uniqueNames.contains(name)) {
              logProbedError("4th column 'name' field must be unique." + LINE_CONTENT + proBedLine, errorMessages);
            } else {
              uniqueNames.add(name);
            }
          }
          if (isInvalidAsqlTriple(asqlTriples.get(4), fields[4])) {
            logProbedError("5th column 'score' " + FIELD_UNSIGNED_INTEGER + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            int score = Integer.parseInt(fields[4]);
            if (score<0 || score >1000) {
              logProbedError("5th column 'score' field must be between 0 - 1000 inclusive." + LINE_CONTENT + proBedLine, errorMessages);
            }
          }
          if (isInvalidAsqlTriple(asqlTriples.get(5), fields[5]) || (!fields[5].equals("-") && !fields[5].equals("+"))) {
            logProbedError("6th column 'strand' field must not be empty and must be either '-' or '+'." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(6), fields[6])) {
            logProbedError("7th column 'thickStart' " + FIELD_UNSIGNED_INTEGER + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(7), fields[7])) {
            logProbedError("8th column 'thickEnd' " + FIELD_UNSIGNED_INTEGER + LINE_CONTENT + proBedLine, errorMessages);
          }
          int thickStart = Integer.parseInt(fields[6]);
          int thickEnd = Integer.parseInt(fields[7]);
          if (thickEnd<thickStart) {
            logProbedError("7th and 8th columns 'thickStart' and 'thickEnd' fields must be in ascending order." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(8), fields[8]) || (!fields[8].equals("0"))) {
            logProbedError("9th column 'reserved' field must not be empty and must be '0'. Line contnent: " + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(9), fields[9])) {
            logProbedError("10th column 'blockCount' field must be an integer contain at least one digit." + LINE_CONTENT + proBedLine, errorMessages);
          }
          int blockCount = Integer.parseInt(fields[9]);
          if (isInvalidAsqlTriple(asqlTriples.get(10), fields[10])) {
            logProbedError("11th column 'blockSizes' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            String blockSizes = fields[10];
            String[] blockSizesSplit = blockSizes.split(",");
            if (blockSizesSplit.length!=blockCount) {
              logProbedError("11th column 'blockSizes' field does not have the same amount of blocks as mentioned in 'blockCount'." + LINE_CONTENT + proBedLine, errorMessages);
            }
            for (String blockSizePart : blockSizesSplit) {
              if (org.apache.commons.lang3.StringUtils.isEmpty(blockSizePart) || !blockSizePart.matches("\\d+")) {
                logProbedError("11th column 'blockSizes' field must list at least one integer containing at least one digit, with multiple values separated by commas." + LINE_CONTENT + proBedLine, errorMessages);
              }
            }
          }
          if (isInvalidAsqlTriple(asqlTriples.get(11), fields[11])) {
            logProbedError("12th column 'chromStarts' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            String chromStarts = fields[11];
            String[] chromStartsSplit = chromStarts.split(",");
            if (chromStartsSplit.length!=blockCount) {
              logProbedError("12th column 'chromStarts' field does not have the same amount of blocks as mentioned in 'blockCount'." + LINE_CONTENT + proBedLine, errorMessages);
            }
            for (String chromStartsPart : chromStartsSplit) {
              if (org.apache.commons.lang3.StringUtils.isEmpty(chromStartsPart) || !chromStartsPart.matches("\\d+")) {
                logProbedError("12th column 'chromStarts' field must list at least one integer containing at least one digit, with multiple values separated by commas." + LINE_CONTENT + proBedLine, errorMessages);
              }
            }
          }
          if (isInvalidAsqlTriple(asqlTriples.get(12), fields[12])) {
            logProbedError("13th column 'proteinAccession' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(13), fields[13])) {
            logProbedError("14th column 'peptideSequence' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(14), fields[14]) ||
              (!fields[14].equals("unique") &&
                  !fields[14].equals("not-unique[same-set]") &&
                  !fields[14].equals("not-unique[subset]") &&
                  !fields[14].equals("not-unique[conflict]") &&
                  !fields[14].equals("not-unique[unknown]"))) {
            logProbedError("15th column 'uniqueness' field must not be empty and must be either: 1. not-unique[same-set], " +
                "2. not-unique[subset], 3. not-unique[conflict], or 4. not-unique[unknown]." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(15), fields[15])) {
            logProbedError("16th column 'genomeRefVersion' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(16), fields[16])) {
            logProbedError("17th column 'psmScore' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(17), fields[17])) {
            logProbedError("18th column 'fdr' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(18), fields[18])) {
            logProbedError("19th column 'modifications' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          } else {
            String modifications = fields[18];
            if (!modifications.equals(".")) {
              String[] modificationsArray = modifications.split(",");
              if (modificationsArray.length<1) {
                logProbedError("19th column 'modifications' field must either be '.' for no modifications, or contain modifications of the format like '5-UNIMOD:4'." + LINE_CONTENT + proBedLine, errorMessages);
              } else {
                for (String modification : modificationsArray) {
                  modification = modification.trim();
                  if (!modification.matches("\\d+-\\w+:\\d+")) {
                    logProbedError("19th column 'modifications' field must either be '.' for no modifications, or contain modifications of the format like '5-UNIMOD:4'." + LINE_CONTENT + proBedLine, errorMessages);
                  }
                }
              }
            }
          }
          if (isInvalidAsqlTriple(asqlTriples.get(19), fields[19])) {
            logProbedError("20th column 'charge' field must not be empty and must contain at least one digit." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(20), fields[20])) {
            logProbedError("21st column 'expMassToCharge' field must not be empty and must contain at least one digit." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(21), fields[21])) {
            logProbedError("22nd column 'calcMassToCharge' field must not be empty and must contain at least one digit." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(22), fields[22])) {
            logProbedError("23rd column 'psmRank' field must not be empty and must contain at least one digit." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(23), fields[23])) {
            logProbedError("24th column 'datasetID' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
          if (isInvalidAsqlTriple(asqlTriples.get(24), fields[24])) {
            logProbedError("25th column 'uri' field must not be empty." + LINE_CONTENT + proBedLine, errorMessages);
          }
        }
      }
    }
  }

  /**
   * This method starts the validation of a proBed file according to the input command line arguments.
   * @param cmd command line arguments.

   */
  private static void validateProBed(CommandLine cmd) {
    File proBed = new File(cmd.getOptionValue(ARG_PROBED));
    String COLUMN_FORMAT = cmd.hasOption(ARG_BED_COLUMN_FORMAT) ? cmd.getOptionValue(ARG_BED_COLUMN_FORMAT) : "BED12+13";
    File REPORT_FILE = cmd.hasOption(ARG_REPORTFILE) ? new File(cmd.getOptionValue(ARG_REPORTFILE)) : null;
    File ASQL_FILE = null;
    if (cmd.hasOption(ARG_ASQLFILE)) {
      new File(cmd.getOptionValue(ARG_ASQLFILE));
    } else {
      URL url = Validator.class.getClassLoader().getResource("probed-1.0.0.as");
      if (url == null) {
        log.error("Unable to read default proBed ASQL schema file!");
      } else {
        try {
          File tempAs = File.createTempFile("probed_default", ".as");
          tempAs.deleteOnExit();
          FileUtils.copyURLToFile(url, tempAs);
          ASQL_FILE = tempAs;
        } catch (IOException e) {
          log.error("Unable to read default proBed ASQL schema file!", e);
        }
      }
    }
    validateProBed(proBed, COLUMN_FORMAT, REPORT_FILE, ASQL_FILE);
  }

  /**
   * This method logs the proBed errors to the error log, and to a Set for them to be iterated over.
   * @param errorMessage the proBed error message.
   * @param errors the Set of errors for the message to be added to.
   */
  private static void logProbedError(String errorMessage, Set<String> errors) {
    log.error(errorMessage);
    errors.add(errorMessage);
  }

  /**
   * This method writes the proBed report to a file.
   * @param report the proBed report
   * @param reportFile the file to write the report to.
   */
  private static void writeProbedReport(Report report, File reportFile) {
    try {
      Files.write(reportFile.toPath(), report.toString().getBytes());
    } catch (Exception e) {
      log.error("Error trying to write to report file: " + reportFile.getPath());
    }
  }

  /**
   * This method checks if a field is allowed to be null or not.
   * @param field the field to check.
   * @param nullable if the field is allowed to be null.
   * @return true if nullable, flase otherwise.
   */
  private static boolean validProbedFieldNullable(String field, boolean nullable) {
    return (field!=null && !field.equalsIgnoreCase(".")) || nullable;
  }

  /**
   * This method checks if a field is a non-empty String.
   * @param field the field to check.
   * @return true if the field is not null or empty, false otherwise
   */
  private static boolean validProbedFieldString(String field) {
    return (!org.apache.commons.lang3.StringUtils.isEmpty(field));
  }

  /**
   * This method checks if a field is an integer.
   * @param field the field to check.
   * @return true if the field is an integer, false otherwise.
   */
  private static boolean validProbedFieldInteger(String field) {
    boolean result = true;
    if (org.apache.commons.lang3.StringUtils.isEmpty(field) || !field.matches(".*\\d+.*")) {
      result = false;
    } else {
      try {
        int integer = Integer.parseInt(field);
        log.debug("Integer OK: " + integer);
      } catch (NumberFormatException nfe) {
        log.error("Unable to cast field to an integer.", nfe);
        result = false;
      }
    }
    return result;
  }
  /**
   * This method checks if a field is an unsigned integer.
   * @param field the field to check.
   * @return true if the field is an unsigned integer, false otherwise.
   */
  private static boolean validProbedFieldUnsignedInteger(String field) {
    return !org.apache.commons.lang3.StringUtils.isEmpty(field) && field.matches(".*\\d+.*") && !field.contains("-") && validProbedFieldInteger(field);
  }

  /**
   * This method checks if a field is a double.
   * @param field the field to check.
   * @return true if the field is a double, false otherwise.
   */
  private static boolean validProbedFieldDouble(String field) {
    boolean result = true;
    if (org.apache.commons.lang3.StringUtils.isEmpty(field) || !field.matches(".*\\d+.*")) {
      result = false;
    } else {
      try {
        double doubleNumber = Double.parseDouble(field);
        log.debug("Field is a double: " + doubleNumber);
      } catch (NumberFormatException nfe) {
        log.error("Unable to cast field to a double.", nfe);
        result = false;
      }
    }
    return result;
  }

  /**
   * This method checks if a field is a character.
   * @param field the field to check.
   * @return true if the field is a character, false otherwise.
   */
  private static boolean validProbedFieldCharacter(String field) {
    return field.length()==1;
  }

  /**
   * This method extracts all the data type information from an ASQL file.
   * @param asqlFile The input .as file.
   * @return A List of AsqlTriple objects of BED field information, in the order they were specified in the .as file.
   */
  private static List<AsqlTriple> extractDatatypesAsql(File asqlFile) {
    List<AsqlTriple> result = new ArrayList<>();
    try {
      List<String> lines = Files.readAllLines(asqlFile.toPath());
      String line;
      AsqlDataType asqlDataType = null;
      String asqlName;
      String asqlDesc;
      if (lines.size()>4) {
        for (int i=3; i<lines.size()-1; i++) {
          line = lines.get(i);
          line = line.replace(";", "");
          String[] parts = line.split(" {2}", 3);
          if (parts.length==3) {
            for (AsqlDataType asqlDataTypeToCheck : AsqlDataType.values()) {
              if (asqlDataTypeToCheck.toString().equals(parts[0])) {
                asqlDataType = asqlDataTypeToCheck;
                break;
              }
            }
            if (asqlDataType==null) {
              log.error("ASQL data type has not been set! " + line);
            }
            asqlName = parts[1];
            asqlDesc = parts[2];
            result.add(new AsqlTriple(asqlDataType, asqlName, asqlDesc));
          } else {
            log.error("aSQL has a line without 3 parts to it, unable to parse properly: " + asqlFile.getPath() + "\n" + lines.get(i));
          }
        }
      } else {
        log.error("aSQL is too short, unable to parse properly: " + asqlFile.getPath());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * This method validates a field's value according to the AsqlTriple information for the data type.
   * @param asqlTriple the information about the data type.
   * @param value the value to be checked.
   * @return true if the value is OK, false otherwise.
   */
  private static boolean isInvalidAsqlTriple(AsqlTriple asqlTriple, String value) {
    boolean result = false;
    switch (asqlTriple.getAsqlDataType()) {
      case STRING:
        result = validProbedFieldString(value);
        break;
      case INT:
        result = validProbedFieldInteger(value);
        break;
      case UINT:
        result = validProbedFieldUnsignedInteger(value);
        break;
      case CHAR_ONE:
        result = validProbedFieldCharacter(value);
        break;
      case INT_BLOCKCOUNT:
        result = validProbedFieldString(value); // needs to be validated in relation to the 'blockcount' field's value, handled elsewhere
        break;
      case DOUBLE:
        result = validProbedFieldDouble(value);
        break;
      default:
        log.error("Unrecognized ASQL data type: " + asqlTriple.getAsqlDataType());
    }
    return !result;
  }
}





