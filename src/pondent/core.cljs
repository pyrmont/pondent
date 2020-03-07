(ns ^:figwheel-hooks pondent.core
  (:require [alandipert.storage-atom :refer [local-storage]]
            [clojure.string :as string]
            [goog.dom :as gdom]
            [goog.uri.utils :as uri]
            [pondent.github :as github]
            [pondent.markdown :as markdown]
            [pondent.posting :as posting]
            [pondent.time :as time]
            [promesa.core :as p]
            [reagent.core :as reagent :refer [atom]]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as router]))

;; Build-time defines
(goog-define gh-client-id "Define in the build file...")
(goog-define gh-auth-proxy "Define in the build file...")


;; App constants
(def max-chars 280)


;; Messages to display
(def messages
  {:success
   {:title "Post Created"
    :body "Your post was created. How about another?"}

   :missing-content
   {:title "Missing Content"
    :body "The post did not contain any content."}

   :missing-slug
   {:title "Missing Slug"
    :body "The post did not contain a slug."}

   :save-failure
   {:title "Save Failed"
    :body "The post was not saved to the repository. The server returned the
          following response:"}})


;; defaults for a post
(defn post-defaults []
  {:content nil
   :date (time/date->str (time/now))
   :slug nil
   :categories nil})


;; the settings
(defonce settings-state
  (local-storage
    (atom {:owner nil :repo nil :branch "master" :posts-dir nil
           :commit-message "Add a post" :user nil :password nil
           :init? false})
    :settings-state))


;; the app state
(defonce app-state (atom nil))
(defonce post-state (atom (post-defaults)))
(defonce result-state (atom nil))


(defn error-page [route]
  [:div {:class "bg-white max-w-md mx-auto my-4 shadow"}
   [:h2 {:class "bg-red-500 text-white font-bold px-4 py-2"} "Error"]
   [:p {:class "border border-t-0 border-red-400 rounded-b bg-red-100 px-4 py-3 text-red-700"} "There was an error."]
   (when-let [error (some-> route :data :error)]
     [:pre {:class "mt-3 overflow-x-auto"} (:body error)])])


(defn authorise-page [route]
  (let [query (-> route :parameters :query)
        code (:code query)]
    (-> (github/auth-token-via-proxy gh-auth-proxy code)
        (p/then (fn [x]
                  (if-let [token (:token x)]
                    (do (swap! settings-state assoc :gh-token token)
                        (router/replace-state ::settings))
                    (router/replace-state ::error)))))
    [:div {:class "bg-white max-w-md mx-auto my-4 p-4 shadow text-center"} "Authorising..."]))


(defn settings-item [item-name label placeholder]
  [:<>
    [:label {:class "font-semibold inline-block text-left w-3/12"} label]
    [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 my-2 w-9/12"
             :type "text"
             :value (item-name @settings-state)
             :placeholder placeholder
             :on-change #(swap! settings-state assoc item-name (-> % .-target .-value))}]])


(defn settings-github [authd?]
  (let [pat? (atom false)]
    (fn [authd?]
      [:<>
       (if @pat?
         [:<>
          [settings-item :user "User:" "Enter the GitHub user"]
          [settings-item :password "Token:" "Enter the GitHub access token"]]
         [:div {:class "inline-block my-2 w-9/12"}
          (let [colour  (if authd? "bg-green-600" "bg-black")
                url     (if authd? (str "https://github.com/settings/connections/applications/" gh-client-id)
                                  (github/auth-url gh-client-id))
                message (if authd? "Authorised with GitHub"
                                  "Authorise with GitHub")]
            [:a#authd {:class (str colour " lbtn lbtn-github text-left text-white")
                       :href url}
              [:i {:class "logo"}]
              [:p {:class "label"} message]])])
       [:div {:class "clearfix"}
        [:div {:class "inline-block my-2 text-left w-9/12"}
         [:input#pat {:class "mr-2"
                      :type "checkbox"
                      :checked @pat?
                      :on-change #(reset! pat? (not @pat?))}]
         [:label {:for "pat"} "Use personal access token"]]]])))


(defn settings-page [route]
  [:div#settings {:class "bg-white text-right max-w-md mx-auto my-4 p-4 shadow"}
   [:form {:on-submit (fn [x]
                        (.preventDefault x)
                        (swap! settings-state assoc :init? true)
                        (router/push-state ::composer))}
    [:h2 {:class "mb-2 text-center text-xl"} "Settings"]
    [settings-item :owner "Owner:" "Enter the repository owner"]
    [settings-item :repo "Repo:" "Enter the repository name"]
    [settings-item :branch "Branch:" "Enter the repository branch"]
    [settings-item :posts-dir "Directory:" "Enter the posts directory"]
    [settings-item :commit-message "Message:" "Enter the commit message"]
    [settings-github (some? (:gh-token @settings-state))]
    [:button {:class "bg-blue-500 hover:bg-blue-700 mt-4 px-4 py-2 rounded text-white"
              :type "submit"} "Compose"]]])


(defn composer-input-date [form input-name label placeholder]
  [:<>
    [:label {:class "font-semibold block mb-1 w-2/12"} label]
    [:input {:class "bg-gray-200 block focus:bg-white border border-gray-400 mb-3 p-2 w-full"
             :type "text"
             :value (input-name @form)
             :placeholder placeholder
             :on-change #(swap! form assoc input-name (-> % .-target .-value))}]])


(defn composer-input-text [form input-name label placeholder]
  [:<>
    [:label {:class "font-semibold block mb-1 w-2/12"} label]
    [:input {:class "bg-gray-200 block focus:bg-white border border-gray-400 mb-3 p-2 w-full"
             :type "text"
             :value (input-name @form)
             :placeholder placeholder
             :on-change #(swap! form assoc input-name (-> % .-target .-value))}]])


(defn composer-message [message]
  (when message
    [:div#message
     (if (posting/success? message)
       {:class "bg-teal-100 border border-teal-500 max-w-md mx-auto text-teal-900 px-4 py-3 my-2 rounded"}
       {:class "bg-red-100 border border-red-400 max-w-md mx-auto text-red-700 px-4 py-3 my-2 rounded"})
     [:h3 {:class "font-bold"} (-> message :kind messages :title)]
     [:p (-> message :kind messages :body)]
     (when-let [error (:error message)]
       [:pre {:class "mt-3 overflow-x-auto"} (:body error)])]))


(defn composer-page [route]
  [:<>
   [composer-message @result-state]
   [:div#composer {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow"}
    (let [chars-left (-> @post-state :content (markdown/chars-left max-chars))
          char-suffix (if (or (= 1 chars-left) (= -1 chars-left)) " character" " characters")
          danger? (< chars-left 15)
          show-title? (or (not (string/blank? (:title @post-state)))
                          (< chars-left -10))]
      [:form {:on-submit (fn [x]
                           (.preventDefault x)
                           (-> (posting/create-post @post-state @settings-state)
                               (p/then
                                 (fn [y]
                                   (reset! result-state y)
                                   (when (posting/success? y)
                                     (reset! post-state (post-defaults)))))))}
       (if show-title?
         [composer-input-text post-state :title "Title" "Enter a title"]
         [:span#counter {:class (str (if danger? "font-semibold text-red-700 ") "float-right mb-1 text-sm")} (str chars-left char-suffix " left")])
       [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                   :value (:content @post-state)
                   :placeholder "What do you want to say?"
                   :on-change #(swap! post-state assoc :content (-> % .-target .-value))}]
       [:p {:class "mb-3 text-gray-500 text-xs"} "You can enter a title once your post is longer than 290 characters."]
       [composer-input-date post-state :date "Date" "YYYY-MM-DD HH:MM"]
       [composer-input-text post-state :slug "Slug" "Enter a slug"]
       [composer-input-text post-state :categories "Categories" "Enter the categories (optional)"]
       [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-1 px-4 py-2 rounded text-white"
                 :type "button"
                 :on-click (fn [x]
                             (reset! post-state (post-defaults))
                             (reset! result-state nil))} "Reset"]
       [:button {:class "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-1 px-4 py-2 rounded text-white"
                 :type "submit"} "Post"]])]
   [:footer {:class "text-center"}
    [:a {:class "text-gray-500 underline"
         :href (router/href ::settings)} "Settings"]]])


(defn index-page [route]
  (router/replace-state (if (:init? @settings-state) ::composer ::settings)))


(defn app-container []
  (let [route (-> @app-state :route)
        view (-> route :data :view)]
    [view route]))


(defn mount [el]
  (reagent/render [app-container] el))


(defn get-app-element []
  (gdom/getElement "app"))


(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))


(defn move-query-params! []
  (let [url (.-href js/document.location)
        path (uri/getPath url)
        query (uri/getQueryData url)
        fragment (or (uri/getFragmentEncoded url) "/")]
    (when query
      (.replaceState js/history nil "" (str path "#" fragment "?" query)))))


(def routes
  [["/" {:name ::index
         :view index-page}]
   ["/error" {:name ::error
              :view error-page}]
   ["/authorise" {:name ::authorise
                  :view authorise-page}]
   ["/composer" {:name ::composer
                 :view composer-page}]
   ["/settings" {:name ::settings
                 :view settings-page}]])


(defn init! []
  (move-query-params!)
  (router/start!
    (reitit/router routes)
    (fn [m] (swap! app-state assoc :route m))
     ;; set to false to enable HistoryAPI
    {:use-fragment true})
  (mount-app-element))


(init!)


;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
;; (mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
