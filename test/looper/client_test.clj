(ns looper.client-test
  (:require [clojure.test :refer :all]
            [looper.client :refer :all]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log]))

(defn disable-logging []
  (alter-var-root #'looper.retry/default-options
                  #(dissoc % :on-failed-attempt)))

(defmacro with-server [[url-sym handler] & body]
  `(let [server# (jetty/run-jetty
                   ~handler
                   {:port 0
                    :join? false})
         ~url-sym (str (.getURI server#))]
     (try
       (disable-logging)
       ~@body
       (finally (.stop server#)))))

(deftest should-work-when-service-available
  (with-server [url (constantly {:status 202})]
    (let [response (get url)]
      (is (= 202 (:status response))))))

(deftest should-work-when-service-becomes-available
  (let [retry-count (atom 0)]
    (with-server [url (fn [_]
                      (swap! retry-count inc)
                        (if (= 2 @retry-count)
                          {:status 202}
                          {:status 500}))]
      (let [response (get url)]
        (is (= 202 (:status response)))
        (is (= 2 @retry-count))))))

(deftest should-not-retry-when-service-reports-400
  (let [retry-count (atom 0)
        error (promise)]
    (with-server [url (fn [_]
                      (swap! retry-count inc)
                      {:status 400})]
      (try
        (get url)
        (catch Exception e
          (is (= 400 (-> e ex-data :status)))
          (is (= 1 @retry-count)))))))


(deftest should-give-up-if-the-service-is-always-unavailable
  (let [retry-count (atom 0)
        error (promise)]
    (with-server [url (fn [_]
                      (swap! retry-count inc)
                      {:status 500})]
      (try
        (get url {:looper/options {:max-retries 2}})
        (catch Exception e
          (is (= 3 @retry-count)))))))

(deftest should-not-throw-when-exceptions-disabled
  (let [retry-count (atom 0)
        error (promise)]
    (with-server [url (fn [_]
                        {:status 500})]
      (let [response (get url {:throw-exceptions false
                               :looper/options {:max-retries 2}})]
        (is (= 500 (:status response)))))))

;; TODO: figure out a way to test that this actually retries
(deftest should-give-up-if-the-service-isn't-reachable
  (try
    (get "http://non_existent" {:looper/options {:max-retries 2}})
    (catch Exception e
      (is (instance? java.net.UnknownHostException e)))))

(deftest every-method-should-work
  (with-server [url (constantly {:status 202})]
    (doseq [method [:copy :delete :get :head :move :options :patch :post :put]]
      (testing method
        (let [response ((resolve (symbol "looper.client" (name method))) url)]
          (is (= 202 (:status response))))))))

(deftest nil-max-retries-retries-indefinitely
  (let [retry-count (atom 0)]
    (try
      (get "http://non_existent" {:looper/options
                                  {:max-retries nil
                                   :retry-if (fn [_ _] (< (swap! retry-count inc) 20))
                                   :backoff-ms [1 2]}})
      (catch Exception e
        (is (instance? java.net.UnknownHostException e))))
    (is (= 20 @retry-count))))
