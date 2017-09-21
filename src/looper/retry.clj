(ns looper.retry
  (:require [diehard.core :as dh]
            [looper.logging :as log]))

(defn- retryable-error?
  "Returns true if the error has no status (so is likely a connection
  error), or has a status of 5xx"
  [_ ex]
  (if ex
    (let [{:keys [status]} (ex-data ex)]
      (or (nil? status)
          (>= status 500)))))

(defn default-failure-log
  [{:keys [method url exception attempt elapsed]}]
  (log/warn exception (str "failed-to-" (name method))
            {:url url
             :attempt attempt
             :elapsed elapsed}))

(defn wrap-failure-handler
  [f method url]
  (if f
    (fn [result exception]
      (f {:result result
          :exception exception
          :method method
          :url url
          :attempt dh/*executions*
          :elapsed dh/*elapsed-time-ms*}))))

(defn wrap-retry-if
  "It must return a boolean to satisfy java"
  [f]
  (if f
    (comp boolean f)))

(def default-options
  ;; generates a sequence of retry ms delays like
  ;; [10 40 160 640 2000 2000 ...]
  {:backoff-ms [10   ;; initial
                2000 ;; max
                4]   ;; factor
   :max-retries 10  ;; will result in 11 total tries
   :on-failed-attempt default-failure-log
   :retry-if retryable-error?})

(def ^:private remove-nil-entries
  "Removes entries from the map if the value is nil."
  (partial reduce-kv
           (fn [acc k v]
             (if (nil? v)
               acc
               (assoc acc k v)))
           {}))

(defn retry [method url f options]
  (dh/with-retry (-> default-options
                     (merge options)
                     (update-in [:retry-if] wrap-retry-if)
                     (update-in [:on-failed-attempt] #(wrap-failure-handler %1 method url))
                     (remove-nil-entries))
    (f)))

