package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tobias on 03/08/2016.
 */
public class Utility {

    private static final Logger log = LoggerFactory.getLogger(Utility.class);

    public static final String ARG_VALIDATION = "v";
    public static final String ARG_CONVERSION = "c";
    public static final String ARG_MZID = "mzid";
    public static final String ARG_PEAK = "peak";
    public static final String ARG_PRIDEXML = "pridexml";
    public static final String ARG_MZTAB = "mztab";
    public static final String ARG_PROBED = "probed";
    public static final String ARG_BIGBED = "bigbed";
    public static final String ARG_OUTPUTFILE = "outputfile";
    public static final String ARG_OUTPUTTFORMAT = "outputformat";

    public enum FileType {MZID, MZTAB, PRIDEXML, UNKNOWN};

    public static void exit() {
        log.info("Exiting application.");
        System.exit(1);
    }

}
