(ns re-frisk-remote.core
  (:require [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [re-frame.subs :refer [query->reaction]]
            [re-frame.trace :as trace :include-macros true]
            [re-frame.core :refer [subscribe] :as re-frame]
            [re-frisk.diff :as diff]
            [re-frisk.delta :as delta]
            [taoensso.sente.packers.transit :as sente-transit]
            [cognitect.transit :as transit]
            [taoensso.timbre :as timbre])
  (:require-macros [reagent.ratom :refer [reaction]]))

;; either nil (do not send), or a map with the following optional keys:
;; :prev-app-db -- app DB last time :refrisk/app-db was sent
;; :prev-event-app-db -- app DB last time :refrisk/events was sent
;; :event-time -- timestamp of last :refrisk/pre-events
;; :prev-id-handlers -- id handlers last time :refrisk/id-handler was sent
(defonce send-state (atom nil))

(defonce ch-chsk (atom {}))

(defonce re-frisk-enabled? true)
(defonce re-frame-10x-enabled? false)

(defonce chsk-send!* (atom {}))
(defonce on-init* (atom nil))
(defonce pre-send* (atom nil))
(defonce id-handler-timer* (atom nil))

(defn- send [message]
  (when message
    (@chsk-send!* message)))

(defn pre-event-callback [value]
  (when (and @send-state re-frisk-enabled?)
    (swap! send-state assoc :event-time (js/Date.now))
    (send [:refrisk/pre-events value])))

(defn post-event-callback [value]
  (when @send-state
    (let [app-db @(subscribe [::db])
          prev-event-app-db (:prev-event-app-db @send-state)
          event-data (if (:event-time @send-state)
                       {:time (- (js/Date.now) (:event-time @send-state))}
                       {:event value})
          app-db-diff (diff/diff prev-event-app-db app-db)
          payload (conj {:app-db-diff app-db-diff}
                        event-data)]
      (swap! send-state assoc :prev-event-app-db app-db :event-time nil)
      (send [:refrisk/events payload]))))

(defn register-trace-cb []
  (trace/register-trace-cb ::cb (fn [traces] (@chsk-send!* [:trace/log traces]))))

(defn id-handlers []
  (into {} (map #(hash-map (first %) @(second %))
                (remove #(or (= (first (ffirst %)) ::db)
                             (nil? (second %)))
                        @query->reaction))))

(defn id-handlers-msg []
  (let [ih-prev (:prev-id-handlers @send-state)
        ih (id-handlers)]
    (swap! send-state assoc :prev-id-handlers ih)
    (if ih-prev
      (when-let [d (delta/delta ih-prev ih)]
        [:refrisk/id-handler-delta d])
      [:refrisk/id-handler ih])))

(defn re-frame-sub [& rest]
  (when @send-state
    (send (id-handlers-msg)))
  (js/clearInterval @id-handler-timer*)
  (reset! id-handler-timer* nil))

(defn- get-db []
  (let [db @(subscribe [::db])]
    (if @pre-send* (@pre-send* db) db)))

(defn- app-db-msg [db]
  (let [db-prev (:prev-app-db @send-state)]
    (swap! send-state assoc :prev-app-db db)
    (if db-prev
      (when-let [d (delta/delta db-prev db)]
        [:refrisk/app-db-delta d])
      [:refrisk/app-db db])))

(defn send-app-db []
  (let [db (get-db)]
    (when @send-state
      (send (app-db-msg db)))
    (when (nil? @id-handler-timer*)
      (reset! id-handler-timer* (js/setInterval re-frame-sub 1000)))))

(defn start-socket [host]
  (let [{:keys [send-fn ch-recv]}
        (sente/make-channel-socket-client!
          "/chsk"
          nil
          {:type     :auto
           :host     host
           :protocol :http
           :params   {:kind :re-frisk-remote}
           :packer   (sente-transit/get-transit-packer
                       :json
                       {:handlerForForeign (fn [x h] (transit/write-handler (fn [o] "ForeignType")
                                                                            (fn [o] "")))}
                       {})})]
    (reset! ch-chsk ch-recv)
    (reset! chsk-send!* send-fn)))

(defmulti event-msg-handler "Sente `event-msg`s handler" :id)

(defmethod event-msg-handler :chsk/state
  [{[{was-open? :open?} {now-open? :open?}] :?data :as msg}]
  (if (not= was-open? now-open?)
    (reset! send-state nil)))

(defmethod event-msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (case (first ?data)
    :refrisk/enable
    (do
      (reset! send-state {})
      (reagent/track! send-app-db))
    :refrisk/disable
    (reset! send-state nil)))

(defmethod event-msg-handler :default [msg]
  nil)

(defonce router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
            @ch-chsk event-msg-handler)))

(defn init []
  (start-router!)
  (when re-frisk-enabled?
    (if re-frame.core/reg-sub
      (re-frame.core/reg-sub ::db (fn [db _] db))
      (re-frame.core/register-sub ::db (fn [db _] (reaction @db))))
    (re-frame/add-post-event-callback post-event-callback))
  (when re-frame-10x-enabled?
    (register-trace-cb))
  (when @on-init* (@on-init*)))

(defn enable-re-frisk-remote! [& [{:keys [host pre-send on-init enable-re-frisk? enable-re-frame-10x?]
                                   :or {enable-re-frisk? true enable-re-frame-10x? false}}]]
  (timbre/merge-config! {:ns-blacklist ["taoensso.sente" "taoensso.sente.*"]})
  (reset! pre-send* pre-send)
  (reset! on-init* on-init)
  (set! re-frisk-enabled? enable-re-frisk?)
  (set! re-frame-10x-enabled? enable-re-frame-10x?)
  (start-socket (or host "localhost:4567"))
  (init))