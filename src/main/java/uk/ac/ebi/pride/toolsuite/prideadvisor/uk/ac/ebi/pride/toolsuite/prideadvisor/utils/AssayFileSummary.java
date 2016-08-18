package uk.ac.ebi.pride.toolsuite.prideadvisor.uk.ac.ebi.pride.toolsuite.prideadvisor.utils;

import uk.ac.ebi.pride.archive.repo.assay.*;
import uk.ac.ebi.pride.archive.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.archive.repo.assay.software.Software;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class AssayFileSummary implements Serializable {
        private int id;
        private String accession;
        private String name;
        private String shortLabel;
        private int numberOfProteins;
        private int numberOfPeptides;
        private int numberOfSpectra;
        private int numberOfUniquePeptides;
        private int numberOfExistingIdentifiedSpectra;
        private int numberOfIdentifiedSpectra;
        private int numberofMissingSpectra;
        private boolean spectrumMatchFragmentIons;
        private final Set<Comparable> missingIdentifiedSpectraIds;
        private boolean ms2Annotation;
        private boolean chromatogram;
        private final Set<AssayPTM> ptms;
        private final Set<Instrument> instruments;
        private final Set<Software> softwares;
        private final Set<Contact> contacts;
        private boolean proteinGroupPresent;
        private String exampleProteinAccession;
        private String searchDatabase;
        private double deltaMzErrorRate;
        private final Set<AssaySampleCvParam> samples;
        private final Set<AssayQuantificationMethodCvParam> quantificationMethods;
        private String experimentalFactor;
        private final Set<PeakFileSummary> peakFileSummaries;
        private final Set<AssayGroupCvParam> cvParams;
        private final Set<AssayGroupUserParam> userParams;

        public AssayFileSummary() {
            this.id = -1;
            this.accession = null;
            this.name = null;
            this.shortLabel = null;
            this.numberOfProteins = 0;
            this.numberOfPeptides = 0;
            this.numberOfSpectra = 0;
            this.numberOfUniquePeptides = 0;
            this.numberOfExistingIdentifiedSpectra = 0;
            this.numberOfIdentifiedSpectra = 0;
            this.numberofMissingSpectra = -1;
            this.spectrumMatchFragmentIons = true;
            this.missingIdentifiedSpectraIds = new LinkedHashSet<>();
            this.ms2Annotation = false;
            this.chromatogram = false;
            this.ptms = new LinkedHashSet<>();
            this.instruments = new LinkedHashSet<>();
            this.softwares = new LinkedHashSet<>();
            this.contacts = new LinkedHashSet<>();
            this.proteinGroupPresent = false;
            this.samples = new LinkedHashSet<>();
            this.quantificationMethods = new LinkedHashSet<>();
            this.experimentalFactor = null;
            this.peakFileSummaries = new LinkedHashSet<>();
            this.cvParams = new LinkedHashSet<>();
            this.userParams = new LinkedHashSet<>();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getAccession() {
            return accession;
        }

        public void setAccession(String accession) {
            this.accession = accession;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortLabel() {
            return shortLabel;
        }

        public void setShortLabel(String shortLabel) {
            this.shortLabel = shortLabel;
        }

        public int getNumberOfProteins() {
            return numberOfProteins;
        }

        public void setNumberOfProteins(int numberOfProteins) {
            this.numberOfProteins = numberOfProteins;
        }

        public int getNumberOfPeptides() {
            return numberOfPeptides;
        }

        public void setNumberOfPeptides(int numberOfPeptides) {
            this.numberOfPeptides = numberOfPeptides;
        }

        public int getNumberOfSpectra() {
            return numberOfSpectra;
        }

        public void setNumberOfSpectra(int numberOfSpectra) {
            this.numberOfSpectra = numberOfSpectra;
        }

        public int getNumberOfUniquePeptides() {
            return numberOfUniquePeptides;
        }

        public void setNumberOfUniquePeptides(int numberOfUniquePeptides) {
            this.numberOfUniquePeptides = numberOfUniquePeptides;
        }

        public int getNumberOfExistingIdentifiedSpectra() {
            return numberOfExistingIdentifiedSpectra;
        }

        public void setNumberOfExistingIdentifiedSpectra(int numberOfExistingIdentifiedSpectra) {
            this.numberOfExistingIdentifiedSpectra = numberOfExistingIdentifiedSpectra;
        }

        public int getNumberOfIdentifiedSpectra() {
            return numberOfIdentifiedSpectra;
        }

        public void setNumberOfIdentifiedSpectra(int numberOfIdentifiedSpectra) {
            this.numberOfIdentifiedSpectra = numberOfIdentifiedSpectra;
        }

        public boolean isSpectrumMatchFragmentIons() {
            return spectrumMatchFragmentIons;
        }

        public void setSpectrumMatchFragmentIons(boolean spectrumMatchFragmentIons) {
            this.spectrumMatchFragmentIons = spectrumMatchFragmentIons;
        }

        public Set<Comparable> getMissingIdentifiedSpectraIds() {
            return missingIdentifiedSpectraIds;
        }

        public void addMissingIdentifiedSpectraIds(Collection<Comparable> spectraIds) {
            this.missingIdentifiedSpectraIds.addAll(spectraIds);
        }

        public int getNumberofMissingSpectra() {
            return numberofMissingSpectra;
        }

        public void setNumberofMissingSpectra(int numberofMissingSpectra) {
            this.numberofMissingSpectra = numberofMissingSpectra;
        }

        public boolean hasMs2Annotation() {
            return ms2Annotation;
        }

        public void setMs2Annotation(boolean ms2Annotation) {
            this.ms2Annotation = ms2Annotation;
        }

        public boolean hasChromatogram() {
            return chromatogram;
        }

        public void setChromatogram(boolean chromatogram) {
            this.chromatogram = chromatogram;
        }

        public Set<AssayPTM> getPtms() {
            return ptms;
        }

        public void addPtms(Collection<AssayPTM> ptms) {
            this.ptms.addAll(ptms);
        }

        public Set<Instrument> getInstruments() {
            return instruments;
        }

        public void addInstruments(Collection<Instrument> instruments) {
            this.instruments.addAll(instruments);
        }

        public Set<Software> getSoftwares() {
            return softwares;
        }

        public void addSoftwares(Collection<Software> softwares) {
            this.softwares.addAll(softwares);
        }

        public Set<Contact> getContacts() {
            return contacts;
        }

        public void addContacts(Collection<Contact> contacts) {
            this.contacts.addAll(contacts);
        }

        public boolean isProteinGroupPresent() {
            return proteinGroupPresent;
        }

        public void setProteinGroupPresent(boolean proteinGroupPresent) {
            this.proteinGroupPresent = proteinGroupPresent;
        }

        public String getExampleProteinAccession() {
            return exampleProteinAccession;
        }

        public void setExampleProteinAccession(String exampleProteinAccession) {
            this.exampleProteinAccession = exampleProteinAccession;
        }

        public String getSearchDatabase() {
            return searchDatabase;
        }

        public void setSearchDatabase(String searchDatabase) {
            this.searchDatabase = searchDatabase;
        }

        public double getDeltaMzErrorRate() {
            return deltaMzErrorRate;
        }

        public void setDeltaMzErrorRate(double deltaMzErrorRate) {
            this.deltaMzErrorRate = deltaMzErrorRate;
        }

        public Set<AssaySampleCvParam> getSamples() {
            return samples;
        }

        public void addSamples(Collection<AssaySampleCvParam> samples) {
            this.samples.addAll(samples);
        }

        public Set<AssayQuantificationMethodCvParam> getQuantificationMethods() {
            return quantificationMethods;
        }

        public void addQuantificationMethods(Collection<AssayQuantificationMethodCvParam> quantificationMethods) {
            this.quantificationMethods.addAll(quantificationMethods);
        }

        public String getExperimentalFactor() {
            return experimentalFactor;
        }

        public void setExperimentalFactor(String experimentalFactor) {
            this.experimentalFactor = experimentalFactor;
        }

        public Set<PeakFileSummary> getPeakFileSummaries() {
            return peakFileSummaries;
        }

        public void addPeakFileSummary(PeakFileSummary peakFileSummary) {
            this.peakFileSummaries.add(peakFileSummary);
        }

        public void addPeakFileSummaries(Collection<PeakFileSummary> peakFileSummaries) {
            this.peakFileSummaries.addAll(peakFileSummaries);
        }

        public Set<AssayGroupCvParam> getCvParams() {
            return cvParams;
        }

        public void addCvParams(Collection<AssayGroupCvParam> cvParams) {
            this.cvParams.addAll(cvParams);
        }

        public Set<AssayGroupUserParam> getUserParams() {
            return userParams;
        }

        public void addUserParams(Collection<AssayGroupUserParam> userParams) {
            this.userParams.addAll(userParams);
        }
    }
