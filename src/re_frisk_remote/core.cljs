(ns re-frisk-remote.core
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as timbre]
            [reagent.core :as reagent]
            [re-frame.subs :refer [query->reaction]]
            [re-frame.core :refer [subscribe] :as re-frame]
            [taoensso.sente.packers.transit :as sente-transit]
            [cognitect.transit :as transit])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defonce chsk-send!* (atom {}))
(defonce on-init* (atom nil))
(defonce pre-send* (atom nil))
(defonce id-handler-timer* (atom nil))

(defn post-event-callback [value]
  (@chsk-send!* [:refrisk/events value]))

(defn re-frame-sub [& rest]
  ;; TODO send diff
  (@chsk-send!* [:refrisk/id-handler
                 (into {} (map #(hash-map (first %) @(second %))
                               (remove #(or (= (first (ffirst %)) ::db)
                                            (nil? (second %)))
                                       @query->reaction)))])
  (js/clearInterval @id-handler-timer*)
  (reset! id-handler-timer* nil))

(defn send-app-db []
  ;; TODO send diff
  (@chsk-send!* [:refrisk/app-db (let [db @(subscribe [::db])]
                                   (if @pre-send* (@pre-send* db) db))])
  (when (nil? @id-handler-timer*)
    (reset! id-handler-timer* (js/setInterval re-frame-sub 1000))))

(defn start-socket [host]
  (let [{:keys [send-fn]}
        (sente/make-channel-socket-client!
          "/chsk"
          {:type     :auto
           :host     host
           :protocol :http
           :packer   (sente-transit/get-transit-packer
                       :json
                       {:handlerForForeign (fn [x h] (transit/write-handler (fn [o] "ForeignType")
                                                                            (fn [o] "")))}
                       {})})]
    (reset! chsk-send!* send-fn)))

(defn init []
  (if re-frame.core/reg-sub
    (re-frame.core/reg-sub ::db (fn [db _] db))
    (re-frame.core/register-sub ::db (fn [db _] (reaction @db))))
  (reagent/track! send-app-db)
  (re-frame/add-post-event-callback post-event-callback)
  (when @on-init* (@on-init*)))

(defn enable-re-frisk-remote! [& [{:keys [host pre-send on-init] :as opts}]]
  (timbre/merge-config! {:ns-blacklist ["taoensso.sente" "taoensso.sente.*"]})
  (reset! pre-send* pre-send)
  (reset! on-init* on-init)
  (start-socket (or host "localhost:4567"))
  (js/setTimeout init 2000))