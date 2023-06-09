### Overview

This is mainly an educational exercise to tease out a hiccup-like graphql syntax for clojure.


**Example**
```clojure
(ns app.core
  (:require
    [hql.core :as hql]))

(defn -main [& _args]
  ; Simple Query
  (println
    (hql/h
      [:hero
       [:id
        :name
        :friends [:id :name]]]))
  ; => { hero { id name friends { id name} } }

  ; With directives (directives are prefixed with "#")
  (println
    (hql/h
      [:hero
       {:showName false}
       [:id
        :name :#include {:if :showName}
        :friends
        [:id
         :name :#skip {:if :showName}]]]))
  ; => { hero { id name @include(if: showName) friends { id name @include(if: showName) } } }

  ; With Fragments + Aliases (inline/explicit)
  ; Aliases are prefixed with "&"
  (println
    (hql/h
      ^:query
      [:HeroComparison {:$first [:Int 3]}
       [:&left :hero {:episode :EMPIRE} [:...charComp]
        :&right :hero {:episode :JEDI} [:...charComp]]

       ^{:fragment [:comparisonFields :Character]}
       [:name
        :friendsConnection {:first :$first}
        [:totalCount :edges [:node [:name]]]]))
  ; => query HeroComparison($first: Int = 3) { left: hero(episode: EMPIRE) { ...charComp } right: hero(episode: JEDI) { ...charComp } } fragment comparisonFields on Character { name friendsConnection(first: $first) { totalCount edges { node { name } } } }

  ; Query with Operation Name + Variables
  (println
    (hql/h
      [^:query
       [:AllHeroesWithPower {:$first [:Int := 100]}
        [:hero {:powerIn :$powers}
         [:id :name :friends]]]]

  ; Mutation
  (println
    (hql/h
      ^:mutation
      [:CreateReview
       {:$ep     :Episode!
        :$review :ReviewInput!}
       [:createReview
        {:episode :$ep
         :review  :$review}
        [:id :stars :commentary]]]))
  ; => mutation CreateReview($ep: Episode!, $review: ReviewInput!) { createReview(episode: $ep, review: $review) {id stars commentary } }


  ; Subscription
  (println
    (hql/h
      ^:subscription
      [:StoryLikeSubscription {:$input :StoryLikeSubscribeInput}
       [:storyLikeSubscribe {:input :$input}
        [:story
         [:likers [:count]
          :likeSentence [:text]]]]]))
  ; => subscription StoryLikeSubscription($input: StoryLikeSubscribeInput) { storyLikeSubscribe(input: $input) { story { likers { count } likeSentence { text } } } }
```

### Roadmap

#### Parser/Transiple
- [x] Shorthand query conversion
- [x] Longhand query conversion (inferred via variables/structure)
- [x] Nested field arguments
- [x] Mutations conversion
- [x] Subscription conversion
- [x] Directives (i.e. `@include`, `@skip`)
- [x] Aliases
- [x] Default values for variables
- [x] Fragments
  - [x] Inline
  - [x] Explicit
