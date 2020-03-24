# Welcome to LDBBC
The *L*inked *D*ata *B*asic *B*asic *C*ontainer.
A rough but fast implementation of one single [Linked Data Platform (LDP)](https://www.w3.org/TR/ldp/) Container.
Supports RDF in JSON-LD, Turtle, RDF/XML, and N-Triples.
Stores data in memory and does not persist data on shutdown.

## Binaries
See [Releases](https://github.com/kaefer3000/ldbbc/releases).

## Compilation
LDBBC can be compiled using Apache Maven:
```sh
$ mvn package
```
You may need the development version of NxParser installed, do so by
```sh
$ git clone https://github.com/nxparser/nxparser
$ cd nxparser
$ mvn -Dmaven.test.skip=true install
```

## Usage

### Deploy on application server
The downloaded/compiled `.war` file can be deployed on an application server (eg. Apache Tomcat, Eclipse Jetty).

### Command line
Alternatively, check out the code and run LDBBC from the command line using the Jetty plugin for Apache Maven:
```sh
$ mvn jetty:run
```
Note that LDBBC requires Maven at at least version `3.1` when [they changed their logging system](https://maven.apache.org/maven-logging.html).
If you are on an earlier version, you have to re-introduce the SLF4J dependencies removed [in a recent commit](https://github.com/kaefer3000/ldbbc/commit/fe01d54f838ed84a99ac55f7585b113a2ba97d4a), see [the corresponding lines in the diff](https://github.com/kaefer3000/ldbbc/commit/fe01d54f838ed84a99ac55f7585b113a2ba97d4a#diff-600376dffeb79835ede4a0b285078036L22).
#### Configuration on the command line
##### Disable request logging
Per default, request logging is enabled on `INFO` level.
Disable request logging by setting the level to `WARNING`:

```sh
$ MAVEN_OPTS=-Dorg.slf4j.simpleLogger.log.org.eclipse.jetty.server.RequestLog=warn mvn jetty:run
```

##### Set port
By default, the server listens on port 8080.
You can change the port by setting the `jetty.port` property:
```sh
$ mvn -Djetty.port=8081 jetty:run
```

#####

![Publish Docker image to GitHub Package Registry (GPR)](https://github.com/uvdsl/ldbbc/workflows/Publish%20Docker%20image%20to%20GitHub%20Package%20Registry%20(GPR)/badge.svg)
