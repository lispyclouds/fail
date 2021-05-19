(ns fail.main
  (:require [clojure.repl :as repl]
            [fail.system :as sys])
  (:gen-class))

(defn shutdown!
  [& _]
  (println "Received SIGINT, Shutting down")
  (sys/stop)
  (shutdown-agents)
  (System/exit 0))

(defn -main
  [& _args]
  (repl/set-break-handler! shutdown!)
  (sys/start))
