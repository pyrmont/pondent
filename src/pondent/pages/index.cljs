(ns pondent.pages.index
  (:require [pondent.pages.settings :as settings]
            [reitit.frontend.easy :as router]))


(defn index-page [route]
  (router/replace-state
    (if (:init? @settings/settings-state) :pondent.core/composer :pondent.core/settings)))
