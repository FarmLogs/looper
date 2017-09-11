(ns looper.client
  (:require [looper.retry :as retry]
            clj-http.client)
  (:refer-clojure :exclude [get]))

(defn with-retry
  ([method url]
   (with-retry method url nil))
  ([method url {async? :async?
                throw-exceptions :throw-exceptions
                retry-opts :looper/options
                :as opts}]
   (assert (not async?) "Async requests aren't supported")
   (assert (not (false? throw-exceptions)) "clj-http exceptions must be enabled")
   (let [method-fn (resolve (symbol "clj-http.client" (name method)))
         opts' (dissoc opts :looper/options)]
     (retry/retry method url #(method-fn url opts') retry-opts))))

(def get
  "Replacement for clj-http.client/get, but with retries."
  (partial with-retry :get))

(def head
  ""
  (partial with-retry :head))

(def post
  ""
  (partial with-retry :post))

(def put
  ""
  (partial with-retry :put))

(def delete
  ""
  (partial with-retry :delete))

(def options
  ""
  (partial with-retry :options))

(def copy
  ""
  (partial with-retry :copy))

(def move
  ""
  (partial with-retry :move))

(def patch
  ""
  (partial with-retry :patch))
