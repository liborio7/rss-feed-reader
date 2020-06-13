FROM clojure:latest

ARG env=dev

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN lein with-profile $env uberjar &&\
    mv target/uberjar/*standalone.jar app-standalone.jar

EXPOSE 3000

CMD ["java", "-jar", "-Xms256M", "-Xmx256M", "app-standalone.jar", "-m", "rss-feed-reader.app"]