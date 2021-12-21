(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]))

(set-refresh-dirs "src" "dev" "test")

(comment
  (refresh))
