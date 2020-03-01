(ns pondent.github
  (:require [goog.crypt.base64 :as base64]
            [httpurr.client.xhr :as http]
            [pondent.time :as time]
            [promesa.core :as p]))

;; General functions

(defn add-headers
  "Add headers to a request."
  [request {:keys [user password] :as headers}]
  (let [userpass (base64/encodeString (str user ":" password))]
    (assoc request :headers {"Accept" "application/json"
                             "Authorization" (str "Basic " userpass)
                             "Content-Type" "application/json"})))

(defn body->map
  "Decode the body of an HTTP response object into a Clojure map."
  [response]
  (-> response :body js/JSON.parse (js->clj :keywordize-keys true)))

(defn github-url
  "Make the GitHub URL."
  [url-part]
  (str "https://api.github.com" url-part))

(defn map->body
  "Encode a Clojure map into the body of an HTTP response object."
  [data]
  (-> data clj->js js/JSON.stringify))

(defn post-path
  "Make the path to the post."
  [dir date slug]
  (str dir (time/date->str date "yyyy-MM-dd") "-" slug ".md"))

;; HTTP methods

(defn PUT
  "Make a PUT request."
  [url payload opts]
  (http/put url (-> {:body payload} (add-headers opts))))

; ;; High level actions

(defn create-post
  "Create a post in the GitHub repository in one step."
  [{:keys [content date slug] :as data}
   {:keys [owner repo dir commit-message user password] :as settings}]
  (let [path    (post-path dir date slug)
        url     (github-url (str "/repos/" owner "/" repo "/contents/" path))
        payload (map->body {:message commit-message
                            :content (base64/encodeString content)})
        opts    {:user user :password password}]
    (PUT url payload opts)))
