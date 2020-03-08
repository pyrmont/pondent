(ns pondent.pages.settings
  (:require [alandipert.storage-atom :refer [local-storage]]
            [pondent.github :as github]
            [promesa.core :as p]
            [reagent.core :as reagent :refer [atom]]
            [reitit.frontend.easy :as router]))


;; the settings
(defonce settings-state
  (local-storage
    (atom {:owner nil :repo nil :branch "master" :posts-dir nil
           :commit-message "Add a post" :user nil :password nil
           :init? false})
    :settings-state))


(defn settings-item [item-name label placeholder]
  [:<>
    [:label {:class "font-semibold inline-block text-left w-3/12"} label]
    [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 my-2 w-9/12"
             :type "text"
             :value (item-name @settings-state)
             :placeholder placeholder
             :on-change #(swap! settings-state assoc item-name (-> % .-target .-value))}]])


(defn settings-github []
  (let [authd? (some? (:gh-token @settings-state))
        pat? (atom false)]
    (if authd?
      (-> (github/authd? (:gh-token @settings-state))
          (p/then #(if-not % (swap! settings-state dissoc :gh-token)))))
    (fn []
      [:<>
       (if @pat?
         [:<>
          [settings-item :user "User:" "Enter the GitHub user"]
          [settings-item :password "Token:" "Enter the GitHub access token"]]
         [:div {:class "inline-block my-2 w-9/12"}
          (let [colour  (if authd? "bg-green-600" "bg-black")
                url     (if authd? (str "https://github.com/settings/connections/applications/" pondent.core/gh-client-id)
                                  (github/auth-url pondent.core/gh-client-id))
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
                        (router/push-state :pondent.core/composer))}
    [:h2 {:class "mb-2 text-center text-xl"} "Settings"]
    [settings-item :owner "Owner:" "Enter the repository owner"]
    [settings-item :repo "Repo:" "Enter the repository name"]
    [settings-item :branch "Branch:" "Enter the repository branch"]
    [settings-item :posts-dir "Directory:" "Enter the posts directory"]
    [settings-item :commit-message "Message:" "Enter the commit message"]
    [settings-github]
    [:button {:class "bg-blue-500 hover:bg-blue-700 mt-4 px-4 py-2 rounded text-white"
              :type "submit"} "Compose"]]])
