package javafx.test;

/**
 * Created by tobias on 02/08/2016.
 */
public class Report {
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalProteins() {
        return totalProteins;
    }

    public void setTotalProteins(int totalProteins) {
        this.totalProteins = totalProteins;
    }

    public int getTotalPeptides() {
        return totalPeptides;
    }

    public void setTotalPeptides(int totalPeptides) {
        this.totalPeptides = totalPeptides;
    }

    public int getTotalSpecra() {
        return totalSpecra;
    }

    public void setTotalSpecra(int totalSpecra) {
        this.totalSpecra = totalSpecra;
    }

    public int getUniquePTMs() {
        return uniquePTMs;
    }

    public void setUniquePTMs(int uniquePTMs) {
        this.uniquePTMs = uniquePTMs;
    }

    public int getDeltaMzPercent() {
        return deltaMzPercent;
    }

    public void setDeltaMzPercent(int deltaMzPercent) {
        this.deltaMzPercent = deltaMzPercent;
    }

    public int getIdentifiedSpectra() {
        return identifiedSpectra;
    }

    public void setIdentifiedSpectra(int identifiedSpectra) {
        this.identifiedSpectra = identifiedSpectra;
    }

    public int getMissingIdSpectra() {
        return missingIdSpectra;
    }

    public void setMissingIdSpectra(int missingIdSpectra) {
        this.missingIdSpectra = missingIdSpectra;
    }

    public boolean isMatchFragIons() {
        return matchFragIons;
    }

    public int getUniquePeptides() {
        return uniquePeptides;
    }

    public void setUniquePeptides(int uniquePeptides) {
        this.uniquePeptides = uniquePeptides;
    }


    public void setMatchFragIons(boolean matchFragIons) {
        this.matchFragIons = matchFragIons;
    }

    private String fileName;
    private int totalProteins;
    private int totalPeptides;
    private int totalSpecra;
    private int uniquePTMs;
    private int deltaMzPercent;
    private int identifiedSpectra;
    private int missingIdSpectra;
    private boolean matchFragIons;
    private int uniquePeptides;


    public Report() {
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append("Total proteins: " + totalProteins);
        sb.append('\n');
        sb.append("Total peptides: " + totalPeptides);
        sb.append('\n');
        sb.append("Total unique peptides: " + uniquePeptides);
        sb.append('\n');
        sb.append("Total spectra: " + totalSpecra);
        sb.append('\n');
        sb.append("Total identified spectra: " + identifiedSpectra);
        sb.append('\n');
        sb.append("Total missing spectra: " + missingIdSpectra);
        sb.append('\n');
        sb.append("Total unique PTMs: " + uniquePTMs);
        sb.append('\n');
        sb.append("Delta m/z: " + deltaMzPercent + "%");
        sb.append('\n');
        sb.append("Match fragment ions: " + matchFragIons);
        sb.append('\n');
        return sb.toString();
    }
}
