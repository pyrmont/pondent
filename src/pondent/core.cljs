(ns ^:figwheel-hooks pondent.core
  (:require [goog.dom :as gdom]
            [pondent.github :as github]
            [pondent.markdown :as markdown]
            [reagent.core :as reagent :refer [atom]]))

;; the maximum number of characters
(def max-chars 280)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:token nil :screen :login}))
(defonce composer-state (atom {:parent-id nil :text nil}))

(defn login []
  (let [v (atom nil)]
    (fn []
      [:div#login {:class "bg-white justify-center max-w-md mx-auto my-4 p-4 shadow"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (swap! app-state assoc :token @v)
                            (swap! app-state assoc :screen :composer))}
        [:h2 {:class "mb-4 text-center text-xl"} "Almost there..."]
        [:label {:class "font-semibold inline-block w-2/12"} "Token:"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-9/12"
                 :type "text"
                 :value @v
                 :placeholder "Enter app token"
                 :on-change #(reset! v (-> % .-target .-value))}]
        [:button {:class "bg-blue-500 hover:bg-blue-700 block mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Submit"]]])))

(defn composer []
  (let [c (atom max-chars)]
    (fn []
      [:div#composer {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (swap! app-state assoc :screen :timeline))}
        [:span#counter {:class "float-right" } @c]
        [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                    :value (:content @composer-state)
                    :placeholder "What do you want to say?"
                    :on-change (fn [x]
                                 (reset! c (-> x .-target .-value (markdown/chars-left max-chars)))
                                 (swap! composer-state assoc :content (-> x .-target .-value)))}]
        [:label {:class "font-semibold inline-block mt-3 w-2/12"} "Slug"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-full"
                 :type "text"
                 :value (:slug @composer-state)
                 :placeholder "Enter a slug (optional)"
                 :on-change #(swap! composer-state assoc :slug (-> % .-target .-value))}]
        [:label {:class "font-semibold inline-block mt-3 w-2/12"} "Categories"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-full"
                 :type "text"
                 :value (:categories @composer-state)
                 :placeholder "Enter the categories (optional)"
                 :on-change #(swap! composer-state assoc :categories (-> % .-target .-value))}]
        [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "button"
                  :on-click #(swap! app-state assoc :screen :timeline)} "Cancel"]
        [:button {:class "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Post"]]])))

(defn app-container []
  (case (:screen @app-state)
    :login [login]
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
