package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.pride.archive.repo.assay.AssayGroupCvParam;
import uk.ac.ebi.pride.archive.repo.assay.AssayGroupUserParam;
import uk.ac.ebi.pride.archive.repo.assay.AssayPTM;
import uk.ac.ebi.pride.archive.repo.assay.Contact;
import uk.ac.ebi.pride.archive.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.archive.repo.assay.software.Software;
import uk.ac.ebi.pride.archive.repo.assay.software.SoftwareCvParam;
import uk.ac.ebi.pride.archive.repo.assay.software.SoftwareUserParam;

import java.util.*;

/**
 * Created by tobias on 02/08/2016.
 */
public class Report {
    private String status = "";
    private String fileName = "";
    private String name = "";
    private String shortLabel = "";
    private Set<Contact> contacts = new HashSet<>();
    private int totalProteins = 0;
    private int totalPeptides = 0;
    private int totalSpecra = 0;
    private Set<AssayPTM> uniquePTMs = new HashSet<>();
    private int deltaMzPercent = 0;
    private int identifiedSpectra = 0;
    private int missingIdSpectra = 0;
    private boolean matchFragIons = false;
    private int uniquePeptides = 0;
    private Set<Instrument> instruments = new HashSet<>();
    private Set<Software> softwareSet = new HashSet<>();
    private String searchDatabase = "";
    private String exampleProteinAccession = "";
    private boolean proteinGroupPresent = false;
    private Set<AssayGroupCvParam> cvParams = new HashSet<>();
    private Set<AssayGroupUserParam> userParams = new HashSet<>();
    private boolean chromatogram = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<AssayGroupUserParam> getUserParams() {
        return userParams;
    }

    public Set<AssayGroupCvParam> getCvParams() {
        return cvParams;
    }

    public boolean isChromatogram() {
        return chromatogram;
    }

    public void setChromatogram(boolean chromatogram) {
        this.chromatogram = chromatogram;
    }

    public Set<PeakFileSummary> getPeakFileSummaries() {
        return peakFileSummaries;
    }

    public void setPeakFileSummaries(Set<PeakFileSummary> peakFileSummaries) {
        this.peakFileSummaries = peakFileSummaries;
    }

    private Set<PeakFileSummary> peakFileSummaries;

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

    public Set<AssayPTM> getUniquePTMs() {
        return uniquePTMs;
    }

    public void setUniquePTMs(Set<AssayPTM> uniquePTMs) {
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

    public void setMatchFragIons(boolean matchFragIons) {
        this.matchFragIons = matchFragIons;
    }

    public int getUniquePeptides() {
        return uniquePeptides;
    }

    public void setUniquePeptides(int uniquePeptides) {
        this.uniquePeptides = uniquePeptides;
    }

    public Set<Instrument> getInstruments() {
        return instruments;
    }

    public void setInstruments(Set<Instrument> instruments) {
        this.instruments = instruments;
    }

    public Set<Software> getSoftwareSet() {
        return softwareSet;
    }

    public void setSoftwareSet(Set<Software> softwareSet) {
        this.softwareSet = softwareSet;
    }

    public String getSearchDatabase() {
        return searchDatabase;
    }

    public void setSearchDatabase(String searchDatabase) {
        this.searchDatabase = searchDatabase;
    }

    public String getExampleProteinAccession() {
        return exampleProteinAccession;
    }

    public void setExampleProteinAccession(String exampleProteinAccession) {
        this.exampleProteinAccession = exampleProteinAccession;
    }

    public boolean isProteinGroupPresent() {
        return proteinGroupPresent;
    }

    public void setProteinGroupPresent(boolean proteinGroupPresent) {
        this.proteinGroupPresent = proteinGroupPresent;
    }

    public void addCvParams(Collection<AssayGroupCvParam> cvParams) {
        this.cvParams.addAll(cvParams);
    }

    public void addUserParams(Collection<AssayGroupUserParam> userParams) {
        this.userParams.addAll(userParams);
    }

    public Report() {

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: " + status);
        sb.append('\n');
        sb.append("FileName: " + fileName);
        sb.append('\n');
        sb.append("Name: " + fileName);
        sb.append('\n');
        sb.append("Shortlabel: " + shortLabel);
        sb.append('\n');
        sb.append("Contacts: " + contactsToString());
        sb.append('\n');
        sb.append("Instruments: " + instrumentsToString());
        sb.append('\n');
        sb.append("Software: " + softwareToString());
        sb.append('\n');
        sb.append("SearchDatabase: " + searchDatabase);
        sb.append('\n');
        sb.append("ExampleProteinAccession: " + exampleProteinAccession);
        sb.append('\n');
        sb.append("ProteinGroupPresent: " + proteinGroupPresent);
        sb.append('\n');
        sb.append("Assay Group CvParams: " + cvParamsToString());
        sb.append('\n');
        sb.append("Assay Group UserParams: " + userParamsToString());
        sb.append('\n');
        sb.append("Chromatogram: " + chromatogram);
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
        sb.append("Total unique PTMs: " + uniquePTMstoString());
        sb.append('\n');
        sb.append("Delta m/z: " + deltaMzPercent + "%");
        sb.append('\n');
        sb.append("Match fragment ions: " + matchFragIons);
        sb.append('\n');
        return sb.toString();
    }

    public String toString(AssayFileSummary assayFileSummary) {
        name = assayFileSummary.getName();
        shortLabel = assayFileSummary.getShortLabel();
        contacts = assayFileSummary.getContacts();
        instruments = assayFileSummary.getInstruments();
        softwareSet = assayFileSummary.getSoftwares();
        searchDatabase = assayFileSummary.getSearchDatabase();
        exampleProteinAccession = assayFileSummary.getExampleProteinAccession();
        proteinGroupPresent = assayFileSummary.isProteinGroupPresent();
        cvParams = assayFileSummary.getCvParams();
        userParams = assayFileSummary.getUserParams();
        chromatogram = assayFileSummary.hasChromatogram();
        totalProteins = assayFileSummary.getNumberOfProteins();
        totalPeptides = assayFileSummary.getNumberOfPeptides();
        uniquePeptides = assayFileSummary.getNumberOfUniquePeptides();
        totalSpecra = assayFileSummary.getNumberOfSpectra();
        identifiedSpectra = assayFileSummary.getNumberOfIdentifiedSpectra();
        missingIdSpectra = assayFileSummary.getNumberofMissingSpectra();
        uniquePTMs = assayFileSummary.getPtms();
        deltaMzPercent = new Double(assayFileSummary.getDeltaMzErrorRate()*100.0).intValue();
        matchFragIons = assayFileSummary.isSpectrumMatchFragmentIons();
        return this.toString();
    }

    private String contactsToString() {
        List<String> result = new ArrayList<>();
        contacts.stream().forEach(person -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(person.getTitle());
            sb.append(",");
            sb.append(person.getFirstName());
            sb.append(",");
            sb.append(person.getLastName());
            sb.append(",");
            sb.append(person.getAffiliation());
            sb.append(",");
            sb.append(person.getEmail());
            sb.append("]");
            result.add(sb.toString());
        });
        return StringUtils.join(result, ",");
    }

