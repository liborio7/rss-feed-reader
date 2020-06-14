FROM clojure:latest

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN lein uberjar &&\
    mv target/*standalone.jar app-standalone.jar

EXPOSE $port

CMD ["java", "-jar", "-Xms256M", "-Xmx256M", "app-standalone.jar", "-m", "rss-feed-reader.app"]