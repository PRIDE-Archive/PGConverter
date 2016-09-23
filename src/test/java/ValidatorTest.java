import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.pgconverter.MainApp;
import uk.ac.ebi.pride.toolsuite.pgconverter.Validator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.pride.toolsuite.pgconverter.utils.Utility.*;

/**
 * This class contains unit tests for file format validation.
 *
 * @author Tobias Ternent
 */

public class ValidatorTest {

  /**
   * This test validated one example mzIdentML file which is related to a single peak .mgf file.
   *
   * @throws Exception if there are problems opening the example file.
   */
  @Test
  public void testMzidValidator() throws Exception{
    URL url = ConverterTest.class.getClassLoader().getResource("test.mzid");
    if (url == null) {
      throw new IllegalStateException("no file for input found!");
    }
    File inputMzidFile = new File(url.toURI());
    url = ConverterTest.class.getClassLoader().getResource("test.mgf");
    if (url == null) {
      throw new IllegalStateException("no file for input found!");
    }
    File inputMgfFile = new File(url.toURI());
    File reportFile = File.createTempFile("testMzid", ".log");
    String[] args = new String[]{"-" + ARG_VALIDATION, "-" + ARG_MZID, inputMzidFile.getPath(), "-" + ARG_PEAK, inputMgfFile.getPath(), "-" + ARG_SKIP_SERIALIZATION, "-" + ARG_REPORTFILE , reportFile.getPath()};
    Validator.startValidation(MainApp.parseArgs(args));
    assertTrue("No errors reported during the conversion from  mzIdentML to MzTab", reportStatus(reportFile));
  }

  /**
   * This test validated one example PRIDE XML file.
   *
   * @throws Exception if there are problems opening the example file.
   */
  @Test
  public void testPridexmlValidator() throws Exception{
    URL url = ConverterTest.class.getClassLoader().getResource("test.xml");
    if (url == null) {
      throw new IllegalStateException("no file for input found!");
    }
    File inputPridexmlFile = new File(url.toURI());
    File reportFile = File.createTempFile("testPridexml", ".log");
    String[] args = new String[]{"-" + ARG_VALIDATION, "-" + ARG_PRIDEXML, inputPridexmlFile.getPath(), "-" + ARG_SKIP_SERIALIZATION, "-" + ARG_REPORTFILE , reportFile.getPath()};
    Validator.startValidation(MainApp.parseArgs(args));
    assertTrue("No errors reported during the conversion from mzIdentML to MzTab", reportStatus(reportFile));
  }

  private boolean reportStatus(File report) throws Exception{
    boolean result = false;
    List<String> reportLines = new ArrayList<>();
    Stream<String> stream = Files.lines(report.toPath(), Charset.defaultCharset());
    stream.forEach(reportLines::add);
    for (String reportLine : reportLines) {
      String[] parts = reportLine.split(": ");
      if (parts.length>0) {
        String key = parts[0];
        String content = parts.length>1 ? parts[1] : "";
        switch (key) {
          case "Status" :
            if (content.contains("ERROR")) {
              //result = false;
              throw new IOException(content);
            } else if (content.contains("OK")) {
              result = true;
            }
            break;
          default:
            break;
        }
      }
    }
    return result;
  }

  //TODO proBed validation, mzTab validation?
}
