(ns aggregate.project-test
  (:require [clojure.test :refer :all]
            [aggregate.core :as agg]
            [aggregate.testsupport :refer :all]
            [clojure.java.jdbc :as jdbc]))


(use-fixtures :each db-fixture)

(def schema
  [:customer [(id-column)
              [:name "varchar(30)"]]
   :person [(id-column)
            [:name "varchar(30)"]]
   :project [(id-column)
             [:name "varchar(30)"]
             (fk-column :person :manager_id false)
             (fk-column :customer false)]
   :task [(id-column)
          [:description "varchar(50)"]
          [:effort "integer"]
          (fk-column :project false)
          (fk-column :person :assignee_id false)]
   :person_project [(fk-column :project false)
                    (fk-column :person false)]])

(def er
  {:customer {:table :customer
              :head? true
              :family :gen
              :uid #{:name} ;; set of fields that uniquely identify an entity
              :fields {:id {:column :id}
                       :name {:column :name}}}
   :person {:table :person
            :head? true
            :family :gen
            :uid :name ;; single identifier can be written without the #{}
            :fields {:id {:column :id}
                     :name {:column :name}}}
   :project {:table :project
             :head? true
             :family :gen
             :uid [:name] ;; can use [] instead of #{} as well
             :fields {:id {:column :id}
                      :name {:column :name}}
             :relations {:manager {:type :m-1
                                   :child-entity :person
                                   :fk-child :manager_id}
                         :customer {:type :m-1
                                    :child-entity :customer
                                    :fk-child :customer_id}
                         :tasks {:type :1-n
                                 :child-entity :task
                                 :fk-parent :project_id}
                         :members {:type :m-n
                                    :child-entity :person
                                    :link-table :person_project
                                    :fk-child :person_id
                                    :fk-parent :project_id}}}
   :task {:table :task
          :head? false
          :family :gen
          :uid :id ;; using the ID itself instead of any unique fields
          :fields {:id {:column :id}
                   :description {:column :description}
                   :effort {:column :effort}}
          :relations {:assignee {:type :m-1
                                :child-entity :person
                                :fk-child :assignee_id}}}})

(def er-config (agg/build-er-config er))

;; To setup a schema in standalone H2
#_ (do (require '[aggregate.h2 :as h2])
       (require '[aggregate.testsupport :refer :all])
       (require '[aggregate.core :as agg])
       (h2/start-db))

#_ (create-schema! @h2/db-con schema)


(def data
  {:customer {(agg/tempid 1) {:name "Big Company"}
              (agg/tempid 2) {:name "Startup"}}
   :person {(agg/tempid :dai) ;; can use keyword too!
            {:name "Daisy"}
            (agg/tempid :min) {:name "Mini"}
            (agg/tempid :mic) {:name "Mickey"}
            (agg/tempid :don) {:name "Donald"}}
   :project {(agg/tempid "java") ;; can use strings as well!
             {:name "Java"
              :manager (agg/tempid :min)
              :customer (agg/tempid 1)
              :members #{(agg/tempid :min)
                         (agg/tempid :mic)
                         (agg/tempid :don)}
              :tasks {1 {:description "compile"
                         :effort 9
                         :assignee (agg/tempid :mic)}
                      2 {:description "deploy"
                         :effort 5
                         :assignee (agg/tempid :don)}}}
             (agg/tempid "sql")
             {:name "SQL"
              :manager (agg/tempid :mic)
              :customer (agg/tempid 2)
              :members #{(agg/tempid :mic)
                         (agg/tempid :don)}
              :tasks {3 {:description "deploy"
                         :effort 3
                         :assignee (agg/tempid :don)}
                      4 {:description "test"
                         :effort 4
                         :assignee (agg/tempid :don)}
                      5 {:description "document"
                         :effort 7
                         :assignee (agg/tempid :mic)}}}}})

(deftest project-tests
  (create-schema! @db-con schema)
  (testing "Saving aggregate."
    (let [{nids :new-id} (agg/save-agg! er-config @db-con data)]
      (testing "Loading from database."
        (let [a (agg/load-family er-config @db-con :gen)]
          (testing "Matching with orignial data."
            (let [expected
                  {:customer
                   (into {}
                         (map (fn [i] [(get-in nids [:customer (agg/tempid i)])
                                       (get-in data [:customer (agg/tempid i)])])
                              [1 2]))
                   :person
                   (into {}
                         (map (fn [i] [(get-in nids [:person (agg/tempid i)])
                                       (get-in data [:person (agg/tempid i)])])
                              [:dai :mic :min :don]))
                   :project
                   (into {}
                         (map (fn [i]
                                [(get-in nids [:project (agg/tempid i)])
                                 (let [{:keys [name manager customer members tasks]}
                                       (get-in data [:project (agg/tempid i)])]
                                   {:name name
                                    :manager (get-in nids [:person manager])
                                    :customer (get-in nids [:customer customer])
                                    :members (into #{}
                                                   (map #(get-in nids [:person %])
                                                        members))
                                    :tasks
                                    (into {}
                                          (map (fn [[i {a :assignee :as t}]]
                                                 [i
                                                  (assoc t :assignee
                                                         (get-in nids [:person a]))])
                                               tasks))})])
                              ["java" "sql"]))}]
              (is (= a expected))))
          (testing "Deleting project Java."
            (let [i (get-in nids [:project (agg/tempid "java")])
                  p (assoc (get-in a [:project i])
                           :id i)]
              (is (= 6 (agg/delete-entity! er-config @db-con p)))))
          (testing "Deleteing project SQL."
            (let [i (get-in nids [:project (agg/tempid "sql")])
                  p (assoc (get-in a [:project i])
                           :id i)]
              (is (= 6 (agg/delete-entity! er-config @db-con p)))))
          (testing "Confirming no projects in database."
            (let [p (agg/load-head-entity er-config @db-con :project {})]
              (is (= {} p))))
          (testing "Checking all persons in database."
            (let [p (-> (agg/load-head-entity er-config @db-con :person {})
                        :person vals)]
              (is (= p [{:name "Daisy"}
                        {:name "Mini"}
                        {:name "Mickey"}
                        {:name "Donald"}]))))
          (testing "Deleting the rest."
            (let [n (agg/delete-agg! er-config @db-con a)]
              (is (= 6 n)))))))))


