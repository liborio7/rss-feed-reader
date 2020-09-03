(ns rss-feed-reader.utils.time
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.tools.logging :as log])
  (:import (java.time Instant LocalDate LocalDateTime ZoneId)
           (java.time.temporal ChronoUnit)
           (java.time.format DateTimeFormatter)))

; instant

(defn instant-now []
  (Instant/now))

(defn instant->long [^Instant inst]
  (.toEpochMilli inst))

(defn long->instant [^Long milli]
  (Instant/ofEpochMilli milli))

(s/def ::time (s/with-gen
                #(instance? Instant %)
                #(gen/fmap
                   rss-feed-reader.utils.time/long->instant
                   (let [^Instant now (rss-feed-reader.utils.time/instant-now)]
                     (gen/large-integer*
                       {:min (-> (.minus now 365 ChronoUnit/DAYS)
                                 (rss-feed-reader.utils.time/instant->long))
                        :max (-> (.plus now 365 ChronoUnit/DAYS)
                                 (rss-feed-reader.utils.time/instant->long))})))))

; formatter

(defn formatter [pattern]
  (DateTimeFormatter/ofPattern pattern))

(def formatters
  (into {} (map
             (fn [[k ^DateTimeFormatter f]] [k (.withZone f (ZoneId/of "UTC"))])
             {:basic-iso-date       (DateTimeFormatter/BASIC_ISO_DATE)
              :iso-date             (DateTimeFormatter/ISO_DATE)
              :iso-date-time        (DateTimeFormatter/ISO_DATE_TIME)
              :iso-instant          (DateTimeFormatter/ISO_INSTANT)
              :iso-local-date       (DateTimeFormatter/ISO_LOCAL_DATE)
              :iso-local-date-time  (DateTimeFormatter/ISO_LOCAL_DATE_TIME)
              :iso-local-time       (DateTimeFormatter/ISO_LOCAL_TIME)
              :iso-offset-date      (DateTimeFormatter/ISO_OFFSET_DATE)
              :iso-offset-date-time (DateTimeFormatter/ISO_OFFSET_DATE_TIME)
              :iso-offset-time      (DateTimeFormatter/ISO_OFFSET_TIME)
              :iso-ordinal-date     (DateTimeFormatter/ISO_ORDINAL_DATE)
              :iso-time             (DateTimeFormatter/ISO_TIME)
              :iso-week-date        (DateTimeFormatter/ISO_WEEK_DATE)
              :iso-zoned-date-time  (DateTimeFormatter/ISO_ZONED_DATE_TIME)
              :rfc822               (formatter "EEE, d MMM yyyy HH:mm:ss Z")
              :mysql                (formatter "yyyy-MM-dd HH:mm:ss")})))

(defn parse [s]
  (let [^LocalDateTime date-time
        (first
          (for [f (vals formatters)
                :let [d (try (LocalDateTime/parse s f) (catch Exception _ nil))]
                :when d] d))]
    (-> date-time
        (.atZone (ZoneId/of "UTC"))
        (.toInstant))))

; date-time

(def date-time-formatter (formatter "yyyy-MM-dd HH:mm:ss"))

(defn string->date-time
  ([s] (string->date-time s date-time-formatter))
  ([s formatter]
   (try
     (LocalDateTime/parse s formatter)
     (catch Exception _
       (log/warn "invalid date" s)))))

(defn date-time->string
  ([^LocalDateTime date-time] (date-time->string date-time date-time-formatter))
  ([^LocalDateTime date-time formatter]
   (try
     (.format date-time formatter)
     (catch Exception _
       (log/warn "fail to unparse date" date-time)))))

(defn date-time->instant [^LocalDateTime date-time]
  (-> date-time
      (.atZone (ZoneId/of "UTC"))
      (.toInstant)))

; date

(def date-formatter (formatter "yyyy-MM-dd"))

(defn string->date
  ([s] (string->date s date-formatter))
  ([s formatter]
   (try
     (LocalDate/parse s formatter)
     (catch Exception _
       (log/warn "fail to parse date" s)))))

(defn date->string
  ([^LocalDate date] (date->string date date-formatter))
  ([^LocalDate date formatter]
   (try
     (.format date formatter)
     (catch Exception _
       (log/warn "fail to unparse date" date)))))

(defn date->instant [^LocalDate date]
  (date-time->instant
    (.atStartOfDay date)))
