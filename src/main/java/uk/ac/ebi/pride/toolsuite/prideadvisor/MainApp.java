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
            }
        }
    }

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
        options.addOption(ARG_REDIS, false, "Will message redis");
        options.addOption(ARG_REDIS_SERVER, true, "Redis server");
        options.addOption(ARG_REDIS_PORT, true, "Redis port");
        options.addOption(ARG_REDIS_PASSWORD, true, "Redis password");
        options.addOption(ARG_REDIS_CHANNEL, true, "Redis channel");
        options.addOption(ARG_REDIS_MESSAGE, true, "Redis message");

        CommandLineParser parser = new DefaultParser();
        return parser.parse( options, args);
    }
}
