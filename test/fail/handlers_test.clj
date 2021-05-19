(ns fail.handlers-test
  (:require [clojure.test :as t]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [fail.utils :as u]
            [fail.handlers :as h]))

(t/deftest helper-tests
  (t/testing "levenshtein distance"
    (t/is (= 1 (h/levenshtein "anu" "manu"))))

  (t/testing "respond with 200"
    (t/is (= {:status 200 :body {:message "yes"}}
             (h/respond "yes"))))

  (t/testing "respond with custom status"
    (t/is (= {:status 404 :body {:message "no"}}
             (h/respond "no" 404))))

  (t/testing "rank by levenshtein distance"
    (t/is (= [{:name "ste"} {:name "rahul"} {:name "anu"}]
             (h/rank "stephen" [{:name "rahul"} {:name "anu"} {:name "ste"}]))))

  (t/testing "get all contacts"
    (u/with-system
      (fn [db]
        (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Rahul" "1234"])

        (t/is (= [{:name "Rahul" :phone "1234"}]
                 (h/get-contacts db))))))

  (t/testing "suggest contact"
    (u/with-system
      (fn [db]
        (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Rahul" "1234"])
        (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Ste" "4321"])

        (t/is (= {:status 404
                  :body   {:message {:error       "Cannot find stephen"
                                     :suggestions ["Ste" "Rahul"]}}}
                 (h/suggest db "stephen")))))))

(t/deftest handlers-test
  (u/with-system
    (fn [db]
      (t/testing "successful health check"
        (t/is (= {:status 200 :body {:message "Ok"}}
                 (h/health-check {:db db}))))))

  (t/testing "failed health check"
    (t/is (= {:status 503 :body {:message "Systems unhealthy"}}
             (h/health-check {}))))

  (t/testing "list all contacts"
    (u/with-system
      (fn [db]
        (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Rahul" "1234"])
        (t/is (= {:status 200
                  :body   {:message [{:name "Rahul" :phone "1234"}]}}
                 (h/all-contacts {:db db}))))))

  (t/testing "add a contact"
    (u/with-system
      (fn [db]
        (h/add-contact {:db         db
                        :parameters {:body {:name "Rahul" :phone "1234"}}})
        (t/is (= {:name "Rahul" :phone "1234"}
                 (jdbc/execute-one! db
                                    ["SELECT * FROM contacts WHERE name='Rahul'"]
                                    {:builder-fn rs/as-unqualified-lower-maps}))))))

  (u/with-system
    (fn [db]
      (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Rahul" "1234"])
      (jdbc/execute-one! db ["INSERT INTO contacts VALUES(?, ?)" "Stephen" "4321"])

      (t/testing "search exact match"
        (t/is (= {:status 200 :body {:message {:name "Rahul" :phone "1234"}}}
                 (h/search-contact {:db         db
                                    :parameters {:query {:name "Rahul"}}}))))

      (t/testing "search with suggestions"
        (t/is (= {:status 404
                  :body   {:message {:error       "Cannot find Ste"
                                     :suggestions ["Stephen" "Rahul"]}}}
                 (h/search-contact {:db         db
                                    :parameters {:query {:name "Ste"}}})))))))
