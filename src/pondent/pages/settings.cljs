(ns pondent.pages.settings
  (:require [alandipert.storage-atom :refer [local-storage]]
            [clojure.string :as string]
            [pondent.github :as github]
            [promesa.core :as p]
            [reagent.core :as reagent :refer [atom]]
            [reitit.frontend.easy :as router]))


;; defaults for the settings
(def settings-defaults
  {:owner nil :repo nil :branch "master" :posts-dir nil :uploads-dir nil
   :uploads-url nil :commit-message "Add a post" :user nil :password nil
   :init? false})


;; the settings
(defonce settings-state
  (local-storage (atom settings-defaults) :settings-state))


(defn update-gh-token! [oauth-token?]
  (if oauth-token?
    (-> (github/authd? (:gh-token @settings-state))
        (p/then #(if-not % (swap! settings-state dissoc :gh-token))))))


(defn settings-item [item-name label placeholder]
  [:<>
    [:label {:class "font-semibold inline-block text-left w-3/12"} label]
    [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 my-2 w-9/12"
             :type "text"
             :value (item-name @settings-state)
             :placeholder placeholder
             :on-change #(swap! settings-state assoc item-name (-> % .-target .-value))}]])


(defn settings-github []
  (let [oauth-token? (not (string/blank? (:gh-token @settings-state)))
        personal-token? (atom (not (string/blank? (:gh-user @settings-state))))]
    (update-gh-token! oauth-token?)
    (fn []
      [:<>
       (if @personal-token?
         [:<>
          [settings-item :gh-user "User:" "Enter the GitHub user"]
          [settings-item :gh-password "Token:" "Enter the GitHub access token"]]
         [:div {:class "inline-block my-2 w-9/12"}
          (let [colour  (if oauth-token? "bg-green-600" "bg-black")
                url     (if oauth-token? (github/app-url pondent.core/gh-client-id)
                                         (github/auth-url pondent.core/gh-client-id))
                message (if oauth-token? "Authorised with GitHub"
                                         "Authorise with GitHub")]
            [:a#authd {:class (str colour " border-gray-300 lbtn lbtn-github")
                       :href url}
              [:i {:class "logo"}]
              [:p {:class "label"} message]])])
       [:div {:class "clearfix"}
        [:div {:class "inline-block my-2 text-left w-9/12"}
         [:input#pat {:class "mr-2"
                      :type "checkbox"
                      :checked @personal-token?
                      :on-change #(swap! personal-token? not)}]
         [:label {:for "pat"} "Use personal access token"]]]])))


(defn settings-page [route]
  [:div#settings {:class "bg-white text-right max-w-md mx-auto my-4 p-4 shadow"}
   [:form {:on-submit (fn [x]
                        (.preventDefault x)
                        (swap! settings-state assoc :init? true)
                        (router/push-state :pondent.core/composer))}
    [:h2 {:class "mb-2 text-center text-xl"} "Settings"]
    [settings-item :owner "Owner:" "Enter the repository owner"]
    [settings-item :repo "Repo:" "Enter the repository name"]
    [settings-item :branch "Branch:" "Enter the repository branch"]
    [settings-item :posts-dir "Posts:" "Enter the posts directory"]
    [settings-item :uploads-dir "Uploads:" "Enter the uploads directory"]
    [settings-item :uploads-url "Uploads URL:" "Enter the uploads URL"]
    [settings-item :commit-message "Message:" "Enter the commit message"]
    [settings-github]
    [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-4 px-4 py-2 rounded text-white"
              :type "button"
              :on-click (fn [x]
                          (reset! settings-state settings-defaults))} "Reset"]
    [:button {:class "bg-blue-500 hover:bg-blue-700 mt-4 px-4 py-2 rounded text-white"
              :type "submit"} "Compose"]]])
