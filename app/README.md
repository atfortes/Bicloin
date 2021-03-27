# Application


## Authors

Group CXX

*(fill-in line above with group identifier e.g. A07 or T22; and then delete this line)*

### Lead developer 

... ... ...

*(fill-in line above with student number, name, and @GitHub identity of module leader; and then delete this line)*

### Contributors

... ... ...

... ... ...

*(fill-in lines above with student number, name, and @GitHub identity of module contributors; and then delete this line)*


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

