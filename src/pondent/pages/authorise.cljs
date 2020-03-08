(ns pondent.pages.authorise
  (:require [pondent.github :as github]
            [pondent.pages.settings :as settings]
            [promesa.core :as p]
            [reitit.frontend.easy :as router]))


(defn authorise-page [route]
  (let [query (-> route :parameters :query)
        code (:code query)]
    (-> (github/auth-token-via-proxy pondent.core/gh-auth-proxy code)
        (p/then (fn [x]
                  (if-let [token (:token x)]
                    (do (swap! settings/settings-state assoc :gh-token token)
                        (router/replace-state :pondent.core/settings))
                    (router/replace-state :pondent.core/error)))))
    [:div {:class "bg-white max-w-md mx-auto my-4 p-4 shadow text-center"} "Authorising..."]))