    private String instrumentsToString() {
        List<String> result = new ArrayList<>();
        instruments.stream().forEachOrdered(instrument -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(instrument.getCvParam().getCvLabel());
            sb.append(",");
            sb.append(instrument.getCvParam().getName());
            sb.append(",");
            sb.append(instrument.getCvParam().getAccession());
            sb.append("]");
            result.add(sb.toString());
        });
        return StringUtils.join(result, ",");
    }

    private String uniquePTMstoString() {
        List<String> result = new ArrayList<>();
        uniquePTMs.stream().forEachOrdered(cvParam -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(cvParam.getCvLabel());
            sb.append(",");
            sb.append(cvParam.getName());
            sb.append(",");
            sb.append(cvParam.getAccession());
            sb.append(",");
            sb.append(cvParam.getValue());
            sb.append("]");
            result.add(sb.toString());
        });
        return StringUtils.join(result, ",");
    }

    private String softwareToString() {
        List<String> result = new ArrayList<>();
        softwareSet.stream().forEachOrdered(software -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(software.getName());
            sb.append(",");
            sb.append(software.getOrder());
            sb.append(",");
            sb.append(software.getVersion());
            sb.append(",");
            sb.append(software.getCustomization());
            sb.append(",");
            sb.append("{");
            List<String> softwareCvPs = new ArrayList<>();
            for (SoftwareCvParam softwareCvParam : software.getSoftwareCvParams()) {
                StringBuilder cvp = new StringBuilder();
                cvp.append("[");
                cvp.append(softwareCvParam.getCvParam().getCvLabel());
                cvp.append(",");
                cvp.append(softwareCvParam.getCvParam().getName());
                cvp.append(",");
                cvp.append(softwareCvParam.getCvParam().getAccession());
                cvp.append(",");
                cvp.append(softwareCvParam.getCvParam().getValue());
                cvp.append("]");
                softwareCvPs.add(cvp.toString());
            }
            if (softwareCvPs.size()>0) {
                StringUtils.join(softwareCvPs, ",");
            }
            sb.append("}");
            sb.append(",");
            sb.append("{");
            List<String> softwareUserPs = new ArrayList<>();
            for (SoftwareUserParam softwareUserParam : software.getSoftwareUserParams()) {
                StringBuilder userp = new StringBuilder();
                userp.append("[");
                userp.append(softwareUserParam.getName());
                userp.append(",");
                userp.append(softwareUserParam.getValue());
                userp.append("]");
                softwareUserPs.add(userp.toString());
            }
            if (softwareUserPs.size()>0) {
                StringUtils.join(softwareUserPs, ",");
            }
            sb.append("}");
            sb.append("]");
            result.add(sb.toString());
        });
        return StringUtils.join(result, ",");
    }

    private String cvParamsToString() {
        StringBuilder sb = new StringBuilder();
        List<String> assayCvPs = new ArrayList<>();
        sb.append("{");
        cvParams.stream().forEachOrdered(assayGroupCvParam -> {
            StringBuilder cvpsb = new StringBuilder();
            cvpsb.append("[");
            cvpsb.append(assayGroupCvParam.getCvLabel());
            cvpsb.append(",");
            cvpsb.append(assayGroupCvParam.getName());
            cvpsb.append(",");
            cvpsb.append(assayGroupCvParam.getAccession());
            cvpsb.append(",");
            cvpsb.append(assayGroupCvParam.getValue());
            cvpsb.append("]");
            assayCvPs.add(cvpsb.toString());
        });
        sb.append(StringUtils.join(assayCvPs, ","));
        sb.append("}");
        return sb.toString();
    }

    private String userParamsToString() {
        StringBuilder sb = new StringBuilder();
        List<String> assayCvPs = new ArrayList<>();
        sb.append("{");
        userParams.stream().forEachOrdered(assayGroupUserParam -> {
            StringBuilder cvpsb = new StringBuilder();
            cvpsb.append("[");
            cvpsb.append(assayGroupUserParam.getName());
            cvpsb.append(",");
            cvpsb.append(assayGroupUserParam.getValue());
            cvpsb.append("]");
            assayCvPs.add(cvpsb.toString());
        });
        sb.append(StringUtils.join(assayCvPs, ","));
        sb.append("}");
        return sb.toString();
    }
}
