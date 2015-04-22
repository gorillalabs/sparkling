(ns sparkling.testutils.records.domain
  (:refer-clojure :exclude [time]))

(defprotocol timestamped
  (time [item])
  )

(defrecord tweet [username tweet timestamp]
  timestamped
  (time [_]
    timestamp
    ))