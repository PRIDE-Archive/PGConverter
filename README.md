# ProteoGenomics Converter tool (PGConverter)
[Download PGConverter](https://drive.google.com/open?id=0ByPwkIg-BdVzRGtWY1JvQVdGbWc)

This library is primarily to offer a command-line tool to convert between the following formats for proteogenomics related data:
mzIdentML -> mzTab -> proBed -> bigBed. For more information about proBed, see the [PSI proBed](http://www.psidev.info/probed) website. For more information about [BED](https://genome.ucsc.edu/FAQ/FAQformat.html#format1) and [bigBed](https://genome.ucsc.edu/goldenpath/help/bigBed.html), please see the UCSC website.

It is also possible to convert regular non-proteogenomics mzIdentML or PRIDE XML files to mzTab.

A secondary function of this library is to gather metadata and statistics about mzIdentML, mzTab, and PRIDE XML files for validation purposes.

Primarily this tool is powered by the [ms-data-core-api](https://github.com/PRIDE-Utilities/ms-data-core-api) library to read/write files.

## Minimum requirements
* Java 1.8, 64-bit
* A dual-core CPU
* 2+ GB RAM for complex mzIdentML files.

## Instructions
1. [Download]((https://drive.google.com/open?id=0ByPwkIg-BdVzRGtWY1JvQVdGbWc)) the tool as a zip archive file.
2. Extract the zip file to a directory.
3. From a terminal / command prompt, navigate to this new extracted directory and execute a command as described under the 'usage' section below.

## Usage
### File conversion
To convert, run the tool with the -c parameter, and the provide 'result' assay files using the -mzid, -mztab, or -pridexml parameter, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths separated by '##' for multiple related peak files. You may specify an output file directly using the -outputfile parameter, or a general output format: this will the same name as the input file name with the extension of the -outputformat.

#### Convert from mzIdentML to mzTab, specifying an output file name
$ java -jar pg-converter.jar -c -mzid /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -outputfile /path/to/output/output.mztab
#### Convert from PRIDE XML to mzTab, specifying an output format
$ java -jar pg-converter.jar -c -pridexml /path/to/data/foo.pride.xml -outputformat mztab
#### Convert from annotated mzTab to proBed
$ java -jar pg-converter.jar -c -mztab /path/to/data/foo.mztab -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -chromsizes /path/to/chrom.txt
#### Convert from proBed to bigBed
$ java -jar pg-converter.jar -c -mztab /path/to/data/foo.pro.bed -chromsizes /path/to/chrom.txt -asqlfile /path/to/aSQL.as -bigbedconverter /path/to/bedToBigBed

## File validation
To validate, run the tool with the -v parameter, and the provide your 'result' assay files, and related 'peak' files if applicable. Peak files can be added with the -peak parameter for a single peak file, or -peaks with paths separated by '##' for multiple related peak files.
By default the report is saved as a serilized object, so to make a human-readiable plain text report use the -skipserialization flag, and provide an output report file to save the output.
### mzIdentML validation
$ java -jar pg-converter.jar  -v -mzid /path/to/data/foo.mzid -peak /path/to/data/bar1.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### mzTab validation
$ java -jar pg-converter.jar r -v -mztab /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -skipserialization -reportfile /path/to/output/outputReport.txt
### PRIDE XML validation
$ java -jar pg-converter.jar  -v -pridexml /path/to/data/foo.pride.xml -skipserialization -reportfile /path/to/output/outputReport.txt

## Troubleshooting
You may need to allocate more RAM for the tool to use. To do so, add an extra parameter at the start of the command along the lines of: -Xmx\<heap size\>g

* e.g. to specify 4GB of RAM:

$ java -Xmx4g -jar pg-converter.jar -c -mzid /path/to/data/foo.mzid -peaks /path/to/data/bar1.mgf##/path/to/data/bar2.mgf -outputfile /path/to/output/output.mztab

* e.g. to specify 100GB of RAM:

$ java -Xmx100g -jar pg-converter.jar  -v -mzid /path/to/data/foo.mzid -peak /path/to/data/bar1.mgf -skipserialization -reportfile /path/to/output/outputReport.txt

## Contact
To get in touch, please either email <pride-support@ebi.ac.uk> or raise an issue onn the [issues page](https://github.com/PRIDE-Toolsuite/PGConverter/issues).




