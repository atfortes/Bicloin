# Bicloin - Group A43

Distributed Systems 2020-2021, 2nd semester project


## Authors

[Armando Fortes](https://github.com/atfortes) & [Diogo Soares](https://github.com/diogosoares22) & [Hugo Pitorro](https://github.com/xtwigs)

For each module, the README file identifies the lead developer and the contributors.

## Getting Started

The overall system is composed of multiple modules.

See the [project statement](https://github.com/tecnico-distsys/Bicloin/blob/main/README.md) for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require theservers to be running.


## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
