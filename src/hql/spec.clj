(ns hql.spec
  (:require
    [expound.alpha :as expound]
    [clojure.spec.alpha :as s]))

(s/def :hql/query-form
  (s/+
    (s/or :kw keyword?
          :qf :hql/query-form)))

(s/def :hql/var-form
  (s/map-of keyword? any?))

(s/def :hql/query-var-form
  (s/tuple keyword? :hql/var-form :hql/query-form))

(s/def :hql/form
  (s/or
    :query
    (s/tuple keyword? :hql/query-form)

    :query-with-vars
    (s/tuple keyword? :hql/var-form :hql/query-var-form)))

(def valid? (partial s/valid? :hql/form))

(def conform (partial s/conform :hql/form))

(def explain (partial expound/expound :hql/form))

(comment
  (require '[hql.spec :as hql-spec] :reload-all)

  (hql-spec/valid?
    [:hero [:id :title]])

  (hql-spec/conform
    ^:mutation
    [:CreateReview
     {:$ep       :Episode!
      :$review   :ReviewInput!
      :$filterId :ID!}
     [:createReview
      {:episode :$ep
       :review  :$review}
      [:id
       :stars
       :commentary
       :actors [:name :birthday]
       :others [:?include :!$filterID]
       :thing [:?skip :filterID
               :one
               :two
               :three]]]])



;  Possible forms are:
;
;    [:kw [:keyword]]
;    [:kw {:map :of} [params]]
;    [:kw {:map :of} [:kw2 {:map :of} [params]]]
;
;  Params are:
;    vector/list of [keyword, ]
;
;  Special keywords:
;    :?include -> [:?include :$someVar (s/or :keyword [])]
;    :?skip -> [:?skip :$someVar (s/or :keyword [])]

)
