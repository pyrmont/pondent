(ns ^:figwheel-hooks pondent.core
  (:require [goog.dom :as gdom]
            [goog.uri.utils :as uri]
            [pondent.pages.about :as about]
            [pondent.pages.authorise :as authorise]
            [pondent.pages.composer :as composer]
            [pondent.pages.error :as error]
            [pondent.pages.index :as index]
            [pondent.pages.settings :as settings]
            [reagent.core :as reagent :refer [atom]]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as router]))


;; Build-time defines
(goog-define gh-client-id "Define in the build file...")
(goog-define gh-auth-proxy "Define in the build file...")


;; the app state
(defonce app-state (atom nil))


(defn app-container []
  (let [route (-> @app-state :route)
        view  (-> route :data :view)]
    [:div#frame
     [view route]
     [:footer {:class "text-center"}
      [:a {:class "mx-4 text-gray-500 underline"
           :href (router/href ::about)} "About"]
      [:a {:class "mx-4 text-gray-500 underline"
           :href (router/href ::composer)} "Composer"]
      [:a {:class "mx-4 text-gray-500 underline"
           :href (router/href ::settings)} "Settings"]
      [:a {:class "mx-4 text-gray-500 underline"
           :href "https://github.com/pyrmont/pondent/"} "Source"]]]))


(defn mount [el]
  (reagent/render [app-container] el))


(defn get-app-element []
  (gdom/getElement "app"))


(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))


(defn move-query-params! []
  (let [url      (.-href js/document.location)
        path     (uri/getPath url)
        query    (uri/getQueryData url)
        fragment (or (uri/getFragmentEncoded url) "/")]
    (when query
      (.replaceState js/history nil "" (str path "#" fragment "?" query)))))


(def routes
  [["/"          {:name ::index
                  :view index/index-page}]
   ["/about"     {:name ::about
                  :view about/about-page}]
   ["/error"     {:name ::error
                  :view error/error-page}]
   ["/authorise" {:name ::authorise
                  :view authorise/authorise-page}]
   ["/composer"  {:name ::composer
                  :view composer/composer-page}]
   ["/settings"  {:name ::settings
                  :view settings/settings-page}]])


(defn init! []
  (move-query-params!)
  (router/start!
    (reitit/router routes)
    (fn [m] (swap! app-state assoc :route m))
     ;; set to false to enable HistoryAPI
    {:use-fragment true})
  (mount-app-element))


(init!)


;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
