(ns pondent.posting
  (:require [clojure.string :as string]
            [goog.crypt.base64 :as base64]
            [pondent.github :as github]
            [pondent.time :as time]
            [promesa.core :as p]))

(defn format-post
  "Format a post for Jekyll."
  [body date title categories]
  (str "---\n"
       "date: " (time/date->str date "yyyy-MM-dd HH:mm:ss Z\n")
       (when title (str "title: \"" title "\"\n"))
       (when categories (str "categories: ["
                             (->> (string/split categories #" ")
                                  (map #(str "\"" % "\""))
                                  (string/join ", "))
                             "]\n"))
       "---\n\n"
       body))

(defn post-path
  "Construct a path to the post."
  [dir date slug]
  (str dir (time/date->str date "yyyy-MM-dd") "-" slug ".md"))

(defn input-errors
  "Return any errors in the input."
  [{:keys [content slug]}]
  (cond
    (string/blank? content) {:kind :missing-content}
    (string/blank? slug)    {:kind :missing-slug}))

(defn success?
  "Returns `true` if `result` represents success."
  [result]
  (and (map? result) (= :success (:kind result))))

; (defn create-post [post settings] (p/promise {:kind :success}))

(defn create-post
  "Create the `post` using the `settings`."
  [post settings]
  (if-let [error (input-errors post)]
    (p/resolved error)
    (let [date (or (time/str->date (:date post))
                   (time/now))
          content (-> (:content post)
                      (format-post date (:title post) (:categories post))
                      base64/encodeString)
          path (post-path (:posts-dir settings) date (:slug post))
          commit-message (:commit-message settings)]
      (-> (github/create-file {:content content :path path :commit-message commit-message}
                          settings)
          (p/then (fn [x]
                    (if (= :success (:status x))
                      {:kind :success}
                      {:kind :save-failure :error x})))))))
