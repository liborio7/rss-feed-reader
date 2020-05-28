FROM clojure:latest

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN lein uberjar &&\
    mv target/uberjar/*standalone.jar app-standalone.jar

EXPOSE 3000

CMD ["java", "-jar", "app-standalone.jar", "-m", "rss-feed-reader.app"]