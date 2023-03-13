(ns hql.core-test
  (:require
    [hql.core :as hql]
    [clojure.test :refer [are deftest testing]]))


(deftest queries
  (testing "simple graphql query transform"
    (are
      [expected given] (= expected (hql/h given))
      "{hero{id title}}"
      [:hero
       [:id :title]]

      "{hero(id: 100){id title friends {id}}}"
      [:hero {:id 100}
       [:id :title :friends [:id]]]

      "{hero(id: 100, showFriends: false, showTitle: true){id title @skip(if: !showTitle) friends @include(if: showFriends){id name}}}"
      [:hero {:id 100 :showFriends false :showTitle true}
       [:id
        :title   [:?skip :!showTitle]
        :friends [:?include :showFriends
                  :id
                  :name]]])))

(deftest mutations
  (testing "simple graphql mutation transform"
    (are
      [expected given] (= expected (hql/h (with-meta given {:mutation true})))

      (str "mutation CreateReview($ep: Episode!, $review: ReviewInput!) "
           "{createReview(episode: $ep, review: $review)"
           "{id stars commentary}}")
      [:CreateReview
       {:$ep     :Episode!
        :$review :ReviewInput!}
       [:createReview
        {:episode :$ep
         :review  :$review}
        [:id :stars :commentary]]])))

