import org.junit.Test;
import uk.ac.ebi.pride.toolsuite.pgconverter.Converter;
import uk.ac.ebi.pride.toolsuite.pgconverter.MainApp;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzTabControllerImpl;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.pride.toolsuite.pgconverter.utils.Utility.*;

/**
 * This class contains unit tests for file format conversion.
 *
 * @author Tobias Ternent
 */

public class ConverterTest {

  /**
   * This test converts an example mzIdentML file into an mzTab file.
   * @throws Exception if there are problems opening the example file.
   */
  @Test
  public void testConvertMzidToMztab() throws Exception{
    URL url = ConverterTest.class.getClassLoader().getResource("test.mzid");
    if (url == null) {
      throw new IllegalStateException("no file for input found!");
    }
    File inputFile = new File(url.toURI());
    File outputFile = File.createTempFile("test", ".mztab");
    String[] args = new String[]{"-" + ARG_CONVERSION, "-" + ARG_INPUTFILE, inputFile.getPath(), "-" + ARG_OUTPUTFILE, outputFile.getPath()};
    Converter.startConversion(MainApp.parseArgs(args));

    MzTabControllerImpl mzTabController = new MzTabControllerImpl(outputFile);
    mzTabController.close();
    assertTrue("No errors reported during the conversion from  mzIdentML to MzTab", outputFile.exists());
  }

  //TODO PRIDE XML to mzTab validation, mzTab to proBed validation? mzIdentML to proBed validation?
}
