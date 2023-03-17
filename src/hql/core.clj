(ns  hql.core
  "Parse hiccup -> graphql queries"
  (:require
    [clojure.string :as str]
    [hql.spec :as s]))

(defn ->list-arg [v]
  (if (not= 1 (count v))
    (throw (ex-info "list argument must be of length 1" {:form v}))
    ;; TODO: determine best way to handle "nilable" list
    ;; or should we assume that a list won't ever be nil?
    (let [nilable? (:nil? (meta v))]
      (str "["
           (cond
             (keyword? (first v)) (name (first v))
             :else (first v))
           "]"
           (if nilable? "" "!")))))

(defn nilable [v]
  (with-meta v {:nil? true}))

(defn ->type-with-default [v]
  (if (not= 2 (count v))
    (throw (ex-info "invalid arg with default type"
                    {:form v
                     :needs "must be a seq of form [:Type value]"}))
    (str (cond
           (keyword? (first v))
             (name (first v))
           (sequential? (first v))
             (->list-arg (first v))
           :else
             (first v))
         " = "
         (cond
           (keyword? (second v))
             (str "\"" (name (second v)) "\"")
           (string? (second v))
             (str "\"" (second v) "\"")
           (sequential? (second v))
             (str "[" (str/join ", " (map #(str "\"" % "\"") (second v))) "]")
           :else
             (second v)))))

(defn vars->str [m]
  (str "("
       (->> m
            (map (fn [[k v]]
                   (str (name k)
                        ": "
                        (cond
                          (list? v)
                            (->list-arg v)
                          (vector? v)
                            (->type-with-default v)
                          (keyword? v)
                            (name v)
                          :else
                            v))))
            (str/join ", "))
       ")"))

(defn query-with-vars? [hf]
  (and (= 3 (count hf))
       (keyword? (nth hf 0))
       (map? (nth hf 1))
       (vector? (nth hf 2))))

(defn query-no-vars? [hf]
  (and (= 2 (count hf))
       (keyword? (nth hf 0))
       (vector? (nth hf 1))))

(defn parse-alias [a]
  (str (subs (name a) 1) ":"))

(defn parse-directive [directive]
  (str "@" (subs (name directive) 1)))

(defn parse-query [hf]
  (cond
    (query-with-vars? hf)
      (let [[resource vars rest-hf] hf]
        (str "{ "
             (name resource)
             (vars->str vars)
             " "
             (parse-query rest-hf)
             " }"))
    (query-no-vars? hf)
      (let [[resource rest-hf] hf]
        (str "{ " (name resource) " " (parse-query rest-hf) " }"))
    (vector? hf)
      (loop [curr-i 0
             last-i (dec (count hf))
             result "{ "
             hf' hf]
        (let [cf (first hf')
              rf (rest hf')]
          (if-not cf
            (str result " }")
            (cond
              (vector? cf)
                (recur (inc curr-i)
                       last-i
                       (str result
                            (parse-query cf)
                            (if (= curr-i last-i) "" " "))
                       rf)
              (map? cf)
                (recur (inc curr-i)
                       last-i
                       ;; removes the space inbetween an argument
                       ;; and a declaration
                       (let [curr (reduce str (drop-last 1 result))]
                          (str curr
                               (vars->str cf)
                               (if (= curr-i last-i)
                                 ""
                                 " ")))
                       rf)
              (or (keyword? cf)
                  (string? cf))
                (let [first-char (first (name cf))
                      alias?     (= \& first-char )
                      directive? (= \# first-char)
                      last?      (= curr-i last-i)]
                  (recur (inc curr-i)
                         last-i
                         (str
                           result
                           (cond
                             alias?     (parse-alias cf)
                             directive? (parse-directive cf)
                             :else      (name cf))
                           (if last? "" " "))
                         rf))
              :else
                (throw (ex-info "invalid form inner" {:kind (class cf)}))))))
      :else
        (throw (ex-info "invalid form outer" {:kind (class hf)}))))

(defn parse-with-op-name [op-name hf]
  (let [[resource vars rest-hf] hf
        ;; Check is vars is actually
        ;; sub-query. This is the case for queries w/ op names,
        ;; as said queries may not have arguments. I am guessing
        ;; the same can be said for subscriptions & mutations w/ no args
        no-args? (and (vector? vars)
                      (nil? rest-hf))
        rest-hf (if no-args? vars rest-hf)
        vars (if no-args? nil vars)]
    (str op-name " " (name resource)
         (if no-args? " " (str (vars->str vars) " "))
         (parse-query rest-hf))))

(defn frament->op-name [fm]
  (str "fragment " (name (first fm)) " on " (name (second fm))))

(defn h [& hiccup-forms?]
  {:pre? [(every? s/valid? hiccup-forms?)]}
  (loop [[hf & rf] hiccup-forms?
         curr-i    0
         result    ""]
    (if-not hf
      result
      (recur
        rf
        (inc curr-i)
        (str
          result
          (cond
            (= curr-i 0) ""
            :else " ")
          (let [meta' (meta hf)]
           (cond
             (true? (:query meta'))
               (parse-with-op-name "query" hf)
             (true? (:mutation meta'))
               (parse-with-op-name "mutation" hf)
             (true? (:subscription meta'))
               (parse-with-op-name "subscription" hf)
             (vector? (:fragment meta'))
               (str (frament->op-name (:fragment meta'))
                    " "
                    (parse-query hf))
             :else
               (parse-query hf))))))))

