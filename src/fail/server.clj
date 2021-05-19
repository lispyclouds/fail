(ns fail.server
  (:require [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.http :as http]
            [reitit.coercion.malli :as malli]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.interceptor.sieppari :as sieppari]
            [fail.handlers :as h]))

(def routes
  [["/contacts"
    {:get
     {:handler h/all-contacts}
     :post
     {:handler    h/add-contact
      :parameters
      {:body
       [:map
        [:name string?]
        [:phone string?]]}}}]
   ["/search"
    {:get
     {:handler    h/search-contact
      :parameters {:query [:map [:name string?]]}}}]
   ["/health" {:get {:handler h/health-check}}]])

(defn system-interceptor
  [db]
  {:enter #(-> %
               (update-in [:request :db] (constantly db)))})

(defn server
  [db]
  (http/ring-handler
    (http/router routes
                 {:data {:coercion     malli/coercion
                         :muuntaja     m/instance
                         :interceptors [(parameters/parameters-interceptor)
                                        (muuntaja/format-negotiate-interceptor)
                                        (muuntaja/format-response-interceptor)
                                        (muuntaja/format-request-interceptor)
                                        (coercion/coerce-response-interceptor)
                                        (coercion/coerce-request-interceptor)
                                        (system-interceptor db)]}})
    (ring/routes
      (ring/create-default-handler
        {:not-found (constantly {:status  404
                                 :headers {"Content-Type" "application/json"}
                                 :body    "{\"message\": \"Took a wrong turn?\"}"})}))
    {:executor sieppari/executor}))
