# Application


## Authors

Group A43

### Lead developer 

Hugo Henrique Pitorro, nº 92478, https://git.rnl.tecnico.ulisboa.pt/ist192478

### Contributors

Armando Fortes, nº 92428, https://git.rnl.tecnico.ulisboa.pt/ist192428

Diogo Soares, nº 92455, https://git.rnl.tecnico.ulisboa.pt/ist192455

## About

This is a CLI (Command-Line Interface) application.


## Instructions for using Maven

To compile and run using _exec_ plugin:

```
mvn compile exec:java
```

To generate launch scripts for Windows and Linux
(the POM is configured to attach appassembler:assemble to the _install_ phase):

```
mvn install
```

To run using appassembler plugin on Linux:

```
./target/appassembler/bin/spotter arg0 arg1 arg2
```

To run using appassembler plugin on Windows:

```
target\appassembler\bin\spotter arg0 arg1 arg2
```


## To configure the Maven project in Eclipse

'File', 'Import...', 'Maven'-'Existing Maven Projects'

'Select root directory' and 'Browse' to the project base folder.

Check that the desired POM is selected and 'Finish'.


----

