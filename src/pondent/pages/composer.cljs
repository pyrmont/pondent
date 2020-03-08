(ns pondent.pages.composer
  (:require [clojure.string :as string]
            [pondent.markdown :as markdown]
            [pondent.posting :as posting]
            [pondent.pages.settings :as settings]
            [pondent.time :as time]
            [promesa.core :as p]
            [reagent.core :as reagent :refer [atom]]
            [reitit.frontend.easy :as router]))


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


(defonce post-state (atom (post-defaults)))
(defonce result-state (atom nil))


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
                           (-> (posting/create-post @post-state @settings/settings-state)
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
         :href (router/href :pondent.core/settings)} "Settings"]]])
