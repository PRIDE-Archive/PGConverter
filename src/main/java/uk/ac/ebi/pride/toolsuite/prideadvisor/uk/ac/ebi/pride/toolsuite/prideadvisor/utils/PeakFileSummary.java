package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils;

import java.io.Serializable;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class PeakFileSummary implements Serializable {
    private String fileName;
    private boolean missing;
    private int numberOfSpectra;

    public PeakFileSummary(String fileName, boolean missing, int numberOfSpectra) {
        this.fileName = fileName;
        this.missing = missing;
        this.numberOfSpectra = numberOfSpectra;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public int getNumberOfSpectra() {
        return numberOfSpectra;
    }

    public void setNumberOfSpectra(int numberOfSpectra) {
        this.numberOfSpectra = numberOfSpectra;
    }
}
