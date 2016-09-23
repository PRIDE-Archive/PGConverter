import org.junit.Test;
import uk.ac.ebi.pride.toolsuite.pgconverter.MainApp;
import uk.ac.ebi.pride.toolsuite.pgconverter.Validator;

import java.io.File;
import java.net.URL;

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
    File reportFile = File.createTempFile("test", ".log");
    String[] args = new String[]{"-" + ARG_VALIDATION, "-" + ARG_MZID, inputMzidFile.getPath(), "-" + ARG_PEAK, inputMgfFile.getPath(), "-" + ARG_SKIP_SERIALIZATION, "-" + ARG_REPORTFILE , reportFile.getPath()};
    Validator.startValidation(MainApp.parseArgs(args));
    assertTrue("No errors reported during the conversion from  mzIdentML to MzTab", reportFile.exists());
  }

  //TODO PRIDE XML validation, proBed validation, mzTab validation?
}
