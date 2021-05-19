(ns fail.handlers
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [failjure.core :as f]))

;; Helpers

(defn levenshtein
  [w1 w2]
  (letfn [(cell-value [same-char? prev-row cur-row col-idx]
                      (min (inc (nth prev-row col-idx))
                           (inc (last cur-row))
                           (+ (nth prev-row (dec col-idx))
                              (if same-char?
                                0
                                1))))]
    (loop [row-idx  1
           max-rows (inc (count w2))
           prev-row (range (inc (count w1)))]
      (if (= row-idx max-rows)
        (last prev-row)
        (let [ch2 (nth w2 (dec row-idx))
              next-prev-row (reduce (fn [cur-row i]
                                      (let [same-char? (= (nth w1 (dec i)) ch2)]
                                        (conj cur-row
                                              (cell-value same-char?
                                                          prev-row
                                                          cur-row
                                                          i))))
                                    [row-idx]
                                    (range 1 (count prev-row)))]
          (recur (inc row-idx) max-rows next-prev-row))))))

(defn respond
  ([data]
   (respond data 200))
  ([data status]
   {:status status
    :body   {:message data}}))

(defn rank
  [name contacts]
  (sort-by #(levenshtein name (:name %)) contacts))

(defn get-contacts
  [db]
  (f/try*
    (jdbc/execute! db
                   ["SELECT * FROM contacts"]
                   {:builder-fn rs/as-unqualified-lower-maps})))

(defn suggest
  [db name]
  (f/try*
    (respond {:error       (str "Cannot find " name)
              :suggestions (->> (get-contacts db)
                                (rank name)
                                (map :name)
                                (take 3))}
             404)))

;; Handlers

(defn all-contacts
  [{db :db}]
  (f/try-all [result (get-contacts db)]
    (respond result)
    (f/when-failed [err]
      (respond (f/message err) 500))))

(defn add-contact
  [{db :db {{:keys [name phone]} :body} :parameters}]
  (f/try-all [_ (jdbc/execute-one! db ["INSERT INTO contacts VALUES (?, ?)" name phone])]
    (respond name)
    (f/when-failed [err]
      (respond (f/message err) 500))))

(defn search-contact
  [{db :db {{:keys [name]} :query} :parameters}]
  (f/try-all [result (jdbc/execute-one! db
                                        ["SELECT * FROM contacts WHERE name=?" name]
                                        {:builder-fn rs/as-unqualified-lower-maps})]
    (if (seq result)
      (respond result)
      (suggest db name))
    (f/when-failed [err]
      (respond (f/message err) 500))))

(defn health-check
  [{db :db}]
  (f/try-all [_ (jdbc/execute-one! db ["SELECT 1+1 AS result"])]
    (respond "Ok")
    (f/when-failed [_]
      (respond "Systems unhealthy" 503))))

(comment
  (levenshtein "anuxyz" "banu")

  (rank "stephen" [{:name "rahul"} {:name "anu"} {:name "ste"}]))
