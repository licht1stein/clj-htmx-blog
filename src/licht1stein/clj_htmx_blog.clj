(ns licht1stein.clj-htmx-blog
  (:gen-class)
  (:require
   [reitit.ring :as ring]
   [muuntaja.core :as m]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   [reitit.ring.middleware.muuntaja
    :refer
    [format-negotiate-middleware format-request-middleware format-response-middleware]]
   [hiccup.core :refer [html]]
   [hiccup.form :as form]
   [org.httpkit.server :refer [run-server]]))

(defonce server (atom nil))

(defn navi [{:keys [href title]}]
  [:a {:href href} title])

(def navigation [{:href "/"
                  :title "Main Page"}
                 {:href "/about"
                  :title "About Page"}])


(defn render-page [{:keys [title body]}]
  (html [:html {:lang :en}
         [:head
          [:title title]
          [:meta {:charset :UTF-8}]
          [:script {:src "https://unpkg.com/htmx.org@1.6.1"}]]
         (merge [:body
                 (into [:div#navi.navi] (mapv navi navigation))
                 [:div#content.content body]])]))

(comment
  (navi {:href "http://example.com" :title "Example"}))



(defn about-page []
  (render-page
   {:title "About Page"
    :body [:h1 "About"]}))

(def default-posts [{:title "My first post"
                     :date "2021-12-01"
                     :body "First post here!"
                     }
                    {:title "My second post"
                     :date "2021-12-02"
                     :body "Second post here!"}])
(def posts (atom default-posts))

(comment
  (swap! posts conj {:title "Dynamic Post"
                     :date "Now"
                     :body "This was dynamic!"})
  )

(defn blog-post [{:keys [title date body image]}]
  [:div.blog-post
   [:h3.blog-post-title title]
   [:p.blog-post-date date]
   [:p.blog-post-body (when image [:img {:src image}]) body]])

(def new-post [:form
               {:hx-post "/api/posts/new" :hx-target "#blog"}
               (form/text-field "title")
               (form/text-field "body")
               [:button "Submit"]])

(defn main-page []
  (render-page
   {:title "Main Page"
    :body [:div
           [:button {:hx-post "/api/posts/reset" :hx-target "#blog"} "Reset"]
           [:div#blog.blog {:hx-get "/api/posts/all"  :hx-trigger "load"}]
           new-post]}))

(comment
  (html (blog-post {:title "My first post"
                    :body "First post here!"})))

(def req (atom nil))

(comment
  @req)

(def router
  (ring/router
   [["/api"
     ["/posts"
      ["/all" {:get (fn [_]
                      (println ::all-posts)
                      {:status  200
                       :headers {"Content-Type" "text/html"}
                       :body    (html (into [:div] (mapv blog-post @posts)))
                       })}]
      ["/reset" (fn [_]
                  (println ::reset-posts)
                  (reset! posts default-posts)
                  {:status 200
                   :body (html (into [:div] (mapv blog-post @posts)))})]
      ["/new" {:post (fn [r]
                       (let [form (:form-params r)]
                         (println ::new-post form)
                         (reset! req r)
                         (swap! posts conj {:title (form "title") :body (form "body")})
                         {:status 200
                          :body (html (into [:div] (mapv blog-post @posts)))}))}]]
     ["/clicked" {:post {:handler (fn [_]
                                    {:status 200
                                     :body   "<h1>replace me</h1>"})}}]]
    ["/" {:get {:handler (fn [_]
                           (println ::home)
                           {:status  200
                            :headers {"Content-Type" "text/html"}
                            :body    (main-page)})}}]
    [
     "/about" {:get {:handler (fn [_]
                                {:status  200
                                 :headers {"Content-Type" "text/html"}
                                 :body    (about-page)})}}]]
   {:data {:muuntaja   m/instance
           :middleware [format-negotiate-middleware
                        format-response-middleware
                        parameters-middleware
                        format-request-middleware
                        ]}}))

(def app (ring/ring-handler router))

(defn stop-server []
  (when-not (nil? @server)
    (println ::stop-server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and https://http-kit.github.io/migration.html#reload
  (println ::-main)
  (reset! server (run-server #'app {:port 8000})))

(comment
  (-main)
  (stop-server))
