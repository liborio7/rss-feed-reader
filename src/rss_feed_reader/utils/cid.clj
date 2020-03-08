(ns rss-feed-reader.utils.cid
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as string])
  (:refer-clojure :exclude [get set])
  (:import (org.slf4j MDC)))

(def cid-key "cid")
(def cid-len 10)

(s/def :utils/cid (s/with-gen
                    (s/and string? #(= cid-len (count %)))
                    #(->> (gen/vector (gen/char-alpha) cid-len)
                          (gen/fmap (fn [seq] (apply str seq)))
                          (gen/fmap string/lower-case))))

(defn generate []
  (->> (repeatedly #(char (rand-nth (range (int \a) (int \z)))))
       (take cid-len)
       (apply str)))

(s/fdef generate
        :args nil?
        :ret :utils/cid)

(defn get []
  (MDC/get cid-key))

(s/fdef get
        :args nil?
        :ret (s/nilable :utils/cid))

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