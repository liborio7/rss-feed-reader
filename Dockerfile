FROM clojure:openjdk-13-lein-slim-buster as builder

WORKDIR /usr/src/app

COPY project.clj project.clj

RUN lein deps

COPY . .

FROM builder as release

RUN lein uberjar && \
    mv target/uberjar/*standalone.jar app-standalone.jar

FROM garthk/unzip as unzip

WORKDIR /usr

ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip .

RUN unzip newrelic-java.zip

FROM openjdk:13-slim-buster as run

WORKDIR /usr/app

COPY --from=release /usr/src/app/app-standalone.jar ./app.jar
COPY --from=unzip /usr/newrelic/newrelic.jar ./newrelic.jar

EXPOSE $port

ENTRYPOINT ["java", "-XX:+UseContainerSupport","-XX:MaxRAMPercentage=85","-XX:+UnlockExperimentalVMOptions","-XX:+UseZGC", \
    "-javaagent:newrelic.jar", \
    "-jar", "-Xms256M", "-Xmx256M", "app.jar", "-m", "rss-feed-reader.app"]