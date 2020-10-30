(ns rss-feed-reader.db.datasource
  (:require [next.jdbc.result-set :as rs]
            [next.jdbc.prepare :as p])
  (:import (java.sql PreparedStatement Timestamp Date)
           (java.time Instant LocalDate LocalDateTime)))

(extend-protocol p/SettableParameter

  Instant
  (set-parameter [^Instant v ^PreparedStatement ps ^long i]
    (.setTimestamp ps i (Timestamp/from v)))

  LocalDate
  (set-parameter [^LocalDate v ^PreparedStatement ps ^long i]
    (.setTimestamp ps i (Timestamp/valueOf (.atStartOfDay v))))

  LocalDateTime
  (set-parameter [^LocalDateTime v ^PreparedStatement ps ^long i]
    (.setTimestamp ps i (Timestamp/valueOf v))))

(extend-protocol rs/ReadableColumn

  Date
  (read-column-by-label [^Date v _]
    (.toLocalDate v))
  (read-column-by-index [^Date v _2 _3]
    (.toLocalDate v))

  Timestamp
  (read-column-by-label [^Timestamp v _]
    (.toInstant v))
  (read-column-by-index [^Timestamp v _2 _3]
    (.toInstant v)))