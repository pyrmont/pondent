(ns pondent.github
  (:require [clojure.string :as string]
            [goog.crypt.base64 :as base64]
            [httpurr.client.xhr :as http]
            [httpurr.status :as s]
            [promesa.core :as p]))


;; General functions

(defn add-headers
  "Add headers to a request."
  [request {:keys [token user password] :as opts}]
  (assoc request :headers
    (merge
      {"Accept" "application/json"
       "Content-Type" "application/json"}
      (when token
        {"Authorization" (str "token " token)})
      (when (not (or (string/blank? user) (string/blank? password)))
        {"Authorization" (str "Basic " (base64/encodeString (str user ":" password)))}))))


(defn body->map
  "Decode the body of an HTTP response object into a Clojure map."
  [body]
  (-> (js/JSON.parse body) (js->clj :keywordize-keys true)))


(defn github-url
  "Make the GitHub URL."
  [url-part]
  (str "https://api.github.com" url-part))


(defn map->body
  "Encode a Clojure map into the body of an HTTP response object."
  [data]
  (-> data clj->js js/JSON.stringify))


(defn process-create-response
  "Process the response from GitHub and return the appropriate."
  [response]
  (assoc response :status (condp = (:status response)
                            s/created      :success
                            s/not-found    :not-found
                            s/unauthorized :unauthorized
                            :error)))


;; HTTP methods

(defn GET
  "Make a GET request."
  [url payload opts]
  (http/get url (-> {:body payload} (add-headers opts))))


(defn PUT
  "Make a PUT request."
  [url payload opts]
  (http/put url (-> {:body payload} (add-headers opts))))


;; High level actions

(defn create-file
  "Create a file in the GitHub repository in one step."
  [{:keys [content path commit-message] :as data}
   {:keys [owner repo user password gh-token] :as settings}]
  (let [url     (github-url (str "/repos/" owner "/" repo "/contents/" path))
        payload (map->body {:message commit-message
                            :content content})
        opts    {:token gh-token :user user :password password}]
    (-> (PUT url payload opts)
        (p/then #(process-create-response %)))))


; ;; Authorisation functions


(defn authd?
  "Check whether the authorisation `gh-token` is valid."
  [gh-token]
  (-> (github-url nil)
      (GET "" {:token gh-token})
      (p/then #(= 200 (:status %)))))


(defn auth-url
  "Make the GitHub authorisation URL."
  [client-id]
  (let [url "https://github.com/login/oauth/authorize?client_id="
        scope "&scope=repo"]
    (str url client-id scope)))


(defn auth-token-via-proxy
  "Get an API token for a given app."
  [proxy-url code]
  (-> (str proxy-url code)
      (GET "" {})
      (p/then #(-> (:body %) body->map))))
