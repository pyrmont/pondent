(ns ^:figwheel-hooks pondent.core
  (:require [alandipert.storage-atom :refer [local-storage]]
            [goog.dom :as gdom]
            [pondent.github :as github]
            [pondent.markdown :as markdown]
            [pondent.time :as time]
            [reagent.core :as reagent :refer [atom]]))

;; the maximum number of characters
(def max-chars 280)

;; define your app data so that it doesn't get over-written on reload
(defonce settings-state
  (local-storage
    (atom {:owner nil :repo nil :branch "master" :posts-dir nil
           :commit-message "Add a post" :user nil :password nil
           :init? false})
    :settings-state))

(defonce app-state (atom {:screen (if (:init? @settings-state) :composer :settings)}))

;; defaults for a post
(defn post-defaults []
  {:content nil
   :date (time/date->str (time/now))
   :slug nil
   :categories nil})

(defn settings-item [item-name label placeholder]
  [:<>
    [:label {:class "font-semibold inline-block text-left w-3/12"} label]
    [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 my-2 w-9/12"
             :type "text"
             :value (item-name @settings-state)
             :placeholder placeholder
             :on-change #(swap! settings-state assoc item-name (-> % .-target .-value))}]])

(defn settings []
  [:div#settings {:class "bg-white text-right max-w-md mx-auto my-4 p-4 shadow"}
   [:form {:on-submit (fn [x]
                        (.preventDefault x)
                        (swap! settings-state assoc :init? true)
                        (swap! app-state assoc :screen :composer))}
    [:h2 {:class "mb-2 text-center text-xl"} "Settings"]
    [settings-item :owner "Owner:" "Enter the repository owner"]
    [settings-item :repo "Repo:" "Enter the repository name"]
    [settings-item :branch "Branch:" "Enter the repository branch"]
    [settings-item :posts-dir "Directory:" "Enter the posts directory"]
    [settings-item :commit-message "Message:" "Enter the commit message"]
    [settings-item :user "User:" "Enter the GitHub user"]
    [settings-item :password "Token:" "Enter the GitHub access token"]
    [:button {:class "bg-blue-500 hover:bg-blue-700 mx-auto mt-4 px-4 py-2 rounded text-white"
              :type "submit"} "Compose"]]])

(defn composer-input-date [form input-name label placeholder]
  [:<>
    [:label {:class "font-semibold block mt-3 w-2/12"} label]
    [:input {:class "bg-gray-200 block focus:bg-white border border-gray-400 p-2 w-full"
             :type "text"
             :value (input-name @form)
             :placeholder placeholder
             :on-change #(swap! form assoc input-name (-> % .-target .-value))}]])

(defn composer-input-text [form input-name label placeholder]
  [:<>
    [:label {:class "font-semibold block mt-3 w-2/12"} label]
    [:input {:class "bg-gray-200 block focus:bg-white border border-gray-400 p-2 w-full"
             :type "text"
             :value (input-name @form)
             :placeholder placeholder
             :on-change #(swap! form assoc input-name (-> % .-target .-value))}]])

(defn composer []
  (let [post (atom (post-defaults))
        counter (atom max-chars)]
    (fn []
      [:<>
       [:div#composer {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow"}
        [:form {:on-submit (fn [x]
                             (.preventDefault x))}
         [:span#counter {:class "float-right" } @counter]
         [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                     :value (:content @post)
                     :placeholder "What do you want to say?"
                     :on-change (fn [x]
                                  (reset! counter (-> x .-target .-value (markdown/chars-left max-chars)))
                                  (swap! post assoc :content (-> x .-target .-value)))}]
         [composer-input-date post :date "Date" "YYYY-MM-DD HH:MM"]
         [composer-input-text post :slug "Slug" "Enter a slug"]
         [composer-input-text post :categories "Categories" "Enter the categories (optional)"]
         [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-4 px-4 py-2 rounded text-white"
                   :type "button"
                   :on-click #(reset! post (post-defaults))} "Reset"]
         [:button {:class "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-4 px-4 py-2 rounded text-white"
                   :type "submit"} "Post"]]]
       [:footer {:class "text-center"}
        [:a {:class "text-gray-500 underline"
             :href ""
             :on-click (fn [x]
                         (.preventDefault x)
                         (swap! app-state assoc :screen :settings))} "Settings"]]])))

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
