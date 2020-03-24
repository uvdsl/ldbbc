FROM maven:3.5.2-jdk-8-alpine AS builder
RUN apk add --no-cache git
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN git clone https://github.com/nxparser/nxparser \
&& cd nxparser \
&& mvn -Dmaven.test.skip=true install\
&& cd ..
&& mvn package -DskipTests

FROM jetty:latest
COPY --from=builder /tmp/target/ldbbc.war /var/lib/jetty/webapps/ldbbc.war
EXPOSE 8080
