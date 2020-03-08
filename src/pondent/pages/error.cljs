(ns pondent.pages.error)


(defn error-page [route]
  [:div {:class "bg-white max-w-md mx-auto my-4 shadow"}
   [:h2 {:class "bg-red-500 text-white font-bold px-4 py-2"} "Error"]
   [:p {:class "border border-t-0 border-red-400 rounded-b bg-red-100 px-4 py-3 text-red-700"} "There was an error."]
   (when-let [error (some-> route :data :error)]
     [:pre {:class "mt-3 overflow-x-auto"} (:body error)])])
