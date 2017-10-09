# Welcome to LDBBC
A rough but fast implementation of one [Linked Data Platform (LDP)](https://www.w3.org/TR/ldp/) Container.
Supports RDF in JSON-LD, Turtle, RDF/XML, and N-Triples.

## Compilation
LDBBC can be compiled using Apache Maven:
```sh
$ mvn package
```

## Usage

### Deploy on application server
The thus compiled `.war` file can be deployed on an application server (eg. Apache Tomcat, Eclipse Jetty).

### Command line
Alternatively, run LDBBC from the command line using the Jetty plugin for Apache Maven:
```sh
$ mvn jetty:run
```
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

