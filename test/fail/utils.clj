(ns fail.utils
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [fail.system :as sys]))

(defn with-system
  [test-fn]
  (let [system (component/system-map
                 :database (sys/map->Database {:db-name     "contacts-test"
                                               :db-user     "bob"
                                               :db-password "bobby-tables"
                                               :db-host     "localhost"
                                               :db-port     5433}))
        {:keys [database]
         :as   com}
        (component/start system)]
    (try
     (test-fn (sys/conn database))
     (finally
      (jdbc/execute! (sys/conn database) ["DROP TABLE contacts;"])
      (component/stop com)))))
