FROM clojure:latest as builder

WORKDIR /usr/src/app

COPY . .

RUN lein uberjar &&\
    mv target/*standalone.jar app-standalone.jar

# use clean base image
FROM openjdk:13-slim-buster

WORKDIR /usr/src/app

COPY --from=builder /usr/src/app/app-standalone.jar ./app.jar

EXPOSE $port

CMD ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=85","-XX:+UnlockExperimentalVMOptions","-XX:+UseZGC", \
     "-jar", "-Xms256M", "-Xmx256M", "app.jar", "-m", "rss-feed-reader.app"]