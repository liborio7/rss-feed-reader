(ns rss-feed-reader.utils.cid
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:refer-clojure :exclude [get set])
  (:import (org.slf4j MDC)))

(def cid-key "cid")
(def cid-len 7)

(s/def :utils/cid (s/with-gen
                    (s/and string? #(= cid-len (count %)))
                    #(->> (gen/vector (gen/char) 7)
                          (gen/fmap (fn [seq] (apply str seq))))))

(defn generate []
  (apply str (take cid-len (repeatedly #(char (+ (rand 26) 65))))))

(s/fdef generate
        :args nil?
        :ret :utils/cid)

(defn get []
  (MDC/get cid-key))

(s/fdef get
        :args nil?
        :ret (s/nilable string?))

(defn set [cid]
  (MDC/put cid-key cid))

(s/fdef set
        :args (s/cat :cid :utils/cid)
        :ret nil?)

(defn set-new []
  (set (generate)))

(s/fdef set-new
        :args nil?
        :ret nil?)