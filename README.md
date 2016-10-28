[![Build Status](https://travis-ci.org/PRIDE-Toolsuite/PGConverter.svg?branch=master)](https://travis-ci.org/PRIDE-Toolsuite/PGConverter)
# PGConverter
TODO add download link
ProteoGenomics Converter tool

This library is primarily to offer a command-line tool to convert between the following formats for proteogenomics related data:
mzIdentML -> mzTab -> proBed -> bigBed

It is also possible to convert regular non-proteogenomics mzIdentML or PRIDE XML files to mzTab.

A secondary function of this library is to gather metadata and statistics about mzIdentML, mzTab, and PRIDE XML files for validation purposes.

Primarily this uses the [ms-data-core-api](https://github.com/PRIDE-Utilities/ms-data-core-api) library.

## Minimum requirements
Java 1.8
dual-core CPU
>2 GB RAM for complex mzIdentML files.

## Instructions
TODO instructions

## Usage
### File conversion
To convert, run the tool with the -c parameter, and the provid 'result' assay files using the -mzid, -mztab, or -pridexml parameter, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths separated by '##' for multiple related peak files. You may specify an output file directly using the -outputfile parameter, or a general output format - this will the same name as the input file name with the extension of the -outputformat.
#### Convert from mzIdentML to mzTab, specifying an output file name
$ java -jar pg-converter.jar -c -mzid /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -outputfile /path/to/output/output.mztab
#### Convert from PRIDE XML to mzTab , specifying an output format
$ java -jar pg-converter.jar -c -pridexml /path/to/data/foo.pride.xml -outputformat mztab

## File validation
To validate, run the tool with the -v parameter, and the provide your 'result' assay files, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths separated by '##' for multiple related peak files.
By default the report is saved as a serilized object, so to make a human-readiable plain text report use the -skipserialization flag, and provide an output report file to save the output.
### mzIdentML validation
$ java -jar pg-converter.jar  -v -mzid /path/to/data/foo.mzid -peak /path/to/data/bar1.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### mzTab
$ java -jar pg-converter.jar r -v -mztab /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### PRIDE XML validation
$ java -jar pg-converter.jar  -v -pridexml /path/to/data/foo.pride.xml -skipserialization -reportfile /path/to/output/outputReport.txt





