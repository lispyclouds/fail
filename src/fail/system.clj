(ns fail.system
  (:require [com.stuartsierra.component :as component]
            [failjure.core :as f]
            [next.jdbc :as jdbc]
            [ring.adapter.jetty :as jetty]
            [fail.server :as srv])
  (:import [java.net ConnectException]))

(defn try-connect
  ([conn-fn]
   (try-connect conn-fn 10 1000))
  ([conn-fn n interval]
   (if (zero? n)
     (throw (ConnectException. "Cannot connect to system"))
     (let [res (f/try*
                 (conn-fn))]
       (if (f/failed? res)
         (do
           (println (format "Connection failed, retrying after %dms" interval))
           (Thread/sleep interval)
           (recur conn-fn (dec n) (+ interval 200)))
         res)))))

(defprotocol IDatabase
  (conn [this]))

(defrecord Database
  [db-host db-port db-name db-user db-password]
  component/Lifecycle
  (start [this]
    (assoc this
           :db
           (let [conn (try-connect
                        #(jdbc/get-connection {:dbtype    "postgresql"
                                               :classname "org.postgresql.Driver"
                                               :dbname    db-name
                                               :user      db-user
                                               :password  db-password
                                               :host      db-host
                                               :port      db-port}))]
             (jdbc/execute! conn ["CREATE TABLE IF NOT EXISTS contacts (name TEXT, phone TEXT)"])
             conn)))
  (stop [this]
    (.close (:db this))
    (assoc this :db nil))
  IDatabase
  (conn [this]
        (:db this)))

(defprotocol IServer
  (server [this]))

(defrecord Server
  [database host port]
  component/Lifecycle
  (start [this]
    (assoc this
           :server
           (jetty/run-jetty (srv/server (conn database))
                            {:host   host
                             :port   port
                             :join?  false
                             :async? true})))
  (stop [this]
    (.stop (:server this))
    (assoc this :server nil))
  IServer
  (server [this]
          (:server this)))

(def system-map
  (component/system-map
    :database (map->Database {:db-host     "localhost"
                              :db-port     5432
                              :db-name     "contacts"
                              :db-user     "bob"
                              :db-password "bobby-tables"})
    :server   (component/using (map->Server {:host "0.0.0.0"
                                             :port 7777})
                               [:database])))

(defonce system nil)

(defn start
  []
  (alter-var-root #'system
                  (constantly (component/start system-map))))

(defn stop
  []
  (alter-var-root #'system
                  #(when %
                     (component/stop %))))

(comment
  (do
    (stop)
    (start)))
