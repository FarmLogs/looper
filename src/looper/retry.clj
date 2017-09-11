(ns looper.retry
  (:require [diehard.core :as dh]
            [looper.logging :as log]))

(defn- default-options
  [method url]
  ;; generates a sequence of retry ms delays like
  ;; [10 40 160 640 2000 2000 ...]
  {:backoff-ms [10   ;; initial
                2000 ;; max
                4]   ;; factor
    :max-retries 10  ;; will result in 11 total tries
    :on-failed-attempt (fn [_ ex]
                         (log/warn ex (str "failed-to-" (name method))
                                   {:url url
                                    :attempt dh/*executions*
                                    :elapsed dh/*elapsed-time-ms*}))
    :retry-if (fn [_ ex]
                (if ex
                  (let [{:keys [status]} (ex-data ex)]
                    (or (nil? status)
                        (>= status 500)))
                  false))})

(defn retry [method url f options]
  (dh/with-retry (merge (default-options method url) options)
    (f)))

