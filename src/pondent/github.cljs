(ns pondent.github
  (:require [goog.crypt.base64 :as base64]
            [httpurr.client.xhr :as http]
            [promesa.core :as p]))

(defonce userpass (atom nil))
(defonce posts-dir (atom nil))
(defonce commit-message (atom "Add a post"))

;; General functions

(defn format-userpass
  "Transform `user` and `password` into a Base64-encoded string appropriate for)
  use in HTTP basic authentication."
  [user password]
  (base64/encodeString (str user ":" password)))

(defn headers
  "Create the headers for requests."
  [request]
  (assoc request :headers {"Accept" "application/json"
                           "Authorization" (str "Basic " @userpass)
                           "Content-Type" "application/json"}))
                           ; "WWW-Authenticate" (str  "Basic realm=\"Pondent\"")}))

(defn github-url
  "Make the GitHub URL."
  [url-part]
  (str "https://api.github.com" url-part))

(defn post-path
  "Make the path to the post."
  [slug]
  (str @posts-dir slug ".md"))

(defn body->map
  "Decode the body of an HTTP response object into a Clojure map."
  [response]
  (-> response :body js/JSON.parse (js->clj :keywordize-keys true)))

(defn map->body
  "Encode a Clojure map into the body of an HTTP response object."
  [data]
  (-> data clj->js js/JSON.stringify))

;; Preparatory functions

(defn commit-data
  "Prepare data to be submitted to GitHub as a commit."
  [prev-commit new-tree]
  {:message @commit-message
   :parents [(:sha prev-commit)]
   :tree (:sha new-tree)})

(defn tree-data
  "Prepare data to be submitted to GitHub as a tree."
  [text-blob post-data prev-tree]
  {:base_tree (:sha prev-tree)
   :tree [
          {:path (post-path (:slug post-data))
           :mode "100644"
           :type "blob"
           :sha (:sha text-blob)}]})

;; HTTP methods

(defn GET
  "Make a GET request."
  [url payload]
  (http/get url (-> {} headers)))

(defn POST
  "Make a POST request."
  [url payload]
  (http/post url (-> {:body payload} headers)))

;; GitHub commands

(defn post-head
  "Post a reference to the HEAD of the `branch` in the GitHub `repo`."
  [sha repo branch]
  (p/let [body (map->body {:sha sha, :force false})
          url (github-url (str "/repos/" repo "/git/refs/heads/" branch))
          response (POST url body)]
    (body->map response)))

(defn post-commit
  "Post `commit` to the GitHub `repo`."
  [commit repo]
  (p/let [body (map->body commit)
          url (github-url (str "/repos/" repo "/git/commits"))
          response (POST url body)]
    (:sha (body->map response))))

(defn post-tree
  "Post `tree` to the GitHub `repo`."
  [tree repo]
  (p/let [body (map->body tree)
          url (github-url (str "/repos/" repo "/git/trees"))
          response (POST url body)]
    (body->map response)))

(defn post-text
  "Post `text` to the GitHub `repo`."
  [text repo]
  (p/let [body (map->body {:content text, :encoding "UTF-8"})
          url (github-url (str "/repos/" repo "/git/blobs"))
          response (POST url body)]
    (body->map response)))

(defn get-tree
  "Get data about the tree referred to in `commit`."
  [commit]
  (p/let [url (-> commit :tree :url)
          response (GET url {})]
    (body->map response)))

(defn get-commit
  "Get data about the commit referred to in `head`."
  [head]
  (p/let [url (-> head :object :url)
          response (GET url {})]
    (body->map response)))

(defn get-head
  "Get a reference to the HEAD of the `branch` in the GitHub `repo`."
  [repo branch]
  (p/let [url (github-url (str "/repos/" repo "/git/ref/heads/" branch))
          response (GET url {})]
    (body->map response)))

; ;; High level actions

(defn create-post
  "Create a post in the GitHub repository."
  [data repo branch]
  (p/let [prev-head    (get-head repo branch)
          prev-commit  (get-commit prev-head)
          prev-tree    (get-tree prev-commit)
          text-input   (:content data)
          text-blob    (post-text text-input repo)
          tree-input   (tree-data text-blob data prev-tree)
          new-tree     (post-tree tree-input repo)
          commit-input (commit-data prev-commit new-tree)
          new-commit   (post-commit commit-input repo)
          new-head     (post-head new-commit repo branch)]
    new-head))

; ;; Entry function

(defn send-post
  "Send data to the GitHub repository."
  [data repo branch]
  (case (:type data)
    "create" (create-post data repo branch)))
