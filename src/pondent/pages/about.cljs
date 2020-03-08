(ns pondent.pages.about
  (:require [clojure.string :as string]))


(def content
  "
  Pondent is a client-side utility for adding Markdown-formatted files to a
  GitHub repository.

  It's primary use is for adding blog posts to the repository of a Jekyll- or
  Hugo-powered blog. It will add frontmatter that encodes the date of
  publishing, the title (if specified) and the categories (if specified).

  It was built by Michael Camilleri as a simpler way to post to his blog. It is
  free to use and its source code is released to the public domain.
  ")


(defn about-page []
  [:div#about {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow"}
   [:h2 {:class "font-bold mb-4"} "Welcome."]
   (for [para (string/split content "\n\n")]
     [:p {:class "mb-4"} para])])
