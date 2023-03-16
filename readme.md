### Overview

This library aims to provide the ability to translate hiccup like data structures
into graphql query strings.

The library is to be as "close to the metal" as can be, in order to provide a


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
  ; => {hero{id name friends {id name}}}

  ; With directives
  (println
    (hql/h
      [:hero
       {:showName false}
       [:id
        :name [:?include :showName]
        :friends
        [:id
         :name [:?include :showName]]]]))
  ; => {hero{id name @include(if: showName) friends {id name @include(if: showName)}}}

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
  ; => mutation CreateReview($ep: Episode!, $review: ReviewInput!) {createReview(episode: $ep, review: $review) {id stars commentary}}

```

### Roadmap
- [x] Shorthand query conversion
- [ ] Longhand query conversion (inferred via variables/structure)
- [ ] Nested field arguments
- [x] Mutations conversion
- [ ] Subscription conversion
- [x] Basic Directives (i.e. `@include`, `@skip`)
  - [ ] Support for flexible/custom directives
  - [ ] Complex directives
- [ ] Fragments
  - [ ] Inline
  - [ ] Explicit
- [ ] Pretty formatting (only compact right now)

