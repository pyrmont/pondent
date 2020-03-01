(ns ^:figwheel-hooks pondent.core
  (:require [goog.dom :as gdom]
            [pondent.github :as github]
            [pondent.markdown :as markdown]
            [reagent.core :as reagent :refer [atom]]))

;; the maximum number of characters
(def max-chars 280)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:screen :settings}))
(defonce settings-state (atom {:owner nil :repo nil :branch "master" :posts-dir nil
                               :commit-message "Add a post" :user nil :password nil}))

(defn settings-item [value label placeholder]
  [:<>
    [:label {:class "font-semibold inline-block text-left w-3/12"} label]
    [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 my-2 w-9/12"
             :type "text"
             :value @value
             :placeholder placeholder
             :on-change #(reset! value (-> % .-target .-value))}]])

(defn settings []
  (let [owner (atom (:owner @settings-state))
        repo (atom (:repo @settings-state))
        branch (atom (:branch @settings-state))
        posts-dir (atom (:posts-dir @settings-state))
        commit-message (atom (:commit-message @settings-state))
        user (atom (:user @settings-state))
        password (atom (:password @settings-state))]
    (fn []
      [:div#settings {:class "bg-white text-right max-w-md mx-auto my-4 p-4 shadow"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (reset! settings-state {:owner @owner
                                                    :repo @repo
                                                    :branch @branch
                                                    :posts-dir @posts-dir
                                                    :commit-message @commit-message
                                                    :user @user
                                                    :password @password})
                            (swap! app-state assoc :screen :composer))}
        [:h2 {:class "mb-2 text-center text-xl"} "Settings"]
        [settings-item owner "Owner:" "Enter the repository owner"]
        [settings-item repo "Repo:" "Enter the repository name"]
        [settings-item branch "Branch:" "Enter the repository branch"]
        [settings-item posts-dir "Directory:" "Enter the posts directory"]
        [settings-item commit-message "Message:" "Enter the commit message"]
        [settings-item user "User:" "Enter the GitHub user"]
        [settings-item password "Token:" "Enter the GitHub access token"]
        [:button {:class "bg-blue-500 hover:bg-blue-700 mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Save"]]])))

(defn composer []
  (let [content (atom nil)
        counter (atom max-chars)
        date (atom nil)
        slug (atom nil)
        categories (atom nil)]
    (fn []
      [:div#composer {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (swap! app-state assoc :screen :composer))}
        [:span#counter {:class "float-right" } @counter]
        [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                    :value @content
                    :placeholder "What do you want to say?"
                    :on-change (fn [x]
                                 (reset! counter (-> x .-target .-value (markdown/chars-left max-chars)))
                                 (reset! content (-> x .-target .-value)))}]
        [:label {:class "font-semibold inline-block mt-3 w-2/12"} "Slug"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-full"
                 :type "text"
                 :value @slug
                 :placeholder "Enter a slug (optional)"
                 :on-change #(reset! slug (-> % .-target .-value))}]
        [:label {:class "font-semibold inline-block mt-3 w-2/12"} "Categories"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-full"
                 :type "text"
                 :value @categories
                 :placeholder "Enter the categories (optional)"
                 :on-change #(reset! categories (-> % .-target .-value))}]
        [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "button"
                  :on-click #(swap! app-state assoc :screen :timeline)} "Reset"]
        [:button {:class "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Post"]]])))

(defn app-container []
  (case (:screen @app-state)
    :settings [settings]
    :composer [composer]))

(defn mount [el]
  (reagent/render [app-container] el))

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
