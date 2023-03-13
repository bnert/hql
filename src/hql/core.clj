(ns  hql.core
  "Parse hiccup -> graphql queries"
  (:require
    [clojure.string :as str]
    [hql.spec :as s]))

(defn decl? [v]
  (and (vector? v)
       (contains? #{:?include :?skip} (nth v 0))
       (<= 2 (count v))))

(defn children? [v]
  (<= 3 (count v)))

(defn children [decl]
  (subvec decl 2))

(defn decl->str [v]
  (str "@"
       (subs (name (nth v 0)) 1)
       "(if: "
       (name (nth v 1))
       ")"))

(defn vars->str [m]
  (->> m
       (map (fn [[k v]]
              (let [v (if (keyword? v) (name v) v)]
                (str (name k) ": " v))))
       (str/join ", ")))

(defn query-with-vars? [hf]
  (and (= 3 (count hf))
       (keyword? (nth hf 0))
       (map? (nth hf 1))
       (vector? (nth hf 2))))

(defn query-no-vars? [hf]
  (and (= 2 (count hf))
       (keyword? (nth hf 0))
       (vector? (nth hf 1))))

(defn parse-query [hf]
  (cond
    (query-with-vars? hf)
      (let [[resource vars & rest-hf] hf]
        (str "{"
             (name resource)
             "("
             (vars->str vars)
             ")"
             (parse-query rest-hf)
             "}"))
    (query-no-vars? hf)
      (let [[resource & rest-hf] hf]
        (str "{" (name resource) (parse-query rest-hf) "}"))
    :else
      (->> hf
           (map
             (fn [v]
               (cond
                 (and (decl? v) (children? v))
                   (str (decl->str v)
                        "{"
                        (parse-query (children v))
                        "}")
                 (decl? v)
                   (decl->str v)
                 (vector? v)
                   (str "{" (parse-query v) "}")
                 :else
                   (name v))))
           (str/join " "))))

(defn parse-mutation [hf]
  (let [[resource vars rest-hf] hf]
    (str "mutation " (name resource)
         "(" (vars->str vars) ") "
         (parse-query rest-hf))))

(defn h [hiccup-form?]
  {:pre? [(s/valid? hiccup-form?)]}
  (when-not (seq hiccup-form?)
    (throw (ex-info "nil hiccup form"
                    {})))
  (let [hf    hiccup-form?
        meta' (meta hf)]
    (if (true? (:mutation meta'))
      (parse-mutation hf)
      (parse-query hf))))

(comment
  (require '[hql.core :as h] :reload-all) 

  (h/h [:hero [:id :title]])

  (h/h
    [:listReviews
      {:episode :$ep
       :review  :$review}
      [:id
       :stars
       :commentary
       :actors [:name :birthday]
       :others [:?include :!$filterID]
       :thing  [:?skip :$filterID
                :one :two :three]]])

  (h/h
    ^:mutation
    [:CreateReview
     {:$ep     :Episode!
      :$review :ReviewInput!}
     [:createReview
      {:episode :$ep
       :review  :$review}
      [:id :stars :commentary]]])
)
