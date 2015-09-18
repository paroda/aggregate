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
              :uid #{:name}
              :fields {:id {:column :id}
                       :name {:column :name}}}
   :person {:table :person
            :head? true
            :family :gen
            :uid #{:name}
            :fields {:id {:column :id}
                     :name {:column :name}}}
   :project {:table :project
             :head? true
             :family :gen
             :uid #{:name}
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
          :uid #{:description}
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


(def agg {:person {1 {:name "Daisy"}
                   2 {:name "Mini"}
                   3 {:name "Mickey"}
                   4 {:name "Donald"}}
          :customer {1 {:name "Big Company"}
                     2 {:name "Startup"}}
          :project {1 {:name "Java"
                       :manager 2
                       :customer 1
                       :members #{1 2 3}
                       :tasks {1 {:id 1
                                  :description "compile"
                                  :effort 9
                                  :assignee 3}}}
                    2 {:name "SQL"
                       :manager 3
                       :customer 2
                       :members #{3 4}
                       :tasks {2 {:id 2
                                  :description "deploy"
                                  :effort 3
                                  :assignee 4}}}}})

(deftest project-tests
  (create-schema! @db-con schema)
  (->> data (map (partial apply agg/save! er @db-con)) doall)
  (testing "Creating a new project"
    (let [saved-project (agg/save! er @db-con :project
                                   {:name "Learning Clojure"
                                    :customer {:id 1 :name "Big Company"}
                                    :tasks [{:description "Buy a good book" :effort 1}
                                            {:description "Install Java" :effort 2}
                                            {:description "Configure Emacs" :effort 4}]
                                    :members [{:id 1 :name "Daisy"}
                                              {:id 2 :name "Mini"}]
                                    :manager {:id 1 :name "Daisy"}})]
      (is (= 1 (record-count @db-con :project)))
      (testing "Assign persons to projects"
        (->> (agg/load manage-person-to-project-er @db-con :project 1)
             (#(update-in % [:members] conj {:id 3 :name "Mickey"}))
             (agg/save! manage-person-to-project-er @db-con)))
      (testing "Assign person to task"
        (->> (agg/load manage-task-to-person-er @db-con :task 1)
             (#(assoc % :assignee {:id 2 :name "Mini"}))
             (agg/save! manage-task-to-person-er @db-con :task))
        (let [loaded-project (agg/load er @db-con :project 1) 
              loaded-daisy (agg/load er @db-con :person 1)
              loaded-mini (agg/load er @db-con :person 2)]
          (is (-> loaded-project :customer))
          (is (= 3 (-> loaded-project :members count)))
          (is (= 1 (-> loaded-daisy :projects_as_member count)))
          (is (= 1 (-> loaded-daisy :projects_as_manager count)))
          (is (= 0 (-> loaded-daisy :tasks count)))
          (is (= 1 (-> loaded-mini :projects_as_member count)))
          (is (= 0 (-> loaded-mini :projects_as_manager count)))
          (is (= 1 (-> loaded-mini :tasks count)))))
      (testing "Delete a person that a task points to"
        (is (-> (agg/load manage-task-to-person-er @db-con :task 1) :assignee))
        (agg/delete! er @db-con (agg/load er @db-con :person 2))
        (is (nil? (-> (agg/load manage-task-to-person-er @db-con :task 1) :assignee))))
      (testing "Delete the project"
        (agg/delete! er @db-con saved-project)))))


