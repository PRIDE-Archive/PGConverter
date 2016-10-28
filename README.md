[![Build Status](https://travis-ci.org/PRIDE-Toolsuite/PGConverter.svg?branch=master)](https://travis-ci.org/PRIDE-Toolsuite/PGConverter)
# PGConverter
TODO add download
ProteoGenomics Converter tool

This library is primarily to convert between the following formats for proteogenomics related data:
mzIdentML -> mzTab -> proBed -> bigBed

It is also possible to convert regular non-proteogenomics mzIdentML or PRIDE XML files to mzTab.

A secondary function of this library is to gather metadata and statistics about mzIdentML and PRIDE XML files for validation purposes.

## Usage
### File conversion
To validate, run the tool with the -c parameter, and the provide your 'result' assay files, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths saparated by '##' for multiple related peak files. You may specify an output file directly using the -outputfile parameter, or an output format to use the same input filename with -outputformat.
#### Convert from mzIdentML to mzTab
$ java -jar pg-converter.jar -c -mzid /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -outputfile /path/to/output/output.mztab
#### Convert from PRIDE XML to mzTab 
$ java -jar pg-converter.jar -c -pridexml /path/to/data/foo.pride.xml -outputformat mztab

## File validation
To validate, run the tool with the -v parameter, and the provide your 'result' assay files, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths saparated by '##' for multiple related peak files.
By default the report is saved as a serilized object, so to make a human-readiable report use the -skipserialization flag, and provide a output report file
### mzIdentML validation
$ java -jar pg-converter.jar  -v -mzid /path/to/data/foo.mzid -peak /path/to/data/bar1.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### mzTab
$ java -jar pg-converter.jar r -v -mztab /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### PRIDE XML validation
$ java -jar pg-converter.jar  -v -pridexml /path/to/data/foo.pride.xml -skipserialization -reportfile /path/to/output/outputReport.txt





