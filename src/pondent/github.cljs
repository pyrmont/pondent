(ns pondent.github
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [goog.crypt.base64 :as base64]
            [httpurr.client.xhr :as http]
            [httpurr.status :as status]
            [promesa.core :as p]))


;; Specs

(def url-path-regex #"[\w./%#?&=-]+")
(def url-regex #"https?://[\w.-]+[\w./%#?&=-]+")
(def slug-regex #"[\w-]")


(s/def ::token (s/nilable string?))
(s/def ::user (s/nilable string?))
(s/def ::password (s/nilable string?))
(s/def ::json string?)
(s/def ::url-path (s/and string? #(re-matches url-path-regex %)))
(s/def ::url (s/and string? #(re-matches url-regex %)))
(s/def ::promise #(instance? js/Promise %))
(s/def ::content string?)
(s/def ::path ::url-path)
(s/def ::commit-message string?)
(s/def ::owner (s/and string? #(re-matches slug-regex %)))
(s/def ::repo (s/and string? #(re-matches slug-regex %)))
(s/def ::gh-user (s/and string? #(re-matches slug-regex %)))
(s/def ::gh-password string?)
(s/def ::file-data (s/keys :req-un [::content ::path ::commit-message]))
(s/def ::settings (s/keys :req-un [::owner ::repo ::gh-user ::gh-password ::gh-token]))



(s/fdef add-headers :args (s/cat :request map? :opts (s/keys :req-un [::token ::user ::password])) :ret map?)
(s/fdef body->map :args (s/cat :body ::json) :ret map?)
(s/fdef github-url :args (s/cat :url-part ::url-path) :ret ::url)
(s/fdef map->body :args (s/cat :data map?) :ret ::json)
(s/fdef process-create-response :args (s/cat :response map?) :ret map?)
(s/fdef GET :args (s/cat :url ::url :body ::json :opts map?) :ret ::promise)
(s/fdef PUT :args (s/cat :url ::url :body ::json :opts map?) :ret ::promise)
(s/fdef create-file :args (s/cat :data ::file-data :settings ::settings) :ret ::promise)
(s/fdef authd? :args (s/cat :gh-token string?) :ret ::promise)
(s/fdef app-url :args (s/cat :client-id string?) :ret ::url)
(s/fdef auth-url :args (s/cat :client-id string?) :ret ::url)
(s/fdef auth-token-via-proxy :args (s/cat :proxy-url ::url :code string?) :ret ::promise)


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
                            status/created      :success
                            status/not-found    :not-found
                            status/unauthorized :unauthorized
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
   {:keys [owner repo gh-user gh-password gh-token] :as settings}]
  (let [url     (github-url (str "/repos/" owner "/" repo "/contents/" path))
        payload (map->body {:message commit-message
                            :content content})
        opts    {:token gh-token :user gh-user :password gh-password}]
    (-> (PUT url payload opts)
        (p/then #(process-create-response %)))))


;; Authorisation functions

(defn authd?
  "Check whether the authorisation `gh-token` is valid."
  [gh-token]
  (-> (github-url nil)
      (GET "" {:token gh-token})
      (p/then #(= 200 (:status %)))))


(defn app-url
  "Make the GitHub application URL."
  [client-id]
  (str "https://github.com/settings/connections/applications/" client-id))


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
