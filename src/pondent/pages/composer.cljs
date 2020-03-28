(ns pondent.pages.composer
  (:require [clojure.string :as string]
            [pondent.file :as file]
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


;; Instructions
(def help
  {:title
    "You can enter a title once your post is longer than 290 characters."

   :files
   "References to your attachments can be inserted manually or will be added
   automatically. Removing a file will remove references to it from your post
   but will not delete it from your repository if uploaded."})


;; defaults for a post
(defn post-defaults []
  {:content nil
   :date (time/date->str (time/now))
   :slug nil
   :categories nil})


;; defaults for a form
(defn form-defaults []
  {:disabled? false})


(defonce post-state (atom (post-defaults)))
(defonce result-state (atom nil))
(defonce files-state (atom #{}))
(defonce form-state (atom (form-defaults)))


(defn reset-states! []
  (reset! post-state (post-defaults))
  (reset! result-state nil)
  (reset! files-state #{})
  (reset! form-state (form-defaults)))


(defn disable-form! []
  (swap! form-state assoc :disabled? true))


(defn insert-into-post! [file]
  (let [url (file/url file @settings/settings-state)
        reference (if (file/image? file) (markdown/image-ref url)
                                         (markdown/link-ref url))
        content (-> @post-state :content (str reference))]
    (swap! post-state assoc :content content)))


(defn remove-from-post! [file]
  (let [url (file/url file @settings/settings-state)
        pattern (markdown/ref-pattern url)
        content (some-> @post-state :content (string/replace pattern ""))]
    (swap! post-state assoc :content content)))


(defn upload-files! []
  (let [settings    @settings/settings-state
        upload-fn   (fn [file]
                      (fn [x] (-> (file/read-async file)
                                  (p/then #(posting/create-file % settings))
                                  (p/then #(do (swap! files-state disj file)
                                               (swap! files-state conj (assoc file :upload-result %)))))))
        to-upload?  #(not (posting/uploaded? (:upload-result %)))
        uploads     (->> @files-state
                         (filter to-upload?)
                         (map #(upload-fn %)))]
    (when (not (empty? uploads))
      (apply p/chain (cons (p/resolved true) uploads)))))


(defn upload-post! []
  (let [post        @post-state
        settings    @settings/settings-state
        content     (:content post)
        images-html (posting/loose-images @files-state content settings)
        data        (assoc post :content (str content images-html))]
    (-> (posting/create-post data settings)
        (p/then (fn [x]
                  (if (posting/success? x)
                    (reset-states!)
                    (swap! form-state assoc :disabled? false))
                  (reset! result-state x))))))


(defn composer-input-file [file]
  [:div.file {:class "clearfix"}
   [:span {:class (str "inline-block truncate w-7/12" (case (posting/uploaded? (:upload-result file))
                                                        nil   nil
                                                        true  " check"
                                                        false " error" ))} (file/filename file)]
   [:span {:class "float-right text-right underline w-5/12"}
    [:a {:class "inline-block mr-6 underline"
         :href "#insert"
         :on-click (fn [x]
                     (.preventDefault x)
                     (insert-into-post! file))} "Insert"]
    [:a {:class "inline-block"
         :href "#remove"
         :on-click (fn [x]
                     (.preventDefault x)
                     (remove-from-post! file)
                     (swap! files-state disj file))} (if (posting/uploaded? (:upload-result file))
                                                      "Detach"
                                                      "Remove")]]])


(defn composer-input-files [form input-name label button-text]
  [:<>
    [:label {:class "font-semibold block mb-1"} label]
    (when (not (empty? @files-state))
      [:div#files {:class "mb-3"}
       (for [file @files-state]
         ^{:key (:id file)} [composer-input-file file])
       [:p {:class "mb-1 mt-3 text-gray-500 text-xs"} (:files help)]])
    [:button {:class (str "bg-teal-400 hover:bg-teal-600 mb-3 px-4 py-2 rounded text-white"
                        " disabled:cursor-default disabled:opacity-50")
              :type "button"
              :on-click #(-> % .-target .-nextSibling .click)
              :aria-hidden true} button-text]
    [:input#uploader {:class "hidden"
                      :type "file"
                      :multiple true
                      :on-change (fn [x]
                                   (let [file-objects (-> x .-target .-files array-seq)
                                         files (map (fn [object]
                                                      {:id (random-uuid) :date (time/now) :object object})
                                                    file-objects)]
                                     (swap! files-state into files)
                                     (set! (.-value (-> x .-target)) "")))}]])


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
                          (< chars-left -10))
          disabled? (:disabled? @form-state)]
      [:form {:on-submit (fn [x]
                           (.preventDefault x)
                           (disable-form!)
                           (p/do! (upload-files!)
                                  (upload-post!)))}
       [:fieldset {:disabled disabled?}
        (if show-title?
          [composer-input-text post-state :title "Title" "Enter a title"]
          [:span#counter {:class (str (if danger? "font-semibold text-red-700 ") "float-right mb-1 text-sm")} (str chars-left char-suffix " left")])
        [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                    :value (:content @post-state)
                    :placeholder "What do you want to say?"
                    :on-change #(swap! post-state assoc :content (-> % .-target .-value))}]
        [:p {:class "mb-3 text-gray-500 text-xs"} (:title help)]
        [composer-input-files post-state :image "Attachment" "Browse..." disabled?]
        [composer-input-date post-state :date "Date" "YYYY-MM-DD HH:MM"]
        [composer-input-text post-state :slug "Slug" "Enter a slug"]
        [composer-input-text post-state :categories "Categories" "Enter the categories (optional)"]]
       [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-1 px-4 py-2 rounded text-white"
                 :type "button"
                 :on-click reset-states!} "Reset"]
       [:button {:class (str "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-1 px-4 py-2 rounded text-white"
                             " disabled:opacity-50 disabled:cursor-not-allowed")
                 :disabled disabled?
                 :type "submit"} "Post"]])]])
