(ns hql.core-test
  (:require
    [clojure.string :as str]
    [hql.core :as hql]
    [clojure.test :refer [are deftest testing]]))

(defn ->terse-gql [s]
  (-> s
      (str/replace #"\n" " ")
      (str/replace #"\s{2,}" " ")
      str/trim))

(deftest queries
  (testing "graphql query transform"
    (are
      [expected given] (= (->terse-gql expected) (apply hql/h given))

      "{ hero { id title } }"
      [[:hero
       [:id :title]]]

      "{ hero(id: 100) { id title friends { id } } }"
      [[:hero {:id 100}
        [:id :title :friends [:id]]]]

      "{ hero(id: 100) { id title friends height(unit: FOOT) } }"
      [[:hero {:id 100}
        [:id :title :friends :height {:unit :FOOT}]]]

      "{ hero(id: 100, showFriends: false, showTitle: true)
        {
          id
          title
          @skip(if: !showTitle)
          powers @valid(not: showTitle)
          friends @include(if: showFriends) {
            id
            name
          }
        }
      }"
      [[:hero {:id 100 :showFriends false :showTitle true}
        [:id
         :title :#skip {:if :!showTitle}
         :powers :#valid {:not :showTitle}
         :friends :#include {:if :showFriends}
         [:id :name]]]]

      "query AllHeroes { hero { id title friends } }"
      [^:query
       [:AllHeroes
        [:hero
         [:id :title :friends]]]]

      "query AllHeroesWithPower($power: [Power!]) {
        hero(powerIn: $powers) {
          id
          name
          friends
        }
      }"
      [^:query
       [:AllHeroesWithPower
        {:$power "[Power!]"}
        [:hero {:powerIn :$powers}
         [:id :name :friends]]]]

      "query AllHeroesWithPower($first: Int = 12, $offset: Int = 0, $match: String = \"\") {
        hero(first: $first, offset: $offset, match: $match) {
          id
          name
          friends
        }
      }"
      [^:query
       [:AllHeroesWithPower
        {:$first [:Int 12] :$offset [:Int 0] :$match [:String ""]}
        [:hero {:first :$first :offset :$offset :match :$match}
         [:id :name :friends]]]]

      "query AllHeroesWithNames($names: [String!]!, $nicks: [String!] = [\"Golden boy\"]) {
        hero(names: $names, nicknames: $nicks) {
          id
          name
          friends
        }
      }"
      [^:query
       [:AllHeroesWithNames
        {:$names (list :String!)
         :$nicks [(hql/nilable (list :String!))
                  ["Golden boy"]]}
        [:hero {:names :$names, :nicknames :$nicks}
         [:id :name :friends]]]]

      "{
         left: hero(episode: EMPIRE) {
           ...charComp
         }
         right: hero(episode: JEDI) {
           ...charComp
         }
      }

      fragment charComp on Character {
        id
        name
        friends {
          id
        }
      }
      "
      [[:&left :hero {:episode :EMPIRE} [:...charComp]
        :&right :hero {:episode :JEDI} [:...charComp]]

       ^{:fragment [:charComp :Character]}
       [:id :name :friends [:id]]]

      "query HeroComparison($first: Int = 3) {
         left: hero(episode: EMPIRE) {
           ...charComp
         }
         right: hero(episode: JEDI) {
           ...charComp
         }
      }

      fragment comparisonFields on Character {
        name
        friendsConnection(first: $first) {
          totalCount
          edges {
            node {
              name
            }
          }
        }
      }"
      [^:query
       [:HeroComparison {:$first [:Int 3]}
        [:&left :hero {:episode :EMPIRE} [:...charComp]
         :&right :hero {:episode :JEDI} [:...charComp]]]

       ^{:fragment [:comparisonFields :Character]}
       [:name
        :friendsConnection {:first :$first}
        [:totalCount :edges [:node [:name]]]]]

      "{
         left: hero(episode: EMPIRE) {
           ...withId
           ...charComp
         }
         right: hero(episode: JEDI) {
           ...charComp
         }
      }

      fragment comparisonFields on Character {
        name
        friendsConnection(first: $first) {
          totalCount
          edges {
            node {
              name
            }
          }
        }
      }

      fragment withId on Character {
        id
      }"
      [[:&left :hero {:episode :EMPIRE} [:...withId :...charComp]
        :&right :hero {:episode :JEDI} [:...charComp]]

       ^{:fragment [:comparisonFields :Character]}
       [:name
        :friendsConnection {:first :$first}
        [:totalCount :edges [:node [:name]]]]

       ^{:fragment [:withId :Character]}
       [:id]])))

(deftest mutations
  (testing "graphql mutation transform"
    (are
      [expected given] (= (->terse-gql expected)
                          (hql/h (with-meta given {:mutation true})))

      "mutation CreateReview($ep: Episode!, $review: ReviewInput!) {
        createReview(episode: $ep, review: $review) {
          id
          stars
          commentary
        }
      }"
      [:CreateReview {:$ep :Episode!, :$review :ReviewInput!}
       [:createReview
        {:episode :$ep, :review :$review}
        [:id :stars :commentary]]])))


(deftest subscriptions
  (testing "graphql subscription transform"
    (are
      [expected given] (= (->terse-gql expected)
                          (hql/h given))

      "subscription StoryLikeSubscription($input: StoryLikeSubscribeInput) {
        storyLikeSubscribe(input: $input) {
          story {
            likers {
              count
            }
            likeSentence {
              text
            }
          }
        }
      }"
      ^:subscription
      [:StoryLikeSubscription {:$input :StoryLikeSubscribeInput}
       [:storyLikeSubscribe {:input :$input}
        [:story
         [:likers [:count]
          :likeSentence [:text]]]]])))

